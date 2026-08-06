package com.typenglish.common;

import lombok.Data;
import java.util.List;

/**
 * 分页响应
 */
@Data
public class PageResult<T> {

    private List<T> items;
    private long total;
    private int page;
    private int size;

    public PageResult(List<T> items, long total, int page, int size) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.size = size;
    }
}
