package com.xu.home.service.medical;

import com.xu.home.domain.medical.MedicalKnowledgeSource;
import com.xu.home.param.medical.po.MedicalKnowledgeItemQueryPO;
import com.xu.home.param.medical.vo.MedicalKnowledgeImportResultVO;
import com.xu.home.param.medical.vo.MedicalKnowledgeItemDetailVO;
import com.xu.home.param.medical.vo.MedicalKnowledgePageVO;
import com.xu.home.param.medical.vo.MedicalKnowledgeSourceDetailVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MedicalKnowledgeService {

    /**
     * 从服务端本地文件或目录导入医学书籍。
     *
     * <p>方法会先校验路径是否为空、是否存在、是否位于允许读取的白名单目录内，
     * 然后收集路径下支持的 pdf/txt/docx 文件，为每个文件创建来源记录和导入任务，
     * 最后异步启动解析、清洗、章节聚合和 AI 提取流程。接口本身只返回任务创建结果，
     * 不等待整本书处理完成。</p>
     *
     * @param account 当前登录账号，用于隔离不同用户的来源书籍和知识条目
     * @param localPath 服务端可访问的本地文件路径或目录路径
     * @return 本次导入创建的来源数量、来源 ID 和来源名称
     */
    MedicalKnowledgeImportResultVO importFromLocal(String account, String localPath);

    /**
     * 从浏览器上传文件导入医学书籍。
     *
     * <p>方法会校验上传文件和文件名，确认扩展名属于 pdf/txt/docx 后，
     * 将文件保存到配置的上传目录，再创建来源记录和导入任务，并异步启动后续处理。
     * 上传目录需要在生产环境配置为持久化目录，否则后续重新提取可能找不到原文件。</p>
     *
     * @param account 当前登录账号，用于写入来源归属并限制后续访问范围
     * @param file 前端 multipart/form-data 上传的医学书籍文件
     * @return 本次上传创建的来源 ID、来源名称和文件数量
     */
    MedicalKnowledgeImportResultVO importFromUpload(String account, MultipartFile file);

    /**
     * 对指定来源书籍重新执行解析和知识提取。
     *
     * <p>方法会先校验来源是否属于当前账号，然后创建重提取任务，清空旧章节块、
     * 逻辑删除旧知识条目并删除旧证据引用，重置来源状态，最后异步重新处理原文件。</p>
     *
     * @param account 当前登录账号
     * @param sourceId 要重新提取的来源书籍 ID
     */
    void reextract(String account, Long sourceId);

    /**
     * 查询当前账号下未删除的来源书籍列表。
     *
     * <p>结果按更新时间和 ID 倒序排列，供前端左侧来源列表展示。</p>
     *
     * @param account 当前登录账号
     * @return 当前账号可访问的来源书籍列表
     */
    List<MedicalKnowledgeSource> getSourceList(String account);

    /**
     * 查询单本来源书籍的详情。
     *
     * <p>详情包含来源基础信息、最近任务记录以及最新生成的 2 条知识条目，
     * 用于前端中间区域展示导入进度和最近产出。</p>
     *
     * @param account 当前登录账号
     * @param sourceId 来源书籍 ID
     * @return 来源详情视图对象
     */
    MedicalKnowledgeSourceDetailVO getSourceDetail(String account, Long sourceId);

    /**
     * 查询当前账号下已生成的知识类型列表，可按来源过滤。
     *
     * @param account 当前登录账号
     * @param sourceId 可选来源书籍 ID；为空时查询当前账号所有来源
     * @return 去重后的知识类型列表，供前端筛选下拉使用
     */
    List<String> getItemTypes(String account, Long sourceId);

    /**
     * 分页查询当前账号下的知识条目。
     *
     * <p>查询会限定在当前账号拥有的来源书籍范围内，并支持按来源、知识类型、
     * 科室和关键词过滤。关键词会匹配标题、关键词、摘要和正文。</p>
     *
     * @param account 当前登录账号
     * @param po 前端传入的分页和筛选参数
     * @return 知识条目分页结果，包含 total 和 list
     */
    MedicalKnowledgePageVO<com.xu.home.domain.medical.MedicalKnowledgeItem> getItemPage(String account, MedicalKnowledgeItemQueryPO po);

    /**
     * 查询单条知识的详情和原文证据。
     *
     * <p>方法会先校验知识条目归属，再读取来源书籍和证据引用。
     * 每条证据会补充章节号、章节标题、页码范围、引用片段和清洗后的章节正文，
     * 便于前端抽屉展示可追溯内容。</p>
     *
     * @param account 当前登录账号
     * @param itemId 知识条目 ID
     * @return 知识详情视图对象
     */
    MedicalKnowledgeItemDetailVO getItemDetail(String account, Long itemId);

    /**
     * 逻辑删除当前账号下的知识条目。
     *
     * <p>方法只将条目标记为删除，不物理删除原始章节块；删除成功后会重新统计来源书籍下
     * 的有效知识数量并回写来源表。</p>
     *
     * @param account 当前登录账号
     * @param itemId 要删除的知识条目 ID
     * @return 是否删除成功
     */
    boolean deleteItem(String account, Long itemId);
}
