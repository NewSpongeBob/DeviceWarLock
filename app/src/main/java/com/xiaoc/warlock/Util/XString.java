package com.xiaoc.warlock.Util;

import java.util.List;

public class XString {
    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 获取字符串的长度，null 返回 0
     */
    public static int length(String str) {
        return str == null ? 0 : str.length();
    }

    /**
     * 首字母大写
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * 判断是否是手机号
     */
    public static boolean isPhoneNumber(String phone) {
        if (isEmpty(phone)) return false;
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 判断是否是邮箱
     */
    public static boolean isEmail(String email) {
        if (isEmpty(email)) return false;
        return email.matches("^[a-zA-Z0-9_.-]+@[a-zA-Z0-9-]+(\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z0-9]{2,6}$");
    }
    /**
     * 比较两个字符串是否完全相同
     */
    public static boolean compareExact(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equals(str2);
    }

    /**
     * 比较两个字符串是否相同（忽略大小写）
     */
    public static boolean compareIgnoreCase(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.equalsIgnoreCase(str2);
    }

    /**
     * 比较两个字符串是否相似（忽略空格）
     */
    public static boolean compareIgnoreSpaces(String str1, String str2) {
        if (str1 == null && str2 == null) return true;
        if (str1 == null || str2 == null) return false;
        return str1.replaceAll("\\s+", "").equals(str2.replaceAll("\\s+", ""));
    }

    /**
     * 比较两个字符串并输出差异
     */
    public static String getDifference(String str1, String str2) {
        if (str1 == null && str2 == null) return "Both strings are null";
        if (str1 == null) return "First string is null, second string: " + str2;
        if (str2 == null) return "Second string is null, first string: " + str1;

        StringBuilder diff = new StringBuilder();
        if (str1.equals(str2)) {
            diff.append("Strings are identical");
        } else {
            diff.append("Strings are different:\n");
            diff.append("String 1 (length ").append(str1.length()).append("): ").append(str1).append("\n");
            diff.append("String 2 (length ").append(str2.length()).append("): ").append(str2).append("\n");

            // 找出第一个不同的字符位置
            int minLength = Math.min(str1.length(), str2.length());
            for (int i = 0; i < minLength; i++) {
                if (str1.charAt(i) != str2.charAt(i)) {
                    diff.append("First difference at position ").append(i)
                            .append(" (char1='").append(str1.charAt(i))
                            .append("', char2='").append(str2.charAt(i))
                            .append("')");
                    break;
                }
            }
        }
        return diff.toString();
    }

    /**
     * 计算两个字符串的相似度（使用Levenshtein距离）
     */
    public static double getSimilarity(String str1, String str2) {
        if (str1 == null && str2 == null) return 1.0;
        if (str1 == null || str2 == null) return 0.0;

        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1] + 1,
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1));
                }
            }
        }

        int maxLength = Math.max(str1.length(), str2.length());
        return 1.0 - (double) dp[str1.length()][str2.length()] / maxLength;
    }
    /**
     * 比较多个字符串是否完全相同
     */
    public static boolean compareExact(String... strings) {
        if (strings == null || strings.length < 2) return true;
        String first = strings[0];
        for (String str : strings) {
            if (!compareExact(first, str)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较多个字符串是否相同（忽略大小写）
     */
    public static boolean compareIgnoreCase(String... strings) {
        if (strings == null || strings.length < 2) return true;
        String first = strings[0];
        for (String str : strings) {
            if (!compareIgnoreCase(first, str)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较多个字符串是否相似（忽略空格）
     */
    public static boolean compareIgnoreSpaces(String... strings) {
        if (strings == null || strings.length < 2) return true;
        String first = strings[0];
        for (String str : strings) {
            if (!compareIgnoreSpaces(first, str)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较字符串列表是否完全相同
     */
    public static boolean compareExact(List<String> strings) {
        if (strings == null || strings.size() < 2) return true;
        String first = strings.get(0);
        for (String str : strings) {
            if (!compareExact(first, str)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较字符串列表是否相同（忽略大小写）
     */
    public static boolean compareIgnoreCase(List<String> strings) {
        if (strings == null || strings.size() < 2) return true;
        String first = strings.get(0);
        for (String str : strings) {
            if (!compareIgnoreCase(first, str)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 比较字符串列表是否相似（忽略空格）
     */
    public static boolean compareIgnoreSpaces(List<String> strings) {
        if (strings == null || strings.size() < 2) return true;
        String first = strings.get(0);
        for (String str : strings) {
            if (!compareIgnoreSpaces(first, str)) {
                return false;
            }
        }
        return true;
    }
}
