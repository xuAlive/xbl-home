package com.xu.home.param.blog.po.deepseek;

import lombok.Data;

import java.io.Serializable;

/**
 * Dee-seek对话信息
 * @TableName deepseek_dialogue_info
 */
@Data
public class DialogueInfoPO implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 对话id
     */
    private Long dialogueId;

    /**
     * 角色
     */
    private String role;

    /**
     * 内容
     */
    private String content;

    /**
     * 用户账号
     */
    private String account;

}