package com.resky.yuaicodemother.controller;

import com.resky.yuaicodemother.common.BaseResponse;
import com.resky.yuaicodemother.common.ResultUtils;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")

public class HealthController {
    @GetMapping("/")
    @Operation(summary = "健康检查")
    public BaseResponse<String> healthCheck() {
       return ResultUtils.success("ok");
    }
}
