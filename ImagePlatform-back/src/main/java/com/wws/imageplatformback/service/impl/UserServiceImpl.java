package com.wws.imageplatformback.service.impl;

import static com.wws.imageplatformback.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wws.imageplatformback.constant.UserConstant;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.manager.auth.StpKit;
import com.wws.imageplatformback.mapper.UserMapper;
import com.wws.imageplatformback.model.dto.user.UserQueryRequest;
import com.wws.imageplatformback.model.entity.User;
import com.wws.imageplatformback.model.enums.UserRoleEnum;
import com.wws.imageplatformback.model.vo.LoginUserVO;
import com.wws.imageplatformback.model.vo.UserVO;
import com.wws.imageplatformback.service.UserService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author Lenovo
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-01-24 20:18:40
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

  @Override
  public long userRegister(String userAccount, String userPassword, String checkPassword) {
    // 1.校验参数
    if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
    }
    if (userAccount.length() < 4) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号长度过短");
    }
    if (userPassword.length() < 6 || checkPassword.length() < 6) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码长度过短");
    }
    if (!userPassword.equals(checkPassword)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
    }
    // 2.检查账户是否和数据库中已有的重复
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("userAccount", userAccount);
    long count = this.baseMapper.selectCount(queryWrapper);
    if (count > 0) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已存在");
    }
    // 3.密码一定要加密
    String encryptPassword = getEncryptPassword(userPassword);
    // 4.插入数据到数据库中
    User user = new User();
    user.setUserAccount(userAccount);
    user.setUserPassword(encryptPassword);
    user.setUserName("无名");
    user.setUserRole(UserRoleEnum.USER.getValue());
    boolean saveResult = this.save(user);
    if (!saveResult) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败, 数据库错误");
    }
    return user.getId();
  }

  @Override
  public LoginUserVO userLogin(
      String userAccount, String userPassword, HttpServletRequest request) {
    // 1.校验
    if (StrUtil.hasBlank(userAccount, userPassword)) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
    }
    if (userAccount.length() < 4) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误");
    }
    if (userPassword.length() < 6) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码错误");
    }
    // 2.对用户密码进行加密
    String encryptPassword = getEncryptPassword(userPassword);
    // 3.查询数据库中的用户是否存在
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("userAccount", userAccount);
    queryWrapper.eq("userPassword", encryptPassword);
    User user = this.baseMapper.selectOne(queryWrapper);
    // 不存在抛异常
    if (user == null) {
      log.info("user login failed, userAccount or userPassword is incorrect");
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或者密码错误");
    }
    // 4.保存用户登录状态
    request.getSession().setAttribute(USER_LOGIN_STATE, user);
    // 5.记录用户登录态到 Sa-token, 便于空间鉴权时使用，注意保证该用户信息 SpringSession 中的信息过期时间一致
    StpKit.SPACE.login(user.getId());
    StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, user);
    return this.getLoginUserVO(user);
  }

  /**
   * 获得脱敏后的登录用户信息
   *
   * @param user 用户
   * @return 脱敏后的用户信息
   */
  @Override
  public LoginUserVO getLoginUserVO(User user) {
    if (user == null) {
      return null;
    }
    LoginUserVO loginUserVO = new LoginUserVO();
    BeanUtils.copyProperties(user, loginUserVO);
    return loginUserVO;
  }

  /**
   * 获得脱敏后的用户信息
   *
   * @param user
   * @return
   */
  @Override
  public UserVO getUserVO(User user) {
    if (user == null) {
      return null;
    }
    UserVO userVO = new UserVO();
    BeanUtils.copyProperties(user, userVO);
    return userVO;
  }

  @Override
  public List<UserVO> getUserVOList(List<User> userList) {
    if (CollUtil.isEmpty(userList)) {
      return new ArrayList<>();
    }
    return userList.stream().map(this::getUserVO).collect(Collectors.toList());
  }

  /**
   * 获取加密后的密码
   *
   * @param userPassword 用户密码
   * @return 加密后的密码
   */
  @Override
  public String getEncryptPassword(String userPassword) {
    // 加盐, 混淆密码
    final String SALT = "wws";
    return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
  }

  /**
   * @param request
   * @return
   */
  @Override
  public User getLoginUser(HttpServletRequest request) {
    // 判断是否登录
    Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
    User currentUser = (User) userObj;
    if (currentUser == null || currentUser.getId() == null) {
      throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
    }
    // 从数据库中查询
    Long userId = currentUser.getId();
    currentUser = this.getById(userId);
    if (currentUser == null) {
      throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }
    return currentUser;
  }

  @Override
  public boolean userLogout(HttpServletRequest request) {
    // 判断用户是否登录
    Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
    if (userObj == null) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
    }
    // 移除登录态
    request.getSession().removeAttribute(USER_LOGIN_STATE);
    return true;
  }

  @Override
  public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
    if (userQueryRequest == null) {
      throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
    }
    Long id = userQueryRequest.getId();
    String userName = userQueryRequest.getUserName();
    String userAccount = userQueryRequest.getUserAccount();
    String userRole = userQueryRequest.getUserRole();
    String userProfile = userQueryRequest.getUserProfile();
    int current = userQueryRequest.getCurrent();
    int pageSize = userQueryRequest.getPageSize();
    String sortField = userQueryRequest.getSortField();
    String sortOrder = userQueryRequest.getSortOrder();
    QueryWrapper<User> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
    if (StrUtil.isNotBlank(userName)) {
      queryWrapper.like("userName", userName);
    }
    if (StrUtil.isNotBlank(userAccount)) {
      queryWrapper.like("userAccount", userAccount);
    }
    if (StrUtil.isNotBlank(userRole)) {
      queryWrapper.eq("userRole", userRole);
    }
    if (StrUtil.isNotBlank(userProfile)) {
      queryWrapper.like("userProfile", userProfile);
    }
    queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
    return queryWrapper;
  }

  @Override
  public boolean isAdmin(User user) {
    return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
  }
}
