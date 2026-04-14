package com.xu.home.dao.blog;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xu.home.domain.blog.SysUser;
import com.xu.home.domain.blog.SysUserLogin;
import com.xu.home.mapper.blog.SysUserInfoMapper;
import com.xu.home.mapper.blog.SysUserLoginMapper;
import com.xu.home.mapper.blog.SysUserMapper;
import com.xu.home.param.blog.vo.sys.LoginLocationStatsVO;
import com.xu.home.param.blog.vo.sys.ProvinceStatVO;
import com.xu.home.param.blog.vo.sys.UserLoginVO;
import com.xu.home.utils.IpAddressUtil;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统用户信息数据库交互层 降低service和其他mapper的依赖
 * 统一管理service和其他的mapper交互 以及其他没有service层的mapper处理
 * 方法需要注释明确，提高代码可阅读性
 */
@Component
public class SysUserDao {

    private final SysUserMapper userMapper;
    private final SysUserInfoMapper userInfoMapper;
    private final SysUserLoginMapper loginMapper;

    public SysUserDao(SysUserMapper userMapper, SysUserInfoMapper userInfoMapper, SysUserLoginMapper loginMapper) {
        this.userMapper = userMapper;
        this.userInfoMapper = userInfoMapper;
        this.loginMapper = loginMapper;
    }
    /**
     * 检查用户账号是否存在 ,存在返回账号
     * @param account
     * @return
     */
    public SysUser existSysUser(String account){
        List<SysUser> sysUserList = userMapper.selectList(new QueryWrapper<SysUser>().eq( "account",account));
        if (CollectionUtils.isEmpty(sysUserList)){
            return null;
        }
        // 一个账号只会对应一条数据
        return sysUserList.get(0);
    }

    public Integer updateUser(SysUser sysUser, Wrapper<SysUser>  wrapper){
        return userMapper.update(sysUser,wrapper);
    }

    /**
     * 插入用户登录记录，自动解析IP地址并保存地理位置信息
     * @param userLogin 用户登录信息
     * @return 是否插入成功
     */
    public Boolean insertUserLogin(SysUserLogin userLogin){
        // 根据IP地址解析地理位置
        if (userLogin.getIp() != null && !userLogin.getIp().isEmpty()) {
            String address = IpAddressUtil.getAddress(userLogin.getIp());
            userLogin.setAddress(address);
        }

        int insert = loginMapper.insert(userLogin);
        return insert == 1;
    }

    @Transactional
    public Boolean deleteAccount(String account) {
        userMapper.deleteUser(account);
        userInfoMapper.delteUserInfo(account);
        return true;
    }

    /**
     * 按账号和IP分组查询登录记录（分页）
     * @param account 账号（可选，为null时查询所有账号）
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 包含 records 和 total 的 Map
     */
    public Map<String, Object> getLoginRecords(String account, int page, int size) {
        int offset = (page - 1) * size;
        List<UserLoginVO> records = loginMapper.selectLoginRecordsGroupByAccountAndIp(account, offset, size);
        int total = loginMapper.countLoginRecordsGroupByAccountAndIp(account);
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    /**
     * 获取登录地点统计信息（地图 + 饼形图）
     * @param account 账号（可选）
     * @return 地点统计VO
     */
    public LoginLocationStatsVO getLoginLocationStats(String account) {
        List<UserLoginVO> locations = loginMapper.selectAllLoginLocations(account);
        List<ProvinceStatVO> provinceStats = loginMapper.selectLoginCountByProvince(account);
        LoginLocationStatsVO vo = new LoginLocationStatsVO();
        vo.setLocations(locations);
        vo.setProvinceStats(provinceStats);
        return vo;
    }
}
