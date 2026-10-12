package com.example.recruitment.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.ai.config.AiProperties;
import com.example.recruitment.ai.dto.InterviewQuestionVO;
import com.example.recruitment.ai.dto.InterviewReportVO;
import com.example.recruitment.ai.dto.InterviewStartRequest;
import com.example.recruitment.ai.dto.InterviewVO;
import com.example.recruitment.ai.entity.AiInterview;
import com.example.recruitment.ai.entity.AiInterviewQuestion;
import com.example.recruitment.ai.llm.LlmClient;
import com.example.recruitment.ai.mapper.AiInterviewMapper;
import com.example.recruitment.ai.mapper.AiInterviewQuestionMapper;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.utils.JsonUtils;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * AI 模拟面试服务：题目生成、作答评分、报告生成。
 * 真实模式调用大模型生成题目与评分，Mock 模式使用内置题库 + 规则评分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiInterviewService {

    private final AiInterviewMapper interviewMapper;
    private final AiInterviewQuestionMapper questionMapper;
    private final JobMapper jobMapper;
    private final AiProperties aiProperties;
    private final LlmClient llmClient;

    private static final List<BankQuestion> QUESTION_BANK = buildBank();

    @Transactional
    public InterviewVO start(InterviewStartRequest request, Long userId) {
        Job job = null;
        if (request.getJobId() != null) {
            job = jobMapper.selectById(request.getJobId());
            if (job == null) {
                throw BusinessException.notFound("岗位不存在");
            }
        }
        int count = request.getQuestionCount() == null ? 5 : request.getQuestionCount();
        count = Math.max(3, Math.min(10, count));

        AiInterview interview = new AiInterview();
        interview.setUserId(userId);
        interview.setJobId(request.getJobId());
        interview.setTitle(StringUtils.hasText(request.getTitle())
                ? request.getTitle() : (job != null ? job.getJobName() + " 模拟面试" : "综合技术模拟面试"));
        interview.setDifficulty(request.getDifficulty());
        interview.setQuestionCount(count);
        interview.setStatus(CommonConstants.INTERVIEW_ONGOING);
        interviewMapper.insert(interview);

        List<BankQuestion> questions = aiProperties.realMode()
                ? generateByLlm(job, request.getDifficulty(), count)
                : pickFromBank(request.getDifficulty(), count);
        int sort = 0;
        for (BankQuestion q : questions) {
            AiInterviewQuestion entity = new AiInterviewQuestion();
            entity.setInterviewId(interview.getId());
            entity.setQuestion(q.question);
            entity.setQuestionType(q.type);
            entity.setDifficulty(q.difficulty);
            entity.setSort(sort++);
            questionMapper.insert(entity);
        }
        return detail(interview.getId(), userId);
    }

    private List<BankQuestion> generateByLlm(Job job, String difficulty, int count) {
        try {
            String jobPart = job == null ? "通用技术岗位（Java 后端方向）" : job.getJobName() + "：" + job.getDescription();
            String json = llmClient.chatJson(
                    "你是一名技术面试官。请针对给定岗位与难度生成面试题，只输出 JSON："
                            + "{\"questions\":[{\"question\":\"题目内容\",\"questionType\":\"Java基础|Spring|MySQL|Redis|项目经验|场景题|算法\","
                            + "\"difficulty\":\"" + difficulty + "\"}]}。共 " + count + " 道，难度覆盖该等级，题目具体、贴近真实面试。",
                    "岗位：" + jobPart + "，题目数量：" + count);
            JsonNode node = JsonUtils.readTree(json);
            JsonNode questions = node == null ? null : node.get("questions");
            List<BankQuestion> list = new ArrayList<>();
            if (questions != null && questions.isArray()) {
                for (JsonNode q : questions) {
                    if (list.size() >= count) {
                        break;
                    }
                    String question = q.path("question").asText("");
                    if (StringUtils.hasText(question)) {
                        list.add(new BankQuestion(question,
                                q.path("questionType").asText("技术问题"), difficulty));
                    }
                }
            }
            if (!list.isEmpty()) {
                return list;
            }
        } catch (Exception e) {
            log.warn("LLM 生成题目失败，使用题库: {}", e.getMessage());
        }
        return pickFromBank(difficulty, count);
    }

    private List<BankQuestion> pickFromBank(String difficulty, int count) {
        List<BankQuestion> pool = QUESTION_BANK.stream()
                .filter(q -> q.difficulty.equals(difficulty)).toList();
        if (pool.size() < count) {
            pool = QUESTION_BANK;
        }
        List<BankQuestion> result = new ArrayList<>(pool);
        long seed = System.currentTimeMillis();
        java.util.Collections.shuffle(result, new Random(seed));
        return result.subList(0, Math.min(count, result.size()));
    }

    public InterviewVO detail(Long id, Long userId) {
        AiInterview interview = requireOwn(id, userId);
        InterviewVO vo = toVO(interview);
        vo.setQuestions(questionMapper.selectList(new LambdaQueryWrapper<AiInterviewQuestion>()
                .eq(AiInterviewQuestion::getInterviewId, id)
                .orderByAsc(AiInterviewQuestion::getSort))
                .stream().map(this::toQuestionVO).toList());
        return vo;
    }

    public List<InterviewVO> history(Long userId) {
        return interviewMapper.selectList(new LambdaQueryWrapper<AiInterview>()
                        .eq(AiInterview::getUserId, userId)
                        .orderByDesc(AiInterview::getId))
                .stream().map(this::toVO).toList();
    }

    /**
     * 作答并 AI 评分
     */
    @Transactional
    public InterviewQuestionVO answer(Long interviewId, Long questionId, String answer, Long userId) {
        AiInterview interview = requireOwn(interviewId, userId);
        if (!CommonConstants.INTERVIEW_ONGOING.equals(interview.getStatus())) {
            throw BusinessException.badRequest("该场面试已结束，无法继续作答");
        }
        AiInterviewQuestion question = questionMapper.selectById(questionId);
        if (question == null || !Objects.equals(question.getInterviewId(), interviewId)) {
            throw BusinessException.notFound("题目不存在");
        }
        question.setAnswer(answer);
        if (aiProperties.realMode()) {
            evaluateByLlm(question);
        } else {
            evaluateByRule(question);
        }
        questionMapper.updateById(question);
        return toQuestionVO(question);
    }

    private void evaluateByLlm(AiInterviewQuestion question) {
        try {
            String json = llmClient.chatJson(
                    "你是技术面试官，请对候选人回答进行评分，只输出 JSON："
                            + "{\"score\":0-100整数,\"feedback\":\"中文点评（指出优点与不足）\"}。"
                            + "评分标准：技术准确性、回答完整度、逻辑性、表达能力。",
                    "面试题：" + question.getQuestion() + "\n难度：" + question.getDifficulty()
                            + "\n候选人回答：" + question.getAnswer());
            JsonNode node = JsonUtils.readTree(json);
            if (node != null && node.has("score")) {
                question.setScore(node.path("score").asInt());
                question.setFeedback(node.path("feedback").asText(""));
                return;
            }
        } catch (Exception e) {
            log.warn("LLM 评分失败，使用规则评分: {}", e.getMessage());
        }
        evaluateByRule(question);
    }

    private void evaluateByRule(AiInterviewQuestion question) {
        String answer = question.getAnswer() == null ? "" : question.getAnswer();
        int length = answer.length();
        List<String> terms = List.of("索引", "事务", "缓存", "redis", "mysql", "spring", "jvm",
                "线程", "锁", "接口", "数据库", "分页", "幂等", "mq", "docker", "ci", "监控", "日志");
        String lower = answer.toLowerCase();
        int hits = 0;
        for (String t : terms) {
            if (lower.contains(t)) {
                hits++;
            }
        }
        int score = Math.min(100, Math.round(40f + Math.min(30f, length / 10f) + hits * 4f));
        question.setScore(score);
        if (score >= 85) {
            question.setFeedback("回答专业、结构清晰，覆盖了关键技术要点，注意结合实际场景进一步展开会更有说服力。");
        } else if (score >= 70) {
            question.setFeedback("整体思路正确，可以再补充实现细节、方案对比与自己的思考，让回答更完整。");
        } else if (score >= 55) {
            question.setFeedback("回答偏简略，建议先梳理解题框架，再结合项目或示例说明关键点。");
        } else {
            question.setFeedback("回答内容过少或与问题相关性不足，建议系统性复习该知识点后再作答。");
        }
    }

    /**
     * 结束面试并生成 AI 面试报告
     */
    @Transactional
    public InterviewReportVO finish(Long interviewId, Long userId) {
        AiInterview interview = requireOwn(interviewId, userId);
        List<AiInterviewQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<AiInterviewQuestion>()
                        .eq(AiInterviewQuestion::getInterviewId, interviewId)
                        .orderByAsc(AiInterviewQuestion::getSort));
        List<AiInterviewQuestion> answered = questions.stream()
                .filter(q -> q.getScore() != null).toList();
        if (answered.isEmpty()) {
            throw BusinessException.badRequest("请先完成至少一道题目的作答再提交面试");
        }
        int total = (int) Math.round(answered.stream().mapToInt(AiInterviewQuestion::getScore)
                .average().orElse(0f));
        Map<String, List<AiInterviewQuestion>> byType = answered.stream()
                .collect(Collectors.groupingBy(AiInterviewQuestion::getQuestionType));
        Map<String, Integer> dimensionScores = new LinkedHashMap<>();
        byType.forEach((type, list) -> dimensionScores.put(type,
                (int) Math.round(list.stream().mapToInt(AiInterviewQuestion::getScore).average().orElse(0f))));

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<java.util.Map.Entry<String, Integer>> sortedEntries =
                new ArrayList<>(dimensionScores.entrySet());
        sortedEntries.sort((a, b) -> b.getValue() - a.getValue());
        for (java.util.Map.Entry<String, Integer> e : sortedEntries) {
            if (e.getValue() >= 80 && strengths.size() < 3) {
                strengths.add(e.getKey() + " 表现优秀（" + e.getValue() + " 分），说明基础扎实");
            } else if (e.getValue() < 70 && weaknesses.size() < 3) {
                weaknesses.add(e.getKey() + " 有待加强（" + e.getValue() + " 分），建议针对性复习");
            }
        }
        if (strengths.isEmpty()) {
            strengths.add("作答态度认真，能完整表达自己的思路");
        }
        if (weaknesses.isEmpty()) {
            weaknesses.add("部分回答深度不足，可补充更多技术细节与案例");
        }
        List<String> suggestions = new ArrayList<>();
        if (total >= 80) {
            suggestions.add("整体表现优秀，可结合真实项目进一步打磨方案设计与架构思维");
        } else if (total >= 60) {
            suggestions.add("重点复习得分较低的维度，并练习“结论先行 + 分点说明 + 例子支撑”的表达结构");
        } else {
            suggestions.add("建议按“基础概念 → 原理 → 场景应用”的顺序系统复习，再进行一轮模拟面试检验提升");
        }
        suggestions.add("将每道题沉淀为面试笔记，补充自己的项目案例与量化成果");

        if (aiProperties.realMode()) {
            try {
                String transcript = questions.stream()
                        .filter(q -> StringUtils.hasText(q.getAnswer()))
                        .map(q -> "Q:" + q.getQuestion() + "\nA:" + q.getAnswer())
                        .collect(Collectors.joining("\n"));
                String json = llmClient.chatJson(
                        "你是技术面试总评专家。根据面试表现输出 JSON："
                                + "{\"strengths\":[\"\"],\"weaknesses\":[\"\"],\"suggestions\":[\"\"]}，各 2-3 条中文。",
                        transcript);
                JsonNode node = JsonUtils.readTree(json);
                if (node != null && node.has("strengths")) {
                    List<String> s = JsonNodeToStrings(node.get("strengths"));
                    List<String> w = JsonNodeToStrings(node.get("weaknesses"));
                    List<String> g = JsonNodeToStrings(node.get("suggestions"));
                    if (!s.isEmpty()) {
                        strengths = s;
                    }
                    if (!w.isEmpty()) {
                        weaknesses = w;
                    }
                    if (!g.isEmpty()) {
                        suggestions = g;
                    }
                }
            } catch (Exception e) {
                log.warn("LLM 生成面试总评失败: {}", e.getMessage());
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalScore", total);
        report.put("dimensionScores", dimensionScores);
        report.put("strengths", strengths);
        report.put("weaknesses", weaknesses);
        report.put("suggestions", suggestions);

        interview.setTotalScore(total);
        interview.setStatus(CommonConstants.INTERVIEW_COMPLETED);
        interview.setReport(JsonUtils.toJson(report));
        interview.setUpdateTime(LocalDateTime.now());
        interviewMapper.updateById(interview);

        InterviewReportVO vo = new InterviewReportVO();
        vo.setInterviewId(interviewId);
        vo.setTitle(interview.getTitle());
        vo.setTotalScore(total);
        vo.setAnsweredCount(answered.size());
        vo.setQuestionCount(questions.size());
        vo.setDimensionScores(dimensionScores);
        vo.setStrengths(strengths);
        vo.setWeaknesses(weaknesses);
        vo.setSuggestions(suggestions);
        return vo;
    }

    public InterviewReportVO report(Long interviewId, Long userId) {
        AiInterview interview = requireOwn(interviewId, userId);
        if (!CommonConstants.INTERVIEW_COMPLETED.equals(interview.getStatus())
                || !StringUtils.hasText(interview.getReport())) {
            throw BusinessException.badRequest("该场面试尚未完成，暂无报告");
        }
        Map<String, Object> report = JsonUtils.parse(interview.getReport(),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {
                });
        InterviewReportVO vo = new InterviewReportVO();
        vo.setInterviewId(interviewId);
        vo.setTitle(interview.getTitle());
        vo.setTotalScore(interview.getTotalScore());
        vo.setAnsweredCount(countAnswered(interviewId));
        vo.setQuestionCount(interview.getQuestionCount());
        if (report != null) {
            vo.setDimensionScores(toIntMap(report.get("dimensionScores")));
            vo.setStrengths(toStrings(report.get("strengths")));
            vo.setWeaknesses(toStrings(report.get("weaknesses")));
            vo.setSuggestions(toStrings(report.get("suggestions")));
        }
        return vo;
    }

    private int countAnswered(Long interviewId) {
        Long count = questionMapper.selectCount(new LambdaQueryWrapper<AiInterviewQuestion>()
                .eq(AiInterviewQuestion::getInterviewId, interviewId)
                .isNotNull(AiInterviewQuestion::getAnswer));
        return count == null ? 0 : count.intValue();
    }

    private AiInterview requireOwn(Long id, Long userId) {
        AiInterview interview = interviewMapper.selectById(id);
        if (interview == null || !Objects.equals(interview.getUserId(), userId)) {
            throw BusinessException.notFound("面试记录不存在");
        }
        return interview;
    }

    private InterviewVO toVO(AiInterview interview) {
        InterviewVO vo = new InterviewVO();
        vo.setId(interview.getId());
        vo.setJobId(interview.getJobId());
        vo.setTitle(interview.getTitle());
        vo.setDifficulty(interview.getDifficulty());
        vo.setQuestionCount(interview.getQuestionCount());
        vo.setTotalScore(interview.getTotalScore());
        vo.setStatus(interview.getStatus());
        vo.setCreateTime(interview.getCreateTime());
        if (interview.getJobId() != null) {
            Job job = jobMapper.selectById(interview.getJobId());
            if (job != null) {
                vo.setJobName(job.getJobName());
            }
        }
        return vo;
    }

    private InterviewQuestionVO toQuestionVO(AiInterviewQuestion q) {
        InterviewQuestionVO vo = new InterviewQuestionVO();
        vo.setId(q.getId());
        vo.setQuestion(q.getQuestion());
        vo.setQuestionType(q.getQuestionType());
        vo.setDifficulty(q.getDifficulty());
        vo.setSort(q.getSort());
        vo.setAnswer(q.getAnswer());
        vo.setScore(q.getScore());
        vo.setFeedback(q.getFeedback());
        return vo;
    }

    private List<String> JsonNodeToStrings(JsonNode node) {
        List<String> list = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode n : node) {
                String s = n.asText("");
                if (StringUtils.hasText(s)) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    private Map<String, Integer> toIntMap(Object obj) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (obj instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (v instanceof Number number) {
                    result.put(String.valueOf(k), number.intValue());
                }
            });
        }
        return result;
    }

    private List<String> toStrings(Object obj) {
        List<String> result = new ArrayList<>();
        if (obj instanceof List<?> list) {
            for (Object o : list) {
                result.add(String.valueOf(o));
            }
        }
        return result;
    }

    private record BankQuestion(String question, String type, String difficulty) {
    }

    private static List<BankQuestion> buildBank() {
        List<BankQuestion> list = new ArrayList<>();
        list.add(new BankQuestion("请介绍 JDK、JRE 与 JVM 的区别与联系", "Java基础", "简单"));
        list.add(new BankQuestion("Java 中 == 与 equals() 有什么区别？String 比较应该怎么做？", "Java基础", "简单"));
        list.add(new BankQuestion("String、StringBuilder、StringBuffer 的区别是什么？", "Java基础", "简单"));
        list.add(new BankQuestion("谈一谈面向对象三大特性以及你在项目中的体现", "Java基础", "简单"));
        list.add(new BankQuestion("什么是 Spring IoC？它解决了什么问题？", "Spring", "简单"));
        list.add(new BankQuestion("Spring Bean 的默认作用域是什么？单例 Bean 线程安全吗？", "Spring", "简单"));
        list.add(new BankQuestion("Spring Boot 相比 Spring 有哪些核心优势？", "Spring", "简单"));
        list.add(new BankQuestion("SQL 中 where 与 having 的区别是什么？", "MySQL", "简单"));
        list.add(new BankQuestion("什么是数据库索引？建索引有哪些注意点？", "MySQL", "简单"));
        list.add(new BankQuestion("MySQL 事务的四大特性是什么？", "MySQL", "简单"));
        list.add(new BankQuestion("JVM 内存区域如何划分？哪些区域可能发生 OOM？", "Java基础", "中等"));
        list.add(new BankQuestion("介绍一下常见的垃圾回收算法以及 JVM 默认垃圾回收器", "Java基础", "中等"));
        list.add(new BankQuestion("HashMap 底层原理是什么？JDK8 相对 JDK7 做了哪些优化？", "Java基础", "中等"));
        list.add(new BankQuestion("synchronized 与 ReentrantLock 的区别是什么？", "Java基础", "中等"));
        list.add(new BankQuestion("Spring Boot 自动配置原理是怎样的？如何自定义 starter？", "Spring", "中等"));
        list.add(new BankQuestion("MyBatis 中 #{} 与 ${} 有什么区别？如何防止 SQL 注入？", "Spring", "中等"));
        list.add(new BankQuestion("Spring 事务传播行为有哪些？什么场景下事务会失效？", "Spring", "中等"));
        list.add(new BankQuestion("MySQL 有哪几种事务隔离级别？默认是什么？", "MySQL", "中等"));
        list.add(new BankQuestion("什么是最左前缀原则？请举例说明联合索引的使用", "MySQL", "中等"));
        list.add(new BankQuestion("如何排查并解决 MySQL 慢查询问题？", "MySQL", "中等"));
        list.add(new BankQuestion("Redis 有哪些常用数据类型？分别适合什么场景？", "Redis", "中等"));
        list.add(new BankQuestion("什么是缓存穿透、缓存击穿、缓存雪崩？如何解决？", "Redis", "中等"));
        list.add(new BankQuestion("如何基于 Redis 实现分布式锁？有哪些注意点？", "Redis", "中等"));
        list.add(new BankQuestion("项目中最有挑战的一个技术点是什么？你是如何解决的？", "项目经验", "中等"));
        list.add(new BankQuestion("你们项目如何设计数据库表与缓存来支撑高并发查询？", "项目经验", "中等"));
        list.add(new BankQuestion("线上接口突然变慢，请描述你的排查思路", "场景题", "困难"));
        list.add(new BankQuestion("高并发秒杀场景如何设计？如何防止超卖？", "场景题", "困难"));
        list.add(new BankQuestion("如果让你设计一个短链服务，你会如何设计？", "场景题", "困难"));
        list.add(new BankQuestion("Redis RDB 与 AOF 持久化机制各有什么优缺点？如何选择？", "Redis", "困难"));
        list.add(new BankQuestion("数据库分库分表如何做？跨库查询与分布式 ID 怎么解决？", "MySQL", "困难"));
        list.add(new BankQuestion("分布式事务有哪些解决方案？各方案的优缺点？", "场景题", "困难"));
        list.add(new BankQuestion("消息队列如何保证消息不丢失、不重复消费？", "场景题", "困难"));
        list.add(new BankQuestion("线程池参数如何设置？任务队列满了会怎样？", "Java基础", "困难"));
        list.add(new BankQuestion("线上 JVM 频繁 Full GC 如何定位与优化？", "Java基础", "困难"));
        list.add(new BankQuestion("谈谈你理解的 CAP 定理，以及它对微服务设计的启发", "场景题", "困难"));
        return list;
    }
}
