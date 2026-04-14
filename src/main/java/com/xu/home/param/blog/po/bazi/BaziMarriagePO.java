package com.xu.home.param.blog.po.bazi;

import lombok.Data;

@Data
public class BaziMarriagePO {
    private BaziMatchPersonPO personA;
    private BaziMatchPersonPO personB;
    private String question;
}
