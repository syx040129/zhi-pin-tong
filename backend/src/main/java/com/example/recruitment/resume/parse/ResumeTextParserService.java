package com.example.recruitment.resume.parse;

import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 简历文本结构化解析：配置真实模型时调用 LLM 结构化抽取；
 * 未配置模型或 Mock 模式下使用规则匹配（正则 + 关键词），保证演示可用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeTextParserService {

    private final AiProperties aiProperties;
    private final LlmClient llmClient;

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    private static final List<String> TECH_KEYWORDS = List.of(
            "Java", "Spring Boot", "Spring", "SpringCloud", "Spring Cloud", "MyBatis", "MyBatis-Plus",
            "MySQL", "Redis", "Vue", "Vue3", "React", "JavaScript", "TypeScript", "Node.js", "HTML",
            "CSS", "Python", "Docker", "Kubernetes", "Linux", "Git", "Maven", "RabbitMQ", "Kafka",
            "Elasticsearch", "MongoDB", "PostgreSQL", "Oracle", "SQL", "Nginx", "微服务", "分布式",
            "高并发", "JVM", "多线程", "算法", "数据结构", "Linux", "Dubbo", "Zookeeper", "Flutter",
            "Android", "iOS", "Objective-C", "Swift", "TensorFlow", "PyTorch", "机器学习", "深度学习",
            "大数据", "Hadoop", "Spark", "Flink", "K8s", "CI/CD", "Jenkins");

    /**
     * 解析简历文本，优先真实大模型，其次规则解析
     */
    public ParsedResume parse(String text) {
        if (!StringUtils.hasText(text)) {
            return new ParsedResume();
        }
        if (aiProperties.realMode()) {
            try {
                String json = llmClient.chatJson(
                        "你是简历信息抽取引擎。从用户提供的简历文本中抽取结构化信息，"
                                + "只输出 JSON，不要输出其他内容。JSON 格式："
                                + "{\"name\":\"\",\"phone\":\"\",\"email\":\"\",\"education\":\"本科\","
                                + "\"school\":\"\",\"major\":\"\",\"skills\":[\"\"],\"summary\":\"\","
                                + "\"experiences\":[{\"type\":\"PROJECT|WORK|INTERNSHIP|EDUCATION\","
                                + "\"title\":\"\",\"company\":\"\",\"content\":\"\",\"startDate\":\"\",\"endDate\":\"\"}]}。"
                                + "无法确定的字段填空字符串或空数组。",
                        text);
                ParsedResume parsed = com.example.recruitment.common.utils.JsonUtils.parse(json, ParsedResume.class);
                if (parsed != null) {
                    parsed.setSummary(StringUtils.hasText(parsed.getSummary())
                            ? parsed.getSummary() : buildSummaryFromText(text));
                    return parsed;
                }
            } catch (Exception e) {
                log.warn("LLM 简历解析失败，降级规则解析: {}", e.getMessage());
            }
        }
        return parseByRules(text);
    }

    /**
     * 规则解析（Mock / 兜底）
     */
    public ParsedResume parseByRules(String text) {
        ParsedResume parsed = new ParsedResume();
        String lower = text.toLowerCase(Locale.ROOT);

        Matcher phone = PHONE.matcher(text);
        if (phone.find()) {
            parsed.setPhone(phone.group());
        }
        Matcher email = EMAIL.matcher(text);
        if (email.find()) {
            parsed.setEmail(email.group());
        }
        parsed.setEducation(detectEducation(text));
        parsed.setSchool(detectAfterKeyword(text, "学校", "院校", "大学"));
        parsed.setMajor(detectAfterKeyword(text, "专业", "主修"));
        parsed.setName(detectName(text));

        List<String> skills = new ArrayList<>();
        for (String keyword : TECH_KEYWORDS) {
            if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                skills.add(keyword);
            }
        }
        parsed.setSkills(skills);
        parsed.setSummary(buildSummaryFromText(text));
        return parsed;
    }

    private String detectName(String text) {
        Matcher phone = PHONE.matcher(text);
        if (phone.find()) {
            int start = Math.max(0, phone.start() - 40);
            String before = text.substring(start, phone.start());
            // 简历行首常见格式：姓名 ｜ 电话
            String[] candidates = before.split("[\\s|｜，,；;]+");
            for (String c : candidates) {
                if (c.length() >= 2 && c.length() <= 6 && !c.matches(".*[0-9@].*")
                        && !c.contains("姓名")) {
                    return c;
                }
            }
        }
        return null;
    }

    private String detectEducation(String text) {
        if (text.contains("博士")) {
            return "博士";
        }
        if (text.contains("硕士") || text.contains("研究生")) {
            return "硕士";
        }
        if (text.contains("本科")) {
            return "本科";
        }
        if (text.contains("大专") || text.contains("专科")) {
            return "大专";
        }
        return null;
    }

    private String detectAfterKeyword(String text, String... keywords) {
        for (String keyword : keywords) {
            int idx = text.indexOf(keyword);
            while (idx >= 0 && idx < text.length()) {
                int start = idx + keyword.length();
                int end = Math.min(text.length(), start + 30);
                String rest = text.substring(start, end);
                String line = rest.split("\\n")[0].trim();
                line = line.replaceFirst("^[:：\\s|｜]+", "");
                line = line.split("[\\s|｜，,；;]+")[0].trim();
                if (line.length() >= 2 && line.length() <= 40 && !line.matches(".*[0-9]+.*")) {
                    return line;
                }
                idx = text.indexOf(keyword, idx + keyword.length());
            }
        }
        return null;
    }

    private String buildSummaryFromText(String text) {
        String compact = text.replaceAll("\\s+", " ").trim();
        return compact.length() > 500 ? compact.substring(0, 500) : compact;
    }
}
