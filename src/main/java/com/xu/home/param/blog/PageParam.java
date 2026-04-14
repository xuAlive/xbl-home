package com.xu.home.param.blog;

import lombok.Data;

@Data
public class PageParam {
    /**
     * 页数
     */
    private Integer page;
    /**
     * 每页大小
     */
    private Integer size;
    /**
     * 总数
     */
    private Integer total;

    public Integer getPage() {
        if (null == page){
            page=1;
        }
        if (page.intValue()<=0){
            page=1;
        }
        return page;
    }

    public Integer getSize() {
        if (null == size){
            size=10;
        }
        return size;
    }
}
