package com.xu.home.service.medical.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xu.home.config.blog.DeepSeekProperties;
import com.xu.home.config.blog.MedicalKnowledgeProperties;
import com.xu.home.domain.medical.MedicalKnowledgeChunk;
import com.xu.home.domain.medical.MedicalKnowledgeItem;
import com.xu.home.domain.medical.MedicalKnowledgeItemRef;
import com.xu.home.domain.medical.MedicalKnowledgeSource;
import com.xu.home.domain.medical.MedicalKnowledgeTask;
import com.xu.home.mapper.medical.MedicalKnowledgeChunkMapper;
import com.xu.home.mapper.medical.MedicalKnowledgeItemMapper;
import com.xu.home.mapper.medical.MedicalKnowledgeItemRefMapper;
import com.xu.home.mapper.medical.MedicalKnowledgeSourceMapper;
import com.xu.home.mapper.medical.MedicalKnowledgeTaskMapper;
import com.xu.home.param.medical.po.MedicalKnowledgeItemQueryPO;
import com.xu.home.param.medical.vo.MedicalKnowledgeEvidenceVO;
import com.xu.home.param.medical.vo.MedicalKnowledgeImportResultVO;
import com.xu.home.param.medical.vo.MedicalKnowledgeItemDetailVO;
import com.xu.home.param.medical.vo.MedicalKnowledgePageVO;
import com.xu.home.param.medical.vo.MedicalKnowledgeSourceDetailVO;
import com.xu.home.service.ai.DeepSeekAssistant;
import com.xu.home.service.medical.MedicalKnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@Slf4j
public class MedicalKnowledgeServiceImpl implements MedicalKnowledgeService {

    private static final ExecutorService TASK_EXECUTOR = Executors.newCachedThreadPool();
    private static final ExecutorService AI_EXECUTOR = Executors.newCachedThreadPool();
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_RUNNING = 2;
    private static final int STATUS_SUCCESS = 3;
    private static final int STATUS_FAILED = 4;
    private static final int SOURCE_TYPE_LOCAL = 1;
    private static final int SOURCE_TYPE_UPLOAD = 2;
    private static final int TASK_TYPE_IMPORT = 1;
    private static final int TASK_TYPE_REEXTRACT = 2;
    private static final int REVIEW_STATUS_DEFAULT = 0;
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(第[一二三四五六七八九十百零0-9]+[章节篇部分].*|[一二三四五六七八九十]+[、.].*|\\d+[、.].*)$");
    private static final Pattern PAGE_NUMBER_PATTERN = Pattern.compile("^\\s*[—\\-]*\\s*\\d+\\s*[—\\-]*\\s*$");
    private static final Pattern BLANK_LINE_PATTERN = Pattern.compile("\\n{3,}");
    private static final Pattern DIRECTORY_LINE_PATTERN = Pattern.compile("^(第?[一二三四五六七八九十百零0-9]+[章节篇部分卷].*?)(\\.{2,}|·{2,}|…{2,}|\\s{2,})\\s*\\d+\\s*$");
    private static final Pattern SIMPLE_DIRECTORY_LINE_PATTERN = Pattern.compile("^.*\\s+\\d{1,4}\\s*$");
    private static final DateTimeFormatter FILE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> SKIP_SECTION_TITLES = Set.of(
            "目录", "目 录", "前言", "序言", "序", "绪论", "引言", "导言", "作者简介", "内容简介", "出版说明", "再版前言"
    );

    private final MedicalKnowledgeSourceMapper sourceMapper;
    private final MedicalKnowledgeTaskMapper taskMapper;
    private final MedicalKnowledgeChunkMapper chunkMapper;
    private final MedicalKnowledgeItemMapper itemMapper;
    private final MedicalKnowledgeItemRefMapper itemRefMapper;
    private final MedicalKnowledgeProperties properties;
    private final DeepSeekAssistant deepSeekAssistant;
    private final DeepSeekProperties deepSeekProperties;

    public MedicalKnowledgeServiceImpl(MedicalKnowledgeSourceMapper sourceMapper,
                                       MedicalKnowledgeTaskMapper taskMapper,
                                       MedicalKnowledgeChunkMapper chunkMapper,
                                       MedicalKnowledgeItemMapper itemMapper,
                                       MedicalKnowledgeItemRefMapper itemRefMapper,
                                       MedicalKnowledgeProperties properties,
                                       DeepSeekAssistant deepSeekAssistant,
                                       DeepSeekProperties deepSeekProperties) {
        this.sourceMapper = sourceMapper;
        this.taskMapper = taskMapper;
        this.chunkMapper = chunkMapper;
        this.itemMapper = itemMapper;
        this.itemRefMapper = itemRefMapper;
        this.properties = properties;
        this.deepSeekAssistant = deepSeekAssistant;
        this.deepSeekProperties = deepSeekProperties;
    }

    @Override
    public MedicalKnowledgeImportResultVO importFromLocal(String account, String localPath) {
        if (StringUtils.isBlank(localPath)) {
            throw new RuntimeException("本地路径不能为空");
        }
        Path rootPath = resolveAndValidateLocalPath(localPath);
        List<Path> targetFiles = collectSupportedFiles(rootPath);
        if (CollectionUtils.isEmpty(targetFiles)) {
            throw new RuntimeException("指定路径下未找到可导入的 pdf/txt/docx 文件");
        }
        MedicalKnowledgeImportResultVO result = new MedicalKnowledgeImportResultVO();
        result.setTotalFiles(targetFiles.size());
        for (Path file : targetFiles) {
            MedicalKnowledgeSource source = createSource(account, file.getFileName().toString(), SOURCE_TYPE_LOCAL,
                    detectFileFormat(file), safeFileSize(file), file.toString(), file.toString(), calculateFileHash(file));
            MedicalKnowledgeTask task = createTask(source.getId(), TASK_TYPE_IMPORT);
            result.getSourceIds().add(source.getId());
            result.getSourceNames().add(source.getSourceName());
            startAsyncTask(account, source.getId(), task.getId(), false);
        }
        return result;
    }

