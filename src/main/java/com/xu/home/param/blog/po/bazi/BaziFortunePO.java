package com.xu.home.param.blog.po.bazi;

import lombok.Data;

@Data
public class BaziFortunePO {
    private String name;
    private Integer birthYear;
    private Integer birthMonth;
    private Integer birthDay;
    private String birthTime;
    private Boolean leapMonth;
    private String calendarType;
    private String gender;
    private String question;
}
