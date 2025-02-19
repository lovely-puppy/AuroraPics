package com.wws.imageplatformback.service;

import com.wws.imageplatformback.model.dto.space.analyze.*;
import com.wws.imageplatformback.model.entity.Space;
import com.wws.imageplatformback.model.entity.User;
import com.wws.imageplatformback.model.vo.space.analyze.*;

import java.util.List;

public interface SpaceAnalyzeService {
  /**
   * 获取空间使用情况分析
   *
   * @param spaceAnalyzeRequest
   * @param loginUser
   * @return
   */
  SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(
      SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser);

  /**
   * 获取空间图片分类情况
   *
   * @param spaceCategoryAnalyzeRequest
   * @param loginUser
   * @return
   */
  List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(
      SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

  /**
   * 获取空间图片标签情况
   *
   * @param spaceTagAnalyzeRequest
   * @param loginUser
   * @return
   */
  List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(
      SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

  /**
   * 获取空间图片大小使用情况
   *
   * @param spaceSizeAnalyzeRequest
   * @param loginUser
   * @return
   */
  List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(
      SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

  /**
   * 获取用户上传行为情况
   * @param spaceUserAnalyzeRequest
   * @param loginUser
   * @return
   */
  List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

  /**
   * 空间使用排行
   * @param spaceRankAnalyzeRequest
   * @param loginUser
   * @return
   */
  List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
