package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.dao.blog.BlogDao;
import com.xu.home.dao.blog.SysUserDao;
import com.xu.home.domain.blog.SysUser;
import com.xu.home.domain.blog.SysUserInfo;
import com.xu.home.param.blog.po.sys.UserInfoPo;
import com.xu.home.param.blog.vo.sys.UserInfoVo;
import com.xu.home.param.blog.vo.sys.UserListVO;
import com.xu.home.service.blog.SysUserInfoService;
import com.xu.home.mapper.blog.SysUserInfoMapper;
import com.xu.home.param.common.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
* @author xubaolin
* @description 针对表【sys_user_info(系统用户详情表)】的数据库操作Service实现
* @createDate 2024-01-21 12:53:40
*/
@Service
public class SysUserInfoServiceImpl extends ServiceImpl<SysUserInfoMapper, SysUserInfo> implements SysUserInfoService{

    private final SysUserDao userDao;
    private final BlogDao blogDao;

    public SysUserInfoServiceImpl(SysUserDao userDao, BlogDao blogDao) {
        this.userDao = userDao;
        this.blogDao = blogDao;
    }

    @Override
    public Response getUserInfoByAccount(String account) {
        //判断账号是否为空
        if (StringUtils.isEmpty(account)){
            return Response.error("账号不能为空");
        }
        //验证账号是否存在
        SysUser sysUser = userDao.existSysUser(account);
        if (Objects.isNull(sysUser)){
            return Response.error("账号不存在，请检查账号是否正确");
        }
        //获取用户信息,如果用户信息为空，新建一个用户信息，赋值账号信息返回
        UserInfoVo userInfoVo = baseMapper.selectByAccount(account);
        if (Objects.isNull(userInfoVo)){
            userInfoVo = new UserInfoVo();
            userInfoVo.setAccount(account);
        }
        return Response.success(userInfoVo);
    }

    @Transactional
    @Override
    public Response updateUserInfo(UserInfoPo po) {
        if (Objects.isNull(po)){
            return Response.error("参数不能为空");
        }
        if (StringUtils.isEmpty(po.getAccount())){
            return Response.error("账号不能为空");
        }
        SysUserInfo sysUserInfo = new SysUserInfo();
        BeanUtils.copyProperties(po,sysUserInfo);
        // 修改sys_user 中的手机号
        if (StringUtils.isNotBlank(sysUserInfo.getPhone())){
            SysUser sysUser = new SysUser();
            sysUser.setPhone(sysUserInfo.getPhone());
            sysUser.setAccount(po.getAccount());
            userDao.updateUser(sysUser,new QueryWrapper<SysUser>().eq("account",po.getAccount()));
        }
        // 使用 INSERT ... ON DUPLICATE KEY UPDATE 语句
        // 如果 account 不存在则插入，存在则更新
        int result = baseMapper.insertOrUpdateUserInfo(sysUserInfo);
        return result > 0 ? Response.success() : Response.error("更新失败");
    }

    @Transactional
    @Override
    public Response deleteAccount(String account) {
        if (StringUtils.isEmpty(account)){
            return Response.error("账号不能为空");
        }
        // 先删除账号 ，再删除博客文章
        userDao.deleteAccount(account);
        blogDao.deleteBlogAccount(account);
        return Response.success();
    }

    @Override
    public Response getUserList() {
        List<UserListVO> userList = baseMapper.selectUserList();
        return Response.success(userList);
    }
}




