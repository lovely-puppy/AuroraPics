package com.wws.imageplatformback.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.mapper.SpaceMapper;
import com.wws.imageplatformback.model.dto.space.SpaceAddRequest;
import com.wws.imageplatformback.model.dto.space.SpaceQueryRequest;
import com.wws.imageplatformback.model.entity.Space;
import com.wws.imageplatformback.model.entity.SpaceUser;
import com.wws.imageplatformback.model.entity.User;
import com.wws.imageplatformback.model.enums.SpaceLevelEnum;
import com.wws.imageplatformback.model.enums.SpaceRoleEnum;
import com.wws.imageplatformback.model.enums.SpaceTypeEnum;
import com.wws.imageplatformback.model.vo.SpaceVO;
import com.wws.imageplatformback.model.vo.UserVO;
import com.wws.imageplatformback.service.SpaceService;
import com.wws.imageplatformback.service.SpaceUserService;
import com.wws.imageplatformback.service.UserService;
import com.wws.imageplatformback.utils.ThrowUtils;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Lenovo
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-02-10 14:39:43
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceService {

  @Resource private UserService userService;
  @Resource private SpaceUserService spaceUserService;
  @Resource private TransactionTemplate transactionTemplate;

  // 为了方便部署，注释掉分库分表
  // @Resource @Lazy private DynamicShardingManager dynamicShardingManager;

  @Override
  public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
    // 1.填充默认参数
    // 转换实体类和dto
    Space space = new Space();
    BeanUtil.copyProperties(spaceAddRequest, space);
    if (StrUtil.isBlank(space.getSpaceName())) {
      space.setSpaceName("未命名空间");
    }
    if (space.getSpaceLevel() == null) {
      space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
    }
    if (space.getSpaceType() == null) {
      space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
    }
    // 填充容量和大小
    this.fillSpaceBySpaceLevel(space);
    // 2.校验参数
    this.validSpace(space, true);
    // 3.权限校验, 非管理员只能创建普通级别的空间
    Long userId = loginUser.getId();
    space.setUserId(userId);
    if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel()
        && !userService.isAdmin(loginUser)) {
      throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员不能创建高级别空间");
    }
    // 4.控制同一用户只能创建一个私有空间以及一个团队空间
    String lock = String.valueOf(userId).intern();
    synchronized (lock) {
      Long newSpaceId =
          transactionTemplate.execute(
              status -> {
                // 判断是否已有空间
                boolean exists =
                    this.lambdaQuery()
                        .eq(Space::getUserId, userId)
                        .eq(Space::getSpaceType, spaceAddRequest.getSpaceType())
                        .exists();
                ThrowUtils.throwIf(exists, ErrorCode.PARAMS_ERROR, "同一用户每类空间只能创建一个");
                // 创建
                boolean result = this.save(space);
                ThrowUtils.throwIf(!result, ErrorCode.SYSTEM_ERROR, "创建失败");
                // 如果是团队空间，关联新增团队成员记录
                if (SpaceTypeEnum.TEAM.getValue() == spaceAddRequest.getSpaceType()) {
                  SpaceUser spaceUser = new SpaceUser();
                  spaceUser.setSpaceId(space.getId());
                  spaceUser.setUserId(userId);
                  spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                  result = spaceUserService.save(spaceUser);
                  ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "创建团队成员记录失败");
                }
                // 创建分表
                // dynamicShardingManager.createSpacePictureTable(space);
                // 返回新写入的数据 id
                return space.getId();
              });
      return newSpaceId;
    }
  }

  @Override
  public void validSpace(Space space, boolean add) {
    ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "空间信息不能为空");
    // 从对象中取值
    String spaceName = space.getSpaceName();
    Integer spaceLevel = space.getSpaceLevel();
    SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
    Integer spaceType = space.getSpaceType();
    SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(spaceType);
    // 创建时校验
    if (add) {
      if (StrUtil.isBlank(spaceName)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
      }
      if (spaceLevel == null) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
      }
      if (spaceType == null) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不能为空");
      }
    }
    // 更新时校验
    if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能超过30个字符");
    }
    if (spaceLevel != null && spaceLevelEnum == null) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不合法");
    }
    // 修改数据时，空间级别进行校验
    if (spaceType != null && spaceTypeEnum == null) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不合法");
    }
  }

  @Override
  public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
    if (space == null) {
      return null;
    }
    // 对象转封装类
    SpaceVO spaceVO = SpaceVO.objToVo(space);
    // 关联用户查询信息
    Long userId = space.getUserId();
    if (userId != null && userId > 0) {
      User user = userService.getById(userId);
      UserVO userVO = userService.getUserVO(user);
      spaceVO.setUser(userVO);
    }
    return spaceVO;
  }

  @Override
  public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
    if (spacePage == null) {
      return null;
    }
    List<Space> spaceList = spacePage.getRecords();
    Page<SpaceVO> spaceVOPage =
        new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
    if (CollUtil.isEmpty(spaceList)) {
      return spaceVOPage;
    }
    // 对象列表转封装对象列表
    List<SpaceVO> spaceVOList =
        spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
    // 关联查询用户信息
    Set<Long> userIdSet = spaceVOList.stream().map(SpaceVO::getUserId).collect(Collectors.toSet());
    Map<Long, List<User>> userIdUserListMap =
        userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
    // 封装用户信息
    spaceVOList.forEach(
        spaceVO -> {
          Long userId = spaceVO.getUserId();
          User user = null;
          if (userIdUserListMap.containsKey(userId)) {
            user = userIdUserListMap.get(userId).get(0);
          }
          spaceVO.setUser(userService.getUserVO(user));
        });
    spaceVOPage.setRecords(spaceVOList);
    return spaceVOPage;
  }

  @Override
  public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
    QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
    if (spaceQueryRequest == null) {
      return queryWrapper;
    }
    // 从对象中取值
    Long id = spaceQueryRequest.getId();
    Long userId = spaceQueryRequest.getUserId();
    String spaceName = spaceQueryRequest.getSpaceName();
    Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
    String sortField = spaceQueryRequest.getSortField();
    String sortOrder = spaceQueryRequest.getSortOrder();
    Integer spaceType = spaceQueryRequest.getSpaceType();
    // 拼接查询条件
    queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
    queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
    queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
    queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
    queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);

    // 排序
    queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
    return queryWrapper;
  }

  @Override
  public void fillSpaceBySpaceLevel(Space space) {
    SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
    if (spaceLevelEnum != null) {
      long maxSize = spaceLevelEnum.getMaxSize();
      if (space.getMaxSize() == null) {
        space.setMaxSize(maxSize);
      }
      long maxCount = spaceLevelEnum.getMaxCount();
      if (space.getMaxCount() == null) {
        space.setMaxCount(maxCount);
      }
    }
  }

  @Override
  public void checkSpaceAuth(User loginUser, Space space) {
    // 仅本人或者管理员可编辑
    if (!space.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
      throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
    }
  }
}
