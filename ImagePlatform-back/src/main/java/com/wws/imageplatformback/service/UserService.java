package com.wws.imageplatformback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wws.imageplatformback.model.dto.user.UserQueryRequest;
import com.wws.imageplatformback.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wws.imageplatformback.model.vo.LoginUserVO;
import com.wws.imageplatformback.model.vo.UserVO;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
* @author Lenovo
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-01-24 20:18:40
*/
public interface UserService extends IService<User> {
    /**
     *
     * @param userAccount 账号
     * @param userPassword 密码
     * @param checkPassword 确认密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取加密后的密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前的登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获得脱敏后的用户视图
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获得脱敏后的用户信息
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户退出登录
     * @param request
     * @return 用户信息
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 判断用户是否为管理员
     */
    boolean isAdmin(User user);
}
