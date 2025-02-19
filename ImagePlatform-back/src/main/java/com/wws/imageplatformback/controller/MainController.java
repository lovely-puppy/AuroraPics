package com.wws.imageplatformback.controller;

import com.wws.imageplatformback.common.BaseResponse;
import com.wws.imageplatformback.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author 汪文松
 * @date 2025-01-18 0:55
 */
@RestController
@RequestMapping("/")
public class MainController {

    /**
     * 健康检查,查看项目是否有问题
     * @return
     */
    @GetMapping("/health")
    public BaseResponse<?> health(HttpServletRequest request){
        request.getSession().setAttribute("health", true);
        return ResultUtils.success("ok");
    }
}
