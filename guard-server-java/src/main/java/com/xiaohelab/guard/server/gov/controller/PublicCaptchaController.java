package com.xiaohelab.guard.server.gov.controller;

import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.gov.dto.CaptchaIssueRequest;
import com.xiaohelab.guard.server.gov.service.CaptchaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 公开域验证码接口（V2.1 §3.8.2.2）。无需登录。 */
@Tag(name = "Public.Captcha", description = "滑块/行为验证码签发")
@RestController
@RequestMapping("/api/v1/public/captcha")
public class PublicCaptchaController {

    private final CaptchaService captchaService;

    public PublicCaptchaController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @PostMapping("/issue")
    public Result<Map<String, Object>> issue(@Valid @RequestBody CaptchaIssueRequest req) {
        return Result.ok(captchaService.issue(req));
    }
}
