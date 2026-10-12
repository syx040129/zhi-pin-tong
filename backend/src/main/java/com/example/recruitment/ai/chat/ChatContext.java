package com.example.recruitment.ai.chat;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 智能助手上下文（数据感知）：把与当前用户相关的业务数据汇总成一份精简资料，
 * 既用于真实模式拼装 Prompt，也用于 Mock 规则回答，避免“答非所问”。
 */
@Data
public class ChatContext {

    /** USER / COMPANY / ADMIN，未登录为 null */
    private String role;

    /* ===== 求职者 ===== */
    private Long resumeId;
    private String resumeName;
    private String education;
    private String school;
    private String major;
    private List<String> skills = new ArrayList<>();
    private Integer completionScore;
    private String summary;
    private Integer experienceCount = 0;
    private List<String> experienceTitles = new ArrayList<>();
    private int applicationCount;
    private int interviewCount;
    /** 推荐岗位：岗位名 -> 匹配度，按匹配度降序，最多 5 条 */
    private Map<String, Integer> topJobs = new LinkedHashMap<>();

    /* ===== 企业 ===== */
    private Long companyId;
    private String companyName;
    private int companyJobCount;
    private int companyOnlineJobCount;
    private int companyPendingApplicationCount;

    /* ===== 管理员 ===== */
    private int platformUserCount;
    private int platformCompanyCount;
    private int platformOnlineJobCount;
    private int platformPendingReportCount;

    /* ===== 岗位详情的即时上下文（“问这个岗位”） ===== */
    private Long focusJobId;
    private String focusJobName;
    private String focusJobCompany;
    private String focusJobCity;
    private String focusJobEducation;
    private String focusJobExperience;
    private String focusJobSalary;
    private String focusJobDescription;
    private List<String> focusJobSkills = new ArrayList<>();
    private List<String> focusMatchedSkills = new ArrayList<>();
    private List<String> focusMissingSkills = new ArrayList<>();

    public boolean hasResume() {
        return resumeId != null;
    }

    public boolean hasFocusJob() {
        return focusJobId != null;
    }

    public String skillText() {
        return skills == null || skills.isEmpty() ? "（简历中尚未填写技能）" : String.join("、", skills);
    }

    public String topJobText() {
        if (topJobs == null || topJobs.isEmpty()) {
            return "（暂无推荐结果，可到「智能匹配」页生成）";
        }
        StringBuilder sb = new StringBuilder();
        topJobs.forEach((name, score) -> sb.append(name).append("（").append(score).append(" 分）、"));
        return sb.substring(0, sb.length() - 1);
    }
}
