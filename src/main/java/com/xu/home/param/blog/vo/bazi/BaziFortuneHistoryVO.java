package com.xu.home.param.blog.vo.bazi;

import lombok.Data;

import java.util.Date;

@Data
public class BaziFortuneHistoryVO {
    private Long id;
    private String gender;
    private String birthDate;
    private String birthTime;
    private Boolean leapMonth;
    private String baZi;
    private String zodiac;
    private String question;
    private String status;
    private Date createTime;
}
