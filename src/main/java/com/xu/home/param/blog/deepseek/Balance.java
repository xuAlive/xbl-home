package com.xu.home.param.blog.deepseek;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class Balance {
    /**
     * Possible values: [CNY, USD]
     * 货币，人民币或美元
     */
    private String currency;

    /**
     * 总的可用余额，包括赠金和充值余额
     */
    @JSONField(name = "total_balance")
    private String totalBalance;

    /**
     * 未过期的赠金余额
     */
    @JSONField(name = "granted_balance")
    private String grantedBalance;

    /**
     * 充值余额
     */
    @JSONField(name = "topped_up_balance")
    private String toppedUpBalance;
}
