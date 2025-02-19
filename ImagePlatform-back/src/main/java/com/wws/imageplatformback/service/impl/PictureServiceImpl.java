package com.wws.imageplatformback.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wws.imageplatformback.api.aliyunai.AliYunAiApi;
import com.wws.imageplatformback.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.wws.imageplatformback.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.manager.CosManager;
import com.wws.imageplatformback.manager.FileManager;
import com.wws.imageplatformback.manager.upload.FilePictureUpload;
import com.wws.imageplatformback.manager.upload.PictureUploadTemplate;
import com.wws.imageplatformback.manager.upload.URLPictureUpload;
import com.wws.imageplatformback.mapper.PictureMapper;
import com.wws.imageplatformback.model.dto.file.UploadPictureResult;
import com.wws.imageplatformback.model.dto.picture.*;
import com.wws.imageplatformback.model.entity.Picture;
import com.wws.imageplatformback.model.entity.Space;
import com.wws.imageplatformback.model.entity.User;
import com.wws.imageplatformback.model.enums.PictureReviewStatusEnum;
import com.wws.imageplatformback.model.vo.PictureVO;
import com.wws.imageplatformback.model.vo.UserVO;
import com.wws.imageplatformback.service.PictureService;
import com.wws.imageplatformback.service.SpaceService;
import com.wws.imageplatformback.service.UserService;
import com.wws.imageplatformback.utils.ThrowUtils;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Lenovo
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-05 18:47:57
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService {

  @Resource private FileManager fileManager;
  @Resource private UserService userService;
  @Resource private FilePictureUpload filePictureUpload;
  @Resource private URLPictureUpload urlPictureUpload;
  @Autowired private CosManager cosManager;
  @Resource private SpaceService spaceService;
  @Resource private TransactionTemplate transactionTemplate;
  @Resource private AliYunAiApi aliYunAiApi;

  @Override
  public PictureVO uploadPicture(
      Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
    // 校验参数
    ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
    // 校验空间是否存在
    Long spaceId = pictureUploadRequest.getSpaceId();
    if (spaceId != null) {
      Space space = spaceService.getById(spaceId);
      ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
      // 改为使用统一的权限校验
      //      // 校验是否有权限
      //      if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
      //        throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
      //      }
      // 校验额度
      if (space.getTotalCount() >= space.getMaxCount()) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数已满");
      }
      if (space.getTotalSize() >= space.getMaxSize()) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小已满");
      }
    }

    // 判断是新增还是删除
    Long pictureId = null;
    if (pictureUploadRequest != null) {
      pictureId = pictureUploadRequest.getId();
    }
    // 如果是更新,判断图片是否存在
    if (pictureId != null) {
      Picture oldPicture = getById(pictureId);
      ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
      //      // 仅本人或管理员可以更新图片
      // 改为使用统一的权限校验
      //      if (!loginUser.getId().equals(oldPicture.getUserId()) &&
      // !userService.isAdmin(loginUser)) {
      //        throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
      //      }
      // 校验空间是否一致
      // 没传 spaceId, 则复用原来图片的spaceId
      if (spaceId == null) {
        if (oldPicture.getSpaceId() != null) {
          spaceId = oldPicture.getSpaceId();
        }
      } else {
        // 传了 spaceId, 则判断是否一致
        if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
          throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片空间不一致");
        }
      }
    }
    // 上传图片, 得到图片信息
    // 根据用户 id 划分目录 => 按照空间划分目录
    String uploadPathPrefix = null;
    if (spaceId == null) {
      // 公共图库
      uploadPathPrefix = String.format("public/%s", loginUser.getId());
    } else {
      uploadPathPrefix = String.format("space/%s", spaceId);
    }
    // 根据 inputSource 的类型区分上传方式
    PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
    if (inputSource instanceof String) {
      pictureUploadTemplate = urlPictureUpload;
    }
    UploadPictureResult uploadPictureResult =
        pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
    // 构造要入库的图片信息
    Picture picture = new Picture();
    picture.setSpaceId(spaceId); // 指定空间id
    picture.setUserId(loginUser.getId());
    picture.setUrl(uploadPictureResult.getUrl());
    picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());
    // 支持外层传入图片名称
    String pictureName = uploadPictureResult.getPicName();
    if (pictureUploadRequest != null && StrUtil.isNotEmpty(pictureUploadRequest.getPictureName())) {
      pictureName = pictureUploadRequest.getPictureName();
    }
    picture.setName(pictureName);
    picture.setPicSize(uploadPictureResult.getPicSize());
    picture.setPicWidth(uploadPictureResult.getPicWidth());
    picture.setPicHeight(uploadPictureResult.getPicHeight());
    picture.setPicScale(uploadPictureResult.getPicScale());
    picture.setPicFormat(uploadPictureResult.getPicFormat());
    // 补充审核参数
    this.fillReviewParams(picture, loginUser);
    // 操作数据库
    // 如果pictureId不为空,则更新,否则新增
    if (pictureId != null) {
      // 如果是更新,需要补充 id 和编辑时间
      picture.setId(pictureId);
      picture.setUpdateTime(new Date());
    }
    // 开启事务
    Long finalSpaceId = spaceId;
    transactionTemplate.execute(
        status -> {
          // 插入数据
          boolean result = this.saveOrUpdate(picture);
          ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败, 数据库操作失败");
          if (finalSpaceId != null) {
            // 更新空间的使用额度
            boolean update =
                spaceService
                    .lambdaUpdate()
                    .eq(Space::getId, finalSpaceId)
                    .setSql("totalSize = totalSize + " + picture.getPicSize())
                    .setSql("totalCount = totalCount + 1")
                    .update();
            ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败, 数据库操作失败");
          }
          return true;
        });
    return PictureVO.objToVo(picture);
  }

  @Override
  public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
    if (picture == null) {
      return null;
    }
    // 对象转封装类
    PictureVO pictureVO = PictureVO.objToVo(picture);
    // 关联用户查询信息
    Long userId = picture.getUserId();
    if (userId != null && userId > 0) {
      User user = userService.getById(userId);
      UserVO userVO = userService.getUserVO(user);
      pictureVO.setUser(userVO);
    }
    return pictureVO;
  }

  /**
   * 分页获取图片封装
   *
   * @param picturePage
   * @param request
   * @return
   */
  @Override
  public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
    if (picturePage == null) {
      return null;
    }
    List<Picture> pictureList = picturePage.getRecords();
    Page<PictureVO> pictureVOPage =
        new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
    if (CollUtil.isEmpty(pictureList)) {
      return pictureVOPage;
    }
    // 对象列表转封装对象列表
    List<PictureVO> pictureVOList =
        pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    // 关联查询用户信息
    Set<Long> userIdSet =
        pictureVOList.stream().map(PictureVO::getUserId).collect(Collectors.toSet());
    Map<Long, List<User>> userIdUserListMap =
        userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
    // 封装用户信息
    pictureVOList.forEach(
        pictureVO -> {
          Long userId = pictureVO.getUserId();
          User user = null;
          if (userIdUserListMap.containsKey(userId)) {
            user = userIdUserListMap.get(userId).get(0);
          }
          pictureVO.setUser(userService.getUserVO(user));
        });
    pictureVOPage.setRecords(pictureVOList);
    return pictureVOPage;
  }

  @Override
  public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    if (pictureQueryRequest == null) {
      return queryWrapper;
    }
    // 从对象中取值
    Long id = pictureQueryRequest.getId();
    String name = pictureQueryRequest.getName();
    String introduction = pictureQueryRequest.getIntroduction();
    String category = pictureQueryRequest.getCategory();
    List<String> tags = pictureQueryRequest.getTags();
    Long picSize = pictureQueryRequest.getPicSize();
    Integer picWidth = pictureQueryRequest.getPicWidth();
    Integer picHeight = pictureQueryRequest.getPicHeight();
    Double picScale = pictureQueryRequest.getPicScale();
    String picFormat = pictureQueryRequest.getPicFormat();
    String searchText = pictureQueryRequest.getSearchText();
    Long userId = pictureQueryRequest.getUserId();
    String sortField = pictureQueryRequest.getSortField();
    String sortOrder = pictureQueryRequest.getSortOrder();
    Long reviewerId = pictureQueryRequest.getReviewerId();
    Integer reviewStatus = pictureQueryRequest.getReviewStatus();
    String reviewMessage = pictureQueryRequest.getReviewMessage();
    Date reviewTime = pictureQueryRequest.getReviewTime();
    Date startEditTime = pictureQueryRequest.getStartEditTime();
    Date endEditTime = pictureQueryRequest.getEndEditTime();
    Long spaceId = pictureQueryRequest.getSpaceId();
    boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
    // 从多字段中搜索
    if (StrUtil.isNotBlank(searchText)) {
      // 需要拼接查询条件
      // and (name like %xxx% or introduction like %xxx%)
      queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
    }
    queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
    queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
    queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
    queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
    queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);
    queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
    queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
    queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
    queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
    queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
    queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
    queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
    queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
    queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
    queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
    queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
    queryWrapper.isNull(nullSpaceId, "spaceId");
    // JSON 数组查询
    if (CollUtil.isNotEmpty(tags)) {
      // tag like "%\"xxx\"%"
      for (String tag : tags) {
        queryWrapper.like("tags", "\"" + tag + "\"");
      }
    }
    // 排序
    queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
    return queryWrapper;
  }

  @Override
  public void validPicture(Picture picture) {
    ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR, "图片信息不能为空");
    // 从对象中取值
    Long id = picture.getId();
    String url = picture.getUrl();
    String introduction = picture.getIntroduction();
    // 修改数据时 id 不能为空
    ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "图片 id 不能为空");
    if (StrUtil.isNotBlank(url)) {
      ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "图片 url 过长");
    }
    if (StrUtil.isNotBlank(introduction)) {
      ThrowUtils.throwIf(introduction.length() > 1000, ErrorCode.PARAMS_ERROR, "图片简介过长");
    }
  }

  @Override
  public void handlePictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
    // 校验参数
    ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR, "图片审核信息不能为空");
    Long id = pictureReviewRequest.getId();
    Integer reviewStatus = pictureReviewRequest.getReviewStatus();
    PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
    if (id == null
        || reviewStatusEnum == null
        || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片审核信息参数错误");
    }
    // 判断图片是否存在
    Picture oldPicture = this.getById(id);
    ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
    // 检验审核状态是否重复
    if (oldPicture.getReviewStatus().equals(reviewStatus)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片审核状态重复");
    }
    // 操作数据库
    Picture updatePicture = new Picture();
    BeanUtil.copyProperties(oldPicture, updatePicture);
    updatePicture.setReviewStatus(reviewStatus);
    updatePicture.setReviewMessage(pictureReviewRequest.getReviewMessage());
    updatePicture.setReviewerId(loginUser.getId());
    updatePicture.setReviewTime(new Date());
    boolean result = this.updateById(updatePicture);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片审核失败");
  }

  /**
   * 填充审核参数
   *
   * @param picture
   * @param loginUser
   */
  @Override
  public void fillReviewParams(Picture picture, User loginUser) {
    if (userService.isAdmin(loginUser)) {
      // 管理员自动过审
      picture.setReviewStatus(PictureReviewStatusEnum.REVIEWED.getValue());
      picture.setReviewerId(loginUser.getId());
      picture.setReviewMessage("管理员自动过审");
      picture.setReviewTime(new Date());
    } else {
      // 普通用户,无论编辑还是创建，默认都是待审核
      picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
    }
  }

  @Override
  public Integer uploadPictureByBatch(
      PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
    // 校验参数
    String searchText = pictureUploadByBatchRequest.getSearchText();
    Integer fetchCount = pictureUploadByBatchRequest.getFetchCount();
    // 名称前缀默认等于搜索值
    String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
    if (StrUtil.isBlank(namePrefix)) {
      namePrefix = searchText;
    }
    ThrowUtils.throwIf(fetchCount > 30, ErrorCode.PARAMS_ERROR, "一次最多抓取 30 张图片");
    // 抓取内容
    String fetchUrl =
        String.format(
            "https://www.bing.com/images/search?q=%s&filters=IsPhoto:true+imagesize:wallpaper&mmasync=1",
            searchText);
    System.out.println(fetchUrl);
    Document document = null;
    try {
      document = Jsoup.connect(fetchUrl).get();
    } catch (IOException e) {
      log.error("获取页面失败", e);
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
    }
    // 解析页面
    Element div = document.getElementsByClass("dgControl").first();
    if (ObjUtil.isEmpty(div)) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
    }
    Elements imgElementList = div.select("a.iusc");
    // 遍历元素，依次上传图片
    int uploadCount = 0;
    for (Element element : imgElementList) {
      String m_attr = element.attr("m");
      String imgUrl = null;
      //      String imgUrl = element.attr("src");
      try {
        imgUrl = JSONUtil.parseObj(m_attr).getStr("murl");
      } catch (Exception e) {
        log.error("解析图片地址失败: m_attr={}", m_attr, e);
      }
      if (StrUtil.isEmpty(imgUrl)) {
        log.info("图片地址为空，跳过: {}", imgUrl);
        continue;
      }
      // 处理图片地址,防止转义或者与cos对象存储冲突
      /*
       * https://www.bing.com/th/id/OIP.dMDBNFtmsMmOAYTWceg05QHaLT?w=123&h=187&c=7&r=0&o=5&dpr=1.5&pid=1.7
       * 只保留
       * https://www.bing.com/th/id/OIP.dMDBNFtmsMmOAYTWceg05QHaLT
       */
      int questionMarkIndex = imgUrl.indexOf("?");
      if (questionMarkIndex != -1) {
        imgUrl = imgUrl.substring(0, questionMarkIndex);
      }
      // 上传图片
      PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
      pictureUploadRequest.setFileUrl(imgUrl);
      pictureUploadRequest.setPictureName(namePrefix + "(" + (uploadCount + 1) + ")");
      try {
        PictureVO pictureVO = this.uploadPicture(imgUrl, pictureUploadRequest, loginUser);
        log.info("上传图片成功: pictureId={}", pictureVO.getId());
        uploadCount++;
      } catch (Exception e) {
        log.error("上传图片失败: imgUrl={}", imgUrl, e);
        continue;
      }
      if (uploadCount >= fetchCount) {
        break;
      }
    }
    return uploadCount;
  }

  @Async
  @Override
  public void clearPictureFile(Picture oldPicture) {
    // 判断图片是否被多条记录使用(本项目不存在一张图片被多条记录使用的情况，添加秒传功能时会存在这种情况)
    String pictureUrl = oldPicture.getUrl();
    long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).count();
    if (count > 1) {
      log.info("图片被多条记录使用，不删除: pictureUrl={}", pictureUrl);
      return;
    }
    // 删除图片文件
    cosManager.deleteObject(pictureUrl);
    // 删除缩略图
    String thumbnailUrl = oldPicture.getThumbnailUrl();
    if (StrUtil.isNotBlank(thumbnailUrl)) {
      cosManager.deleteObject(thumbnailUrl);
    }
  }

  @Override
  public void deletePicture(long pictureId, User loginUser) {
    ThrowUtils.throwIf(pictureId <= 0, ErrorCode.PARAMS_ERROR);
    ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
    // 判断是否存在
    Picture oldPicture = this.getById(pictureId);
    ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
    // 校验权限
    // 已经改为使用注解鉴权
    // checkPictureAuth(oldPicture, loginUser);

    transactionTemplate.execute(
        status -> {
          // 操作数据库
          boolean result = this.removeById(pictureId);
          ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
          // 更新空间的使用额度, 释放额度
          boolean update =
              spaceService
                  .lambdaUpdate()
                  .eq(Space::getId, oldPicture.getSpaceId())
                  .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                  .setSql("totalCount = totalCount - 1")
                  .update();
          ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败, 数据库操作失败");
          return true;
        });
    // 异步清理文件
    this.clearPictureFile(oldPicture);
  }

  @Override
  public void editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
    // 在此处将实体类和 DTO 进行转换
    Picture picture = new Picture();
    BeanUtils.copyProperties(pictureEditRequest, picture);
    // 注意将 list 转为 string
    picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
    // 设置编辑时间
    picture.setEditTime(new Date());
    // 数据校验
    this.validPicture(picture);
    // 判断是否存在
    long id = pictureEditRequest.getId();
    Picture oldPicture = this.getById(id);
    ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
    // 校验权限
    // 已经改为使用注解鉴权
    // checkPictureAuth(oldPicture, loginUser);
    // 补充审核参数
    this.fillReviewParams(picture, loginUser);
    // 操作数据库
    boolean result = this.updateById(picture);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
  }

  @Override
  public void checkPictureAuth(Picture picture, User loginUser) {
    Long spaceId = picture.getSpaceId();
    Long userId = loginUser.getId();
    if (spaceId == null) {
      // 公共图片
      if (!picture.getUserId().equals(userId) && !userService.isAdmin(loginUser)) {
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
      }
    } else {
      // 私有空间，仅空间管理员可以操作
      if (!picture.getUserId().equals(loginUser.getId())) {
        throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
      }
    }
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public void editPictureByBatch(
      PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
    List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
    Long spaceId = pictureEditByBatchRequest.getSpaceId();
    String category = pictureEditByBatchRequest.getCategory();
    List<String> tags = pictureEditByBatchRequest.getTags();

    // 1. 校验参数
    ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
    ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
    // 2. 校验空间权限
    Space space = spaceService.getById(spaceId);
    ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
    if (!loginUser.getId().equals(space.getUserId())) {
      throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
    }

    // 3. 查询指定图片，仅选择需要的字段
    List<Picture> pictureList =
        this.lambdaQuery()
            .select(Picture::getId, Picture::getSpaceId)
            .eq(Picture::getSpaceId, spaceId)
            .in(Picture::getId, pictureIdList)
            .list();

    if (pictureList.isEmpty()) {
      return;
    }
    // 4. 更新分类和标签
    pictureList.forEach(
        picture -> {
          if (StrUtil.isNotBlank(category)) {
            picture.setCategory(category);
          }
          if (CollUtil.isNotEmpty(tags)) {
            picture.setTags(JSONUtil.toJsonStr(tags));
          }
        });
    // 批量重命名
    String nameRule = pictureEditByBatchRequest.getNameRule();
    fillPictureWithNameRule(pictureList, nameRule);

    // 5. 批量更新
    boolean result = this.updateBatchById(pictureList);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
  }

  @Override
  public CreateOutPaintingTaskResponse createPictureOutPaintingTask(
      CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
    // 获取图片信息
    Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
    Picture picture = this.getById(pictureId);
    ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
    // 权限校验
    // 已经改为使用注解鉴权
    // checkPictureAuth(picture, loginUser);
    // 创建扩图任务
    CreateOutPaintingTaskRequest request = new CreateOutPaintingTaskRequest();
    CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
    input.setImageUrl(picture.getUrl());
    request.setInput(input);
    request.setParameters(createPictureOutPaintingTaskRequest.getParameters());
    // 创建任务
    return aliYunAiApi.createOutPaintingTask(request);
  }

  /**
   * nameRule 格式：图片{序号}
   *
   * @param pictureList
   * @param nameRule
   */
  private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
    if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
      return;
    }
    long count = 1;
    try {
      for (Picture picture : pictureList) {
        String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
        picture.setName(pictureName);
      }
    } catch (Exception e) {
      log.error("名称解析错误", e);
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
    }
  }
}
