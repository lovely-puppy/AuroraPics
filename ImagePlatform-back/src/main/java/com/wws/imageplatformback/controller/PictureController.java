package com.wws.imageplatformback.controller;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.wws.imageplatformback.annotation.AuthCheck;
import com.wws.imageplatformback.api.aliyunai.AliYunAiApi;
import com.wws.imageplatformback.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wws.imageplatformback.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.wws.imageplatformback.api.imagesearch.ImageSearchApiFacade;
import com.wws.imageplatformback.api.imagesearch.model.ImageSearchResult;
import com.wws.imageplatformback.common.BaseResponse;
import com.wws.imageplatformback.common.DeleteRequest;
import com.wws.imageplatformback.common.ResultUtils;
import com.wws.imageplatformback.constant.UserConstant;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.manager.auth.SpaceUserAuthManager;
import com.wws.imageplatformback.manager.auth.StpKit;
import com.wws.imageplatformback.manager.auth.annotation.SaSpaceCheckPermission;
import com.wws.imageplatformback.manager.auth.model.SpaceUserPermissionConstant;
import com.wws.imageplatformback.model.dto.picture.*;
import com.wws.imageplatformback.model.entity.Picture;
import com.wws.imageplatformback.model.entity.Space;
import com.wws.imageplatformback.model.entity.User;
import com.wws.imageplatformback.model.enums.PictureReviewStatusEnum;
import com.wws.imageplatformback.model.vo.PictureTagCategory;
import com.wws.imageplatformback.model.vo.PictureVO;
import com.wws.imageplatformback.service.PictureService;
import com.wws.imageplatformback.service.SpaceService;
import com.wws.imageplatformback.service.UserService;
import com.wws.imageplatformback.utils.ThrowUtils;
import io.swagger.annotations.ApiOperation;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequestMapping("/picture")
public class PictureController {
  /** 本地缓存 */
  private final Cache<String, String> LOCAL_CACHE =
      Caffeine.newBuilder()
          .initialCapacity(1024)
          .maximumSize(10000L) // 最大一万条
          // 缓存 5 分钟移除
          .expireAfterWrite(5L, TimeUnit.MINUTES)
          .build();

  @Resource private UserService userService;
  @Resource private PictureService pictureService;
  @Resource private StringRedisTemplate stringRedisTemplate;
  @Resource private SpaceService spaceService;
  @Autowired private AliYunAiApi aliYunAiApi;
  @Autowired private SpaceUserAuthManager spaceUserAuthManager;

