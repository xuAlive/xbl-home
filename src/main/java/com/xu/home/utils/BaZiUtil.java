package com.xu.home.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 生辰八字计算工具类。
 * 1900-2100 年之间优先按节气算法计算；超出范围则回退到兼容的近似算法。
 */
public class BaZiUtil {

    private static final String[] TIAN_GAN = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
    private static final String[] DI_ZHI = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
    private static final String[] SHENG_XIAO = {"鼠", "牛", "虎", "兔", "龙", "蛇", "马", "羊", "猴", "鸡", "狗", "猪"};
    private static final String[] SHI_CHEN = {"子时", "丑时", "寅时", "卯时", "辰时", "巳时", "午时", "未时", "申时", "酉时", "戌时", "亥时"};
    private static final String[] SOLAR_TERMS = {
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
            "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
            "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
            "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
    };
    private static final int[] SOLAR_TERM_INFO = {
            0, 21208, 42467, 63836, 85337, 107014, 128867, 150921,
            173149, 195551, 218072, 240693, 263343, 285989, 308563, 331033,
            353350, 375494, 397447, 419210, 440795, 462224, 483532, 504758
    };
    private static final int[] LUNAR_INFO = {
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
            0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
            0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
            0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
            0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
            0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
            0x0d520
    };
    private static final int SUPPORTED_MIN_YEAR = 1900;
    private static final int SUPPORTED_MAX_YEAR = 2100;

    public static Map<String, Object> calculate(int year, int month, int day) {
        return calculate(year, month, day, null, null);
    }

    public static Map<String, Object> calculate(int year, int month, int day, Integer hour) {
        return calculate(year, month, day, hour, null);
    }

    public static Map<String, Object> calculate(int year, int month, int day, Integer hour, Integer minute) {
        Map<String, Object> result = new HashMap<>();
        LocalDate birthDate = LocalDate.of(year, month, day);
        boolean precise = year >= SUPPORTED_MIN_YEAR && year <= SUPPORTED_MAX_YEAR;

        String yearGanZhi = precise ? getYearGanZhiPrecise(birthDate) : getYearGanZhiLegacy(year);
        String monthGanZhi = precise ? getMonthGanZhiPrecise(birthDate) : getMonthGanZhiLegacy(year, month);
        String dayGanZhi = precise ? getDayGanZhiPrecise(birthDate) : getDayGanZhiLegacy(year, month, day);

        result.put("yearPillar", yearGanZhi);
        result.put("yearGan", yearGanZhi.substring(0, 1));
        result.put("yearZhi", yearGanZhi.substring(1, 2));

        int zodiacIndex = Math.floorMod((precise ? resolveGanZhiYear(birthDate) : year) - 4, 12);
        result.put("zodiac", SHENG_XIAO[zodiacIndex]);

        result.put("monthPillar", monthGanZhi);
        result.put("monthGan", monthGanZhi.substring(0, 1));
        result.put("monthZhi", monthGanZhi.substring(1, 2));

        result.put("dayPillar", dayGanZhi);
        result.put("dayGan", dayGanZhi.substring(0, 1));
        result.put("dayZhi", dayGanZhi.substring(1, 2));

        if (hour != null && hour >= 0 && hour <= 23) {
            String hourGanZhi = getHourGanZhi(dayGanZhi.substring(0, 1), hour);
            int shiChenIndex = getShiChenIndex(hour);
            result.put("hourPillar", hourGanZhi);
            result.put("hourGan", hourGanZhi.substring(0, 1));
            result.put("hourZhi", hourGanZhi.substring(1, 2));
            result.put("shiChen", SHI_CHEN[shiChenIndex]);
            result.put("baZi", yearGanZhi + " " + monthGanZhi + " " + dayGanZhi + " " + hourGanZhi);
            result.put("fourPillars", new String[]{yearGanZhi, monthGanZhi, dayGanZhi, hourGanZhi});
        } else {
            result.put("hourPillar", null);
            result.put("shiChen", null);
            result.put("baZi", yearGanZhi + " " + monthGanZhi + " " + dayGanZhi);
            result.put("fourPillars", new String[]{yearGanZhi, monthGanZhi, dayGanZhi});
        }

        LunarInfo lunarInfo = precise ? getLunarInfo(birthDate) : new LunarInfo(null, null, null, null);
        result.put("birthYear", year);
        result.put("birthMonth", month);
        result.put("birthDay", day);
        result.put("birthHour", hour);
        result.put("birthMinute", minute);
        result.put("lunarMonth", lunarInfo.lunarMonth());
        result.put("lunarDay", lunarInfo.lunarDay());
        result.put("lunarText", lunarInfo.lunarText());
        result.put("solarTerm", lunarInfo.solarTerm());
        result.put("algorithm", precise ? "solar-term" : "legacy");

        return result;
    }

