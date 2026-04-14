package com.xu.home.config.blog;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "upload.image")
public class UploadConfig {

    /**
     * 图片存储路径，如 /home/img
     */
    private String storagePath;

    /**
     * 图片访问域名，如 https://www.xbl-blog.top
     */
    private String domain;

    /**
     * URL前缀，如 /img
     */
    private String urlPrefix;

    /**
     * 拼接完整的图片访问URL
     */
    public String buildImageUrl(String subDir, String fileName) {
        return domain + urlPrefix + "/" + subDir + "/" + fileName;
    }

    /**
     * 拼接完整的存储目录路径
     */
    public String buildStoragePath(String subDir) {
        return storagePath + "/" + subDir;
    }
}
