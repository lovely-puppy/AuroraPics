package com.wws.imageplatformback.api.aliyunai;

import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.wws.imageplatformback.api.aliyunai.model.CreateOutPaintingTaskRequest;
import com.wws.imageplatformback.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.wws.imageplatformback.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.wws.imageplatformback.exception.BusinessException;
import com.wws.imageplatformback.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AliYunAiApi {

  // 创建任务地址
  public static final String CREATE_OUT_PAINTING_TASK_URL =
      "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";
  // 查询任务状态
  public static final String GET_OUT_PAINTING_TASK_URL =
      "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

  // 读取配置文件
  @Value("${aliYunAi.apiKey}")
  private String apiKey;

  // 创建任务
  public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest request) {
    if (request == null) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "请求参数不能为空");
    }
    // 发送请求
    HttpRequest httpRequest =
        HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
            .header(Header.AUTHORIZATION, "Bearer " + apiKey)
            .header("X-DashScope-Async", "enable")
            .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
            .body(JSONUtil.toJsonStr(request));
    // 处理响应
    try (HttpResponse httpResponse = httpRequest.execute()) {
      if (!httpResponse.isOk()) {
        log.error("创建任务失败，状态码：{}，响应内容：{}", httpResponse.getStatus(), httpResponse.body());
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败");
      }
      CreateOutPaintingTaskResponse response =
          JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
      if (response.getCode() != null) {
        log.error("创建任务失败，错误码：{}，错误信息：{}", response.getCode(), response.getMessage());
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI 扩图失败" + response.getMessage());
      }
      return response;
    }
  }

  // 查询创建的任务结果
  public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
    if (taskId == null) {
      throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务ID不能为空");
    }
    String url = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
    try (HttpResponse httpResponse =
        HttpRequest.get(url).header("Authorization", "Bearer " + apiKey).execute()) {
      if (!httpResponse.isOk()) {
        log.error("查询任务失败，状态码：{}，响应内容：{}", httpResponse.getStatus(), httpResponse.body());
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取任务结果失败");
      }
      return JSONUtil.toBean(httpResponse.body(), GetOutPaintingTaskResponse.class);
    }
  }
}
