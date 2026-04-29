package com.xu.home.service.medical.impl;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicalKnowledgeServiceImplTest {

    private final MedicalKnowledgeServiceImpl service = new MedicalKnowledgeServiceImpl(
            null, null, null, null, null, null, null, null
    );

    @Test
    void shouldRecognizePdfCatalogLinesWithChineseIndexAndDotLeaders() throws Exception {
        assertTrue(invokeBoolean("isDirectoryLine", "一、适应证................ 30"));
        assertTrue(invokeBoolean("isDirectoryLine", "四、慢性疼痛........................ 50"));
        assertTrue(invokeBoolean("isDirectoryLine", "第七章 围手术期处理........................ 120"));
        assertTrue(invokeBoolean("isDirectoryLine", "录 四、呼吸性酸中毒........................25 五、混合性酸碱平衡失调........................"));
    }

    @Test
    void shouldNotTreatNormalSectionTitleAsCatalogLine() throws Exception {
        assertFalse(invokeBoolean("isDirectoryLine", "一、适应证"));
        assertFalse(invokeBoolean("isDirectoryLine", "第七章 围手术期处理"));
    }

    @Test
    void shouldDropCatalogAndPrefaceLikeContentBeforeAiExtraction() throws Exception {
        String content = """
                目录
                一、适应证................ 30
                二、全身麻醉诱导................45
                录 四、呼吸性酸中毒........................25 五、混合性酸碱平衡失调........................
                绪言
                第七章围手术期处理由李先强编写;第八章外科患者的营养代谢、第十一章第三节胸部损伤由韩俊录
                正文内容应该保留，用于描述麻醉处理原则。
                """;

        String cleaned = invokeString("cleanChapterContent", content);

        assertFalse(cleaned.contains("适应证................ 30"));
        assertFalse(cleaned.contains("呼吸性酸中毒"));
        assertFalse(cleaned.contains("编写"));
        assertTrue(cleaned.contains("正文内容应该保留"));
    }

    @Test
    void shouldSkipXuyanAsPrefaceSection() throws Exception {
        assertTrue(invokeBoolean("shouldSkipSectionTitle", "绪言"));
        assertTrue(invokeBoolean("looksLikePrefaceContent", "绪言", "绪言\n本书修订说明"));
    }

    private boolean invokeBoolean(String methodName, String value) throws Exception {
        Method method = MedicalKnowledgeServiceImpl.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, value);
    }

    private boolean invokeBoolean(String methodName, String first, String second) throws Exception {
        Method method = MedicalKnowledgeServiceImpl.class.getDeclaredMethod(methodName, String.class, String.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, first, second);
    }

    private String invokeString(String methodName, String value) throws Exception {
        Method method = MedicalKnowledgeServiceImpl.class.getDeclaredMethod(methodName, String.class);
        method.setAccessible(true);
        return (String) method.invoke(service, value);
    }
}
