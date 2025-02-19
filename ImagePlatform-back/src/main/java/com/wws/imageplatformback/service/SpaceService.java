package com.wws.imageplatformback.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wws.imageplatformback.model.dto.picture.PictureQueryRequest;
import com.wws.imageplatformback.model.dto.space.SpaceAddRequest;
import com.wws.imageplatformback.model.dto.space.SpaceQueryRequest;
import com.wws.imageplatformback.model.entity.Picture;
import com.wws.imageplatformback.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wws.imageplatformback.model.entity.User;
import com.wws.imageplatformback.model.vo.PictureVO;
import com.wws.imageplatformback.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author Lenovo
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-02-10 14:39:43
*/
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验空间
     * @param space
     * @param add 是否为创建时校验
     */
    public void validSpace(Space space, boolean add);

    /**
     * 获取空间包装类(单条)
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 获取查询对象
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 根据空间等级填充空间
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 校验空间权限
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);
}
