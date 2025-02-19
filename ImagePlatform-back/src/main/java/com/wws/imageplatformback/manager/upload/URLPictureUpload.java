package com.wws.imageplatformback.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.utils.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class URLPictureUpload extends PictureUploadTemplate{
    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 校验非空
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        // 校验 url 格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件地址格式不正确");
        }
        // 校验 url 协议
        ThrowUtils.throwIf(
                !fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"),
                ErrorCode.PARAMS_ERROR,
                "仅支持HTTP或HTTPS协议的文件地址");
        // 发送 HEAD 请求,校验文件是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回,无需执行其它判断
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }

            // 文件存在, 文件类型校验
            String contentType = httpResponse.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOWED_IMAGE_TYPES =
                        Arrays.asList(
                                "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp", "image/jpg");
                ThrowUtils.throwIf(
                        !ALLOWED_IMAGE_TYPES.contains(contentType), ErrorCode.PARAMS_ERROR, "图片格式不支持");
            }
            // 文件大小校验
            String contentLength = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLength)) {
                try {
                    long size = NumberUtil.parseLong(contentLength);
                    final long ONE_MB = 1024 * 1024;
                    ThrowUtils.throwIf(size > 2 * ONE_MB, ErrorCode.PARAMS_ERROR, "图片大小不能超过2MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式不正确");
                }
            }
        } finally {
            // 释放资源
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
    }

    @Override
    protected String getOriginalFileName(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        String fileUrl = (String) inputSource;
        // 下载文件到临时目录
        HttpUtil.downloadFile(fileUrl, file);
    }
}
