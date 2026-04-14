package com.xu.home.controller.blog;

import com.xu.home.config.blog.UploadConfig;
import com.xu.home.domain.blog.BlogImage;
import com.xu.home.param.blog.po.blog.ArticlePo;
import com.xu.home.param.blog.po.blog.BlogArticlePo;
import com.xu.home.service.blog.BlogArticleInfoService;
import com.xu.home.service.blog.BlogArticleBrowsingHistoryService;
import com.xu.home.service.blog.BlogImageService;
import com.xu.home.Interceptor.common.annotation.RequirePermission;
import com.xu.home.utils.common.context.UserContext;
import com.xu.home.param.common.response.Response;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RequestMapping("/blog/article")
@RestController
public class ArticleController {

    private final BlogArticleInfoService articleInfoService;
    private final BlogArticleBrowsingHistoryService browsingHistoryService;
    private final BlogImageService blogImageService;
    private final UploadConfig uploadConfig;

    public ArticleController(BlogArticleInfoService articleInfoService,
                             BlogArticleBrowsingHistoryService browsingHistoryService,
                             BlogImageService blogImageService,
                             UploadConfig uploadConfig) {
        this.articleInfoService = articleInfoService;
        this.browsingHistoryService = browsingHistoryService;
        this.blogImageService = blogImageService;
        this.uploadConfig = uploadConfig;
    }

    @PostMapping("/createOrUpdateArticle")
    @RequirePermission("article:publish")
    public Response createOrUpdateArticle(@RequestBody BlogArticlePo po){
        return articleInfoService.createOrUpdateArticle(po);
    }

    @GetMapping("/listArticle")
    public Response listArticle(ArticlePo po){
        return articleInfoService.listArticle(po);
    }

    @PostMapping("/deleteArticle")
    @RequirePermission("article:delete")
    public Response deleteArticle(@RequestBody BlogArticlePo po){
        return articleInfoService.deleteArticle(po.getId(),po.getAccount());
    }

    @GetMapping("/getArticleById")
    public Response getArticleById(Integer id,
                                   @RequestParam(value = "recordView", defaultValue = "true") Boolean recordView){
        String viewerAccount = Boolean.TRUE.equals(recordView) ? UserContext.getCurrentAccount() : null;
        return articleInfoService.getArticleById(id, viewerAccount);
    }

    @GetMapping("/listBrowsingHistory")
    public Response listBrowsingHistory() {
        return browsingHistoryService.listBrowsingHistory(UserContext.getCurrentAccount());
    }

    @PostMapping("/deleteBrowsingHistory")
    public Response deleteBrowsingHistory(@RequestParam("id") Integer id) {
        return browsingHistoryService.deleteBrowsingHistory(UserContext.getCurrentAccount(), id);
    }

    @PostMapping("/clearBrowsingHistory")
    public Response clearBrowsingHistory() {
        return browsingHistoryService.clearBrowsingHistory(UserContext.getCurrentAccount());
    }

    @PostMapping("/uploadImage")
    @RequirePermission("article:publish")
    public Response uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Response.error("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            return Response.error("文件名不能为空");
        }
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }
        LocalDateTime now = LocalDateTime.now();
        String subDir = now.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String fileName = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + "_" + System.currentTimeMillis() + extension;
        String dirPath = uploadConfig.buildStoragePath(subDir);
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            file.transferTo(new File(dirPath + "/" + fileName));
        } catch (IOException e) {
            return Response.error("图片上传失败: " + e.getMessage());
        }
        String imageUrl = uploadConfig.buildImageUrl(subDir, fileName);

        // 记录图片上传信息到数据库
        BlogImage blogImage = new BlogImage();
        blogImage.setAccount(UserContext.getCurrentUser().getAccount());
        blogImage.setOriginalName(originalFilename);
        blogImage.setFileName(fileName);
        blogImage.setFilePath(dirPath + "/" + fileName);
        blogImage.setImageUrl(imageUrl);
        blogImage.setFileSize(file.getSize());
        blogImageService.save(blogImage);

        return Response.success(imageUrl);
    }
}
