package com.wws.imageplatformback.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.wws.imageplatformback.config.CosClientConfig;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.model.dto.file.UploadPictureResult;
import com.wws.imageplatformback.utils.ThrowUtils;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@Component
@Deprecated
public class FileManager {
  @Resource private CosManager cosManager;

  @Resource private CosClientConfig cosClientConfig;

  public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
    // 校验图片
    validPicture(multipartFile);
    // 图片上传地址
    String uuid = RandomUtil.randomString(16);
    String originFileName = multipartFile.getOriginalFilename();
    // 自己拼接文件上传路径,而不是使用原文文件名称,可以增强安全性
    String uploadFileName =
        String.format(
            "%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originFileName));
    String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
    // 解析结果并返回
    File file = null;
    try {
      file = File.createTempFile(uploadPath, null);
      multipartFile.transferTo(file);
      PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
      // 获取图片信息对象
      ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
      // 计算宽高
      int picWidth = imageInfo.getWidth();
      int picHeight = imageInfo.getHeight();
      double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
      // 封装返回结果
      UploadPictureResult uploadPictureResult = new UploadPictureResult();
      uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
      uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
      uploadPictureResult.setPicSize(FileUtil.size(file));
      uploadPictureResult.setPicWidth(picWidth);
      uploadPictureResult.setPicHeight(picHeight);
      uploadPictureResult.setPicScale(picScale);
      uploadPictureResult.setPicFormat(imageInfo.getFormat());
      // 返回可访问的地址
      return uploadPictureResult;
    } catch (Exception e) {
      log.error("file upload error, filepath = {} ", uploadPath, e);
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
    } finally {
      // 清理临时文件
      deleteTmpFile(file);
    }
  }

  private void deleteTmpFile(File file) {
    if (file != null) {
      // 删除临时文件
      boolean deleteResult = file.delete();
      if (!deleteResult) {
        log.error("file delete error, filepath = {} ", file.getAbsolutePath());
      }
    }
  }

  /**
   * 校验图片
   *
   * @param multipartFile
   * @return
   */
  private void validPicture(MultipartFile multipartFile) {
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

  /**
   * 通过 url 上传图片
   */
  public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
    // 校验图片
    // todo
    validPicture(fileUrl);
    // 图片上传地址
    String uuid = RandomUtil.randomString(16);
    // todo
    String originalFileName = FileUtil.mainName(fileUrl);
    // 自己拼接文件上传路径,而不是使用原文文件名称,可以增强安全性
    String uploadFileName =
        String.format(
            "%s_%s.%s",
            DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originalFileName));
    String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
    // 解析结果并返回
    File file = null;
    try {
      file = File.createTempFile(uploadPath, null);
      // todo
      HttpUtil.downloadFile(fileUrl, file);
      PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
      // 获取图片信息对象
      ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
      // 计算宽高
      int picWidth = imageInfo.getWidth();
      int picHeight = imageInfo.getHeight();
      double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
      // 封装返回结果
      UploadPictureResult uploadPictureResult = new UploadPictureResult();
      uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
      uploadPictureResult.setPicName(FileUtil.mainName(originalFileName));
      uploadPictureResult.setPicSize(FileUtil.size(file));
      uploadPictureResult.setPicWidth(picWidth);
      uploadPictureResult.setPicHeight(picHeight);
      uploadPictureResult.setPicScale(picScale);
      uploadPictureResult.setPicFormat(imageInfo.getFormat());
      // 返回可访问的地址
      return uploadPictureResult;
    } catch (Exception e) {
      log.error("file upload error, filepath = {} ", uploadPath, e);
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
    } finally {
      // 清理临时文件
      deleteTmpFile(file);
    }
  }

  /** 根据url校验文件 */
  public void validPicture(String fileUrl) {
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
}