    public static Map<String, Object> calculateFromLunar(int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth, Integer hour, Integer minute) {
        LocalDate solarDate = lunarToSolar(lunarYear, lunarMonth, lunarDay, isLeapMonth);
        Map<String, Object> result = calculate(solarDate.getYear(), solarDate.getMonthValue(), solarDate.getDayOfMonth(), hour, minute);
        result.put("inputLunarYear", lunarYear);
        result.put("inputLunarMonth", lunarMonth);
        result.put("inputLunarDay", lunarDay);
        result.put("inputLeapMonth", isLeapMonth);
        result.put("inputLunarText", buildLunarText(lunarMonth, lunarDay, isLeapMonth));
        result.put("solarDate", solarDate.toString());
        result.put("birthYear", lunarYear);
        result.put("birthMonth", lunarMonth);
        result.put("birthDay", lunarDay);
        result.put("calendarType", "lunar");
        return result;
    }

    public static Map<String, Object> calculate(LocalDateTime dateTime) {
        return calculate(
                dateTime.getYear(),
                dateTime.getMonthValue(),
                dateTime.getDayOfMonth(),
                dateTime.getHour(),
                dateTime.getMinute()
        );
    }

    public static String[] getTianGan() {
        return TIAN_GAN.clone();
    }

    public static String[] getDiZhi() {
        return DI_ZHI.clone();
    }

    public static String[] getShengXiao() {
        return SHENG_XIAO.clone();
    }

    public static String[] getShiChen() {
        return SHI_CHEN.clone();
    }

    private static String getYearGanZhiPrecise(LocalDate birthDate) {
        return toGanZhiYear(resolveGanZhiYear(birthDate));
    }

    private static int resolveGanZhiYear(LocalDate birthDate) {
        LocalDate liChun = LocalDate.of(birthDate.getYear(), 2, getSolarTermDay(birthDate.getYear(), 3));
        return birthDate.isBefore(liChun) ? birthDate.getYear() - 1 : birthDate.getYear();
    }

    private static String getMonthGanZhiPrecise(LocalDate birthDate) {
        int year = birthDate.getYear();
        int month = birthDate.getMonthValue();
        int firstNode = getSolarTermDay(year, month * 2 - 1);
        int offset = (year - 1900) * 12 + month + 11;
        if (birthDate.getDayOfMonth() >= firstNode) {
            offset++;
        }
        return toGanZhi(offset);
    }

    private static String getDayGanZhiPrecise(LocalDate birthDate) {
        int dayCyclical = (int) Math.floor(birthDate.toEpochDay() + 25567 + 10);
        return toGanZhi(dayCyclical);
    }

    private static String getHourGanZhi(String dayGan, int hour) {
        int dayGanIndex = getGanIndex(dayGan);
        int hourZhiIndex = getShiChenIndex(hour);
        int hourGanBase = switch (dayGanIndex % 5) {
            case 0 -> 0;
            case 1 -> 2;
            case 2 -> 4;
            case 3 -> 6;
            default -> 8;
        };
        return TIAN_GAN[(hourGanBase + hourZhiIndex) % 10] + DI_ZHI[hourZhiIndex];
    }

