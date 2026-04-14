package com.xu.home.controller.blog;

import com.xu.home.param.blog.po.blog.CommentListPo;
import com.xu.home.param.blog.po.blog.CommentPo;
import com.xu.home.service.blog.BlogCommentService;
import com.xu.home.Interceptor.common.annotation.RequirePermission;
import com.xu.home.param.common.response.Response;
import org.springframework.web.bind.annotation.*;

/**
 * 评论 Controller
 */
@RequestMapping("/blog/comment")
@RestController
public class CommentController {

    private final BlogCommentService commentService;

    public CommentController(BlogCommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 添加评论或回复
     */
    @PostMapping("/add")
    @RequirePermission("comment:add")
    public Response addComment(@RequestBody CommentPo po) {
        return commentService.addComment(po);
    }

    /**
     * 获取文章的评论列表
     */
    @GetMapping("/listByArticle")
    public Response listByArticle(CommentListPo po) {
        return commentService.listCommentsByArticle(po);
    }

    /**
     * 获取最新评论列表
     */
    @GetMapping("/listLatest")
    public Response listLatest(@RequestParam(value = "limit", defaultValue = "10") Integer limit) {
        return commentService.listLatestComments(limit);
    }

    /**
     * 删除评论
     */
    @PostMapping("/delete")
    @RequirePermission("comment:delete")
    public Response deleteComment(@RequestParam("id") Integer id, @RequestParam("account") String account) {
        return commentService.deleteComment(id, account);
    }
}
