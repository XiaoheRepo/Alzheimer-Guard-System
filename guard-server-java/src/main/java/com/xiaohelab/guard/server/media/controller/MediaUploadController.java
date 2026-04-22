package com.xiaohelab.guard.server.media.controller;

import com.xiaohelab.guard.server.common.dto.Result;
import com.xiaohelab.guard.server.common.security.AuthUser;
import com.xiaohelab.guard.server.common.security.SecurityUtil;
import com.xiaohelab.guard.server.media.dto.MediaUploadSignRequest;
import com.xiaohelab.guard.server.media.service.MediaUploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 媒体直传凭证接口（V2.1 §3.8.3）。 */
@Tag(name = "Media", description = "对象存储直传凭证")
@RestController
@RequestMapping("/api/v1/media")
public class MediaUploadController {

    private final MediaUploadService service;

    public MediaUploadController(MediaUploadService service) {
        this.service = service;
    }

    @PostMapping("/upload-sign")
    public Result<Map<String, Object>> uploadSign(@Valid @RequestBody MediaUploadSignRequest req) {
        AuthUser user = SecurityUtil.current();
        return Result.ok(service.sign(user.getUserId(), req));
    }
}
