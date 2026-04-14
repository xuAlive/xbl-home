package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.domain.blog.BlogImage;
import com.xu.home.mapper.blog.BlogImageMapper;
import com.xu.home.service.blog.BlogImageService;
import org.springframework.stereotype.Service;

@Service
public class BlogImageServiceImpl extends ServiceImpl<BlogImageMapper, BlogImage> implements BlogImageService {
}
