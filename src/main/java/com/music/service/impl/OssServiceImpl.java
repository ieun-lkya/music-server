package com.music.service.impl;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.music.config.OssProperties;
import com.music.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
public class OssServiceImpl implements OssService {

    @Autowired
    private OssProperties ossProperties;

    // 定义项目专属的 OSS 根目录，防止污染你原来的文件
    private static final String PROJECT_ROOT = "echo_music/";


    @Override
    public String uploadByteArray(byte[] content, String ext, String subFolder) {
        String monthDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;
        String objectName = PROJECT_ROOT + subFolder + monthDir + "/" + newFileName;

        OSS ossClient = null;
        try {
            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

            ossClient = OSSClientBuilder.create()
                    .endpoint(ossProperties.getEndpoint())
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(ossProperties.getRegion())
                    .build();

            // 这里完美使用了你之前老项目里的 ByteArrayInputStream ！
            java.io.InputStream inputStream = new java.io.ByteArrayInputStream(content);
            ossClient.putObject(ossProperties.getBucketName(), objectName, inputStream);

            return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + objectName;
        } catch (Exception e) {
            log.error("====== [OSS] 字节流上传异常 ======", e);
            throw new RuntimeException("字节流上传失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public String uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new RuntimeException("文件名不能为空");
        }

        // 1. 获取后缀名并统一转成小写 (如 .mp3, .jpg)
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        // 2. 【核心新增】：智能路由，判断是歌曲还是封面
        String subFolder = "others/"; // 兜底目录
        if (ext.equals(".mp3") || ext.equals(".wav") || ext.equals(".flac")) {
            subFolder = "songs/";
        } else if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp")) {
            subFolder = "covers/";
        }

        // 3. 继续保留按月归档的好习惯
        String monthDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String newFileName = UUID.randomUUID().toString().replace("-", "") + ext;

        // 4. 拼装最终的超强隔离路径： echo_music/songs/2026/03/xxxx.mp3
        String objectName = PROJECT_ROOT + subFolder + monthDir + "/" + newFileName;

        OSS ossClient = null;
        try {
            // 魔法代码：读取系统环境变量
            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

            ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
            clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

            ossClient = OSSClientBuilder.create()
                    .endpoint(ossProperties.getEndpoint())
                    .credentialsProvider(credentialsProvider)
                    .clientConfiguration(clientBuilderConfiguration)
                    .region(ossProperties.getRegion())
                    .build();

            // 上传文件
            InputStream inputStream = file.getInputStream();
            ossClient.putObject(ossProperties.getBucketName(), objectName, inputStream);

            // 拼接返回给 Vue 的可访问 URL
            String url = "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + objectName;
            log.info("====== [OSS] 文件上传成功，分类路径: [{}], 完整链接: [{}] ======", objectName, url);
            return url;

        } catch (Exception e) {
            log.error("====== [OSS] 文件上传异常 ======", e);
            throw new RuntimeException("文件上传失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }
        OSS ossClient = null;
        try {
            // 1. 拼接出你专属的阿里云域名根路径
            String domain = "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/";

            // 2. 校验：如果这个链接不是咱们阿里云的（比如是默认头像），就别乱删
            if (!fileUrl.startsWith(domain)) {
                return;
            }

            // 3. 剥离域名，提取出真正的 objectName (例如: covers/2026/03/xxx.jpg)
            String objectName = fileUrl.replace(domain, "");

            // 4. 初始化 OSS 客户端并执行删除
            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();
            ossClient = OSSClientBuilder.create()
                    .endpoint(ossProperties.getEndpoint())
                    .credentialsProvider(credentialsProvider)
                    .build();

            ossClient.deleteObject(ossProperties.getBucketName(), objectName);
            log.info("====== [OSS] 成功清理云端废弃文件: {} ======", objectName);

        } catch (Exception e) {
            // 删除失败不抛出异常，只打日志，防止阻断主业务
            log.error("====== [OSS] 云端文件清理失败: {} ======", fileUrl, e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public String uploadFileWithCustomName(org.springframework.web.multipart.MultipartFile file, String dir, String fileName) {
        OSS ossClient = null;
        try {
            // 直接拼接你想要的目录和文件名 (例如: songs/七里香-周杰伦.mp3)
            String objectName = PROJECT_ROOT + dir + fileName;

            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

            ossClient = OSSClientBuilder.create()
                    .endpoint(ossProperties.getEndpoint())
                    .credentialsProvider(credentialsProvider)
                    .build();

            ossClient.putObject(ossProperties.getBucketName(), objectName, file.getInputStream());

            // 返回拼接好的阿里云链接
            return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + objectName;
        } catch (Exception e) {
            log.error("====== [OSS] 自定义命名上传异常 ======", e);
            throw new RuntimeException("文件上传失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public String uploadByteArrayWithCustomName(byte[] content, String dir, String fileName) {
        OSS ossClient = null;
        try {
            String objectName = PROJECT_ROOT + dir + fileName;
            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

            ossClient = OSSClientBuilder.create()
                    .endpoint(ossProperties.getEndpoint())
                    .credentialsProvider(credentialsProvider)
                    .build();

            java.io.InputStream inputStream = new java.io.ByteArrayInputStream(content);
            ossClient.putObject(ossProperties.getBucketName(), objectName, inputStream);

            return "https://" + ossProperties.getBucketName() + "." + ossProperties.getEndpoint() + "/" + objectName;
        } catch (Exception e) {
            log.error("====== [OSS] 字节流自定义命名上传异常 ======", e);
            throw new RuntimeException("字节流上传失败");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    @Override
    public void deleteOssFile(String fileUrl) {
        // 1. 防呆校验：如果是空，或者是默认头像，直接放行
        if (fileUrl == null || fileUrl.isEmpty() || !fileUrl.contains("aliyuncs.com")) {
            return;
        }

        try {
            // 2. 从完整 URL 中精准剥离出 ObjectName
            String endpointStr = "aliyuncs.com/";
            int index = fileUrl.indexOf(endpointStr);
            if (index == -1) return;
            String objectName = fileUrl.substring(index + endpointStr.length());

            // 阿里云官方底层会自动去读 ALIBABA_CLOUD_ACCESS_KEY_ID 和 ALIBABA_CLOUD_ACCESS_KEY_SECRET 两个环境变量
            EnvironmentVariableCredentialsProvider credentialsProvider =
                    CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();


            // 4. 构建极其安全的 OSS 客户端
            OSS ossClient = new OSSClientBuilder().build(
                    ossProperties.getEndpoint(),
                    credentialsProvider
            );

            // 5. 执行物理毁灭！
            ossClient.deleteObject(ossProperties.getBucketName(), objectName);

            // 记得关门，释放连接
            ossClient.shutdown();

            System.out.println("架构师指令生效，环境变量鉴权通过，已彻底清理云盘垃圾: " + objectName);
        } catch (Exception e) {
            System.err.println("阿里云文件删除失败：" + e.getMessage());
        }
    }

}