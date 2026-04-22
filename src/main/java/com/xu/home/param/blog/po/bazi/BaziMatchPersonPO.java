package com.xu.home.param.blog.po.bazi;

import lombok.Data;

@Data
public class BaziMatchPersonPO {
    private String name;
    private Integer birthYear;
    private Integer birthMonth;
    private Integer birthDay;
    private String birthTime;
    private Boolean leapMonth;
    private String calendarType;
    private String gender;
}
