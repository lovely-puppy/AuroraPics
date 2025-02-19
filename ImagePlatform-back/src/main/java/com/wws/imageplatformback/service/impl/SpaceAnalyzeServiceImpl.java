package com.wws.imageplatformback.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.mapper.SpaceMapper;
import com.wws.imageplatformback.model.dto.space.analyze.*;
import com.wws.imageplatformback.model.entity.Picture;
import com.wws.imageplatformback.model.entity.Space;
import com.wws.imageplatformback.model.entity.User;
import com.wws.imageplatformback.model.vo.space.analyze.*;
import com.wws.imageplatformback.service.PictureService;
import com.wws.imageplatformback.service.SpaceAnalyzeService;
import com.wws.imageplatformback.service.SpaceService;
import com.wws.imageplatformback.service.UserService;
import com.wws.imageplatformback.utils.ThrowUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceAnalyzeService {

  @Resource private UserService userService;
  @Resource private SpaceService spaceService;
  @Resource private PictureService pictureService;

  /**
   * 校验空间分析权限
   *
   * @param spaceAnalyzeRequest
   * @param loginUser
   */
  private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
    boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
    boolean queryAll = spaceAnalyzeRequest.isQueryAll();
    // 全空间分析或公共图库分析，仅管理员可以访问
    if (queryAll || queryPublic) {
      ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "没有权限");
    } else {
      // 分析特定空间，仅管理员和空间拥有者可以访问
      Long spaceId = spaceAnalyzeRequest.getSpaceId();
      ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
      Space space = spaceService.getById(spaceId);
      ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间不存在");
      spaceService.checkSpaceAuth(loginUser, space);
    }
  }

  /**
   * 根据请求对象封装查询条件
   *
   * @param spaceAnalyzeRequest
   */
  private void fillAnalyzeQueryWrapper(
      SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
    boolean queryAll = spaceAnalyzeRequest.isQueryAll();
    // 全空间分析
    if (queryAll) {
      return;
    }
    boolean queryPublic = spaceAnalyzeRequest.isQueryPublic();
    // 公共图库分析
    if (queryPublic) {
      queryWrapper.isNull("spaceId");
      return;
    }
    Long spaceId = spaceAnalyzeRequest.getSpaceId();
    // 分析特定空间
    if (spaceId != null) {
      queryWrapper.eq("spaceId", spaceId);
      return;
    }
    throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
  }

  @Override
  public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(
      SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
    // 校验参数
    // 校验权限（查询所有空间还是特定空间）
    if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
      // 全空间或公共空间可以从picture表查询
      // 仅管理员可以访问
      checkSpaceAnalyzeAuth(spaceAnalyzeRequest, loginUser);
      // 统计图库的使用情况
      QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
      queryWrapper.select("picSize");
      // 补充查询范围
      fillAnalyzeQueryWrapper(spaceAnalyzeRequest, queryWrapper);
      // 为了优化空间，节约对象资源而没有使用pictureService.list()
      List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
      long usedSize = pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();
      long usedCount = pictureObjList.size();
      // 封装返回结果
      SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
      spaceUsageAnalyzeResponse.setUsedCount(usedCount);
      spaceUsageAnalyzeResponse.setUsedSize(usedSize);
      // 公共图库无容量或者数量限制
      spaceUsageAnalyzeResponse.setMaxCount(null);
      spaceUsageAnalyzeResponse.setMaxSize(null);
      spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
      spaceUsageAnalyzeResponse.setCountUsageRatio(null);
      return spaceUsageAnalyzeResponse;
    } else {
      // 特定空间可以从space表查询
      Long spaceId = spaceAnalyzeRequest.getSpaceId();
      ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
      // 获取空间信息
      Space space = spaceService.getById(spaceId);
      ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
      // 权限校验，仅管理员和空间拥有着可以使用
      spaceService.checkSpaceAuth(loginUser, space);
      // 构造返回结果
      SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
      spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
      spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
      spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
      spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
      // 计算比例
      double sizeUsageRatio =
          NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue();
      double countUsageRatio =
          NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue();
      spaceUsageAnalyzeResponse.setSizeUsageRatio(sizeUsageRatio);
      spaceUsageAnalyzeResponse.setCountUsageRatio(countUsageRatio);
      return spaceUsageAnalyzeResponse;
    }
  }

  @Override
  public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
      SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
    ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

    // 检查权限
    checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);

    // 构造查询条件
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    // 根据分析范围补充查询条件
    fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);

    // 使用 MyBatis-Plus 分组查询
    queryWrapper
        .select("category AS category", "COUNT(*) AS count", "SUM(picSize) AS totalSize")
        .groupBy("category");

    // 查询并转换结果
    return pictureService.getBaseMapper().selectMaps(queryWrapper).stream()
        .map(
            result -> {
              String category =
                  result.get("category") != null ? result.get("category").toString() : "未分类";
              Long count = ((Number) result.get("count")).longValue();
              Long totalSize = ((Number) result.get("totalSize")).longValue();
              return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
            })
        .collect(Collectors.toList());
  }

  @Override
  public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(
      SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
    ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

    // 检查权限
    checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);

    // 构造查询条件
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);

    // 查询所有符合条件的标签
    queryWrapper.select("tags");
    List<String> tagsJsonList =
        pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
            .filter(ObjUtil::isNotNull)
            .map(Object::toString)
            .collect(Collectors.toList());

    // 合并所有标签并统计使用次数
    Map<String, Long> tagCountMap =
        tagsJsonList.stream()
            .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
            .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

    // 转换为响应对象，按使用次数降序排序
    return tagCountMap.entrySet().stream()
        .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue())) // 降序排列
        .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(
      SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
    ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

    // 检查权限
    checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

    // 构造查询条件
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);

    // 查询所有符合条件的图片大小
    queryWrapper.select("picSize");
    List<Long> picSizes =
        pictureService.getBaseMapper().selectObjs(queryWrapper).stream()
            .map(size -> ((Number) size).longValue())
            .collect(Collectors.toList());

    // 定义分段范围，注意使用有序 Map
    Map<String, Long> sizeRanges = new LinkedHashMap<>();
    sizeRanges.put("<100KB", picSizes.stream().filter(size -> size < 100 * 1024).count());
    sizeRanges.put(
        "100KB-500KB",
        picSizes.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
    sizeRanges.put(
        "500KB-1MB",
        picSizes.stream().filter(size -> size >= 500 * 1024 && size < 1 * 1024 * 1024).count());
    sizeRanges.put(">1MB", picSizes.stream().filter(size -> size >= 1 * 1024 * 1024).count());

    // 转换为响应对象
    return sizeRanges.entrySet().stream()
        .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(
      SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
    ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    // 检查权限
    checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

    // 构造查询条件
    QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
    fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
    Long userId = spaceUserAnalyzeRequest.getUserId();
    queryWrapper.eq(ObjUtil.isNotNull(userId), "userId", userId);

    // 分析维度：每日、每周、每月
    String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
    switch (timeDimension) {
      case "day":
        queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m-%d') AS period", "COUNT(*) AS count");
        break;
      case "week":
        queryWrapper.select("YEARWEEK(createTime) AS period", "COUNT(*) AS count");
        break;
      case "month":
        queryWrapper.select("DATE_FORMAT(createTime, '%Y-%m') AS period", "COUNT(*) AS count");
        break;
      default:
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的时间维度");
    }

    // 分组和排序
    queryWrapper.groupBy("period").orderByAsc("period");

    // 查询结果并转换
    List<Map<String, Object>> queryResult = pictureService.getBaseMapper().selectMaps(queryWrapper);
    return queryResult.stream()
        .map(
            result -> {
              String period = result.get("period").toString();
              Long count = ((Number) result.get("count")).longValue();
              return new SpaceUserAnalyzeResponse(period, count);
            })
        .collect(Collectors.toList());
  }

  @Override
  public List<Space> getSpaceRankAnalyze(
      SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
    ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

    // 仅管理员可查看空间排行
    ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "无权查看空间排行");

    // 构造查询条件
    QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
    queryWrapper
        .select("id", "spaceName", "userId", "totalSize")
        .orderByDesc("totalSize")
        .last("LIMIT " + spaceRankAnalyzeRequest.getTopN()); // 取前 N 名

    // 查询结果
    return spaceService.list(queryWrapper);
  }
}
