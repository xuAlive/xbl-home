package com.xu.home.param.blog.deepseek;

import lombok.Data;

/**
 * 多轮对话
 */
@Data
public class Completion {
    /**
     * 角色
     */
    private String role;
    /**
     * 内容
     */
    private String content;
}
