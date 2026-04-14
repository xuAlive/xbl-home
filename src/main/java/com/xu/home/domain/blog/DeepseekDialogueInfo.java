package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * Dee-seek对话信息
 * @TableName deepseek_dialogue_info
 */
@TableName(value ="deepseek_dialogue_info")
@Data
public class DeepseekDialogueInfo implements Serializable {
    /**
     * 
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 对话id
     */
    @TableField(value = "dialogue_id")
    private Long dialogueId;

    /**
     * 角色
     */
    @TableField(value = "role")
    private String role;

    /**
     * 内容
     */
    @TableField(value = "content")
    private String content;

    /**
     * 用户账号
     */
    @TableField(value = "account")
    private String account;

    /**
     * 创建时间
     */
    @TableField(value = "creat_time")
    private Date creatTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}