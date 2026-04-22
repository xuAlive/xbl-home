package com.xu.home.param.blog.vo.bazi;

import lombok.Data;

import java.util.Date;

@Data
public class BaziMarriageHistoryVO {
    private Long id;
    private String personAName;
    private String personBName;
    private String personABaZi;
    private String personBBaZi;
    private String question;
    private String status;
    private Date createTime;
}
