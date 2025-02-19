package com.wws.imageplatformback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wws.imageplatformback.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.model.dto.picture.*;
import com.wws.imageplatformback.model.entity.Picture;
import com.wws.imageplatformback.model.entity.Space;
import com.wws.imageplatformback.model.entity.User;
import com.wws.imageplatformback.model.vo.PictureVO;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Lenovo
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-02-05 18:47:57
 */
public interface PictureService extends IService<Picture> {
  /**
   * 上传图片
   *
   * @param inputSource 上传的文件
   * @param pictureUploadRequest 上传的请求
   * @param loginUser 登录用户
   * @return 返回图片信息
   */
  PictureVO uploadPicture(
      Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

  /**
   * 获取图片包装类(单条)
   *
   * @param picture
   * @param request
   * @return
   */
  PictureVO getPictureVO(Picture picture, HttpServletRequest request);

  /**
   * 获取图片包装类（分页）
   *
   * @param picturePage
   * @param request
   * @return
   */
  Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);

  /**
   * 获取查询对象
   *
   * @param pictureQueryRequest
   * @return
   */
  public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

  /**
   * 校验图片
   *
   * @param picture
   */
  public void validPicture(Picture picture);

  /**
   * 处理图片审核
   *
   * @param pictureReviewRequest
   * @param loginUser
   */
  void handlePictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

  /**
   * 填充图片审核参数
   *
   * @param picture
   * @param loginUser
   */
  void fillReviewParams(Picture picture, User loginUser);

  /**
   * 批量创建图片
   *
   * @param pictureUploadByBatchRequest
   * @param loginUser
   * @return 创建的图片数量
   */
  Integer uploadPictureByBatch(
      PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);

  /** 清理图片文件 */
  void clearPictureFile(Picture oldPicture);

  /**
   * 删除图片
   *
   * @param pictureId
   * @param loginUser
   */
  void deletePicture(long pictureId, User loginUser);

  /**
   * 更新图片
   *
   * @param pictureEditRequest
   * @param loginUser
   */
  void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

  /**
   * 校验空间图片权限
   *
   * @param picture
   * @param loginUser
   */
  void checkPictureAuth(Picture picture, User loginUser);

  /** 批量编辑图片 */
  void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

  /**
   * 创建扩图任务
   */
  CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);
}
