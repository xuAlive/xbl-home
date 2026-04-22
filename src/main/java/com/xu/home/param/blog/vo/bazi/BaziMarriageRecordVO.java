package com.xu.home.param.blog.vo.bazi;

import lombok.Data;

import java.util.Date;

@Data
public class BaziMarriageRecordVO {
    private Long id;
    private BaziMarriagePersonVO personA;
    private BaziMarriagePersonVO personB;
    private String question;
    private String fortuneContent;
    private String status;
    private String errorMessage;
    private Date createTime;
}