    @Override
    public MedicalKnowledgeImportResultVO importFromUpload(String account, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (StringUtils.isBlank(originalFilename)) {
            throw new RuntimeException("上传文件名不能为空");
        }
        String format = detectFileFormat(originalFilename);
        if (!isSupportedFormat(format)) {
            throw new RuntimeException("仅支持 pdf、txt、docx 文件");
        }
        try {
            Path uploadDir = properties.resolveUploadDir();
            Files.createDirectories(uploadDir);
            String fileName = FILE_TIME_FORMATTER.format(LocalDateTime.now()) + "_" + sanitizeFileName(originalFilename);
            Path target = uploadDir.resolve(fileName).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            MedicalKnowledgeSource source = createSource(account, originalFilename, SOURCE_TYPE_UPLOAD, format,
                    file.getSize(), null, target.toString(), calculateFileHash(target));
            MedicalKnowledgeTask task = createTask(source.getId(), TASK_TYPE_IMPORT);
            MedicalKnowledgeImportResultVO result = new MedicalKnowledgeImportResultVO();
            result.setTotalFiles(1);
            result.getSourceIds().add(source.getId());
            result.getSourceNames().add(source.getSourceName());
            startAsyncTask(account, source.getId(), task.getId(), false);
            return result;
        } catch (IOException ex) {
            throw new RuntimeException("上传文件保存失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void reextract(String account, Long sourceId) {
        MedicalKnowledgeSource source = getOwnedSource(account, sourceId);
        MedicalKnowledgeTask task = createTask(sourceId, TASK_TYPE_REEXTRACT);
        resetSourceExtractionData(sourceId);
        source.setParseStatus(STATUS_PENDING);
        source.setExtractStatus(STATUS_PENDING);
        source.setChapterCount(0);
        source.setChunkCount(0);
        source.setKnowledgeCount(0);
        source.setErrorMessage(null);
        source.setUpdateTime(LocalDateTime.now());
        sourceMapper.updateById(source);
        startAsyncTask(account, sourceId, task.getId(), true);
    }

    @Override
    public List<MedicalKnowledgeSource> getSourceList(String account) {
        LambdaQueryWrapper<MedicalKnowledgeSource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalKnowledgeSource::getAccount, account)
                .eq(MedicalKnowledgeSource::getIsDelete, 0)
                .orderByDesc(MedicalKnowledgeSource::getUpdateTime)
                .orderByDesc(MedicalKnowledgeSource::getId);
        return sourceMapper.selectList(wrapper);
    }

    @Override
    public MedicalKnowledgeSourceDetailVO getSourceDetail(String account, Long sourceId) {
        MedicalKnowledgeSource source = getOwnedSource(account, sourceId);
        MedicalKnowledgeSourceDetailVO vo = new MedicalKnowledgeSourceDetailVO();
        vo.setSource(source);

        LambdaQueryWrapper<MedicalKnowledgeTask> taskWrapper = new LambdaQueryWrapper<>();
        taskWrapper.eq(MedicalKnowledgeTask::getSourceId, sourceId)
                .orderByDesc(MedicalKnowledgeTask::getCreateTime)
                .orderByDesc(MedicalKnowledgeTask::getId);
        vo.setTasks(taskMapper.selectList(taskWrapper));

        LambdaQueryWrapper<MedicalKnowledgeItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MedicalKnowledgeItem::getSourceId, sourceId)
                .eq(MedicalKnowledgeItem::getIsDelete, 0)
                .orderByDesc(MedicalKnowledgeItem::getUpdateTime)
                .last("limit 2");
        vo.setLatestItems(itemMapper.selectList(itemWrapper));
        return vo;
    }

    /**
     * 读取当前账号下的知识类型枚举值，供前端筛选下拉使用。
     */
    @Override
    public List<String> getItemTypes(String account, Long sourceId) {
        LambdaQueryWrapper<MedicalKnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(MedicalKnowledgeItem::getItemType)
                .eq(MedicalKnowledgeItem::getIsDelete, 0)
                .inSql(MedicalKnowledgeItem::getSourceId, ownedSourceSubQuery(account));
        if (sourceId != null) {
            wrapper.eq(MedicalKnowledgeItem::getSourceId, sourceId);
        }
        wrapper.isNotNull(MedicalKnowledgeItem::getItemType)
                .groupBy(MedicalKnowledgeItem::getItemType)
                .orderByAsc(MedicalKnowledgeItem::getItemType);
        return itemMapper.selectList(wrapper).stream()
                .map(MedicalKnowledgeItem::getItemType)
                .filter(StringUtils::isNotBlank)
                .toList();
    }

