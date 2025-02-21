package com.wws.imageplatformback.api.deepseek.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeepSeekResultResponse {
  private String reasoningContent; // 推理内容
  private String content; // 推理结果
}
