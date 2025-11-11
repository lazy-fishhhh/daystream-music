package com.future.music.constants;

/**
 * 系统常用格式校验正则表达式常量类
 */
public class RegexPatterns {

    // ================================== 账号与安全相关 ==================================
    /**
     * 登录账号校验：6-20位字母、数字、下划线，首位不能为数字
     */
    public static final String ACCOUNT_REGEX = "^[a-zA-Z_][a-zA-Z0-9_]{5,19}$";

    /**
     * 密码强度校验：8-20位，包含大小写字母、数字和特殊符号（!@#$%^&*）
     */
    public static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,20}$";

    /**
     * 邮箱格式校验：支持主流邮箱格式（含多级域名）
     */
    public static final String EMAIL_REGEX = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";

    /**
     * 手机号校验：中国大陆手机号（11位，以13/14/15/16/17/18/19开头）
     */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";

    /**
     * 身份证号校验：18位，支持最后一位为X（大小写）
     */
    public static final String ID_CARD_REGEX = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";


    // ================================== 用户信息相关 ==================================
    /**
     * 用户名/昵称校验：2-16位，支持汉字、字母、数字、下划线
     */
    public static final String USERNAME_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9_]{2,16}$";

    /**
     * 校验汉字性别：仅允许"未知"、"男"、"女"
     */
    public static final String GENDER_CHINESE_REGEX = "^(未知|男|女)$";

    /**
     * 地址校验：1-100位，支持汉字、字母、数字、标点（逗号、句号、空格等）
     */
    public static final String ADDRESS_REGEX = "^[\\u4e00-\\u9fa5a-zA-Z0-9,，.。\\s-]{1,100}$";

    /**
     * 个人简介校验：1-500位，支持常见字符（禁止包含脚本标签）
     */
    public static final String BIO_REGEX = "^[^<>'\"\\s]{1,500}$";



    // ================================== 工具方法 ==================================
    /**
     * 校验字符串是否匹配正则
     * @param str 待校验字符串
     * @param regex 正则表达式
     * @return 匹配结果（null或空字符串返回false）
     */
    public static boolean matches(String str, String regex) {
        if (str == null || str.trim().isEmpty()) {
            return false;
        }
        return str.matches(regex);
    }
}