package com.music.service;

import org.springframework.web.multipart.MultipartFile;

public interface OssService {

    // 1. 原来的普通上传（自动生成 UUID）
    String uploadFile(MultipartFile file);

    // 2. 原来的字节流上传（自动生成 UUID）
    String uploadByteArray(byte[] content, String ext, String subFolder);

    // 3. 自定义文件名的普通上传 (处理音频、手动封面)
    String uploadFileWithCustomName(MultipartFile file, String dir, String fileName);

    // 4. 自定义文件名的字节流上传 (专治 MP3 自动提取封面)
    String uploadByteArrayWithCustomName(byte[] content, String dir, String fileName);

    // 5. 【修复红线】：根据完整 URL 删除阿里云上的废弃文件！
    void deleteFile(String fileUrl);

    void deleteOssFile(String fileUrl);

}