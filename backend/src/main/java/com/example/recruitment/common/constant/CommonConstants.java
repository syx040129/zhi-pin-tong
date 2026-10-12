package com.example.recruitment.common.constant;

/**
 * 通用常量
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** 响应状态码 */
    public static final int CODE_OK = 200;
    public static final int CODE_BAD_REQUEST = 400;
    public static final int CODE_UNAUTHORIZED = 401;
    public static final int CODE_FORBIDDEN = 403;
    public static final int CODE_NOT_FOUND = 404;
    public static final int CODE_CONFLICT = 409;
    public static final int CODE_SERVER_ERROR = 500;
    public static final int CODE_AI_ERROR = 503;

    /** 用户状态 */
    public static final String USER_ENABLED = "ENABLED";
    public static final String USER_DISABLED = "DISABLED";

    /** 岗位状态 */
    public static final String JOB_ONLINE = "ONLINE";
    public static final String JOB_OFFLINE = "OFFLINE";

    /** 企业状态 */
    public static final String COMPANY_ACTIVE = "ACTIVE";
    public static final String COMPANY_DISABLED = "DISABLED";

    /** 投递状态 */
    public static final String APP_PENDING = "PENDING";
    public static final String APP_VIEWED = "VIEWED";
    public static final String APP_INTERVIEW = "INTERVIEW";
    public static final String APP_REJECTED = "REJECTED";
    public static final String APP_HIRED = "HIRED";

    /** AI 面试状态 */
    public static final String INTERVIEW_ONGOING = "ONGOING";
    public static final String INTERVIEW_COMPLETED = "COMPLETED";

    /** 举报状态 */
    public static final String REPORT_PENDING = "PENDING";
    public static final String REPORT_HANDLED = "HANDLED";
    public static final String REPORT_IGNORED = "IGNORED";

    /** 日志类型 */
    public static final String LOG_LOGIN = "LOGIN";
    public static final String LOG_OPERATE = "OPERATE";
    public static final String LOG_AI = "AI";
    public static final String LOG_ERROR = "ERROR";
}
