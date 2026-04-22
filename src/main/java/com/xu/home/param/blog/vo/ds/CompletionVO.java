package com.xu.home.param.blog.vo.ds;

import com.xu.home.param.blog.deepseek.Completion;
import lombok.Data;

import java.util.Date;

@Data
public class CompletionVO extends Completion {
    private Long dialogueId;
    private Date createTime;
}
