package com.xu.home.param.blog.deepseek;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * 查看余额
 */
@Data
public class BalanceInfo {

    @JSONField(name = "is_available")
    private Boolean isAvailable;

    @JSONField(name = "balance_infos")
    private List<Balance> balanceInfos;

}
