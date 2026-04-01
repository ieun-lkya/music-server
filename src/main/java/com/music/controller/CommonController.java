package com.music.controller;

import com.music.common.Result;
import com.music.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/common") // 结合全局的 /api，实际访问路径是 /api/common/upload
public class CommonController {

    @Autowired
    private OssService ossService;

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        log.info("====== 1. [Controller] 接收到文件上传请求: [{}] ======", file.getOriginalFilename());
        // 调用我们写好的超强智能隔离版 OSS 上传服务
        String url = ossService.uploadFile(file);
        return Result.success(url);
    }
}