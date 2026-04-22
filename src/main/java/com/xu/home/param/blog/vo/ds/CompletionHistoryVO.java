package com.xu.home.param.blog.vo.ds;

import lombok.Data;

import java.util.Date;

@Data
public class CompletionHistoryVO {
    private Long dialogueId;
    private String content;
    private Date createTime;
}
