package com.example.recruitment.resume.parse;

import lombok.Data;

import java.util.List;

/**
 * 结构化解析后的简历内容（AI 或规则提取）
 */
@Data
public class ParsedResume {

    private String name;
    private String phone;
    private String email;
    private String education;
    private String school;
    private String major;
    private List<String> skills = List.of();
    private String summary;
    /** type/title/company/content/startDate/endDate */
    private List<ParsedExperience> experiences = List.of();

    @Data
    public static class ParsedExperience {
        private String type;
        private String title;
        private String company;
        private String content;
        private String startDate;
        private String endDate;
    }
}
