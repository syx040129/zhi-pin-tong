package com.example.recruitment.ai.chat;

import com.example.recruitment.ai.entity.AiChatMessage;
import com.example.recruitment.common.constant.RoleConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 真实模式 Prompt 设计：把「角色人设 + 用户业务资料 + 多轮对话历史」拼装为一次调用。
 * 约定模型只输出 JSON：{reply, suggestions[]}，与 Mock 模式输出结构保持一致。
 */
@Component
public class ChatPrompt {

    /** 助手人设与输出约束（每次调用作为 system） */
    public String systemPrompt(ChatContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是「智聘AI」招聘平台的智能助手，服务求职者、企业招聘方与平台管理员。\n")
                .append("回答要求：\n")
                .append("1. 只回答招聘、求职、简历、面试、职业发展、平台使用相关的问题，其他问题礼貌拒答并引导回招聘话题；\n")
                .append("2. 用中文、口语化，结论先行，分点说明；纯文本，不要使用 Markdown 语法（如需编号用 1. 2. 3.）；\n")
                .append("3. 全文控制在 400 字以内，信息密度高，不说客套话；\n")
                .append("4. 结合下方用户资料作答，能引用具体技能、岗位名称与匹配度就引用；资料为空时给出通用建议，不要编造用户数据；\n")
                .append("5. 只输出 JSON，不要输出任何多余文字，格式：")
                .append("{\"reply\":\"回答正文\",\"suggestions\":[\"推荐追问1\",\"推荐追问2\",\"推荐追问3\"]}；")
                .append("suggestions 是用户看完这条回答后最可能继续追问的 3 个短问题（每个不超过 20 字）。");
        return sb.toString();
    }

    /** 用户资料 + 对话历史（每次调用作为 user） */
    public String userPrompt(ChatContext context, List<AiChatMessage> history, String question) {
        StringBuilder sb = new StringBuilder();
        sb.append("【当前用户资料】\n");
        sb.append(roleLine(context));
        if (RoleConstants.COMPANY.equals(context.getRole())) {
            sb.append("企业名称：").append(nvl(context.getCompanyName(), "未完善")).append("\n");
            sb.append("在招岗位：").append(context.getCompanyJobCount())
                    .append(" 个（在线 ").append(context.getCompanyOnlineJobCount()).append(" 个）\n");
            sb.append("待处理投递：").append(context.getCompanyPendingApplicationCount()).append(" 条\n");
        } else if (RoleConstants.ADMIN.equals(context.getRole())) {
            sb.append("平台注册求职者：").append(context.getPlatformUserCount()).append(" 人\n");
            sb.append("入驻企业：").append(context.getPlatformCompanyCount()).append(" 家\n");
            sb.append("在线岗位：").append(context.getPlatformOnlineJobCount()).append(" 个\n");
            sb.append("待处理举报：").append(context.getPlatformPendingReportCount()).append(" 条\n");
        } else {
            if (context.hasResume()) {
                sb.append("简历：").append(nvl(context.getResumeName(), "未命名"))
                        .append("，学历 ").append(nvl(context.getEducation(), "未填写"))
                        .append("，专业 ").append(nvl(context.getMajor(), "未填写"))
                        .append("，学校 ").append(nvl(context.getSchool(), "未填写"))
                        .append("，完整度 ").append(context.getCompletionScore() == null ? "未知" : context.getCompletionScore())
                        .append("\n");
                sb.append("技能：").append(context.skillText()).append("\n");
                if (StringUtils.hasText(context.getSummary())) {
                    sb.append("个人简介：").append(abbreviate(context.getSummary(), 200)).append("\n");
                }
                if (!context.getExperienceTitles().isEmpty()) {
                    sb.append("经历数量：").append(context.getExperienceCount())
                            .append("（").append(abbreviate(String.join("、", context.getExperienceTitles()), 120)).append("）\n");
                }
            } else {
                sb.append("简历：暂无\n");
            }
            sb.append("投递记录：").append(context.getApplicationCount())
                    .append(" 条，其中面试中 ").append(context.getInterviewCount()).append(" 条\n");
            sb.append("系统推荐岗位：").append(context.topJobText()).append("\n");
        }
        if (context.hasFocusJob()) {
            sb.append("【用户当前正在浏览的岗位】\n");
            sb.append("岗位：").append(context.getFocusJobName());
            if (StringUtils.hasText(context.getFocusJobCompany())) {
                sb.append("（").append(context.getFocusJobCompany()).append("）");
            }
            sb.append("\n");
            sb.append("城市：").append(nvl(context.getFocusJobCity(), "不限"))
                    .append("，薪资：").append(nvl(context.getFocusJobSalary(), "面议"))
                    .append("，经验：").append(nvl(context.getFocusJobExperience(), "不限"))
                    .append("，学历：").append(nvl(context.getFocusJobEducation(), "不限")).append("\n");
            if (!context.getFocusJobSkills().isEmpty()) {
                sb.append("要求技能：").append(String.join("、", context.getFocusJobSkills())).append("\n");
            }
            if (!context.getFocusMatchedSkills().isEmpty() || !context.getFocusMissingSkills().isEmpty()) {
                sb.append("用户已命中：").append(context.getFocusMatchedSkills().isEmpty()
                        ? "无" : String.join("、", context.getFocusMatchedSkills())).append("\n");
                sb.append("用户缺失：").append(context.getFocusMissingSkills().isEmpty()
                        ? "无" : String.join("、", context.getFocusMissingSkills())).append("\n");
            }
            if (StringUtils.hasText(context.getFocusJobDescription())) {
                sb.append("岗位描述：").append(abbreviate(context.getFocusJobDescription(), 400)).append("\n");
            }
        }
        if (history != null && !history.isEmpty()) {
            sb.append("【近期对话】\n");
            for (AiChatMessage message : history) {
                sb.append("USER".equals(message.getRole()) ? "用户：" : "助手：")
                        .append(abbreviate(message.getContent(), 200)).append("\n");
            }
        }
        sb.append("【用户本次提问】\n").append(question);
        return sb.toString();
    }

    private String roleLine(ChatContext context) {
        if (RoleConstants.COMPANY.equals(context.getRole())) {
            return "身份：企业招聘方\n";
        }
        if (RoleConstants.ADMIN.equals(context.getRole())) {
            return "身份：平台管理员\n";
        }
        if (RoleConstants.USER.equals(context.getRole())) {
            return "身份：求职者\n";
        }
        return "身份：未登录访客（资料为空，只给通用建议）\n";
    }

    private String nvl(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String flat = text.replaceAll("\\s+", " ").trim();
        return flat.length() <= max ? flat : flat.substring(0, max) + "…";
    }
}
