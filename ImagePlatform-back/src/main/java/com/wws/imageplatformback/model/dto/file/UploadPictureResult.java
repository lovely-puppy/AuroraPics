package com.wws.imageplatformback.model.dto.file;

import lombok.Data;

/**
 * 上传图片的结果
 */
@Data
public class UploadPictureResult {
    /**
     * 图片的名字
     */
    private String picName;

    /**
     * 图片的 url
     */
    private String url;

    /**
     * 缩略图url
     */
    private String thumbnailUrl;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片的缩放比例
     */
    private Double picScale;

    /**
     * 图片的格式
     */
    private String picFormat;

}
