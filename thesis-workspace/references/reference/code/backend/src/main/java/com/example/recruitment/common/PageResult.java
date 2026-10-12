package com.example.recruitment.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 统一分页返回格式
 */
@Data
public class PageResult<T> {

    private List<T> records;
    private long total;
    private long page;
    private long size;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.records = page.getRecords();
        r.total = page.getTotal();
        r.page = page.getCurrent();
        r.size = page.getSize();
        return r;
    }

    public static <T> PageResult<T> of(List<T> records, long total, long page, long size) {
        PageResult<T> r = new PageResult<>();
        r.records = records;
        r.total = total;
        r.page = page;
        r.size = size;
        return r;
    }

    /**
     * 从原始分页对象映射为指定类型的分页结果
     */
    public static <T> PageResult<T> map(IPage<?> page, List<T> records) {
        PageResult<T> r = new PageResult<>();
        r.records = records;
        r.total = page.getTotal();
        r.page = page.getCurrent();
        r.size = page.getSize();
        return r;
    }
}
