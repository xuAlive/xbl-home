package com.xu.home.service.ai.qwen;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xu.home.domain.medical.MedicalKnowledgeChunk;
import com.xu.home.domain.medical.MedicalKnowledgeItem;
import com.xu.home.domain.medical.MedicalKnowledgeItemRef;
import com.xu.home.domain.medical.MedicalKnowledgeSource;
import com.xu.home.mapper.medical.MedicalKnowledgeChunkMapper;
import com.xu.home.mapper.medical.MedicalKnowledgeItemMapper;
import com.xu.home.mapper.medical.MedicalKnowledgeItemRefMapper;
import com.xu.home.mapper.medical.MedicalKnowledgeSourceMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
public class MedicalKnowledgeQcToolFactory {

    private final MedicalKnowledgeItemMapper itemMapper;
    private final MedicalKnowledgeItemRefMapper itemRefMapper;
    private final MedicalKnowledgeChunkMapper chunkMapper;
    private final MedicalKnowledgeSourceMapper sourceMapper;

    public MedicalKnowledgeQcToolFactory(MedicalKnowledgeItemMapper itemMapper,
                                         MedicalKnowledgeItemRefMapper itemRefMapper,
                                         MedicalKnowledgeChunkMapper chunkMapper,
                                         MedicalKnowledgeSourceMapper sourceMapper) {
        this.itemMapper = itemMapper;
        this.itemRefMapper = itemRefMapper;
        this.chunkMapper = chunkMapper;
        this.sourceMapper = sourceMapper;
    }

    /**
     * 为当前账号创建只读医疗知识检索工具，供千问 Agent 调用。
     */
    public Object create(String account) {
        return new MedicalKnowledgeQcTools(account, itemMapper, itemRefMapper, chunkMapper, sourceMapper);
    }

    private static class MedicalKnowledgeQcTools {

        private final String account;
        private final MedicalKnowledgeItemMapper itemMapper;
        private final MedicalKnowledgeItemRefMapper itemRefMapper;
        private final MedicalKnowledgeChunkMapper chunkMapper;
        private final MedicalKnowledgeSourceMapper sourceMapper;

        private MedicalKnowledgeQcTools(String account,
                                        MedicalKnowledgeItemMapper itemMapper,
                                        MedicalKnowledgeItemRefMapper itemRefMapper,
                                        MedicalKnowledgeChunkMapper chunkMapper,
                                        MedicalKnowledgeSourceMapper sourceMapper) {
            this.account = account;
            this.itemMapper = itemMapper;
            this.itemRefMapper = itemRefMapper;
            this.chunkMapper = chunkMapper;
            this.sourceMapper = sourceMapper;
        }

        /**
         * 根据病例问题检索当前账号可见的医疗知识条目。
         */
        @Tool("根据病例问题检索医疗知识库，返回最相关的知识条目列表，适用于主诉、现病史、诊断一致性和规范性判断。")
        public String searchMedicalKnowledge(@P("病例问题、病例文本或待检索的医学描述") String query,
                                             @P("知识类型，可为空，例如疾病、诊断、治疗") String itemType,
                                             @P("科室，可为空，例如心内科、呼吸内科") String department,
                                             @P("返回数量，建议 1 到 8") Integer topK) {
            int limit = normalizeTopK(topK);
            log.info("千问知识库检索开始, account={}, query={}, itemType={}, department={}, topK={}",
                    account, abbreviate(query, 120), StringUtils.defaultIfBlank(itemType, "-"),
                    StringUtils.defaultIfBlank(department, "-"), limit);
            LambdaQueryWrapper<MedicalKnowledgeItem> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MedicalKnowledgeItem::getIsDelete, 0)
                    .inSql(MedicalKnowledgeItem::getSourceId, ownedSourceSubQuery(account));
            if (StringUtils.isNotBlank(itemType)) {
                wrapper.eq(MedicalKnowledgeItem::getItemType, itemType.trim());
            }
            if (StringUtils.isNotBlank(department)) {
                wrapper.like(MedicalKnowledgeItem::getDepartment, department.trim());
            }

            List<String> searchTerms = extractSearchTerms(query);
            if (!searchTerms.isEmpty()) {
                wrapper.and(w -> {
                    boolean first = true;
                    for (String term : searchTerms) {
                        if (!first) {
                            w.or();
                        }
                        w.like(MedicalKnowledgeItem::getTitle, term)
                                .or().like(MedicalKnowledgeItem::getKeywords, term)
                                .or().like(MedicalKnowledgeItem::getSummary, term)
                                .or().like(MedicalKnowledgeItem::getContent, term);
                        first = false;
                    }
                });
            }

            wrapper.orderByDesc(MedicalKnowledgeItem::getConfidenceScore)
                    .orderByDesc(MedicalKnowledgeItem::getUpdateTime)
                    .last("limit " + limit);

