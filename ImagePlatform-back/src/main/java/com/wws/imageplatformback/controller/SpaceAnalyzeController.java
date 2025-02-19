package com.wws.imageplatformback.controller;

import com.wws.imageplatformback.common.BaseResponse;
import com.wws.imageplatformback.common.ResultUtils;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.model.dto.space.analyze.*;
import com.wws.imageplatformback.model.entity.Space;
import com.wws.imageplatformback.model.entity.User;
import com.wws.imageplatformback.model.vo.space.analyze.*;
import com.wws.imageplatformback.service.SpaceAnalyzeService;
import com.wws.imageplatformback.service.UserService;
import com.wws.imageplatformback.utils.ThrowUtils;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {
  @Resource private UserService userService;
  @Resource private SpaceAnalyzeService spaceAnalyzeService;

  /**
   * 获取空间的使用状态
   *
   * @param spaceUsageAnalyzeRequest
   * @param request
   * @return
   */
  @PostMapping("/usage")
  public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(
      @RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, HttpServletRequest request) {
    ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse =
        spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
    return ResultUtils.success(spaceUsageAnalyzeResponse);
  }

  /**
   * 获取空间图片分类分析
   *
   * @param spaceCategoryAnalyzeRequest
   * @param request
   * @return
   */
  @PostMapping("/category")
  public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(
      @RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest,
      HttpServletRequest request) {
    ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    List<SpaceCategoryAnalyzeResponse> resultList =
        spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
    return ResultUtils.success(resultList);
  }

  /**
   * 获取空间标签情况
   *
   * @param spaceTagAnalyzeRequest
   * @param request
   * @return
   */
  @PostMapping("/tag")
  public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(
      @RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, HttpServletRequest request) {
    ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    List<SpaceTagAnalyzeResponse> resultList =
        spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
    return ResultUtils.success(resultList);
  }

  /**
   * 获取空间大小情况
   *
   * @param spaceSizeAnalyzeRequest
   * @param request
   * @return
   */
  @PostMapping("/size")
  public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(
      @RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, HttpServletRequest request) {
    ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    List<SpaceSizeAnalyzeResponse> resultList =
        spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
    return ResultUtils.success(resultList);
  }

  /**
   * 获取用户上传行为
   *
   * @param spaceUserAnalyzeRequest
   * @param request
   * @return
   */
  @PostMapping("/user")
  public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(
      @RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, HttpServletRequest request) {
    ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    List<SpaceUserAnalyzeResponse> resultList =
        spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
    return ResultUtils.success(resultList);
  }

  @PostMapping("/rank")
  public BaseResponse<List<Space>> getSpaceRankAnalyze(
      @RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, HttpServletRequest request) {
    ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
    User loginUser = userService.getLoginUser(request);
    List<Space> resultList =
        spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
    return ResultUtils.success(resultList);
  }
}
