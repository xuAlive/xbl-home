package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_system_message")
public class SystemMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String creatorAccount;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
