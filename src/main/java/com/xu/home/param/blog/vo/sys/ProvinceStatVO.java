package com.xu.home.param.blog.vo.sys;

import lombok.Data;

/**
 * 省份登录统计VO
 */
@Data
public class ProvinceStatVO {
    /**
     * 省份名称
     */
    private String province;

    /**
     * 登录次数
     */
    private Integer count;
}
