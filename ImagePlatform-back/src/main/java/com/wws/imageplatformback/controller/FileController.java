package com.wws.imageplatformback.controller;

import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.wws.imageplatformback.annotation.AuthCheck;
import com.wws.imageplatformback.common.BaseResponse;
import com.wws.imageplatformback.common.ResultUtils;
import com.wws.imageplatformback.constant.UserConstant;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.manager.CosManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {
    @Resource
    private CosManager cosManager;

    /**
     * 测试上传文件
     * @param multipartFile
     * @return
     */
    @PostMapping("/test/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        //文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        //创建临时文件
        File file = null;
        try {
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            // 返回可访问的地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = {} ", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {} ", filepath);
                }
            }
        }
    }

    @GetMapping("/test/download")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public void testDownloadFile(@RequestParam String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInputStream = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInputStream = cosObject.getObjectContent();
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + filepath);
            // 写入响应流
            response.getOutputStream().write(IOUtils.toByteArray(cosObjectInputStream));
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = {} ", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件下载失败");
        } finally {
            // 释放流
            if (cosObjectInputStream != null) {
                cosObjectInputStream.close();
            }
        }
    }
}
