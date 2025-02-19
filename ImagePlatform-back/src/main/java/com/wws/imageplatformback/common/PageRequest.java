package com.wws.imageplatformback.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 汪文松
 * @date 2025-01-18 0:09
 * 通用分页请求类
 */
@Data
public class PageRequest {
    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";
}
