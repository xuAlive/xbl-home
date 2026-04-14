package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName(value = "bazi_fortune_record")
public class BaziFortuneRecord implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("account")
    private String account;

    @TableField("gender")
    private String gender;

    @TableField("birth_datetime")
    private Date birthDatetime;

    @TableField("input_birth_date")
    private String inputBirthDate;

    @TableField("input_birth_time")
    private String inputBirthTime;

    @TableField("is_leap_month")
    private Integer isLeapMonth;

    @TableField("question")
    private String question;

    @TableField("year_pillar")
    private String yearPillar;

    @TableField("month_pillar")
    private String monthPillar;

    @TableField("day_pillar")
    private String dayPillar;

    @TableField("hour_pillar")
    private String hourPillar;

    @TableField("ba_zi")
    private String baZi;

    @TableField("zodiac")
    private String zodiac;

    @TableField("shi_chen")
    private String shiChen;

    @TableField("lunar_text")
    private String lunarText;

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
