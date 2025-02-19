package com.wws.imageplatformback.common;

import lombok.Data;

import java.io.Serializable;

/**
 * @author 汪文松
 * @date 2025-01-18 0:10
 * 通用删除请求包装类
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
