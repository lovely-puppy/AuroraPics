package com.wws.imageplatformback.controller;

import com.wws.imageplatformback.api.deepseek.ChatCompletionsExample;
import com.wws.imageplatformback.api.deepseek.model.DeepSeekResultRequest;
import com.wws.imageplatformback.api.deepseek.model.DeepSeekResultResponse;
import com.wws.imageplatformback.common.BaseResponse;
import com.wws.imageplatformback.common.ResultUtils;
import com.wws.imageplatformback.exception.ErrorCode;
import com.wws.imageplatformback.utils.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/deepseek")
public class DeepSeekController {

    @PostMapping("/search")
    public BaseResponse<DeepSeekResultResponse> getDeepSeekResult(@RequestBody DeepSeekResultRequest deepSeekResultRequest) {
        ThrowUtils.throwIf(deepSeekResultRequest == null, ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        ChatCompletionsExample chatCompletionsExample = new ChatCompletionsExample();
        DeepSeekResultResponse deepSeekResult = chatCompletionsExample.getDeepSeekResult(deepSeekResultRequest.getSearchText());
        return ResultUtils.success(deepSeekResult);
    }
}