            List<MedicalKnowledgeItem> items = itemMapper.selectList(wrapper);
            List<KnowledgeItemToolResult> result = new ArrayList<>();
            List<Long> itemIds = new ArrayList<>();
            for (MedicalKnowledgeItem item : items) {
                MedicalKnowledgeSource source = sourceMapper.selectById(item.getSourceId());
                itemIds.add(item.getId());
                result.add(new KnowledgeItemToolResult(
                        item.getId(),
                        item.getTitle(),
                        item.getItemType(),
                        item.getDepartment(),
                        item.getKeywords(),
                        StringUtils.defaultIfBlank(item.getSummary(), abbreviate(item.getContent(), 220)),
                        item.getConfidenceScore(),
                        source == null ? null : source.getSourceName()
                ));
            }
            log.info("千问知识库检索完成, account={}, hitCount={}, itemIds={}",
                    account, result.size(), itemIds);
            return JSON.toJSONString(result);
        }

        /**
         * 根据知识条目 ID 拉取原文证据，供模型生成可追溯质控结论。
         */
        @Tool("获取某条医疗知识的原文证据、来源书籍和章节信息，适用于给出质控依据时引用。")
        public String getMedicalKnowledgeEvidence(@P("知识条目 ID") Long itemId,
                                                  @P("返回证据条数，建议 1 到 3") Integer limit) {
            int evidenceLimit = Math.max(1, Math.min(limit == null ? 2 : limit, 3));
            log.info("千问知识证据读取开始, account={}, itemId={}, limit={}", account, itemId, evidenceLimit);
            if (itemId == null) {
                return "[]";
            }

            MedicalKnowledgeItem item = itemMapper.selectById(itemId);
            if (item == null || item.getIsDelete() != null && item.getIsDelete() == 1) {
                return "[]";
            }

            MedicalKnowledgeSource source = sourceMapper.selectById(item.getSourceId());
            if (source == null || source.getIsDelete() != null && source.getIsDelete() == 1
                    || !StringUtils.equals(source.getAccount(), account)) {
                return "[]";
            }

            LambdaQueryWrapper<MedicalKnowledgeItemRef> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MedicalKnowledgeItemRef::getKnowledgeItemId, itemId)
                    .orderByAsc(MedicalKnowledgeItemRef::getSortOrder)
                    .orderByAsc(MedicalKnowledgeItemRef::getId)
                    .last("limit " + evidenceLimit);

            List<MedicalKnowledgeEvidenceToolResult> result = new ArrayList<>();
            for (MedicalKnowledgeItemRef ref : itemRefMapper.selectList(wrapper)) {
                MedicalKnowledgeChunk chunk = chunkMapper.selectById(ref.getChunkId());
                if (chunk == null) {
                    continue;
                }
                result.add(new MedicalKnowledgeEvidenceToolResult(
                        item.getId(),
                        item.getTitle(),
                        source.getSourceName(),
                        chunk.getChapterTitle(),
                        ref.getQuoteText(),
                        abbreviate(chunk.getCleanContent(), 500)
                ));
            }
            log.info("千问知识证据读取完成, account={}, itemId={}, evidenceCount={}", account, itemId, result.size());
            return JSON.toJSONString(result);
        }

        /**
         * 构造当前账号可见来源的子查询，确保工具仅读取本人知识库。
         */
        private String ownedSourceSubQuery(String account) {
            return "select id from medical_knowledge_source where is_delete = 0 and account = '" + account.replace("'", "''") + "'";
        }

        /**
         * 提取检索关键词，避免把整段病例文本原样塞进 like 查询。
         */
        private List<String> extractSearchTerms(String query) {
            if (StringUtils.isBlank(query)) {
                return List.of();
            }
            String normalized = query.replace("：", " ")
                    .replace("，", " ")
                    .replace(",", " ")
                    .replace("。", " ")
                    .replace("；", " ")
                    .replace(";", " ")
                    .replace("\n", " ");
            String[] rawTerms = normalized.split("\\s+");
            Set<String> terms = new LinkedHashSet<>();
            for (String rawTerm : rawTerms) {
                String term = StringUtils.trimToEmpty(rawTerm);
                if (term.length() < 2) {
                    continue;
                }
                if (term.length() > 24) {
                    term = term.substring(0, 24);
                }
                terms.add(term.toLowerCase(Locale.ROOT));
                if (terms.size() >= 6) {
                    break;
                }
            }
            return new ArrayList<>(terms);
        }

        /**
         * 统一限制工具召回条数，避免单次取数过多拉高上下文成本。
         */
        private int normalizeTopK(Integer topK) {
            if (topK == null) {
                return 5;
            }
            return Math.max(1, Math.min(topK, 8));
        }

        /**
         * 缩短长文本，防止工具返回过长内容影响模型上下文。
         */
        private String abbreviate(String value, int maxLength) {
            if (StringUtils.isBlank(value)) {
                return "";
            }
            String normalized = value.replace("\n", " ").trim();
            if (normalized.length() <= maxLength) {
                return normalized;
            }
            return normalized.substring(0, maxLength) + "...";
        }
    }

    private record KnowledgeItemToolResult(Long itemId,
                                           String title,
                                           String itemType,
                                           String department,
                                           String keywords,
                                           String summary,
                                           BigDecimal confidenceScore,
                                           String sourceName) {
    }

    private record MedicalKnowledgeEvidenceToolResult(Long itemId,
                                                      String title,
                                                      String sourceName,
                                                      String chapterTitle,
                                                      String quoteText,
                                                      String cleanContent) {
    }
}
