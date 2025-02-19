package com.wws.imageplatformback.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.model.dto.file.UploadPictureResult;
import com.wws.imageplatformback.utils.ThrowUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

@Service
public class FilePictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }

    @Override
    protected String getOriginalFileName(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "图片不能为空");
        // 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024;
        ThrowUtils.throwIf(fileSize > ONE_MB * 2, ErrorCode.PARAMS_ERROR, "图片大小不能超过2MB");
        // 校验文件后缀
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表
        final List<String> allowSuffixList = Arrays.asList("jpg", "png", "jpeg", "bmp", "gif", "webp");
        ThrowUtils.throwIf(!allowSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "图片格式不支持");
    }
}
