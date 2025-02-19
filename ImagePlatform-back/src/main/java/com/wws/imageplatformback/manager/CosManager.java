package com.wws.imageplatformback.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.wws.imageplatformback.config.CosClientConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 腾讯云 COS 管理器
 */
@Component
public class CosManager {
  @Resource private CosClientConfig cosClientConfig;

  @Resource private COSClient cosClient;

  /**
   * 将本地文件上传到 COS
   *
   * @param key 唯一键
   * @param file 文件
   * @return
   */
  public PutObjectResult putObject(String key, File file)
      throws CosClientException, CosServiceException {
    PutObjectRequest putObjectRequest =
        new PutObjectRequest(cosClientConfig.getBucket(), key, file);
    return cosClient.putObject(putObjectRequest);
  }

  /**
   * 删除对象
   * @param key
   * @return
   */
  public void deleteObject(String key) {
    cosClient.deleteObject(cosClientConfig.getBucket(), key);
  }

  /**
   * 下载对象
   *
   * @param key 唯一键
   */
  public COSObject getObject(String key) {
    GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
    return cosClient.getObject(getObjectRequest);
  }

  /** 上传并解析图片 */
  public PutObjectResult putPictureObject(String key, File file)
      throws CosClientException, CosServiceException {
    PutObjectRequest putObjectRequest =
        new PutObjectRequest(cosClientConfig.getBucket(), key, file);
    // 对图片进行处理 (获取图片基本信息也被视作为一种图片的处理)
    PicOperations picOperations = new PicOperations();
    // 1 表示返回原图信息
    picOperations.setIsPicInfo(1);

    List<PicOperations.Rule> ruleList = new ArrayList<>();
    // 图片压缩（转成 webp 格式）
    String webpKey = FileUtil.mainName(key) + ".webp";
    PicOperations.Rule compressRule = new PicOperations.Rule();
    compressRule.setBucket(cosClientConfig.getBucket());
    compressRule.setFileId(webpKey);
    compressRule.setRule("imageMogr2/format/webp/quality/75");
    ruleList.add(compressRule);
    // 缩略图处理, 仅对 > 20KB 的图片进行处理
    if (file.length() > 20 * 1024) {
      PicOperations.Rule thumbnailRule = new PicOperations.Rule();
      thumbnailRule.setBucket(cosClientConfig.getBucket());
      String thumbnailKey = FileUtil.mainName(key) + "_thumbnail." + FileUtil.getSuffix(key);
      thumbnailRule.setFileId(thumbnailKey);
      // 缩放规则 /thumbnail/<Width>x<Height>> (如果大于原图宽高则不处理)
      thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>", 256, 256));
      ruleList.add(thumbnailRule);
    }

    // 构造处理函数
    picOperations.setRules(ruleList);
    putObjectRequest.setPicOperations(picOperations);
    return cosClient.putObject(putObjectRequest);
  }
}