    private static int getShiChenIndex(int hour) {
        if (hour == 23 || hour == 0) {
            return 0;
        }
        return ((hour + 1) / 2) % 12;
    }

    private static int getGanIndex(String gan) {
        for (int i = 0; i < TIAN_GAN.length; i++) {
            if (TIAN_GAN[i].equals(gan)) {
                return i;
            }
        }
        return 0;
    }

    private static int getSolarTermDay(int year, int termIndex) {
        LocalDateTime offDate = LocalDateTime.of(1900, 1, 6, 2, 5)
                .plusNanos((long) ((31556925974.7D * (year - 1900) + SOLAR_TERM_INFO[termIndex - 1] * 60000L) * 1_000_000));
        return offDate.getDayOfMonth();
    }

    private static LunarInfo getLunarInfo(LocalDate birthDate) {
        LocalDate baseDate = LocalDate.of(1900, 1, 31);
        int offset = (int) (birthDate.toEpochDay() - baseDate.toEpochDay());
        if (offset < 0) {
            return new LunarInfo(null, null, null, null);
        }

        int year = 1900;
        while (year < 2101 && offset > 0) {
            int temp = getLunarYearDays(year);
            if (offset < temp) {
                break;
            }
            offset -= temp;
            year++;
        }

        int leapMonth = getLeapMonth(year);
        boolean isLeap = false;
        int month = 1;
        while (month <= 12) {
            int temp;
            if (leapMonth > 0 && month == leapMonth + 1 && !isLeap) {
                month--;
                isLeap = true;
                temp = getLeapDays(year);
            } else {
                temp = getMonthDays(year, month);
            }

            if (offset < temp) {
                break;
            }
            offset -= temp;
            if (isLeap && month == leapMonth + 1) {
                isLeap = false;
            }
            month++;
        }

        int lunarDay = offset + 1;
        return new LunarInfo(month, lunarDay, buildLunarText(month, lunarDay, isLeap), resolveSolarTerm(birthDate));
    }

    public static LocalDate lunarToSolar(int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth) {
        if (lunarYear < SUPPORTED_MIN_YEAR || lunarYear > SUPPORTED_MAX_YEAR) {
            throw new IllegalArgumentException("当前仅支持 1900-2100 年之间的农历日期");
        }
        int leapMonth = getLeapMonth(lunarYear);
        if (isLeapMonth && leapMonth != lunarMonth) {
            throw new IllegalArgumentException("该年份不存在对应的闰月");
        }
        int monthDays = isLeapMonth ? getLeapDays(lunarYear) : getMonthDays(lunarYear, lunarMonth);
        if (lunarDay < 1 || lunarDay > monthDays) {
            throw new IllegalArgumentException("农历日期不合法");
        }

        int offset = 0;
        for (int year = 1900; year < lunarYear; year++) {
            offset += getLunarYearDays(year);
        }

        for (int month = 1; month < lunarMonth; month++) {
            offset += getMonthDays(lunarYear, month);
            if (getLeapMonth(lunarYear) == month) {
                offset += getLeapDays(lunarYear);
            }
        }

        if (isLeapMonth && leapMonth == lunarMonth) {
            offset += getMonthDays(lunarYear, lunarMonth);
        }

        offset += lunarDay - 1;
        return LocalDate.of(1900, 1, 31).plusDays(offset);
    }

    private static String resolveSolarTerm(LocalDate birthDate) {
        int month = birthDate.getMonthValue();
        int day = birthDate.getDayOfMonth();
        int firstTermDay = getSolarTermDay(birthDate.getYear(), month * 2 - 1);
        int secondTermDay = getSolarTermDay(birthDate.getYear(), month * 2);
        if (day == firstTermDay) {
            return SOLAR_TERMS[month * 2 - 2];
        }
        if (day == secondTermDay) {
            return SOLAR_TERMS[month * 2 - 1];
        }
        return null;
    }