  /**
   * 上传图片 （可重新上传）
   *
   * @param multipartFile
   * @param pictureUploadRequest
   * @param request
   * @return
   */
  @PostMapping("/upload")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
  //    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<PictureVO> uploadPicture(
      @RequestPart("file") MultipartFile multipartFile,
      PictureUploadRequest pictureUploadRequest,
      HttpServletRequest request) {
    User loginUser = userService.getLoginUser(request);
    PictureVO pictureVO =
        pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
    return ResultUtils.success(pictureVO);
  }

  /**
   * 通过url上传图片
   *
   * @param pictureUploadRequest
   * @param request
   * @return
   */
  @PostMapping("/upload/url")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
  public BaseResponse<PictureVO> uploadPictureByUrl(
      @RequestBody PictureUploadRequest pictureUploadRequest, HttpServletRequest request) {
    User loginUser = userService.getLoginUser(request);
    String fileUrl = pictureUploadRequest.getFileUrl();
    PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
    return ResultUtils.success(pictureVO);
  }

  /** 删除图片 */
  @PostMapping("/delete")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
  public BaseResponse<Boolean> deletePicture(
      @RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
    if (deleteRequest == null || deleteRequest.getId() <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    User loginUser = userService.getLoginUser(request);
    Long pictureId = deleteRequest.getId();
    pictureService.deletePicture(pictureId, loginUser);
    return ResultUtils.success(true);
  }

  /** 更新图片（仅管理员可用） */
  @PostMapping("/update")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Boolean> updatePicture(
      @RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
    if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    // 将实体类和 DTO 进行转换
    Picture picture = new Picture();
    BeanUtils.copyProperties(pictureUpdateRequest, picture);
    // 注意将 list 转为 string
    picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
    // 数据校验
    pictureService.validPicture(picture);
    // 判断是否存在
    long id = pictureUpdateRequest.getId();
    Picture oldPicture = pictureService.getById(id);
    ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
    // 补充审核参数
    User loginUser = userService.getLoginUser(request);
    pictureService.fillReviewParams(picture, loginUser);
    // 操作数据库
    boolean result = pictureService.updateById(picture);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    return ResultUtils.success(true);
  }

  /** 根据 id 获取图片（仅管理员可用） */
  @GetMapping("/get")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
    ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
    // 查询数据库
    Picture picture = pictureService.getById(id);
    ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
    // 获取封装类
    return ResultUtils.success(picture);
  }

  /** 根据 id 获取图片（封装类） */
  @GetMapping("/get/vo")
  public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
    ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
    // 查询数据库
    Picture picture = pictureService.getById(id);
    ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
    // 校验空间权限
    Long spaceId = picture.getSpaceId();
    Space space = null;
    if (spaceId != null) {
      boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
      ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
      // User loginUser = userService.getLoginUser(request);
      // 已经改为使用注解鉴权
      // pictureService.checkPictureAuth(picture, loginUser);
      space = spaceService.getById(spaceId);
      ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
    }
    User loginUser = userService.getLoginUser(request);
    List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
    PictureVO pictureVO = pictureService.getPictureVO(picture, request);
    pictureVO.setPermissionList(permissionList);
    // 获取封装类
    return ResultUtils.success(pictureVO);
  }

  /** 分页获取图片列表（仅管理员可用） */
  @PostMapping("/list/page")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Page<Picture>> listPictureByPage(
      @RequestBody PictureQueryRequest pictureQueryRequest) {
    long current = pictureQueryRequest.getCurrent();
    long size = pictureQueryRequest.getPageSize();
    // 查询数据库
    Page<Picture> picturePage =
        pictureService.page(
            new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
    return ResultUtils.success(picturePage);
  }

  /** 分页获取图片列表（封装类） */
  @PostMapping("/list/page/vo")
  public BaseResponse<Page<PictureVO>> listPictureVOByPage(
      @RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
    long current = pictureQueryRequest.getCurrent();
    long size = pictureQueryRequest.getPageSize();
    // 限制爬虫
    ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
    // 空间权限校验
    Long spaceId = pictureQueryRequest.getSpaceId();
    if (spaceId == null) {
      // 公开图库
      // 普通用户只能看到审核通过的图片
      pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.REVIEWED.getValue());
      pictureQueryRequest.setNullSpaceId(true);
    } else {
      boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
      ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
      //      // 私有空间
      //      User loginUser = userService.getLoginUser(request);
      //      Space space = spaceService.getById(spaceId);
      //      ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
      //      if (!loginUser.getId().equals(space.getUserId())) {
      //        // 不是空间主人，没有权限看到空间主人创建的图片
      //        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限查看该空间图片");
      //      }
    }
    // 查询数据库
    Page<Picture> picturePage =
        pictureService.page(
            new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
    // 获取封装类
    return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
  }

  //  /** 分页获取图片列表（封装类， 有缓存） */
  //  @PostMapping("/list/page/vo/cache")
  //  public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(
  //      @RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
  //    long current = pictureQueryRequest.getCurrent();
  //    long size = pictureQueryRequest.getPageSize();
  //    // 限制爬虫
  //    ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
  //    // 普通用户只能看到审核通过的图片
  //    pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.REVIEWED.getValue());
  //
  //    // 查询缓存，缓存中没有, 再查询数据库
  //    // 构建缓存的key
  //    String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
  //    String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
  //    // String redisKey = String.format("wwspicture:listPictureVOByPage:%s", hashKey);
  //    String cacheKey = String.format("listPictureVOByPage:%s", hashKey);
  //    // 操作redis, 从缓存中查询
  //    // String redisValue = stringRedisTemplate.opsForValue().get(redisKey);
  //    // 操作caffeine, 从本地缓存中查询
  //    String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
  //    if (cachedValue != null) {
  //      // 缓存命中，保存结果
  //      Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
  //      return ResultUtils.success(cachedPage);
  //    }
  //
  //    // 查询数据库
  //    Page<Picture> picturePage =
  //        pictureService.page(
  //            new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
  //    Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
  //    // 存入redis 缓存
  //    String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
  //    // 设置缓存过期时间 5-10分钟, 防止缓存雪崩
  //    //int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
  //    // stringRedisTemplate.opsForValue().set(redisKey, cacheValue, cacheExpireTime,
  // TimeUnit.SECONDS);
  //    // 存入caffeine 缓存
  //    LOCAL_CACHE.put(cacheKey, cacheValue);
  //    // 获取封装类
  //    return ResultUtils.success(pictureVOPage);
  //  }
  /** 分页获取图片列表（封装类， 有缓存） 多级缓存 */
  @Deprecated
  @PostMapping("/list/page/vo/cache")
  public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(
      @RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
    long current = pictureQueryRequest.getCurrent();
    long size = pictureQueryRequest.getPageSize();
    // 限制爬虫
    ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
    // 普通用户只能看到审核通过的图片
    pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.REVIEWED.getValue());

    // 查询缓存，缓存中没有, 再查询数据库
    // 构建缓存的key
    String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
    String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
    String cacheKey = String.format("wwspicture:listPictureVOByPage:%s", hashKey);
    // 1.操作caffeine, 从本地缓存中查询
    String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
    if (cachedValue != null) {
      // 缓存命中，返回结果
      Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
      return ResultUtils.success(cachedPage);
    }
    // 2.本地缓存未命中，操作redis, 从缓存中查询
    cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
    if (cachedValue != null) {
      // 缓存命中，保存结果, 并更新本地缓存
      LOCAL_CACHE.put(cacheKey, cachedValue);
      Page<PictureVO> cachedPage = JSONUtil.toBean(cachedValue, Page.class);
      return ResultUtils.success(cachedPage);
    }
    // 3.查询数据库
    Page<Picture> picturePage =
        pictureService.page(
            new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
    Page<PictureVO> pictureVOPage = pictureService.getPictureVOPage(picturePage, request);
    // 4.更新缓存
    // 更新redis 缓存
    String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
    // 设置缓存过期时间 5-10分钟, 防止缓存雪崩
    int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
    stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
    // 更新caffeine 缓存
    LOCAL_CACHE.put(cacheKey, cacheValue);
    // 获取封装类
    return ResultUtils.success(pictureVOPage);
  }

  /** 编辑图片（给用户使用） */
  @PostMapping("/edit")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
  public BaseResponse<Boolean> editPicture(
      @RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
    if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
    User loginUser = userService.getLoginUser(request);
    pictureService.editPicture(pictureEditRequest, loginUser);
    return ResultUtils.success(true);
  }

  @GetMapping("/tag_category")
  public BaseResponse<PictureTagCategory> listPictureTagCategory() {
    PictureTagCategory pictureTagCategory = new PictureTagCategory();
    List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "动漫", "校园", "美女", "热血");
    List<String> categoryList = Arrays.asList("模板", "动漫", "表情包", "素材", "海报");
    pictureTagCategory.setTagList(tagList);
    pictureTagCategory.setCategoryList(categoryList);
    return ResultUtils.success(pictureTagCategory);
  }

  /**
   * 审核图片
   *
   * @param pictureReviewRequest
   * @param request
   * @return
   */
  @PostMapping("/review")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Boolean> handlePictureReview(
      @RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {
    ThrowUtils.throwIf(
        pictureReviewRequest == null || pictureReviewRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    pictureService.handlePictureReview(pictureReviewRequest, loginUser);
    return ResultUtils.success(true);
  }

  /** 批量抓取并创建图片 */
  @PostMapping("/upload/batch")
  @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
  public BaseResponse<Integer> uploadPictureByBatch(
      @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    Integer uploadCount =
        pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
    return ResultUtils.success(uploadCount);
  }

  /** 以图搜图（关键词） */
  @ApiOperation(value = "以图搜图")
  @PostMapping("/search/picture")
  public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(
      @RequestBody SearchPictureByPictureRequest requestParam) {
    ThrowUtils.throwIf(ObjectUtil.isNull(requestParam), ErrorCode.PARAMS_ERROR);
    Long pictureId = requestParam.getPictureId();
    ThrowUtils.throwIf(ObjectUtil.isNull(pictureId) || pictureId <= 0, ErrorCode.PARAMS_ERROR);
    Picture oldPicture = pictureService.getById(pictureId);
    ThrowUtils.throwIf(ObjectUtil.isNull(oldPicture), ErrorCode.NOT_FOUND_ERROR);
    List<ImageSearchResult> resultList = ImageSearchApiFacade.searchImage(oldPicture.getName());
    return ResultUtils.success(resultList);
  }

  /** 批量编辑图片 */
  @PostMapping("/edit/batch")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
  public BaseResponse<Boolean> editPictureByBatch(
      @RequestBody PictureEditByBatchRequest pictureEditByBatchRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
    return ResultUtils.success(true);
  }

  /** 创建 AI 扩图任务 */
  @PostMapping("/out_painting/create_task")
  @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
  public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
      @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(
        createPictureOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
    ThrowUtils.throwIf(
        createPictureOutPaintingTaskRequest.getPictureId() == null,
        ErrorCode.PARAMS_ERROR,
        "图片ID不能为空");
    User loginUser = userService.getLoginUser(request);
    CreateOutPaintingTaskResponse response =
        pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
    return ResultUtils.success(response);
  }

  /** 查询 AI 扩图任务 */
  @GetMapping("/out_painting/get_task")
  public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
    ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "任务ID不能为空");
    GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
    return ResultUtils.success(task);
  }
}