    @Override
    public MedicalKnowledgePageVO<MedicalKnowledgeItem> getItemPage(String account, MedicalKnowledgeItemQueryPO po) {
        Page<MedicalKnowledgeItem> page = new Page<>(Math.max(po.getPageNum(), 1), Math.max(po.getPageSize(), 1));
        LambdaQueryWrapper<MedicalKnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalKnowledgeItem::getIsDelete, 0)
                .inSql(MedicalKnowledgeItem::getSourceId, ownedSourceSubQuery(account));
        if (po.getSourceId() != null) {
            wrapper.eq(MedicalKnowledgeItem::getSourceId, po.getSourceId());
        }
        if (StringUtils.isNotBlank(po.getItemType())) {
            wrapper.eq(MedicalKnowledgeItem::getItemType, po.getItemType().trim());
        }
        if (StringUtils.isNotBlank(po.getDepartment())) {
            wrapper.eq(MedicalKnowledgeItem::getDepartment, po.getDepartment().trim());
        }
        if (StringUtils.isNotBlank(po.getKeyword())) {
            String keyword = po.getKeyword().trim();
            wrapper.and(w -> w.like(MedicalKnowledgeItem::getTitle, keyword)
                    .or().like(MedicalKnowledgeItem::getKeywords, keyword)
                    .or().like(MedicalKnowledgeItem::getSummary, keyword)
                    .or().like(MedicalKnowledgeItem::getContent, keyword));
        }
        wrapper.orderByDesc(MedicalKnowledgeItem::getUpdateTime)
                .orderByDesc(MedicalKnowledgeItem::getId);
        Page<MedicalKnowledgeItem> ret = itemMapper.selectPage(page, wrapper);
        MedicalKnowledgePageVO<MedicalKnowledgeItem> vo = new MedicalKnowledgePageVO<>();
        vo.setTotal(ret.getTotal());
        vo.setList(ret.getRecords());
        return vo;
    }

    @Override
    public MedicalKnowledgeItemDetailVO getItemDetail(String account, Long itemId) {
        MedicalKnowledgeItem item = getOwnedItem(account, itemId);
        MedicalKnowledgeSource source = getOwnedSource(account, item.getSourceId());
        MedicalKnowledgeItemDetailVO vo = new MedicalKnowledgeItemDetailVO();
        vo.setItem(item);
        vo.setSource(source);

        LambdaQueryWrapper<MedicalKnowledgeItemRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(MedicalKnowledgeItemRef::getKnowledgeItemId, itemId)
                .orderByAsc(MedicalKnowledgeItemRef::getSortOrder)
                .orderByAsc(MedicalKnowledgeItemRef::getId);
        List<MedicalKnowledgeItemRef> refs = itemRefMapper.selectList(refWrapper);
        for (MedicalKnowledgeItemRef ref : refs) {
            MedicalKnowledgeChunk chunk = chunkMapper.selectById(ref.getChunkId());
            if (chunk == null) {
                continue;
            }
            MedicalKnowledgeEvidenceVO evidence = new MedicalKnowledgeEvidenceVO();
            evidence.setRefId(ref.getId());
            evidence.setChunkId(chunk.getId());
            evidence.setChapterNo(chunk.getChapterNo());
            evidence.setChapterTitle(chunk.getChapterTitle());
            evidence.setPageFrom(chunk.getPageFrom());
            evidence.setPageTo(chunk.getPageTo());
            evidence.setQuoteText(ref.getQuoteText());
            evidence.setCleanContent(chunk.getCleanContent());
            evidence.setSortOrder(ref.getSortOrder());
            vo.getReferences().add(evidence);
        }
        return vo;
    }

    @Override
    public boolean deleteItem(String account, Long itemId) {
        MedicalKnowledgeItem item = getOwnedItem(account, itemId);
        MedicalKnowledgeSource source = getOwnedSource(account, item.getSourceId());
        item.setIsDelete(1);
        item.setUpdateTime(LocalDateTime.now());
        int updated = itemMapper.updateById(item);
        refreshKnowledgeCount(source.getId(), source.getAccount());
        return updated > 0;
    }

    /**
     * 将来源处理任务提交到异步线程池，避免导入请求长时间阻塞。
     */
    private void startAsyncTask(String account, Long sourceId, Long taskId, boolean clearBeforeRun) {
        CompletableFuture.runAsync(() -> processSource(account, sourceId, taskId, clearBeforeRun), TASK_EXECUTOR);
    }

    /**
     * 执行单本书籍的完整处理流程：解析、清洗、章节归并、AI 提取、结果入库。
     */
    private void processSource(String account, Long sourceId, Long taskId, boolean clearBeforeRun) {
        MedicalKnowledgeSource source = getOwnedSource(account, sourceId);
        MedicalKnowledgeTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return;
        }
        log.info("医疗知识库任务开始, sourceId={}, taskId={}, sourceName={}, fileFormat={}",
                sourceId, taskId, source.getSourceName(), source.getFileFormat());
        updateTaskStatus(task, STATUS_RUNNING, "开始解析文件");
        updateSourceStatus(source, STATUS_RUNNING, STATUS_PENDING, null);
        try {
            if (clearBeforeRun) {
                resetSourceExtractionData(sourceId);
            }
            Path filePath = PathsHolder.resolve(source.getStoragePath());
            ParsedDocument document = parseDocument(filePath, source.getSourceName(), source.getFileFormat());
            List<MedicalKnowledgeChunk> chunks = buildChunks(sourceId, document);
            persistChunks(sourceId, chunks);
            log.info("医疗知识库文本解析完成, sourceId={}, taskId={}, chapterCount={}, chunkCount={}",
                    sourceId, taskId, document.chapterCount(), chunks.size());
            source = getOwnedSource(account, sourceId);
            source.setChapterCount(document.chapterCount());
            source.setChunkCount(chunks.size());
            source.setParseStatus(STATUS_SUCCESS);
            source.setExtractStatus(STATUS_RUNNING);
            source.setErrorMessage(null);
            source.setUpdateTime(LocalDateTime.now());
            sourceMapper.updateById(source);
            updateTaskStatus(task, STATUS_RUNNING, "文本解析完成，开始 AI 提取");

            int knowledgeCount = extractKnowledgeItems(source, task, chunks);
            source = getOwnedSource(account, sourceId);
            source.setKnowledgeCount(knowledgeCount);
            source.setExtractStatus(STATUS_SUCCESS);
            source.setUpdateTime(LocalDateTime.now());
            sourceMapper.updateById(source);
            updateTaskStatus(task, STATUS_SUCCESS, "提取完成，生成 " + knowledgeCount + " 条知识");
            log.info("医疗知识库任务完成, sourceId={}, taskId={}, knowledgeCount={}", sourceId, taskId, knowledgeCount);
        } catch (Exception ex) {
            log.error("医疗知识库处理失败, sourceId={}, taskId={}", sourceId, taskId, ex);
            updateTaskStatus(task, STATUS_FAILED, safeMessage(ex));
            updateSourceStatus(source, STATUS_FAILED, STATUS_FAILED, safeMessage(ex));
        }
    }

    /**
     * 按文件格式路由到不同解析器，统一产出章节级文档结构。
     */
    private ParsedDocument parseDocument(Path path, String sourceName, String fileFormat) throws IOException {
        String format = StringUtils.defaultIfBlank(fileFormat, detectFileFormat(path));
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "pdf" -> parsePdf(path, sourceName);
            case "txt" -> parseTxt(path, sourceName);
            case "docx" -> parseDocx(path, sourceName);
            default -> throw new RuntimeException("暂不支持文件格式: " + format);
        };
    }

    /**
     * 逐页读取 PDF，并在后续阶段按章节标题进行归并。
     */
    private ParsedDocument parsePdf(Path path, String sourceName) throws IOException {
        List<ParsedSegment> pageSegments = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = cleanRawText(stripper.getText(document));
                if (StringUtils.isBlank(pageText)) {
                    continue;
                }
                String title = findHeadingFromText(pageText);
                String content = removeLeadingHeading(pageText, title);
                pageSegments.add(new ParsedSegment(title, content, page, page));
            }
            return new ParsedDocument(resolveSourceTitle(sourceName), normalizeChapterSegments(resolveSourceTitle(sourceName), pageSegments, true));
        }
    }

    /**
     * 读取 TXT 全文并按标题初步切分章节。
     */
    private ParsedDocument parseTxt(Path path, String sourceName) throws IOException {
        String content = readTextWithFallback(path);
        List<ParsedSegment> segments = splitPlainTextSegments(content);
        return new ParsedDocument(resolveSourceTitle(sourceName), normalizeChapterSegments(resolveSourceTitle(sourceName), segments, false));
    }

    /**
     * 读取 DOCX 段落内容并按标题段落聚合章节。
     */
    private ParsedDocument parseDocx(Path path, String sourceName) throws IOException {
        List<ParsedSegment> segments = new ArrayList<>();
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder current = new StringBuilder();
            String currentTitle = resolveSourceTitle(sourceName);
            int chapterNo = 1;
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = cleanInlineText(paragraph.getText());
                if (StringUtils.isBlank(text)) {
                    continue;
                }
                if (isHeading(text) && current.length() > 0) {
                    segments.add(new ParsedSegment(currentTitle, current.toString(), chapterNo, chapterNo));
                    current = new StringBuilder();
                    currentTitle = text;
                    chapterNo++;
                    continue;
                }
                if (isHeading(text) && current.length() == 0) {
                    currentTitle = text;
                    continue;
                }
                current.append(text).append('\n');
            }
            if (current.length() > 0) {
                segments.add(new ParsedSegment(currentTitle, current.toString(), chapterNo, chapterNo));
            }
        }
        return new ParsedDocument(resolveSourceTitle(sourceName), normalizeChapterSegments(resolveSourceTitle(sourceName), segments, false));
    }

    /**
     * 基于纯文本中的标题特征做初步章节切分。
     */
    private List<ParsedSegment> splitPlainTextSegments(String content) {
        List<ParsedSegment> segments = new ArrayList<>();
        String[] lines = cleanRawText(content).split("\\n");
        String currentTitle = null;
        StringBuilder currentContent = new StringBuilder();
        int chapterNo = 1;
        for (String line : lines) {
            String normalized = cleanInlineText(line);
            if (StringUtils.isBlank(normalized)) {
                continue;
            }
            if (isHeading(normalized) && currentContent.length() > 0) {
                segments.add(new ParsedSegment(currentTitle, currentContent.toString(), chapterNo, chapterNo));
                chapterNo++;
                currentTitle = normalized;
                currentContent = new StringBuilder();
                continue;
            }
            if (isHeading(normalized) && currentContent.length() == 0) {
                currentTitle = normalized;
                continue;
            }
            currentContent.append(normalized).append('\n');
        }
        if (currentContent.length() > 0) {
            segments.add(new ParsedSegment(currentTitle, currentContent.toString(), chapterNo, chapterNo));
        }
        if (segments.isEmpty() && StringUtils.isNotBlank(content)) {
            segments.add(new ParsedSegment(null, cleanRawText(content), 1, 1));
        }
        return segments;
    }

    /**
     * 将标准化后的章节内容转换为数据库中的章节块记录。
     */
    private List<MedicalKnowledgeChunk> buildChunks(Long sourceId, ParsedDocument document) {
        List<MedicalKnowledgeChunk> chunks = new ArrayList<>();
        int chunkNo = 1;
        int chapterNo = 1;
        for (ParsedSegment segment : document.segments()) {
            String cleaned = cleanChapterContent(segment.content());
            if (StringUtils.isBlank(cleaned)) {
                continue;
            }
            MedicalKnowledgeChunk chunk = new MedicalKnowledgeChunk();
            chunk.setSourceId(sourceId);
            chunk.setChapterNo(chapterNo);
            chunk.setChapterTitle(StringUtils.defaultIfBlank(segment.title(), "第" + chapterNo + "章"));
            chunk.setPageFrom(segment.pageFrom());
            chunk.setPageTo(segment.pageTo());
            chunk.setChunkNo(chunkNo++);
            chunk.setRawContent(segment.content());
            chunk.setCleanContent(cleaned);
            chunk.setContentHash(sha256Hex(cleaned));
            chunk.setTokenEstimate(Math.max(cleaned.length() / 2, 1));
            chunk.setCreateTime(LocalDateTime.now());
            chunk.setUpdateTime(LocalDateTime.now());
            chunks.add(chunk);
            chapterNo++;
        }
        return chunks;
    }

    /**
     * 重建某本书的章节块数据，确保重新提取时不会混入旧记录。
     */
    private void persistChunks(Long sourceId, List<MedicalKnowledgeChunk> chunks) {
        LambdaQueryWrapper<MedicalKnowledgeChunk> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(MedicalKnowledgeChunk::getSourceId, sourceId);
        chunkMapper.delete(deleteWrapper);
        for (MedicalKnowledgeChunk chunk : chunks) {
            chunkMapper.insert(chunk);
        }
    }

    /**
     * 按章节遍历提取知识条目，并在提取过程中增量写入结果。
     */
    private int extractKnowledgeItems(MedicalKnowledgeSource source, MedicalKnowledgeTask task, List<MedicalKnowledgeChunk> chunks) {
        markExistingItemsDeleted(source.getId());
        Map<String, MedicalKnowledgeItem> itemMap = new LinkedHashMap<>();
        int totalChunks = chunks.size();
        int progressInterval = Math.max(properties.getProgressUpdateInterval(), 1);
        for (int index = 0; index < chunks.size(); index++) {
            MedicalKnowledgeChunk chunk = chunks.get(index);
            int current = index + 1;
            log.info("医疗知识库开始提取 chunk, sourceId={}, taskId={}, progress={}/{}, chunkId={}, chunkNo={}, chapterTitle={}",
                    source.getId(), task.getId(), current, totalChunks, chunk.getId(), chunk.getChunkNo(), chunk.getChapterTitle());
            List<ExtractedKnowledge> extracted = extractFromChunk(source, chunk, current, totalChunks);
            for (ExtractedKnowledge knowledge : extracted) {
                if (StringUtils.isBlank(knowledge.title()) || StringUtils.isBlank(knowledge.content())) {
                    continue;
                }
                String dedupKey = buildDedupKey(source.getId(), knowledge.title(), knowledge.itemType(), knowledge.department());
                MedicalKnowledgeItem item = itemMap.get(dedupKey);
                if (item == null) {
                    item = buildKnowledgeItem(source.getId(), knowledge, dedupKey);
                    itemMap.put(dedupKey, item);
                    itemMapper.insert(item);
                } else if (item.getId() == null) {
                    itemMapper.insert(item);
                } else {
                    mergeKnowledgeItem(item, knowledge);
                    itemMapper.updateById(item);
                }
                ensureItemReference(item.getId(), chunk);
            }
            log.info("医疗知识库完成提取 chunk, sourceId={}, taskId={}, progress={}/{}, chunkId={}, extractedCount={}, totalKnowledge={}",
                    source.getId(), task.getId(), current, totalChunks, chunk.getId(), extracted.size(), itemMap.size());
            if (current == 1 || current == totalChunks || current % progressInterval == 0) {
                updateExtractionProgress(source, task, current, totalChunks, itemMap.size());
            }
        }
        refreshKnowledgeCount(source.getId(), source.getAccount());
        return itemMap.size();
    }

    /**
     * 针对单个章节调用 AI 做二次提取，失败时自动切换到规则兜底。
     */
    private List<ExtractedKnowledge> extractFromChunk(MedicalKnowledgeSource source, MedicalKnowledgeChunk chunk, int current, int total) {
        String prompt = """
                你是医疗知识抽取助手，下面给你的是医学书籍清洗后的整章内容，请基于章节做二次提取，产出适合作为知识库展示的知识条目。
                返回要求：
                1. 只返回 JSON。
                2. 格式固定为 {"items":[...]}。
                3. 根据整章内容提取 1 到 5 条知识。
                4. 每个 item 包含字段：
                   itemType、title、keywords、department、summary、content、confidenceScore。
                5. keywords 为字符串数组。
                6. content 需要是可直接展示的中文说明，不要照抄整段原文。
                7. 如果文本无有效医疗知识，返回 {"items":[]}。

                书籍名称：%s
                章节标题：%s
                章节内容：
                %s
                """.formatted(source.getSourceName(),
                StringUtils.defaultIfBlank(chunk.getChapterTitle(), "未命名章节"),
                chunk.getCleanContent());
        try {
            long startTime = System.currentTimeMillis();
            log.info("医疗知识库调用 AI 开始, sourceId={}, chunkId={}, progress={}/{}, promptLength={}, timeoutSeconds={}",
                    source.getId(), chunk.getId(), current, total, prompt.length(), Math.max(properties.getAiTimeoutSeconds(), 10));
            String answer = CompletableFuture
                    .supplyAsync(() -> deepSeekAssistant.chat(prompt), AI_EXECUTOR)
                    .orTimeout(Math.max(properties.getAiTimeoutSeconds(), 10), TimeUnit.SECONDS)
                    .exceptionally(ex -> {
                        throw new CompletionException(ex);
                    })
                    .join();
            long costMs = System.currentTimeMillis() - startTime;
            log.info("医疗知识库调用 AI 完成, sourceId={}, chunkId={}, progress={}/{}, costMs={}, responseLength={}, responsePreview={}",
                    source.getId(), chunk.getId(), current, total, costMs,
                    answer == null ? 0 : answer.length(), abbreviateForLog(answer, 200));
            List<ExtractedKnowledge> items = parseKnowledgeResult(answer);
            if (!items.isEmpty()) {
                return items;
            }
            log.warn("AI 提取返回空结果，使用规则兜底, sourceId={}, chunkId={}, progress={}/{}, responseLength={}, responsePreview={}",
                    source.getId(), chunk.getId(), current, total,
                    answer == null ? 0 : answer.length(), abbreviateForLog(answer, 300));
        } catch (Exception ex) {
            Throwable root = unwrapCompletionException(ex);
            log.warn("AI 提取失败，改用规则兜底, sourceId={}, chunkId={}, progress={}/{}, message={}",
                    source.getId(), chunk.getId(), current, total, safeMessage(root));
        }
        return List.of(buildFallbackKnowledge(source, chunk));
    }

    /**
     * 将当前提取进度同步回任务表和来源表，便于前端和排查日志查看。
     */
    private void updateExtractionProgress(MedicalKnowledgeSource source, MedicalKnowledgeTask task,
                                          int current, int totalChunks, int currentKnowledgeCount) {
        String message = "AI 提取进度 " + current + "/" + totalChunks + "，当前累计知识 " + currentKnowledgeCount + " 条";
        task.setResultMessage(message);
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);

        MedicalKnowledgeSource latestSource = sourceMapper.selectById(source.getId());
        if (latestSource != null) {
            latestSource.setExtractStatus(STATUS_RUNNING);
            latestSource.setKnowledgeCount(currentKnowledgeCount);
            latestSource.setUpdateTime(LocalDateTime.now());
            sourceMapper.updateById(latestSource);
        }
    }

    /**
     * 解析模型返回的 JSON，并转换为内部知识条目对象。
     */
    private List<ExtractedKnowledge> parseKnowledgeResult(String answer) {
        if (StringUtils.isBlank(answer)) {
            return List.of();
        }
        int start = answer.indexOf('{');
        int end = answer.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return List.of();
        }
        String jsonText = answer.substring(start, end + 1);
        try {
            JSONObject jsonObject = JSON.parseObject(jsonText);
            JSONArray items = jsonObject.getJSONArray("items");
            if (items == null || items.isEmpty()) {
                return List.of();
            }
            List<ExtractedKnowledge> result = new ArrayList<>();
            for (int i = 0; i < items.size(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (item == null) {
                    continue;
                }
                List<String> keywords = item.getList("keywords", String.class);
                result.add(new ExtractedKnowledge(
                        normalizeKnowledgeValue(item.getString("itemType"), "知识点"),
                        normalizeKnowledgeValue(item.getString("title"), null),
                        keywords == null ? List.of() : keywords,
                        normalizeKnowledgeValue(item.getString("department"), "综合"),
                        normalizeKnowledgeValue(item.getString("summary"), null),
                        normalizeKnowledgeValue(item.getString("content"), null),
                        normalizeConfidence(item.get("confidenceScore"))
                ));
            }
            return result.stream()
                    .filter(item -> StringUtils.isNotBlank(item.title()) && StringUtils.isNotBlank(item.content()))
                    .toList();
        } catch (Exception ex) {
            log.warn("解析 AI 返回 JSON 失败: {}", ex.getMessage());
            return List.of();
        }
    }

    /**
     * 在 AI 失败或空结果时，基于章节文本生成最基础的兜底知识条目。
     */
    private ExtractedKnowledge buildFallbackKnowledge(MedicalKnowledgeSource source, MedicalKnowledgeChunk chunk) {
        String cleanContent = chunk.getCleanContent();
        String title = StringUtils.defaultIfBlank(findHeadingFromText(cleanContent),
                StringUtils.defaultIfBlank(chunk.getChapterTitle(), resolveSourceTitle(source.getSourceName())));
        String summary = abbreviate(cleanContent, 120);
        String content = abbreviate(cleanContent, 500);
        return new ExtractedKnowledge("知识点", title, extractKeywords(title, cleanContent), "综合", summary, content, BigDecimal.valueOf(0.50D));
    }

    /**
     * 根据提取结果创建新的知识条目实体。
     */
    private MedicalKnowledgeItem buildKnowledgeItem(Long sourceId, ExtractedKnowledge knowledge, String dedupKey) {
        MedicalKnowledgeItem item = new MedicalKnowledgeItem();
        item.setSourceId(sourceId);
        item.setItemType(normalizeKnowledgeValue(knowledge.itemType(), "知识点"));
        item.setTitle(knowledge.title().trim());
        item.setKeywords(String.join("、", deduplicateKeywords(knowledge.keywords())));
        item.setDepartment(normalizeKnowledgeValue(knowledge.department(), "综合"));
        item.setSummary(normalizeKnowledgeValue(knowledge.summary(), abbreviate(knowledge.content(), 120)));
        item.setContent(knowledge.content().trim());
        item.setStructuredData(JSON.toJSONString(Map.of(
                "itemType", item.getItemType(),
                "title", item.getTitle(),
                "keywords", deduplicateKeywords(knowledge.keywords()),
                "department", item.getDepartment(),
                "summary", item.getSummary(),
                "content", item.getContent()
        )));
        item.setConfidenceScore(knowledge.confidenceScore());
        item.setDedupKey(dedupKey);
        item.setReviewStatus(REVIEW_STATUS_DEFAULT);
        item.setCreateTime(LocalDateTime.now());
        item.setUpdateTime(LocalDateTime.now());
        item.setIsDelete(0);
        return item;
    }

    /**
     * 将重复 dedupKey 的知识结果合并到已有条目中，减少重复数据。
     */
    private void mergeKnowledgeItem(MedicalKnowledgeItem target, ExtractedKnowledge incoming) {
        Set<String> keywords = new LinkedHashSet<>();
        keywords.addAll(splitKeywords(target.getKeywords()));
        keywords.addAll(deduplicateKeywords(incoming.keywords()));
        target.setKeywords(String.join("、", keywords));
        if (StringUtils.length(incoming.content()) > StringUtils.length(target.getContent())) {
            target.setContent(incoming.content());
        }
        if (StringUtils.length(incoming.summary()) > StringUtils.length(target.getSummary())) {
            target.setSummary(incoming.summary());
        }
        if (incoming.confidenceScore() != null
                && (target.getConfidenceScore() == null || incoming.confidenceScore().compareTo(target.getConfidenceScore()) > 0)) {
            target.setConfidenceScore(incoming.confidenceScore());
        }
        target.setStructuredData(JSON.toJSONString(Map.of(
                "itemType", target.getItemType(),
                "title", target.getTitle(),
                "keywords", splitKeywords(target.getKeywords()),
                "department", target.getDepartment(),
                "summary", target.getSummary(),
                "content", target.getContent()
        )));
        target.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 在重新提取前逻辑删除旧知识条目，并清空对应证据引用。
     */
    private void markExistingItemsDeleted(Long sourceId) {
        LambdaQueryWrapper<MedicalKnowledgeItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MedicalKnowledgeItem::getSourceId, sourceId)
                .eq(MedicalKnowledgeItem::getIsDelete, 0);
        List<MedicalKnowledgeItem> items = itemMapper.selectList(wrapper);
        if (items.isEmpty()) {
            return;
        }
        List<Long> ids = items.stream().map(MedicalKnowledgeItem::getId).toList();
        LambdaQueryWrapper<MedicalKnowledgeItemRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.in(MedicalKnowledgeItemRef::getKnowledgeItemId, ids);
        itemRefMapper.delete(refWrapper);

        LambdaUpdateWrapper<MedicalKnowledgeItem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(MedicalKnowledgeItem::getId, ids)
                .set(MedicalKnowledgeItem::getIsDelete, 1)
                .set(MedicalKnowledgeItem::getUpdateTime, LocalDateTime.now());
        itemMapper.update(null, updateWrapper);
    }

    /**
     * 为知识条目补充章节证据引用，同一章节不会重复插入。
     */
    private void ensureItemReference(Long knowledgeItemId, MedicalKnowledgeChunk chunk) {
        if (knowledgeItemId == null || chunk == null || chunk.getId() == null) {
            return;
        }
        LambdaQueryWrapper<MedicalKnowledgeItemRef> refWrapper = new LambdaQueryWrapper<>();
        refWrapper.eq(MedicalKnowledgeItemRef::getKnowledgeItemId, knowledgeItemId)
                .eq(MedicalKnowledgeItemRef::getChunkId, chunk.getId())
                .last("limit 1");
        MedicalKnowledgeItemRef existing = itemRefMapper.selectOne(refWrapper);
        if (existing != null) {
            return;
        }

        LambdaQueryWrapper<MedicalKnowledgeItemRef> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(MedicalKnowledgeItemRef::getKnowledgeItemId, knowledgeItemId);
        Long refCount = itemRefMapper.selectCount(countWrapper);

        MedicalKnowledgeItemRef ref = new MedicalKnowledgeItemRef();
        ref.setKnowledgeItemId(knowledgeItemId);
        ref.setChunkId(chunk.getId());
        ref.setQuoteText(buildQuote(chunk.getCleanContent()));
        ref.setSortOrder(refCount == null ? 1 : refCount.intValue() + 1);
        ref.setCreateTime(LocalDateTime.now());
        itemRefMapper.insert(ref);
    }

    /**
     * 清空某本书的章节块和知识条目，为重新提取做准备。
     */
    private void resetSourceExtractionData(Long sourceId) {
        LambdaQueryWrapper<MedicalKnowledgeChunk> chunkWrapper = new LambdaQueryWrapper<>();
        chunkWrapper.eq(MedicalKnowledgeChunk::getSourceId, sourceId);
        chunkMapper.delete(chunkWrapper);
        markExistingItemsDeleted(sourceId);
    }

    /**
     * 重新统计来源书籍下的知识条目数量，并回写来源表。
     */
    private void refreshKnowledgeCount(Long sourceId, String account) {
        LambdaQueryWrapper<MedicalKnowledgeItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(MedicalKnowledgeItem::getSourceId, sourceId)
                .eq(MedicalKnowledgeItem::getIsDelete, 0);
        Long count = itemMapper.selectCount(itemWrapper);
        MedicalKnowledgeSource source = getOwnedSource(account, sourceId);
        source.setKnowledgeCount(count == null ? 0 : count.intValue());
        source.setUpdateTime(LocalDateTime.now());
        sourceMapper.updateById(source);
    }

    /**
     * 创建书籍来源记录，描述导入文件及其当前处理状态。
     */
    private MedicalKnowledgeSource createSource(String account, String sourceName, Integer sourceType, String fileFormat,
                                                Long fileSize, String localPath, String storagePath, String fileHash) {
        MedicalKnowledgeSource source = new MedicalKnowledgeSource();
        source.setAccount(account);
        source.setSourceName(sourceName);
        source.setSourceType(sourceType);
        source.setFileFormat(fileFormat);
        source.setFileSize(fileSize == null ? 0L : fileSize);
        source.setFileHash(fileHash);
        source.setLocalPath(localPath);
        source.setStoragePath(storagePath);
        source.setParseStatus(STATUS_PENDING);
        source.setExtractStatus(STATUS_PENDING);
        source.setChapterCount(0);
        source.setChunkCount(0);
        source.setKnowledgeCount(0);
        source.setCreateTime(LocalDateTime.now());
        source.setUpdateTime(LocalDateTime.now());
        source.setIsDelete(0);
        sourceMapper.insert(source);
        return source;
    }

    /**
     * 创建导入或重提取任务记录。
     */
    private MedicalKnowledgeTask createTask(Long sourceId, Integer taskType) {
        MedicalKnowledgeTask task = new MedicalKnowledgeTask();
        task.setSourceId(sourceId);
        task.setTaskType(taskType);
        task.setTaskStatus(STATUS_PENDING);
        task.setModelName(deepSeekProperties.getModel().getChat());
        task.setPromptVersion("medical-knowledge-v1");
        task.setResultMessage("任务已创建");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    /**
     * 更新任务状态、进度消息以及开始结束时间。
     */
    private void updateTaskStatus(MedicalKnowledgeTask task, Integer status, String message) {
        task.setTaskStatus(status);
        task.setResultMessage(message);
        if (status == STATUS_RUNNING && task.getStartTime() == null) {
            task.setStartTime(LocalDateTime.now());
        }
        if (status == STATUS_SUCCESS || status == STATUS_FAILED) {
            task.setFinishTime(LocalDateTime.now());
        }
        task.setUpdateTime(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    /**
     * 更新来源书籍的解析/提取状态和错误信息。
     */
    private void updateSourceStatus(MedicalKnowledgeSource source, Integer parseStatus, Integer extractStatus, String errorMessage) {
        source.setParseStatus(parseStatus);
        source.setExtractStatus(extractStatus);
        source.setErrorMessage(errorMessage);
        source.setUpdateTime(LocalDateTime.now());
        sourceMapper.updateById(source);
    }

    /**
     * 校验并获取当前账号可访问的来源书籍。
     */
    private MedicalKnowledgeSource getOwnedSource(String account, Long sourceId) {
        MedicalKnowledgeSource source = sourceMapper.selectById(sourceId);
        if (source == null || source.getIsDelete() != null && source.getIsDelete() == 1) {
            throw new RuntimeException("知识来源不存在");
        }
        if (!StringUtils.equals(source.getAccount(), account)) {
            throw new RuntimeException("无权访问该知识来源");
        }
        return source;
    }

    /**
     * 校验并获取当前账号可访问的知识条目。
     */
    private MedicalKnowledgeItem getOwnedItem(String account, Long itemId) {
        MedicalKnowledgeItem item = itemMapper.selectById(itemId);
        if (item == null || item.getIsDelete() != null && item.getIsDelete() == 1) {
            throw new RuntimeException("知识条目不存在");
        }
        getOwnedSource(account, item.getSourceId());
        return item;
    }

    /**
     * 校验本地导入路径是否存在且位于允许读取的白名单目录中。
     */
    private Path resolveAndValidateLocalPath(String localPath) {
        Path path = PathsHolder.resolve(localPath);
        if (!Files.exists(path)) {
            throw new RuntimeException("本地路径不存在: " + localPath);
        }
        List<Path> allowedRoots = properties.resolveAllowedLocalRoots();
        if (!allowedRoots.isEmpty()) {
            boolean matched = allowedRoots.stream().anyMatch(path::startsWith);
            if (!matched) {
                throw new RuntimeException("当前路径不在允许读取的根目录范围内");
            }
        }
        return path;
    }

    /**
     * 从给定路径中收集支持导入的书籍文件。
     */
    private List<Path> collectSupportedFiles(Path path) {
        try {
            if (Files.isRegularFile(path)) {
                return isSupportedFormat(detectFileFormat(path)) ? List.of(path) : List.of();
            }
            try (var stream = Files.walk(path, 1)) {
                return stream.filter(Files::isRegularFile)
                        .filter(file -> isSupportedFormat(detectFileFormat(file)))
                        .sorted(Comparator.comparing(Path::toString))
                        .toList();
            }
        } catch (IOException ex) {
            throw new RuntimeException("读取本地目录失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 判断文件扩展名是否属于当前支持的导入格式。
     */
    private boolean isSupportedFormat(String format) {
        return StringUtils.equalsAnyIgnoreCase(format, "pdf", "txt", "docx");
    }

    /**
     * 从 Path 对象中解析文件扩展名。
     */
    private String detectFileFormat(Path path) {
        return detectFileFormat(path.getFileName().toString());
    }

    /**
     * 从文件名字符串中提取扩展名。
     */
    private String detectFileFormat(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 按常见中文文本编码顺序尝试读取纯文本文件。
     */
    private String readTextWithFallback(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        List<Charset> charsets = List.of(StandardCharsets.UTF_8, Charset.forName("GBK"), StandardCharsets.UTF_16LE);
        for (Charset charset : charsets) {
            String text = new String(bytes, charset);
            if (StringUtils.isNotBlank(text)) {
                return text;
            }
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 做最基础的文本标准化处理，去除页码和多余空行。
     */
    private String cleanRawText(String text) {
        if (text == null) {
            return "";
        }
        String[] lines = text.replace("\r", "\n").split("\n");
        List<String> validLines = new ArrayList<>();
        for (String line : lines) {
            String normalized = cleanInlineText(line);
            if (StringUtils.isBlank(normalized)) {
                validLines.add("");
                continue;
            }
            if (PAGE_NUMBER_PATTERN.matcher(normalized).matches()) {
                continue;
            }
            validLines.add(normalized);
        }
        return BLANK_LINE_PATTERN.matcher(String.join("\n", validLines)).replaceAll("\n\n").trim();
    }

    /**
     * 清理行内空白字符，减少标题识别和正文去重时的噪声。
     */
    private String cleanInlineText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u3000', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    /**
     * 对章节正文做进一步清洗，过滤目录、乱码、版权说明和重复行。
     */
    private String cleanChapterContent(String content) {
        if (content == null) {
            return "";
        }
        String[] lines = cleanRawText(content).split("\\n");
        List<String> keptLines = new ArrayList<>();
        Set<String> seenLines = new HashSet<>();
        for (String line : lines) {
            String normalized = cleanInlineText(line);
            if (StringUtils.isBlank(normalized)) {
                continue;
            }
            if (isDirectoryLine(normalized) || isGarbledLine(normalized) || isCopyrightLine(normalized)) {
                continue;
            }
            if (seenLines.add(normalized)) {
                keptLines.add(normalized);
            }
        }
        return String.join("\n", keptLines).trim();
    }

    /**
     * 从多行文本中尝试识别首个章节标题。
     */
    private String findHeadingFromText(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        String[] lines = text.split("\\n");
        for (String line : lines) {
            String candidate = cleanInlineText(line);
            if (isHeading(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * 判断某一行是否符合章节标题特征。
     */
    private boolean isHeading(String text) {
        return StringUtils.isNotBlank(text) && text.length() <= 60 && HEADING_PATTERN.matcher(text).matches();
    }

    /**
     * 将原始分段重新规整为章节级结构，并过滤目录、前言和乱码内容。
     */
    private List<ParsedSegment> normalizeChapterSegments(String documentTitle, List<ParsedSegment> rawSegments, boolean dropUntitledLeading) {
        List<ParsedSegment> normalized = new ArrayList<>();
        String currentTitle = null;
        StringBuilder currentContent = new StringBuilder();
        Integer currentPageFrom = null;
        Integer currentPageTo = null;

        for (ParsedSegment raw : rawSegments) {
            String title = normalizeSegmentTitle(raw.title());
            String content = cleanChapterContent(removeLeadingHeading(raw.content(), title));
            if (StringUtils.isBlank(content)) {
                continue;
            }
            boolean skipByTitle = shouldSkipSectionTitle(title);
            boolean skipByContent = looksLikeDirectoryContent(content) || looksLikePrefaceContent(title, content) || containsMostlyGarbled(content);
            if (skipByTitle || skipByContent) {
                continue;
            }
            if (StringUtils.isNotBlank(title) && isHeading(title)) {
                if (StringUtils.isNotBlank(currentTitle) || currentContent.length() > 0) {
                    ParsedSegment segment = buildSegment(currentTitle, currentContent, currentPageFrom, currentPageTo);
                    if (segment != null) {
                        normalized.add(segment);
                    }
                }
                currentTitle = title;
                currentContent = new StringBuilder(content);
                currentPageFrom = raw.pageFrom();
                currentPageTo = raw.pageTo();
                continue;
            }
            if (StringUtils.isBlank(currentTitle) && dropUntitledLeading) {
                continue;
            }
            if (currentPageFrom == null) {
                currentPageFrom = raw.pageFrom();
            }
            currentPageTo = raw.pageTo();
            if (currentContent.length() > 0) {
                currentContent.append('\n');
            }
            currentContent.append(content);
        }

        ParsedSegment finalSegment = buildSegment(currentTitle, currentContent, currentPageFrom, currentPageTo);
        if (finalSegment != null) {
            normalized.add(finalSegment);
        }

        if (normalized.isEmpty() && !rawSegments.isEmpty()) {
            String merged = cleanChapterContent(rawSegments.stream()
                    .map(ParsedSegment::content)
                    .reduce("", (left, right) -> left + "\n" + right));
            if (StringUtils.isNotBlank(merged) && !containsMostlyGarbled(merged) && !looksLikeDirectoryContent(merged)) {
                normalized.add(new ParsedSegment(documentTitle, merged, rawSegments.get(0).pageFrom(), rawSegments.get(rawSegments.size() - 1).pageTo()));
            }
        }
        return normalized;
    }

    /**
     * 根据当前聚合状态构建章节分段对象。
     */
    private ParsedSegment buildSegment(String title, StringBuilder content, Integer pageFrom, Integer pageTo) {
        String cleaned = cleanChapterContent(content == null ? null : content.toString());
        if (StringUtils.isBlank(cleaned) || containsMostlyGarbled(cleaned) || looksLikeDirectoryContent(cleaned)) {
            return null;
        }
        return new ParsedSegment(StringUtils.trimToNull(title), cleaned, pageFrom, pageTo);
    }

    /**
     * 标准化章节标题文本，减少空白符对章节归并的影响。
     */
    private String normalizeSegmentTitle(String title) {
        String normalized = cleanInlineText(title);
        if (StringUtils.isBlank(normalized)) {
            return null;
        }
        return normalized.replaceAll("\\s+", "");
    }

    /**
     * 在正文开头去掉重复出现的标题行。
     */
    private String removeLeadingHeading(String content, String title) {
        if (StringUtils.isBlank(content) || StringUtils.isBlank(title)) {
            return content;
        }
        String normalizedContent = cleanRawText(content);
        String[] lines = normalizedContent.split("\\n", 2);
        if (lines.length > 0 && StringUtils.equals(cleanInlineText(lines[0]).replaceAll("\\s+", ""), title.replaceAll("\\s+", ""))) {
            return lines.length > 1 ? lines[1] : "";
        }
        return normalizedContent;
    }

    /**
     * 判断标题是否属于目录、前言等应整体跳过的章节。
     */
    private boolean shouldSkipSectionTitle(String title) {
        if (StringUtils.isBlank(title)) {
            return false;
        }
        String normalized = title.replaceAll("\\s+", "");
        return SKIP_SECTION_TITLES.stream().anyMatch(skip -> normalized.contains(skip.replaceAll("\\s+", "")));
    }

    /**
     * 基于标题和内容判断当前章节是否属于前言、序言等无效内容。
     */
    private boolean looksLikePrefaceContent(String title, String content) {
        if (shouldSkipSectionTitle(title)) {
            return true;
        }
        String preview = abbreviateForLog(content, 120);
        return preview.startsWith("前言") || preview.startsWith("序言") || preview.startsWith("目录");
    }

    /**
     * 判断一段内容是否整体更像目录页而不是正文。
     */
    private boolean looksLikeDirectoryContent(String content) {
        if (StringUtils.isBlank(content)) {
            return true;
        }
        String[] lines = content.split("\\n");
        int effectiveLines = 0;
        int directoryLines = 0;
        for (String line : lines) {
            String normalized = cleanInlineText(line);
            if (StringUtils.isBlank(normalized)) {
                continue;
            }
            effectiveLines++;
            if (isDirectoryLine(normalized)) {
                directoryLines++;
            }
        }
        if (effectiveLines == 0) {
            return true;
        }
        return directoryLines >= 3 && directoryLines * 1.0 / effectiveLines >= 0.6;
    }

    /**
     * 判断单行文本是否具备目录项特征。
     */
    private boolean isDirectoryLine(String line) {
        if (StringUtils.isBlank(line)) {
            return false;
        }
        String normalized = cleanInlineText(line);
        if (StringUtils.equalsAny(normalized, "目录", "目 录")) {
            return true;
        }
        if (DIRECTORY_LINE_PATTERN.matcher(normalized).matches()) {
            return true;
        }
        return SIMPLE_DIRECTORY_LINE_PATTERN.matcher(normalized).matches()
                && normalized.length() <= 40
                && (normalized.contains("章") || normalized.contains("节") || normalized.contains("篇"));
    }

    /**
     * 根据异常字符占比判断内容是否可能为乱码。
     */
    private boolean containsMostlyGarbled(String text) {
        if (StringUtils.isBlank(text)) {
            return true;
        }
        int effective = 0;
        int weird = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            effective++;
            if (!isAllowedTextChar(ch)) {
                weird++;
            }
        }
        return effective > 0 && weird * 1.0 / effective >= 0.35;
    }

    /**
     * 判断单行文本是否明显为乱码内容。
     */
    private boolean isGarbledLine(String line) {
        if (StringUtils.isBlank(line)) {
            return false;
        }
        return containsMostlyGarbled(line) || line.contains("�");
    }

    /**
     * 判断字符是否属于正文允许保留的字符集合。
     */
    private boolean isAllowedTextChar(char ch) {
        if (Character.isLetterOrDigit(ch)) {
            return true;
        }
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
            return true;
        }
        return "，。；：！？、“”‘’（）()【】《》—-·,.:;!?/%+*=<>[]".indexOf(ch) >= 0;
    }

    /**
     * 识别版权或禁止转载类说明行。
     */
    private boolean isCopyrightLine(String line) {
        if (StringUtils.isBlank(line)) {
            return false;
        }
        return line.startsWith("版权所有") || line.startsWith("Copyright") || line.contains("未经许可不得");
    }

    /**
     * 从文件名中提取书籍展示标题。
     */
    private String resolveSourceTitle(String sourceName) {
        if (StringUtils.isBlank(sourceName)) {
            return "医疗知识";
        }
        int dotIndex = sourceName.lastIndexOf('.');
        return dotIndex > 0 ? sourceName.substring(0, dotIndex) : sourceName;
    }

    /**
     * 根据来源、标题、类型和科室构造稳定的去重键。
     */
    private String buildDedupKey(Long sourceId, String title, String itemType, String department) {
        return sha256Hex(sourceId + "|" + StringUtils.defaultString(title).trim().toLowerCase(Locale.ROOT)
                + "|" + StringUtils.defaultString(itemType).trim().toLowerCase(Locale.ROOT)
                + "|" + StringUtils.defaultString(department).trim().toLowerCase(Locale.ROOT));
    }

    /**
     * 生成适合详情页展示的证据引用摘要。
     */
    private String buildQuote(String content) {
        return abbreviate(content, 200);
    }

    /**
     * 将长文本截断到指定长度，避免摘要和日志过长。
     */
    private String abbreviate(String value, int maxLength) {
        if (StringUtils.isBlank(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    /**
     * 为日志输出压缩文本内容，减少换行和无意义空白。
     */
    private String abbreviateForLog(String value, int maxLength) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return abbreviate(value.replaceAll("\\s+", " ").trim(), maxLength);
    }

    /**
     * 从标题和正文中提取一组基础关键词，供规则兜底时使用。
     */
    private List<String> extractKeywords(String title, String content) {
        Set<String> keywords = new LinkedHashSet<>();
        addPossibleKeyword(keywords, title);
        String[] parts = StringUtils.defaultString(content).split("[，。；、\\s]");
        for (String part : parts) {
            addPossibleKeyword(keywords, part);
            if (keywords.size() >= 6) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }

    /**
     * 将可能的关键词加入集合，并过滤过短或过长的无效项。
     */
    private void addPossibleKeyword(Set<String> keywords, String candidate) {
        String normalized = StringUtils.trimToEmpty(candidate);
        if (normalized.length() >= 2 && normalized.length() <= 12) {
            keywords.add(normalized);
        }
    }

    /**
     * 将数据库中的关键词字符串拆分为列表结构。
     */
    private List<String> splitKeywords(String keywords) {
        if (StringUtils.isBlank(keywords)) {
            return new ArrayList<>();
        }
        String[] parts = keywords.split("[、,，]");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (StringUtils.isNotBlank(part)) {
                result.add(part.trim());
            }
        }
        return result;
    }

    /**
     * 对关键词列表做去重和空值过滤。
     */
    private List<String> deduplicateKeywords(List<String> keywords) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (keywords != null) {
            for (String keyword : keywords) {
                if (StringUtils.isNotBlank(keyword)) {
                    result.add(keyword.trim());
                }
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 构造当前账号可访问来源书籍的子查询，用于列表分页过滤。
     */
    private String ownedSourceSubQuery(String account) {
        return "select id from medical_knowledge_source where account = '" + account.replace("'", "''") + "' and is_delete = 0";
    }

    /**
     * 规范化模型返回的置信度，确保值落在 0 到 1 之间。
     */
    private BigDecimal normalizeConfidence(Object value) {
        if (value == null) {
            return BigDecimal.valueOf(0.70D);
        }
        try {
            BigDecimal score = new BigDecimal(String.valueOf(value));
            if (score.compareTo(BigDecimal.ZERO) < 0) {
                return BigDecimal.ZERO;
            }
            if (score.compareTo(BigDecimal.ONE) > 0) {
                return BigDecimal.ONE;
            }
            return score;
        } catch (Exception ex) {
            return BigDecimal.valueOf(0.70D);
        }
    }

    /**
     * 统一处理模型返回的字符串字段，避免空白值污染结果。
     */
    private String normalizeKnowledgeValue(String value, String defaultValue) {
        return StringUtils.defaultIfBlank(StringUtils.trimToNull(value), defaultValue);
    }

    /**
     * 提取异常消息并控制长度，便于写入数据库和日志。
     */
    private String safeMessage(Throwable throwable) {
        String message = throwable == null ? null : throwable.getMessage();
        return StringUtils.isBlank(message) ? "处理失败，请稍后重试" : StringUtils.left(message, 1800);
    }

    /**
     * 解开异步执行包装异常，拿到最底层真实错误。
     */
    private Throwable unwrapCompletionException(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof CompletionException completionException && completionException.getCause() != null) {
            return unwrapCompletionException(completionException.getCause());
        }
        return throwable;
    }

    /**
     * 安全读取文件大小，失败时返回 0。
     */
    private Long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException ex) {
            return 0L;
        }
    }

    /**
     * 计算文件哈希，用于标识相同来源文件。
     */
    private String calculateFileHash(Path path) {
        try {
            return sha256Hex(Files.readAllBytes(path));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 对字符串内容计算 SHA-256 哈希值。
     */
    private String sha256Hex(String text) {
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 对字节数组计算 SHA-256 哈希值。
     */
    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder builder = new StringBuilder();
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new RuntimeException("计算哈希失败", ex);
        }
    }

    /**
     * 清理文件名中的非法字符，避免写入磁盘时失败。
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private record ParsedDocument(String title, List<ParsedSegment> segments) {
        private int chapterCount() {
            return segments.size();
        }
    }

    private record ParsedSegment(String title, String content, Integer pageFrom, Integer pageTo) {
    }

    private record ExtractedKnowledge(String itemType,
                                      String title,
                                      List<String> keywords,
                                      String department,
                                      String summary,
                                      String content,
                                      BigDecimal confidenceScore) {
    }

    private static final class PathsHolder {
        private PathsHolder() {
        }

        private static Path resolve(String path) {
            return Path.of(path).toAbsolutePath().normalize();
        }
    }
}
