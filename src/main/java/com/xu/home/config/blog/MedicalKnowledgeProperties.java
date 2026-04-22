package com.xu.home.config.blog;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "medical.knowledge")
public class MedicalKnowledgeProperties {

    private String uploadDir;

    private List<String> allowedLocalRoots = new ArrayList<>();

    private Integer chunkSize = 1600;

    private Integer maxChunkSize = 2200;

    private Integer aiTimeoutSeconds = 60;

    private Integer progressUpdateInterval = 5;

    public Path resolveUploadDir() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public List<Path> resolveAllowedLocalRoots() {
        List<Path> result = new ArrayList<>();
        for (String root : allowedLocalRoots) {
            if (root != null && !root.isBlank()) {
                result.add(Paths.get(root.trim()).toAbsolutePath().normalize());
            }
        }
        return result;
    }
}
