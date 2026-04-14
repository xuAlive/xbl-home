package com.xu.home.utils;

import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.InputStream;

/**
 * IP地址解析工具类
 * 使用ip2region库进行离线IP地址解析
 */
public class IpAddressUtil {
    private static final Logger logger = LoggerFactory.getLogger(IpAddressUtil.class);

    private static Searcher searcher = null;
    private static byte[] vIndex;

    static {
        try {
            // 从classpath加载ip2region.xdb文件
            ClassPathResource resource = new ClassPathResource("ip2region/ip2region.xdb");
            InputStream inputStream = resource.getInputStream();
            vIndex = FileCopyUtils.copyToByteArray(inputStream);
            searcher = Searcher.newWithBuffer(vIndex);
            logger.info("IP地址解析工具初始化成功");
        } catch (Exception e) {
            logger.error("IP地址解析工具初始化失败", e);
        }
    }

    public static void main(String[] args) {
        System.out.println(getAddress("125.121.174.146"));
    }
    /**
     * 根据IP地址获取地理位置信息
     * @param ip IP地址
     * @return 地理位置信息，格式：国家|区域|省份|城市|ISP
     */
    public static String getAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return "未知";
        }

        // 处理本地IP
        if (isLocalIp(ip)) {
            return "本地局域网";
        }

        try {
            if (searcher != null) {
                String region = searcher.search(ip);
                // ip2region返回格式：国家|区域|省份|城市|ISP
                // 格式化输出，去掉0的部分
                return formatAddress(region);
            }
        } catch (Exception e) {
            logger.error("解析IP地址失败: " + ip, e);
        }

        return "未知";
    }

    /**
     * 格式化地址信息
     * @param region 原始地址信息
     * @return 格式化后的地址
     */
    private static String formatAddress(String region) {
        if (region == null || region.trim().isEmpty()) {
            return "未知";
        }

        String[] parts = region.split("\\|");
        StringBuilder address = new StringBuilder();

        for (String part : parts) {
            if (part != null && !part.equals("0") && !part.trim().isEmpty()) {
                if (address.length() > 0) {
                    address.append("-");
                }
                address.append(part);
            }
        }

        return address.length() > 0 ? address.toString() : "未知";
    }

    /**
     * 判断是否为本地IP
     * @param ip IP地址
     * @return true-本地IP false-非本地IP
     */
    private static boolean isLocalIp(String ip) {
        return ip.equals("127.0.0.1")
            || ip.equals("0:0:0:0:0:0:0:1")
            || ip.equals("localhost")
            || ip.startsWith("192.168.")
            || ip.startsWith("10.")
            || (ip.startsWith("172.") && isInRange(ip, 16, 31));
    }

    /**
     * 判断IP是否在指定范围内（用于172.16.0.0-172.31.255.255）
     */
    private static boolean isInRange(String ip, int min, int max) {
        String[] parts = ip.split("\\.");
        if (parts.length >= 2) {
            try {
                int second = Integer.parseInt(parts[1]);
                return second >= min && second <= max;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
}
