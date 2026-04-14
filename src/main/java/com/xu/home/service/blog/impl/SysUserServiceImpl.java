package com.xu.home.service.blog.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xu.home.dao.blog.SysUserDao;
import com.xu.home.domain.blog.SysRole;
import com.xu.home.domain.blog.SysUser;
import com.xu.home.domain.blog.SysUserInfo;
import com.xu.home.domain.blog.SysUserLogin;
import com.xu.home.mapper.blog.SysUserInfoMapper;
import com.xu.home.mapper.blog.SysUserMapper;
import com.xu.home.param.common.UserToken;
import com.xu.home.param.blog.po.sys.LoginUserPo;
import com.xu.home.service.blog.SysRoleService;
import com.xu.home.service.blog.SysUserService;
import com.xu.home.utils.PasswordUtil;
import com.xu.home.utils.common.JWTUtil;
import com.xu.home.param.common.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

/**
* @author xubaolin
* @description 针对表【sys_user(系统用户表)】的数据库操作Service实现
* @createDate 2024-01-21 12:53:40
*/
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService{

    private final SysUserDao userDao;
    private final SysRoleService sysRoleService;
    private final SysUserInfoMapper sysUserInfoMapper;

    public SysUserServiceImpl(SysUserDao userDao, SysRoleService sysRoleService, SysUserInfoMapper sysUserInfoMapper) {
        this.userDao = userDao;
        this.sysRoleService = sysRoleService;
        this.sysUserInfoMapper = sysUserInfoMapper;
    }


    @Override
    public Response login(LoginUserPo po, HttpServletRequest servletRequest) {
        //判断对象是否为空
        if(po == null){
            return Response.error("登录信息不能为空");
        }
        // TODO 验证码是否正确
        if (StringUtils.isEmpty(po.getAccount()) || StringUtils.isEmpty(po.getPassword())){
            return Response.error("用户名或密码不能为空");
        }
        //验证账号是否存在
        SysUser sysUser = baseMapper.selectUser(po.getAccount(),null,null);
        if (Objects.isNull(sysUser)){
            return Response.error("账号不存在");
        }
        //验证密码是否正确
        if (!PasswordUtil.matches(po.getPassword(), sysUser.getPassword())){
            return Response.error("密码错误");
        }
        if (PasswordUtil.needsUpgrade(sysUser.getPassword())) {
            SysUser updateUser = new SysUser();
            updateUser.setPassword(PasswordUtil.encode(po.getPassword()));
            baseMapper.update(updateUser, new QueryWrapper<SysUser>().eq("account", po.getAccount()));
        }
        //登陆成功 记录登陆用户的ip
        String userIP = servletRequest.getHeader("X-Forwarded-For");
        if (userIP != null && userIP.contains(",")) {
            userIP = userIP.split(",")[0].trim();
        }
        if (userIP == null || userIP.isBlank()) {
            userIP = servletRequest.getRemoteAddr();
        }

        SysUserLogin sysUserLogin = new SysUserLogin();
        sysUserLogin.setAccount(po.getAccount());
        sysUserLogin.setIp(userIP);
        userDao.insertUserLogin(sysUserLogin);
        //生成token
        UserToken userToken = new UserToken();
        // 设置用户信息到token中
        userToken.setAccount(sysUser.getAccount());
        userToken.setPhone(sysUser.getPhone());
        // 可选：设置用户名，这里暂时设置为账号，如需从user_info表获取可以额外查询
        userToken.setUserName(sysUser.getAccount());

        String token = JWTUtil.createToken(userToken);
        log.info("{} 登陆成功", po.getAccount());
        return Response.success(token);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response register(LoginUserPo po) {
        //判断对象是否为空
        if(po == null){
            return Response.error("注册信息不能为空");
        }
        if (StringUtils.isEmpty(po.getAccount()) || StringUtils.isEmpty(po.getPassword())){
            return Response.error("用户名或密码不能为空");
        }
        //验证账号是否存在
        List<SysUser> sysUsers = baseMapper.selectList(new QueryWrapper<SysUser>().eq("account",po.getAccount()));
        if (!CollectionUtils.isEmpty(sysUsers)){
            return Response.error("账号已存在");
        }

        try {
            SysUser sysUser = new SysUser();
            BeanUtils.copyProperties(po,sysUser);
            sysUser.setPassword(PasswordUtil.encode(po.getPassword()));
            // 新注册用户默认手机号未验证
            sysUser.setPhoneVerified(0);
            int insert = baseMapper.insert(sysUser);

            if (insert == 1) {
                // 为新用户分配GUEST角色
                SysRole guestRole = sysRoleService.selectByRoleCode("GUEST");
                if (guestRole != null) {
                    sysRoleService.assignRoleToUser(po.getAccount(), guestRole.getId());
                    log.info("用户{}注册成功，分配GUEST角色", po.getAccount());
                } else {
                    log.warn("GUEST角色不存在，请检查数据库");
                }

                // 创建用户详情记录（保存昵称）
                if (!StringUtils.isEmpty(po.getNickname())) {
                    SysUserInfo userInfo = new SysUserInfo();
                    userInfo.setAccount(po.getAccount());
                    userInfo.setName(po.getNickname());
                    sysUserInfoMapper.insert(userInfo);
                }

                // 生成token并返回，注册后直接登录
                UserToken userToken = new UserToken();
                userToken.setAccount(sysUser.getAccount());
                userToken.setUserName(!StringUtils.isEmpty(po.getNickname()) ? po.getNickname() : sysUser.getAccount());
                String token = JWTUtil.createToken(userToken);
                log.info("{} 注册成功，返回token", po.getAccount());
                return Response.success(token);
            }
            return Response.error("注册失败");
        } catch (Exception e) {
            log.error("注册失败", e);
            throw new RuntimeException("注册失败");
        }
    }


}



