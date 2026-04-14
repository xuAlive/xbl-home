package com.xu.home.param.blog.vo.sys;

import lombok.Data;

import java.util.List;

/**
 * 登录地点统计VO（地图 + 饼形图）
 */
@Data
public class LoginLocationStatsVO {
    /**
     * 地点列表（用于地图标点）
     */
    private List<UserLoginVO> locations;

    /**
     * 省份统计（用于饼形图）
     */
    private List<ProvinceStatVO> provinceStats;
}
