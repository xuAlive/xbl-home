package com.xu.home.param.blog.vo.bazi;

import lombok.Data;

import java.util.Date;

@Data
public class BaziFortuneRecordVO {
    private Long id;
    private String gender;
    private String birthDate;
    private String birthTime;
    private Boolean leapMonth;
    private String solarDate;
    private String solarTime;
    private String question;
    private String yearPillar;
    private String monthPillar;
    private String dayPillar;
    private String hourPillar;
    private String baZi;
    private String zodiac;
    private String shiChen;
    private String lunarText;
    private String fortuneContent;
    private String status;
    private String errorMessage;
    private Date createTime;
}
