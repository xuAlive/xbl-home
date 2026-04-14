package com.xu.home.domain.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "blog_image")
@Data
public class BlogImage implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField(value = "account")
    private String account;

    @TableField(value = "original_name")
    private String originalName;

    @TableField(value = "file_name")
    private String fileName;

    @TableField(value = "file_path")
    private String filePath;

    @TableField(value = "image_url")
    private String imageUrl;

    @TableField(value = "file_size")
    private Long fileSize;

    @TableField(value = "create_time")
    private Date createTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
