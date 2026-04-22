package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value = "bazi_marriage_record")
public class BaziMarriageRecord implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("account")
    private String account;

    @TableField("person_a_name")
    private String personAName;

    @TableField("person_a_gender")
    private String personAGender;

    @TableField("person_a_calendar_type")
    private String personACalendarType;

    @TableField("person_a_birth_datetime")
    private Date personABirthDatetime;

    @TableField("person_a_input_birth_date")
    private String personAInputBirthDate;

    @TableField("person_a_input_birth_time")
    private String personAInputBirthTime;

    @TableField("person_a_is_leap_month")
    private Integer personAIsLeapMonth;

    @TableField("person_a_year_pillar")
    private String personAYearPillar;

    @TableField("person_a_month_pillar")
    private String personAMonthPillar;

    @TableField("person_a_day_pillar")
    private String personADayPillar;

    @TableField("person_a_hour_pillar")
    private String personAHourPillar;

    @TableField("person_a_ba_zi")
    private String personABaZi;

    @TableField("person_a_zodiac")
    private String personAZodiac;

    @TableField("person_a_shi_chen")
    private String personAShiChen;

    @TableField("person_a_lunar_text")
    private String personALunarText;

    @TableField("person_b_name")
    private String personBName;

    @TableField("person_b_gender")
    private String personBGender;

    @TableField("person_b_calendar_type")
    private String personBCalendarType;

    @TableField("person_b_birth_datetime")
    private Date personBBirthDatetime;

    @TableField("person_b_input_birth_date")
    private String personBInputBirthDate;

    @TableField("person_b_input_birth_time")
    private String personBInputBirthTime;

    @TableField("person_b_is_leap_month")
    private Integer personBIsLeapMonth;

    @TableField("person_b_year_pillar")
    private String personBYearPillar;

    @TableField("person_b_month_pillar")
    private String personBMonthPillar;

    @TableField("person_b_day_pillar")
    private String personBDayPillar;

    @TableField("person_b_hour_pillar")
    private String personBHourPillar;

    @TableField("person_b_ba_zi")
    private String personBBaZi;

    @TableField("person_b_zodiac")
    private String personBZodiac;

    @TableField("person_b_shi_chen")
    private String personBShiChen;

    @TableField("person_b_lunar_text")
    private String personBLunarText;

    @TableField("question")
    private String question;

    @TableField("prompt_text")
    private String promptText;

    @TableField("fortune_content")
    private String fortuneContent;

    @TableField("model_name")
    private String modelName;

    @TableField("status")
    private String status;

    @TableField("error_message")
    private String errorMessage;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
