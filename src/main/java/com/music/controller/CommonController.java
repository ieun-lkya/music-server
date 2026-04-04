package com.music.controller;

import com.music.common.Result;
import com.music.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {

    @Autowired
    private OssService ossService;

    @PostMapping("/upload")
    public Result<String> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "common/") String folder) {
        
        log.info("====== [Controller] 接收到文件上传请求: [{}], 目标目录: [{}] ======", file.getOriginalFilename(), folder);
        
        //  终极隔离：强行提取后缀，生成全新的防弹 UUID 文件名
        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // 生成例如：avatar_a1b2c3d4.png
        String safeFileName = "avatar_" + UUID.randomUUID().toString().substring(0, 8) + ext;
        
        //  调用我们给 Admin 用的那个带目录指定能力的底层方法！
        String url = ossService.uploadFileWithCustomName(file, folder, safeFileName);
        
        return Result.success(url);
    }
}