    private static String buildLunarText(int month, int day, boolean isLeap) {
        String[] monthNames = {"正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"};
        String[] dayFirst = {"初", "十", "廿", "卅"};
        String[] daySecond = {"日", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        String dayText;
        if (day == 10) {
            dayText = "初十";
        } else if (day == 20) {
            dayText = "二十";
        } else if (day == 30) {
            dayText = "三十";
        } else {
            dayText = dayFirst[day / 10] + daySecond[day % 10];
        }
        return (isLeap ? "闰" : "") + monthNames[month - 1] + "月" + dayText;
    }

    private static int getLunarYearDays(int year) {
        int sum = 348;
        int yearData = LUNAR_INFO[year - 1900];
        for (int i = 0x8000; i > 0x8; i >>= 1) {
            sum += (yearData & i) != 0 ? 1 : 0;
        }
        return sum + getLeapDays(year);
    }

    private static int getLeapMonth(int year) {
        return LUNAR_INFO[year - 1900] & 0xf;
    }

    private static int getLeapDays(int year) {
        return getLeapMonth(year) > 0 ? ((LUNAR_INFO[year - 1900] & 0x10000) != 0 ? 30 : 29) : 0;
    }

    private static int getMonthDays(int year, int month) {
        return (LUNAR_INFO[year - 1900] & (0x10000 >> month)) != 0 ? 30 : 29;
    }

    private static String getYearGanZhiLegacy(int year) {
        int offset = year - 1984;
        return TIAN_GAN[Math.floorMod(offset, 10)] + DI_ZHI[Math.floorMod(offset, 12)];
    }

    private static String getMonthGanZhiLegacy(int year, int month) {
        int yearGanIndex = Math.floorMod(year - 1984, 10);
        int monthZhiIndex = (month + 1) % 12;
        int monthGanBase = switch (yearGanIndex) {
            case 0, 5 -> 2;
            case 1, 6 -> 4;
            case 2, 7 -> 6;
            case 3, 8 -> 8;
            default -> 0;
        };
        return TIAN_GAN[(monthGanBase + month - 1) % 10] + DI_ZHI[monthZhiIndex];
    }

    private static String getDayGanZhiLegacy(int year, int month, int day) {
        int baseDays2000 = getDaysFrom1900(2000, 1, 1);
        int targetDays = getDaysFrom1900(year, month, day);
        int diff2000 = targetDays - baseDays2000;
        return TIAN_GAN[Math.floorMod(4 + diff2000, 10)] + DI_ZHI[Math.floorMod(10 + diff2000, 12)];
    }

    private static int getDaysFrom1900(int year, int month, int day) {
        int days = 0;
        for (int y = 1900; y < year; y++) {
            days += isLeapYear(y) ? 366 : 365;
        }

        int[] daysInMonth = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (isLeapYear(year)) {
            daysInMonth[1] = 29;
        }
        for (int m = 1; m < month; m++) {
            days += daysInMonth[m - 1];
        }
        return days + day;
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    private static String toGanZhiYear(int year) {
        int ganKey = (year - 3) % 10;
        int zhiKey = (year - 3) % 12;
        if (ganKey == 0) {
            ganKey = 10;
        }
        if (zhiKey == 0) {
            zhiKey = 12;
        }
        return TIAN_GAN[ganKey - 1] + DI_ZHI[zhiKey - 1];
    }

    private static String toGanZhi(int offset) {
        return TIAN_GAN[Math.floorMod(offset, 10)] + DI_ZHI[Math.floorMod(offset, 12)];
    }

    private record LunarInfo(Integer lunarMonth, Integer lunarDay, String lunarText, String solarTerm) {
    }
}
