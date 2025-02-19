package com.wws.imageplatformback.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.wws.imageplatformback.config.CosClientConfig;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.manager.CosManager;
import com.wws.imageplatformback.model.dto.file.UploadPictureResult;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

/**
 * 图片上传模板
 */
@Slf4j
public abstract class PictureUploadTemplate {
  @Resource protected CosManager cosManager;

  @Resource protected CosClientConfig cosClientConfig;

  public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
    // 校验图片
    validPicture(inputSource);
    // 图片上传地址
    String uuid = RandomUtil.randomString(16);
    String originFileName = getOriginalFileName(inputSource);
    // 自己拼接文件上传路径,而不是使用原文文件名称,可以增强安全性
    String uploadFileName =
        String.format(
            "%s_%s.%s", DateUtil.formatDate(new Date()), uuid, (FileUtil.getSuffix(originFileName).isEmpty()) ? "webp" : FileUtil.getSuffix(originFileName));
    String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFileName);
    // 解析结果并返回
    File file = null;
    try {
      // 创建临时文件
      file = File.createTempFile(uploadPath, null);
      // 处理文件来源
      processFile(inputSource, file);
      // 上传图片到对象存储
      PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
      // 获取图片信息对象
      ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
      // 获取到图片信息处理结果
      ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
      List<CIObject> objectList = processResults.getObjectList();
      if (CollUtil.isNotEmpty(objectList)) {
        // 获取压缩之后得到的文件信息
        CIObject compressedCiObject = objectList.get(0);
        // 缩略图默认等于压缩图
        CIObject thumbnailCiObject = compressedCiObject;
        // 如果存在缩略图,则获取缩略图
        if (objectList.size() > 1) {
          thumbnailCiObject = objectList.get(1);
        }
        // 封装压缩图的返回效果
        return buildResult(originFileName, compressedCiObject, thumbnailCiObject);
      }
      return buildResult(imageInfo, uploadPath, originFileName, file);
    } catch (Exception e) {
      log.error("file upload error, filepath = {} ", uploadPath, e);
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
    } finally {
      // 清理临时文件
      deleteTmpFile(file);
    }
  }

  /**
   * 封装返回结果
   * @param originFileName 原始文件名
   * @param compressedCiObject 压缩后的对象
   * @return
   */
  private UploadPictureResult buildResult(String originFileName, CIObject compressedCiObject, CIObject thumbnailCiObject) {
    // 计算宽高
    int picWidth = compressedCiObject.getWidth();
    int picHeight = compressedCiObject.getHeight();
    double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
    // 封装返回结果
    UploadPictureResult uploadPictureResult = new UploadPictureResult();
    uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
    uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
    uploadPictureResult.setPicSize(compressedCiObject.getSize().longValue());
    uploadPictureResult.setPicWidth(picWidth);
    uploadPictureResult.setPicHeight(picHeight);
    uploadPictureResult.setPicScale(picScale);
    uploadPictureResult.setPicFormat(compressedCiObject.getFormat());
    uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + "/" + thumbnailCiObject.getKey());
    // 返回可访问的地址
    return uploadPictureResult;
  }

  /**
   *
   * @param imageInfo 对象存储返回的图片信息
   * @param uploadPath
   * @param originFileName
   * @param file
   * @return
   */
  private UploadPictureResult buildResult(ImageInfo imageInfo, String uploadPath, String originFileName, File file) {
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
  }

  /**
   * 处理输入源并生成本地临时文件
   * @param inputSource
   * @param file
   */
  protected abstract void processFile(Object inputSource, File file) throws IOException;
  /**
   * 获取原始文件名称
   * @param inputSource
   * @return
   */
  protected abstract String getOriginalFileName(Object inputSource);

  /**
   * 校验输入源(本地文件或URL)
   * @param inputSource
   */
  protected abstract void validPicture(Object inputSource);

  /**
   * 清理临时文件
   * @param file
   */
   public void deleteTmpFile(File file) {
    if (file != null) {
      // 删除临时文件
      boolean deleteResult = file.delete();
      if (!deleteResult) {
        log.error("file delete error, filepath = {} ", file.getAbsolutePath());
      }
    }
  }
}
