package com.xu.home.service.crawler.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.crawler.BookCrawlerChapter;
import com.xu.home.domain.crawler.BookCrawlerTask;
import com.xu.home.mapper.crawler.BookCrawlerTaskMapper;
import com.xu.home.param.crawler.po.BookCrawlerPreviewPO;
import com.xu.home.param.crawler.po.BookCrawlerTaskSavePO;
import com.xu.home.param.crawler.vo.BookCrawlerPreviewChapterVO;
import com.xu.home.param.crawler.vo.BookCrawlerTaskDetailVO;
import com.xu.home.service.crawler.BookCrawlerChapterService;
import com.xu.home.service.crawler.BookCrawlerTaskService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Parser;
import org.jsoup.select.Selector;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 书籍爬虫任务服务实现
 */
@Service
@Slf4j
public class BookCrawlerTaskServiceImpl extends ServiceImpl<BookCrawlerTaskMapper, BookCrawlerTask>
        implements BookCrawlerTaskService {

    private static final ExecutorService CRAWLER_EXECUTOR = Executors.newCachedThreadPool();
    private static final Pattern INVALID_FILE_CHAR_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");
    private static final Set<String> BLOCK_TAGS = Set.of(
            "p", "div", "section", "article", "li", "dd", "dt",
            "h1", "h2", "h3", "h4", "h5", "h6", "blockquote"
    );
    private static final Set<String> INVALID_CONTENT_MARKERS = Set.of(
            "正文加载中...",
            "正文加载中",
            "网络异常，请点击这里尝试刷新",
            "文本解析异常",
            "本章内容缺失"
    );
    private static final int TASK_STATUS_PENDING = 1;
    private static final int TASK_STATUS_RUNNING = 2;
    private static final int TASK_STATUS_SUCCESS = 3;
    private static final int TASK_STATUS_PARTIAL_FAILED = 4;
    private static final int TASK_STATUS_FAILED = 5;
    private static final int TASK_STATUS_PAUSED = 6;
    private static final int CHAPTER_STATUS_PENDING = 1;
    private static final int CHAPTER_STATUS_SUCCESS = 2;
    private static final int CHAPTER_STATUS_FAILED = 3;
    private static final Pattern SEQUENTIAL_CHAPTER_URL_PATTERN = Pattern.compile("^(.*?/du-)(\\d+)(\\.html.*)?$");

    private final BookCrawlerChapterService bookCrawlerChapterService;

    public BookCrawlerTaskServiceImpl(BookCrawlerChapterService bookCrawlerChapterService) {
        this.bookCrawlerChapterService = bookCrawlerChapterService;
    }

    @Override
    public List<BookCrawlerTask> getTaskList(String account) {
        LambdaQueryWrapper<BookCrawlerTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookCrawlerTask::getAccount, account)
                .eq(BookCrawlerTask::getIsDelete, 0)
                .orderByDesc(BookCrawlerTask::getUpdateTime)
                .orderByDesc(BookCrawlerTask::getId);
        return list(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveTask(String account, BookCrawlerTaskSavePO po) {
        validateTaskSavePO(po);
        BookCrawlerTask task;
        LocalDateTime now = LocalDateTime.now();
        if (po.getId() == null) {
            task = new BookCrawlerTask();
            task.setAccount(account);
            task.setCreateTime(now);
            task.setIsDelete(0);
            task.setStatus(TASK_STATUS_PENDING);
            task.setTotalChapters(0);
            task.setSuccessChapters(0);
            task.setFailedChapters(0);
        } else {
            task = getOwnedTask(account, po.getId());
            if (task.getStatus() != null && task.getStatus() == TASK_STATUS_RUNNING) {
                throw new RuntimeException("任务执行中，暂不允许修改");
            }
        }

        BeanUtils.copyProperties(po, task, "id", "account", "createTime", "isDelete", "status",
                "totalChapters", "successChapters", "failedChapters", "lastMessage", "lastStartTime", "lastFinishTime");
        task.setTaskName(normalizeBlankToNull(task.getTaskName()));
        task.setSiteName(normalizeBlankToNull(task.getSiteName()));
        task.setBookName(normalizeBlankToNull(task.getBookName()));
        task.setCatalogUrl(normalizeBlankToNull(task.getCatalogUrl()));
        task.setChapterLinkSelector(normalizeBlankToNull(task.getChapterLinkSelector()));
        task.setChapterTitleSelector(normalizeBlankToNull(task.getChapterTitleSelector()));
        task.setContentSelector(normalizeBlankToNull(task.getContentSelector()));
        task.setContentRemoveSelectors(normalizeBlankToNull(task.getContentRemoveSelectors()));
        task.setSaveDirectory(normalizeBlankToNull(task.getSaveDirectory()));
        task.setStartChapterNum(defaultPositive(po.getStartChapterNum(), 1));
        task.setIntervalSeconds(defaultNonNegative(po.getIntervalSeconds(), 1));
        task.setUpdateTime(now);

        if (task.getId() == null) {
            save(task);
        } else {
            updateById(task);
        }
        return task.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTask(String account, Long taskId) {
        BookCrawlerTask task = getOwnedTask(account, taskId);
        if (task.getStatus() != null && task.getStatus() == TASK_STATUS_RUNNING) {
            throw new RuntimeException("任务执行中，暂不允许删除");
        }

        LambdaUpdateWrapper<BookCrawlerTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BookCrawlerTask::getId, taskId)
                .eq(BookCrawlerTask::getAccount, account)
                .set(BookCrawlerTask::getIsDelete, 1)
                .set(BookCrawlerTask::getUpdateTime, LocalDateTime.now());
        return update(wrapper);
    }

    @Override
    public BookCrawlerTaskDetailVO getTaskDetail(String account, Long taskId) {
        BookCrawlerTask task = getOwnedTask(account, taskId);
        BookCrawlerTaskDetailVO vo = new BookCrawlerTaskDetailVO();
        vo.setTask(task);
        vo.setChapters(bookCrawlerChapterService.getByTaskId(taskId));
        return vo;
    }

    @Override
    public List<BookCrawlerPreviewChapterVO> previewChapters(BookCrawlerPreviewPO po) {
        validatePreviewPO(po);
        return fetchPreviewChapters(po.getCatalogUrl(), po.getChapterLinkSelector());
    }

    @Override
    public void startTask(String account, Long taskId) {
        BookCrawlerTask task = getOwnedTask(account, taskId);
        if (task.getStatus() != null && task.getStatus() == TASK_STATUS_RUNNING) {
            throw new RuntimeException("任务已在执行中，请稍后查看");
        }

        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<BookCrawlerTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BookCrawlerTask::getId, taskId)
                .eq(BookCrawlerTask::getAccount, account)
                .eq(BookCrawlerTask::getIsDelete, 0)
                .set(BookCrawlerTask::getStatus, TASK_STATUS_RUNNING)
                .set(BookCrawlerTask::getSuccessChapters, 0)
                .set(BookCrawlerTask::getFailedChapters, 0)
                .set(BookCrawlerTask::getTotalChapters, 0)
                .set(BookCrawlerTask::getLastMessage, "任务已提交，开始抓取")
                .set(BookCrawlerTask::getLastStartTime, now)
                .set(BookCrawlerTask::getLastFinishTime, null)
                .set(BookCrawlerTask::getUpdateTime, now);
        update(wrapper);

        CompletableFuture.runAsync(() -> doCrawl(taskId, account), CRAWLER_EXECUTOR);
    }

    @Override
    public void pauseTask(String account, Long taskId) {
        BookCrawlerTask task = getOwnedTask(account, taskId);
        if (task.getStatus() == null || task.getStatus() != TASK_STATUS_RUNNING) {
            throw new RuntimeException("当前任务不在执行中，无法暂停");
        }

        LambdaUpdateWrapper<BookCrawlerTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BookCrawlerTask::getId, taskId)
                .eq(BookCrawlerTask::getAccount, account)
                .eq(BookCrawlerTask::getStatus, TASK_STATUS_RUNNING)
                .set(BookCrawlerTask::getStatus, TASK_STATUS_PAUSED)
                .set(BookCrawlerTask::getLastMessage, "暂停请求已提交，当前章节处理完成后会停止")
                .set(BookCrawlerTask::getUpdateTime, LocalDateTime.now());
        if (!update(wrapper)) {
            throw new RuntimeException("暂停任务失败，请稍后重试");
        }
    }

    private void doCrawl(Long taskId, String account) {
        BookCrawlerTask task = getOwnedTask(account, taskId);
        log.info("书籍爬虫任务开始执行, taskId={}, account={}, taskName={}, catalogUrl={}",
                taskId, account, task.getTaskName(), task.getCatalogUrl());
        try {
            List<BookCrawlerPreviewChapterVO> previewChapters = fetchPreviewChapters(task.getCatalogUrl(), task.getChapterLinkSelector());
            List<BookCrawlerPreviewChapterVO> targetChapters = filterTargetChapters(task, previewChapters);
            if (targetChapters.isEmpty()) {
                throw new RuntimeException("未找到可抓取的章节，请检查章节范围和选择器");
            }

            prepareChapterRecords(task, targetChapters);

            Path taskDirectory = prepareTaskDirectory(task, previewChapters);
            String resolvedBookName = resolveBookName(task, previewChapters, taskDirectory.getFileName().toString());
            Path mergedFilePath = taskDirectory.resolve(sanitizeFileName(resolvedBookName) + ".txt");
            Files.deleteIfExists(mergedFilePath);

            int successCount = 0;
            int failedCount = 0;
            int total = targetChapters.size();

            for (int i = 0; i < targetChapters.size(); i++) {
                if (isTaskPaused(taskId)) {
                    finishPausedTask(taskId, successCount, failedCount, total);
                    return;
                }

                BookCrawlerPreviewChapterVO chapterPreview = targetChapters.get(i);
                BookCrawlerChapter chapterRecord = getTaskChapter(taskId, chapterPreview.getChapterIndex());
                try {
                    log.info("开始抓取章节, taskId={}, chapterIndex={}, title={}, url={}",
                            taskId, chapterPreview.getChapterIndex(), chapterPreview.getChapterTitle(), chapterPreview.getChapterUrl());
                    markChapterStarted(chapterRecord);
                    ChapterFetchResult fetchResult = fetchChapterContent(task, chapterPreview);
                    Path chapterFilePath = taskDirectory.resolve(buildChapterFileName(chapterPreview.getChapterIndex(), fetchResult.chapterTitle));
                    writeChapterFile(chapterFilePath, fetchResult.chapterTitle, chapterPreview.getChapterUrl(), fetchResult.content);
                    appendToMergedFile(mergedFilePath, fetchResult.chapterTitle, chapterPreview.getChapterUrl(), fetchResult.content);
                    markChapterSuccess(chapterRecord, fetchResult.chapterTitle, chapterFilePath, fetchResult.content);
                    log.info("章节抓取成功, taskId={}, chapterIndex={}, title={}, contentLength={}, savePath={}",
                            taskId, chapterPreview.getChapterIndex(), fetchResult.chapterTitle, fetchResult.content.length(), chapterFilePath);
                    successCount++;
                } catch (Exception ex) {
                    log.error("章节抓取失败, taskId={}, chapterIndex={}, title={}, url={}",
                            taskId, chapterPreview.getChapterIndex(), chapterPreview.getChapterTitle(), chapterPreview.getChapterUrl(), ex);
                    markChapterFailed(chapterRecord, chapterPreview.getChapterTitle(), ex);
                    failedCount++;
                }

                updateTaskProgress(taskId, successCount, failedCount, total);

                if (i < targetChapters.size() - 1
                        && task.getIntervalSeconds() != null
                        && task.getIntervalSeconds() > 0
                        && !sleepInterval(taskId, task.getIntervalSeconds(), successCount, failedCount, total)) {
                    return;
                }
            }

            BookCrawlerTask finalTask = getById(taskId);
            if (finalTask != null) {
                finalTask.setBookName(resolvedBookName);
                finalTask.setStatus(failedCount == 0 ? TASK_STATUS_SUCCESS : (successCount == 0 ? TASK_STATUS_FAILED : TASK_STATUS_PARTIAL_FAILED));
                finalTask.setLastMessage(failedCount == 0
                        ? "抓取完成，共成功保存 " + successCount + " 章"
                        : "抓取完成，成功 " + successCount + " 章，失败 " + failedCount + " 章");
                finalTask.setTotalChapters(total);
                finalTask.setSuccessChapters(successCount);
                finalTask.setFailedChapters(failedCount);
                finalTask.setLastFinishTime(LocalDateTime.now());
                finalTask.setUpdateTime(LocalDateTime.now());
                updateById(finalTask);
            }
        } catch (Exception ex) {
            log.error("书籍爬虫任务执行失败, taskId={}, account={}, taskName={}",
                    taskId, account, task.getTaskName(), ex);
            LambdaUpdateWrapper<BookCrawlerTask> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(BookCrawlerTask::getId, taskId)
                    .eq(BookCrawlerTask::getAccount, account)
                    .set(BookCrawlerTask::getStatus, TASK_STATUS_FAILED)
                    .set(BookCrawlerTask::getLastMessage, safeMessage(ex))
                    .set(BookCrawlerTask::getLastFinishTime, LocalDateTime.now())
                    .set(BookCrawlerTask::getUpdateTime, LocalDateTime.now());
            update(wrapper);
        }
    }

    private List<BookCrawlerPreviewChapterVO> fetchPreviewChapters(String catalogUrl, String chapterLinkSelector) {
        Document document = fetchDocument(catalogUrl);
        SelectorMatchResult chapterLinkMatch = resolveChapterLinkMatch(document, catalogUrl, chapterLinkSelector);
        Elements chapterElements = chapterLinkMatch.elements;

        List<BookCrawlerPreviewChapterVO> chapters = new ArrayList<>();
        Set<String> uniqueUrls = new LinkedHashSet<>();
        int index = 1;
        for (Element element : chapterElements) {
            String chapterUrl = resolveAbsoluteUrl(element, catalogUrl);
            if (!StringUtils.hasText(chapterUrl) || !uniqueUrls.add(chapterUrl)) {
                continue;
            }

            String title = element.text();
            if (!StringUtils.hasText(title)) {
                title = element.attr("title");
            }
            if (!StringUtils.hasText(title)) {
                title = "第" + index + "章";
            }

            BookCrawlerPreviewChapterVO item = new BookCrawlerPreviewChapterVO();
            item.setChapterIndex(index++);
            item.setChapterTitle(title.trim());
            item.setChapterUrl(chapterUrl);
            chapters.add(item);
        }

        if (chapters.isEmpty()) {
            throw new RuntimeException("目录页未解析到有效章节链接");
        }
        chapters = expandSequentialChaptersIfNeeded(document, catalogUrl, chapters);
        log.info("目录页解析完成, url={}, configuredSelector={}, actualSelector={}, matchedCount={}, pageTitle={}, sampleLinks={}",
                catalogUrl, chapterLinkSelector, chapterLinkMatch.selector, chapters.size(), document.title(), buildPreviewSamples(chapters));
        return chapters;
    }

    private ChapterFetchResult fetchChapterContent(BookCrawlerTask task, BookCrawlerPreviewChapterVO preview) {
        if (isShuqiReaderUrl(preview.getChapterUrl())) {
            return fetchShuqiChapterContent(preview);
        }

        Document chapterDocument = fetchDocument(preview.getChapterUrl());
        SelectorMatchResult contentMatch = resolveContentMatch(chapterDocument, preview.getChapterUrl(), task.getContentSelector());
        Element contentElement = contentMatch.element;

        removeNoise(contentElement, task.getContentRemoveSelectors());
        String title = resolveChapterTitle(chapterDocument, task.getChapterTitleSelector(), preview.getChapterTitle());

        String content = extractContent(contentElement);
        if (isInvalidContent(content)) {
            log.warn("正文内容无效, url={}, selector={}, title={}, contentSnippet={}",
                    preview.getChapterUrl(), contentMatch.selector, title, abbreviate(content, 120));
            throw new RuntimeException("正文内容为空或仍为占位内容，请检查正文选择器或站点是否需要专用解析");
        }

        ChapterFetchResult result = new ChapterFetchResult();
        result.chapterTitle = title;
        result.content = content;
        return result;
    }

    private ChapterFetchResult fetchShuqiChapterContent(BookCrawlerPreviewChapterVO preview) {
        String pageHtml = fetchPageHtml(preview.getChapterUrl());
        Document chapterDocument = Jsoup.parse(pageHtml, preview.getChapterUrl());
        Element pageDataElement = chapterDocument.selectFirst(".js-dataChapters");
        if (pageDataElement == null || !StringUtils.hasText(pageDataElement.text())) {
            throw new RuntimeException("未找到书旗章节元数据，无法解析正文");
        }

        JSONObject pageData = JSON.parseObject(pageDataElement.text());
        String chapterId = extractQueryParam(preview.getChapterUrl(), "cid");
        if (!StringUtils.hasText(chapterId)) {
            throw new RuntimeException("未找到书旗章节ID，无法定位正文接口");
        }

        JSONObject chapterConfig = findShuqiChapterConfig(pageData, chapterId);
        if (chapterConfig == null) {
            throw new RuntimeException("未在书旗章节列表中找到当前章节配置");
        }
        if (!chapterConfig.getBooleanValue("isFreeRead")) {
            throw new RuntimeException("当前章节为付费章节，未购买时无法抓取正文");
        }

        String freeContentUrlPrefix = pageData.getString("freeContUrlPrefix");
        String contentUrlSuffix = Parser.unescapeEntities(chapterConfig.getString("contUrlSuffix"), false);
        if (!StringUtils.hasText(freeContentUrlPrefix) || !StringUtils.hasText(contentUrlSuffix)) {
            throw new RuntimeException("书旗正文接口参数缺失，无法抓取正文");
        }

        String responseText = fetchText(freeContentUrlPrefix + contentUrlSuffix, preview.getChapterUrl());
        JSONObject responseJson = JSON.parseObject(responseText);
        if (responseJson == null || responseJson.getIntValue("state") != 200) {
            throw new RuntimeException("书旗正文接口返回异常");
        }

        String rawContent = responseJson.getString("ChapterContent");
        if (!StringUtils.hasText(rawContent)) {
            throw new RuntimeException("书旗正文接口未返回正文内容");
        }

        String decodedContent = decodeShuqiChapterContent(rawContent);
        if (isInvalidContent(decodedContent)) {
            throw new RuntimeException("书旗正文解析失败，返回的仍是占位内容");
        }

        ChapterFetchResult result = new ChapterFetchResult();
        result.chapterTitle = StringUtils.hasText(chapterConfig.getString("chapterName"))
                ? chapterConfig.getString("chapterName").trim()
                : preview.getChapterTitle();
        result.content = decodedContent;
        return result;
    }

    private SelectorMatchResult resolveChapterLinkMatch(Document document, String catalogUrl, String configuredSelector) {
        if (StringUtils.hasText(configuredSelector)) {
            Elements configuredElements = safeSelect(document, configuredSelector.trim());
            if (!configuredElements.isEmpty()) {
                return new SelectorMatchResult(configuredSelector.trim(), configuredElements);
            }
        }

        for (String candidateSelector : buildChapterLinkSelectorCandidates(catalogUrl)) {
            Elements candidateElements = safeSelect(document, candidateSelector);
            Elements validElements = filterChapterLinkElements(candidateElements, catalogUrl);
            if (!validElements.isEmpty()) {
                return new SelectorMatchResult(candidateSelector, validElements);
            }
        }

        Elements candidateElements = safeSelect(document, "a[href*='/du-'], a[href*='reader?'], .list a, #list a, dd a, table a");
        log.warn("目录页章节选择器未命中, url={}, selector={}, title={}, fallbackCount={}, sampleLinks={}, htmlSnippet={}",
                catalogUrl,
                configuredSelector,
                document.title(),
                candidateElements.size(),
                buildLinkSamples(candidateElements, catalogUrl),
                abbreviate(document.body() == null ? document.outerHtml() : document.body().html(), 400));
        if (!candidateElements.isEmpty()) {
            throw new RuntimeException("目录页未匹配到章节链接，可留空自动识别，或尝试 a[href*='/du-']");
        }
        throw new RuntimeException("目录页未匹配到章节链接，请确认目录页地址可访问");
    }

    private SelectorMatchResult resolveContentMatch(Document document, String chapterUrl, String configuredSelector) {
        if (StringUtils.hasText(configuredSelector)) {
            Element configuredElement = safeSelectFirst(document, configuredSelector.trim());
            if (configuredElement != null) {
                return new SelectorMatchResult(configuredSelector.trim(), configuredElement);
            }
        }

        for (String candidateSelector : buildContentSelectorCandidates(chapterUrl)) {
            Element candidateElement = safeSelectFirst(document, candidateSelector);
            if (candidateElement == null) {
                continue;
            }
            String candidateText = extractContent(candidateElement);
            if (!isInvalidContent(candidateText) && candidateText.length() >= 20) {
                return new SelectorMatchResult(candidateSelector, candidateElement);
            }
        }

        Elements candidateElements = safeSelect(document, "#content, .content, .articlebody, .chapter-content, article");
        log.warn("正文选择器未命中, url={}, selector={}, title={}, candidateCount={}, htmlSnippet={}",
                chapterUrl,
                configuredSelector,
                document.title(),
                candidateElements.size(),
                abbreviate(document.body() == null ? document.outerHtml() : document.body().html(), 400));
        throw new RuntimeException("正文区域未识别成功，可留空自动识别，或尝试 #content / .articlebody / .chapter-content");
    }

    private String resolveChapterTitle(Document chapterDocument, String configuredSelector, String fallbackTitle) {
        if (StringUtils.hasText(configuredSelector)) {
            Element configuredTitle = safeSelectFirst(chapterDocument, configuredSelector.trim());
            if (configuredTitle != null && StringUtils.hasText(configuredTitle.text())) {
                return configuredTitle.text().trim();
            }
        }

        for (String candidateSelector : List.of("h1", ".chapter-title", ".bookname h1", ".title h1", ".content h1")) {
            Element titleElement = safeSelectFirst(chapterDocument, candidateSelector);
            if (titleElement != null && StringUtils.hasText(titleElement.text())) {
                return titleElement.text().trim();
            }
        }
        return fallbackTitle;
    }

    private List<BookCrawlerPreviewChapterVO> expandSequentialChaptersIfNeeded(Document document, String catalogUrl, List<BookCrawlerPreviewChapterVO> chapters) {
        if (!supportsSequentialExpansion(document, catalogUrl, chapters)) {
            return chapters;
        }

        Map<Integer, BookCrawlerPreviewChapterVO> chapterNumberMap = new HashMap<>();
        SequentialUrlTemplate template = null;
        int minNumber = Integer.MAX_VALUE;
        int maxNumber = Integer.MIN_VALUE;
        boolean hasGap = false;
        int previousNumber = -1;

        for (BookCrawlerPreviewChapterVO chapter : chapters) {
            Integer chapterNumber = extractSequentialChapterNumber(chapter.getChapterUrl());
            if (chapterNumber == null) {
                return chapters;
            }
            if (template == null) {
                template = buildSequentialUrlTemplate(chapter.getChapterUrl());
            }
            if (template == null) {
                return chapters;
            }
            chapterNumberMap.put(chapterNumber, chapter);
            minNumber = Math.min(minNumber, chapterNumber);
            maxNumber = Math.max(maxNumber, chapterNumber);
            if (previousNumber > 0 && chapterNumber - previousNumber > 1) {
                hasGap = true;
            }
            previousNumber = chapterNumber;
        }

        if (!hasGap || minNumber != 1 || maxNumber <= chapters.size()) {
            return chapters;
        }

        List<BookCrawlerPreviewChapterVO> expandedChapters = new ArrayList<>(maxNumber);
        for (int chapterNumber = minNumber; chapterNumber <= maxNumber; chapterNumber++) {
            BookCrawlerPreviewChapterVO existingChapter = chapterNumberMap.get(chapterNumber);
            BookCrawlerPreviewChapterVO current = existingChapter;
            if (current == null) {
                current = new BookCrawlerPreviewChapterVO();
                current.setChapterTitle("第" + chapterNumber + "章");
                current.setChapterUrl(template.prefix + chapterNumber + template.suffix);
            }
            current.setChapterIndex(expandedChapters.size() + 1);
            expandedChapters.add(current);
        }

        log.info("目录页已自动补齐顺序章节, url={}, originalCount={}, expandedCount={}, maxChapterNumber={}",
                catalogUrl, chapters.size(), expandedChapters.size(), maxNumber);
        return expandedChapters;
    }

    private boolean supportsSequentialExpansion(Document document, String catalogUrl, List<BookCrawlerPreviewChapterVO> chapters) {
        if (!StringUtils.hasText(catalogUrl) || chapters.size() < 2) {
            return false;
        }
        if (catalogUrl.contains("qb5.io")) {
            return true;
        }
        return document.outerHtml().contains("load_more(") && document.outerHtml().contains("list.json");
    }

    private Integer extractSequentialChapterNumber(String chapterUrl) {
        if (!StringUtils.hasText(chapterUrl)) {
            return null;
        }
        Matcher matcher = SEQUENTIAL_CHAPTER_URL_PATTERN.matcher(chapterUrl);
        if (!matcher.matches()) {
            return null;
        }
        return Integer.parseInt(matcher.group(2));
    }

    private SequentialUrlTemplate buildSequentialUrlTemplate(String chapterUrl) {
        if (!StringUtils.hasText(chapterUrl)) {
            return null;
        }
        Matcher matcher = SEQUENTIAL_CHAPTER_URL_PATTERN.matcher(chapterUrl);
        if (!matcher.matches()) {
            return null;
        }
        return new SequentialUrlTemplate(matcher.group(1), matcher.group(3) == null ? "" : matcher.group(3));
    }

    private List<String> buildChapterLinkSelectorCandidates(String catalogUrl) {
        List<String> selectors = new ArrayList<>();
        selectors.add("a[href*='/du-']");
        selectors.add("div.chapterbox table td a");
        selectors.add(".list a[href]");
        selectors.add("ul.list a[href]");
        selectors.add("#list a[href]");
        selectors.add("dd a[href]");
        selectors.add("dl dd a[href]");
        selectors.add("table td a[href]");
        selectors.add("a[href*='reader?']");
        if (StringUtils.hasText(catalogUrl) && catalogUrl.contains("qb5.io")) {
            selectors.add("a[href*='/xs-'][href*='/du-']");
        }
        return selectors;
    }

    private List<String> buildContentSelectorCandidates(String chapterUrl) {
        List<String> selectors = new ArrayList<>();
        selectors.add("#content");
        selectors.add(".articlebody");
        selectors.add(".chapter-content");
        selectors.add(".content");
        selectors.add(".read-content");
        selectors.add("article");
        selectors.add(".yd_text2");
        if (StringUtils.hasText(chapterUrl) && chapterUrl.contains("qb5.io")) {
            selectors.add(0, "#content");
            selectors.add(1, ".articlebody");
        }
        return selectors;
    }

    private Elements filterChapterLinkElements(Elements source, String catalogUrl) {
        Elements validElements = new Elements();
        for (Element element : source) {
            if (isLikelyChapterLink(element, catalogUrl)) {
                validElements.add(element);
            }
        }
        return validElements;
    }

    private boolean isLikelyChapterLink(Element element, String catalogUrl) {
        String href = resolveAbsoluteUrl(element, catalogUrl);
        String title = element.text();
        if (!StringUtils.hasText(href) || !StringUtils.hasText(title)) {
            return false;
        }
        String normalizedTitle = title.trim();
        if (normalizedTitle.length() > 64) {
            return false;
        }
        return href.contains("/du-")
                || href.contains("reader?")
                || normalizedTitle.matches("^(第?[0-9一二三四五六七八九十百千零两]+[章节卷回篇].*)$")
                || normalizedTitle.toLowerCase().matches("^chapter\\s*\\d+.*$");
    }

    private Elements safeSelect(Document document, String selector) {
        if (!StringUtils.hasText(selector)) {
            return new Elements();
        }
        try {
            return document.select(selector);
        } catch (Selector.SelectorParseException ex) {
            throw new RuntimeException("选择器格式错误: " + selector);
        }
    }

    private Element safeSelectFirst(Document document, String selector) {
        Elements elements = safeSelect(document, selector);
        return elements.isEmpty() ? null : elements.first();
    }

    @Transactional(rollbackFor = Exception.class)
    protected void prepareChapterRecords(BookCrawlerTask task, List<BookCrawlerPreviewChapterVO> targetChapters) {
        LambdaQueryWrapper<BookCrawlerChapter> removeWrapper = new LambdaQueryWrapper<>();
        removeWrapper.eq(BookCrawlerChapter::getTaskId, task.getId());
        bookCrawlerChapterService.remove(removeWrapper);

        LocalDateTime now = LocalDateTime.now();
        List<BookCrawlerChapter> records = new ArrayList<>();
        for (BookCrawlerPreviewChapterVO preview : targetChapters) {
            BookCrawlerChapter chapter = new BookCrawlerChapter();
            chapter.setTaskId(task.getId());
            chapter.setChapterIndex(preview.getChapterIndex());
            chapter.setChapterTitle(preview.getChapterTitle());
            chapter.setChapterUrl(preview.getChapterUrl());
            chapter.setContentLength(0);
            chapter.setCrawlStatus(CHAPTER_STATUS_PENDING);
            chapter.setCreateTime(now);
            chapter.setUpdateTime(now);
            records.add(chapter);
        }
        bookCrawlerChapterService.saveBatch(records);

        LambdaUpdateWrapper<BookCrawlerTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BookCrawlerTask::getId, task.getId())
                .set(BookCrawlerTask::getTotalChapters, targetChapters.size())
                .set(BookCrawlerTask::getSuccessChapters, 0)
                .set(BookCrawlerTask::getFailedChapters, 0)
                .set(BookCrawlerTask::getLastMessage, "已解析目录，共 " + targetChapters.size() + " 章待抓取")
                .set(BookCrawlerTask::getUpdateTime, LocalDateTime.now());
        update(wrapper);
    }

    private List<BookCrawlerPreviewChapterVO> filterTargetChapters(BookCrawlerTask task, List<BookCrawlerPreviewChapterVO> previewChapters) {
        int start = Math.max(defaultPositive(task.getStartChapterNum(), 1), 1);
        int end = task.getEndChapterNum() == null ? previewChapters.size() : task.getEndChapterNum();
        if (start > previewChapters.size()) {
            return new ArrayList<>();
        }
        end = Math.min(end, previewChapters.size());
        if (end < start) {
            throw new RuntimeException("结束章节不能小于开始章节");
        }
        return new ArrayList<>(previewChapters.subList(start - 1, end));
    }

    private Path prepareTaskDirectory(BookCrawlerTask task, List<BookCrawlerPreviewChapterVO> previewChapters) throws IOException {
        String resolvedBookName = resolveBookName(task, previewChapters, task.getTaskName());
        Path baseDirectory = resolveSaveDirectory(task.getSaveDirectory());
        Path taskDirectory = baseDirectory.resolve(sanitizeFileName(resolvedBookName));
        Files.createDirectories(taskDirectory);
        return taskDirectory;
    }

    private String resolveBookName(BookCrawlerTask task, List<BookCrawlerPreviewChapterVO> previewChapters, String fallback) {
        if (StringUtils.hasText(task.getBookName())) {
            return task.getBookName().trim();
        }
        if (!previewChapters.isEmpty() && StringUtils.hasText(previewChapters.get(0).getChapterTitle())) {
            return fallback;
        }
        return fallback;
    }

    private Path resolveSaveDirectory(String saveDirectory) {
        Path directory = Paths.get(saveDirectory.trim());
        if (directory.isAbsolute()) {
            return directory.normalize();
        }
        return Paths.get(System.getProperty("user.dir")).resolve(directory).normalize();
    }

    private void writeChapterFile(Path path, String title, String url, String content) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(title).append(System.lineSeparator())
                .append("来源: ").append(url).append(System.lineSeparator()).append(System.lineSeparator())
                .append(content).append(System.lineSeparator());
        Files.writeString(path, builder.toString(), StandardCharsets.UTF_8);
    }

    private void appendToMergedFile(Path mergedFilePath, String title, String url, String content) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append(title).append(System.lineSeparator())
                .append("来源: ").append(url).append(System.lineSeparator()).append(System.lineSeparator())
                .append(content).append(System.lineSeparator()).append(System.lineSeparator());
        Files.writeString(
                mergedFilePath,
                builder.toString(),
                StandardCharsets.UTF_8,
                Files.exists(mergedFilePath) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE
        );
    }

    private void removeNoise(Element contentElement, String removeSelectors) {
        contentElement.select("script,style,noscript").remove();
        for (String selector : resolveRemoveSelectors(removeSelectors)) {
            contentElement.select(selector).remove();
        }
    }

    private List<String> resolveRemoveSelectors(String removeSelectors) {
        if (StringUtils.hasText(removeSelectors)) {
            return splitSelectors(removeSelectors);
        }
        return List.of(
                "h1",
                "#banner",
                ".banner",
                ".ads",
                ".ad",
                ".advert",
                ".readad",
                ".bottomlink",
                "iframe",
                "ins"
        );
    }

    private List<String> splitSelectors(String selectors) {
        String normalized = selectors.replace("；", ";").replace("\n", ";").replace(",", ";");
        List<String> result = new ArrayList<>();
        for (String item : normalized.split(";")) {
            if (StringUtils.hasText(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private String extractContent(Element contentElement) {
        StringBuilder builder = new StringBuilder();
        appendNodeText(contentElement, builder);
        String text = builder.toString()
                .replace('\u00A0', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" +\n", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
        if (!StringUtils.hasText(text)) {
            text = contentElement.wholeText().trim();
        }
        return text;
    }

    private void appendNodeText(Node node, StringBuilder builder) {
        if (node instanceof TextNode textNode) {
            String text = textNode.text();
            if (StringUtils.hasText(text)) {
                builder.append(text.trim());
            }
            return;
        }

        if (node instanceof Element element) {
            if ("br".equalsIgnoreCase(element.tagName())) {
                builder.append("\n");
                return;
            }

            boolean block = BLOCK_TAGS.contains(element.tagName().toLowerCase());
            if (block && builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                builder.append("\n");
            }

            for (Node childNode : element.childNodes()) {
                appendNodeText(childNode, builder);
            }

            if (block && builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                builder.append("\n");
            }
        }
    }

    private Document fetchDocument(String url) {
        return Jsoup.parse(fetchPageHtml(url), url);
    }

    private String fetchPageHtml(String url) {
        try {
            Connection.Response response = buildConnection(url)
                    .execute();
            String body = response.body();
            log.info("页面请求完成, url={}, statusCode={}, bodyLength={}", url, response.statusCode(), body == null ? 0 : body.length());
            return body;
        } catch (IOException ex) {
            log.error("页面请求失败, url={}", url, ex);
            throw new RuntimeException("请求页面失败: " + url);
        }
    }

    private String fetchText(String url, String referer) {
        try {
            Connection connection = buildConnection(url)
                    .ignoreContentType(true)
                    .header("Accept", "application/json, text/plain, */*");
            if (StringUtils.hasText(referer)) {
                connection.referrer(referer);
                connection.header("Referer", referer);
            }
            Connection.Response response = connection.execute();
            String body = response.body();
            log.info("文本请求完成, url={}, referer={}, statusCode={}, bodyLength={}",
                    url, referer, response.statusCode(), body == null ? 0 : body.length());
            return body;
        } catch (IOException ex) {
            log.error("文本请求失败, url={}, referer={}", url, referer, ex);
            throw new RuntimeException("请求正文接口失败: " + url);
        }
    }

    private Connection buildConnection(String url) {
        return Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept-Language", "zh-CN,zh;q=0.9")
                .timeout(20000)
                .followRedirects(true)
                .ignoreHttpErrors(true);
    }

    private String resolveAbsoluteUrl(Element element, String catalogUrl) {
        String href = element.absUrl("href");
        if (StringUtils.hasText(href)) {
            return href;
        }
        String rawHref = element.attr("href");
        if (!StringUtils.hasText(rawHref)) {
            return null;
        }
        return URI.create(catalogUrl).resolve(rawHref).toString();
    }

    private BookCrawlerTask getOwnedTask(String account, Long taskId) {
        if (taskId == null) {
            throw new RuntimeException("任务ID不能为空");
        }
        LambdaQueryWrapper<BookCrawlerTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookCrawlerTask::getId, taskId)
                .eq(BookCrawlerTask::getAccount, account)
                .eq(BookCrawlerTask::getIsDelete, 0);
        BookCrawlerTask task = getOne(wrapper);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        return task;
    }

    private BookCrawlerChapter getTaskChapter(Long taskId, Integer chapterIndex) {
        LambdaQueryWrapper<BookCrawlerChapter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BookCrawlerChapter::getTaskId, taskId)
                .eq(BookCrawlerChapter::getChapterIndex, chapterIndex)
                .last("limit 1");
        BookCrawlerChapter chapter = bookCrawlerChapterService.getOne(wrapper);
        if (chapter == null) {
            throw new RuntimeException("章节记录不存在");
        }
        return chapter;
    }

    private void markChapterStarted(BookCrawlerChapter chapter) {
        chapter.setCrawlStatus(CHAPTER_STATUS_PENDING);
        chapter.setErrorMessage(null);
        chapter.setStartTime(LocalDateTime.now());
        chapter.setFinishTime(null);
        chapter.setUpdateTime(LocalDateTime.now());
        bookCrawlerChapterService.updateById(chapter);
    }

    private void markChapterSuccess(BookCrawlerChapter chapter, String title, Path filePath, String content) {
        chapter.setChapterTitle(title);
        chapter.setSavePath(filePath.toString());
        chapter.setContentLength(content.length());
        chapter.setCrawlStatus(CHAPTER_STATUS_SUCCESS);
        chapter.setErrorMessage(null);
        chapter.setFinishTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());
        bookCrawlerChapterService.updateById(chapter);
    }

    private void markChapterFailed(BookCrawlerChapter chapter, String title, Exception ex) {
        chapter.setChapterTitle(title);
        chapter.setCrawlStatus(CHAPTER_STATUS_FAILED);
        chapter.setErrorMessage(safeMessage(ex));
        chapter.setFinishTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());
        bookCrawlerChapterService.updateById(chapter);
    }

    private void updateTaskProgress(Long taskId, int successCount, int failedCount, int total) {
        LambdaUpdateWrapper<BookCrawlerTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BookCrawlerTask::getId, taskId)
                .eq(BookCrawlerTask::getStatus, TASK_STATUS_RUNNING)
                .set(BookCrawlerTask::getTotalChapters, total)
                .set(BookCrawlerTask::getSuccessChapters, successCount)
                .set(BookCrawlerTask::getFailedChapters, failedCount)
                .set(BookCrawlerTask::getLastMessage, "抓取中，成功 " + successCount + " 章，失败 " + failedCount + " 章")
                .set(BookCrawlerTask::getUpdateTime, LocalDateTime.now());
        update(wrapper);
    }

    private boolean sleepInterval(Long taskId, Integer intervalSeconds, int successCount, int failedCount, int total) {
        long remainMillis = intervalSeconds * 1000L;
        try {
            while (remainMillis > 0) {
                if (isTaskPaused(taskId)) {
                    finishPausedTask(taskId, successCount, failedCount, total);
                    return false;
                }
                long sleepMillis = Math.min(remainMillis, 200L);
                Thread.sleep(sleepMillis);
                remainMillis -= sleepMillis;
            }
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("抓取任务被中断");
        }
    }

    private boolean isTaskPaused(Long taskId) {
        BookCrawlerTask task = getById(taskId);
        return task != null && task.getStatus() != null && task.getStatus() == TASK_STATUS_PAUSED;
    }

    private void finishPausedTask(Long taskId, int successCount, int failedCount, int total) {
        LambdaUpdateWrapper<BookCrawlerTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(BookCrawlerTask::getId, taskId)
                .eq(BookCrawlerTask::getStatus, TASK_STATUS_PAUSED)
                .set(BookCrawlerTask::getTotalChapters, total)
                .set(BookCrawlerTask::getSuccessChapters, successCount)
                .set(BookCrawlerTask::getFailedChapters, failedCount)
                .set(BookCrawlerTask::getLastMessage, "任务已暂停，当前已成功 " + successCount + " 章，失败 " + failedCount + " 章")
                .set(BookCrawlerTask::getLastFinishTime, LocalDateTime.now())
                .set(BookCrawlerTask::getUpdateTime, LocalDateTime.now());
        update(wrapper);
    }

    private JSONObject findShuqiChapterConfig(JSONObject pageData, String chapterId) {
        JSONArray chapterList = pageData.getJSONArray("chapterList");
        if (chapterList == null || chapterList.isEmpty()) {
            chapterList = pageData.getJSONArray("chapters");
        }
        if (chapterList == null) {
            return null;
        }

        for (int i = 0; i < chapterList.size(); i++) {
            JSONObject volume = chapterList.getJSONObject(i);
            if (volume == null) {
                continue;
            }
            JSONArray volumeList = volume.getJSONArray("volumeList");
            if (volumeList == null) {
                continue;
            }
            for (int j = 0; j < volumeList.size(); j++) {
                JSONObject chapter = volumeList.getJSONObject(j);
                if (matchesShuqiChapterId(chapter, chapterId)) {
                    return chapter;
                }
            }
        }
        return null;
    }

    private boolean matchesShuqiChapterId(JSONObject chapter, String chapterId) {
        if (chapter == null || !StringUtils.hasText(chapterId)) {
            return false;
        }
        return chapterId.equals(chapter.getString("cid"))
                || chapterId.equals(chapter.getString("chapterId"))
                || chapterId.equals(chapter.getString("id"));
    }

    private String extractQueryParam(String url, String paramName) {
        String query = URI.create(url).getRawQuery();
        if (!StringUtils.hasText(query)) {
            return null;
        }
        for (String pair : query.split("&")) {
            String[] items = pair.split("=", 2);
            if (items.length == 2 && paramName.equals(items[0])) {
                return items[1];
            }
        }
        return null;
    }

    private String decodeShuqiChapterContent(String rawContent) {
        String decoded = rawContent;
        try {
            decoded = decodeShuqiBase64Content(rawContent.trim());
        } catch (Exception ignored) {
            decoded = rawContent.trim();
        }

        Document document = Jsoup.parseBodyFragment(decoded);
        String content = extractContent(document.body());
        if (StringUtils.hasText(content)) {
            return content;
        }
        return Parser.unescapeEntities(decoded, false).trim();
    }

    private String decodeShuqiBase64Content(String content) {
        String shifted = shiftShuqiLetters(content);
        String normalizedBase64 = shifted.replaceAll("[^A-Za-z0-9+/=]", "");
        byte[] decodedBytes = Base64.getDecoder().decode(normalizedBase64);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    private String shiftShuqiLetters(String content) {
        StringBuilder builder = new StringBuilder(content.length());
        for (int i = 0; i < content.length(); i++) {
            char current = content.charAt(i);
            if (!Character.isLetter(current)) {
                builder.append(current);
                continue;
            }
            int shiftedCode = (Character.toLowerCase(current) - 83) % 26;
            if (shiftedCode <= 0) {
                shiftedCode += 26;
            }
            int base = Character.isUpperCase(current) ? 64 : 96;
            builder.append((char) (shiftedCode + base));
        }
        return builder.toString();
    }

    private boolean isShuqiReaderUrl(String url) {
        return StringUtils.hasText(url) && url.contains("shuqi.com/reader");
    }

    private boolean isInvalidContent(String content) {
        if (!StringUtils.hasText(content)) {
            return true;
        }
        String normalized = content.trim();
        if (INVALID_CONTENT_MARKERS.contains(normalized)) {
            return true;
        }
        return normalized.length() <= 32
                && (normalized.contains("正文加载中")
                || normalized.contains("网络异常")
                || normalized.contains("文本解析异常")
                || normalized.contains("本章内容缺失"));
    }

    private void validateTaskSavePO(BookCrawlerTaskSavePO po) {
        if (po == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (!StringUtils.hasText(po.getTaskName())) {
            throw new RuntimeException("任务名称不能为空");
        }
        if (!StringUtils.hasText(po.getCatalogUrl())) {
            throw new RuntimeException("目录页地址不能为空");
        }
        if (!StringUtils.hasText(po.getSaveDirectory())) {
            throw new RuntimeException("保存目录不能为空");
        }
        int startChapter = defaultPositive(po.getStartChapterNum(), 1);
        if (po.getEndChapterNum() != null && po.getEndChapterNum() < startChapter) {
            throw new RuntimeException("结束章节不能小于开始章节");
        }
        if (defaultNonNegative(po.getIntervalSeconds(), 1) < 0) {
            throw new RuntimeException("章节间隔时间不能小于0");
        }
    }

    private void validatePreviewPO(BookCrawlerPreviewPO po) {
        if (po == null) {
            throw new RuntimeException("请求参数不能为空");
        }
        if (!StringUtils.hasText(po.getCatalogUrl())) {
            throw new RuntimeException("目录页地址不能为空");
        }
    }

    private int defaultPositive(Integer value, int defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }

    private int defaultNonNegative(Integer value, int defaultValue) {
        return value == null ? defaultValue : Math.max(value, 0);
    }

    private String normalizeBlankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String sanitizeFileName(String input) {
        String source = StringUtils.hasText(input) ? input.trim() : "book";
        String sanitized = INVALID_FILE_CHAR_PATTERN.matcher(source).replaceAll("_")
                .replaceAll("\\s+", "_");
        return sanitized.length() > 120 ? sanitized.substring(0, 120) : sanitized;
    }

    private String buildLinkSamples(Elements elements, String baseUrl) {
        List<String> samples = new ArrayList<>();
        for (int i = 0; i < elements.size() && i < 5; i++) {
            Element element = elements.get(i);
            String text = StringUtils.hasText(element.text()) ? element.text().trim() : "(空标题)";
            String href = resolveAbsoluteUrl(element, baseUrl);
            samples.add(text + " -> " + href);
        }
        return samples.isEmpty() ? "-" : String.join(" | ", samples);
    }

    private String buildPreviewSamples(List<BookCrawlerPreviewChapterVO> chapters) {
        List<String> samples = new ArrayList<>();
        for (int i = 0; i < chapters.size() && i < 5; i++) {
            BookCrawlerPreviewChapterVO chapter = chapters.get(i);
            samples.add(chapter.getChapterTitle() + " -> " + chapter.getChapterUrl());
        }
        return samples.isEmpty() ? "-" : String.join(" | ", samples);
    }

    private String abbreviate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private String buildChapterFileName(Integer chapterIndex, String title) {
        return String.format("%04d_%s.txt", chapterIndex, sanitizeFileName(title));
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return StringUtils.hasText(message) ? message : "抓取失败，请检查站点规则或网络状态";
    }

    private static class ChapterFetchResult {
        private String chapterTitle;
        private String content;
    }

    private static class SelectorMatchResult {
        private final String selector;
        private final Elements elements;
        private final Element element;

        private SelectorMatchResult(String selector, Elements elements) {
            this.selector = selector;
            this.elements = elements;
            this.element = elements == null || elements.isEmpty() ? null : elements.first();
        }

        private SelectorMatchResult(String selector, Element element) {
            this.selector = selector;
            this.element = element;
            this.elements = new Elements();
            if (element != null) {
                this.elements.add(element);
            }
        }
    }

    private static class SequentialUrlTemplate {
        private final String prefix;
        private final String suffix;

        private SequentialUrlTemplate(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
        }
    }
}
