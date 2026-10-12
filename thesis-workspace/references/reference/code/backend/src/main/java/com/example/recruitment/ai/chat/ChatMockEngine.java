package com.example.recruitment.ai.chat;

import com.example.recruitment.common.constant.RoleConstants;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * AI 智能助手 Mock 规则引擎：无需 API Key、无网络依赖即可回答招聘全流程问题。
 * 规则按“命中关键词数量最多”优先匹配；答案结合 {@link ChatContext} 中的真实用户数据，
 * 输出结构（回答 + 推荐追问）与真实模式完全一致。
 */
@Component
public class ChatMockEngine {

    public record MockAnswer(String content, List<String> suggestions) {
    }

    private record Topic(String category, List<String> keywords, Function<ChatContext, String> answer,
                         List<String> suggestions) {
    }

    /** 求职者主题（含通用主题） */
    private static final List<Topic> USER_TOPICS = buildUserTopics();

    /** 企业端主题 */
    private static final List<Topic> COMPANY_TOPICS = buildCompanyTopics();

    /** 管理员主题 */
    private static final List<Topic> ADMIN_TOPICS = buildAdminTopics();

    /**
     * 生成规则回答
     */
    public MockAnswer answer(String question, ChatContext context) {
        String text = question == null ? "" : question.trim();
        String lower = text.toLowerCase(Locale.ROOT);
        Topic best = null;
        int bestHits = 0;
        for (Topic topic : topicsFor(context.getRole())) {
            int hits = 0;
            for (String keyword : topic.keywords) {
                if (lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                    hits++;
                }
            }
            if (hits > bestHits) {
                bestHits = hits;
                best = topic;
            }
        }
        if (best == null) {
            return fallback(context);
        }
        return new MockAnswer(best.answer.apply(context), best.suggestions);
    }

    /* ==================== 各主题回答 ==================== */

    /** 按角色合并主题：通用/求职者主题对所有角色可见，企业与管理员追加专属主题 */
    private static List<Topic> topicsFor(String role) {
        if (RoleConstants.COMPANY.equals(role)) {
            return concat(USER_TOPICS, COMPANY_TOPICS);
        }
        if (RoleConstants.ADMIN.equals(role)) {
            return concat(USER_TOPICS, ADMIN_TOPICS);
        }
        return USER_TOPICS;
    }

    private static List<Topic> concat(List<Topic> first, List<Topic> second) {
        List<Topic> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private static List<Topic> buildUserTopics() {
        List<Topic> topics = new ArrayList<>();

        topics.add(new Topic("平台介绍",
                List.of("你是什么", "你是谁", "你能做什么", "能干什么", "怎么用", "帮助", "介绍一下你", "有什么功能", "助手"),
                ctx -> "我是智聘AI 的智能求职助手，可以陪你走完整个招聘流程：" +
                        "\n1. 简历：AI 五维分析、对照岗位 JD 逐条优化；" +
                        "\n2. 匹配：语义 50% + 技能 25% + 经验 15% + 学历 10% 计算人岗匹配度；" +
                        "\n3. 推荐：按你的技能画像批量打分推荐岗位（6 小时缓存，可手动刷新）；" +
                        "\n4. 面试：AI 模拟面试出题、逐题评分与总评报告；" +
                        "\n5. 投递：投递跟踪、收藏岗位、举报反馈。" +
                        roleHint(ctx),
                List.of("AI 岗位匹配是怎么算分的？", "简历怎么优化更容易过筛？", "面试前应该准备什么？")));

        topics.add(new Topic("岗位匹配",
                List.of("匹配度", "匹配", "匹配算法", "怎么算", "打分", "分数", "适合我吗", "能匹配"),
                ctx -> "人岗匹配度的计算口径是固定的四项加权：" +
                        "\n• 语义匹配 50%：简历全文与岗位 JD 的向量余弦相似度；" +
                        "\n• 技能匹配 25%：岗位技能按权重加权，命中权重和 / 总权重；" +
                        "\n• 经验匹配 15%：按岗位经验要求与你的工作/实习/项目数量；" +
                        "\n• 学历匹配 10%：学历等级对照，达标 100、差 1 档 80。" +
                        "\n在岗位详情页点「AI 匹配分析」即可看到命中技能、缺失技能与推荐理由。" +
                        resumeHint(ctx) + jobHint(ctx),
                List.of("我缺哪些技能？", "简历怎么优化更容易过筛？", "帮我推荐几个岗位")));

        topics.add(new Topic("岗位推荐",
                List.of("推荐", "推荐岗位", "什么岗位适合", "有哪些岗位", "找工作", "找什么工作", "就业方向"),
                ctx -> {
                    StringBuilder sb = new StringBuilder("系统会以你最近一份简历的技能画像为基准，对所有在线岗位逐个打分后取 Top 10。" +
                            "\n• 你的技能画像：" + ctx.skillText() +
                            "\n• 当前推荐结果：" + ctx.topJobText());
                    if (!"（暂无推荐结果，可到「智能匹配」页生成）".equals(ctx.topJobText())) {
                        sb.append("\n建议优先投递匹配度 80 分以上、且缺失技能不超过 1-2 项的岗位；到「智能匹配」页点「刷新推荐」可以强制重算。");
                    } else {
                        sb.append("\n先完善简历技能并上传附件，再到「智能匹配」页生成推荐，匹配度会更准。");
                    }
                    return sb.toString();
                },
                List.of("我缺哪些技能？", "投递后多久有回复？", "先投大厂还是先投小公司？")));

        topics.add(new Topic("简历优化",
                List.of("简历", "优化", "润色", "改写", "怎么写", "完善", "过筛", "附件"),
                ctx -> "简历筛选的第一原则是「与目标岗位 JD 对齐」：" +
                        "\n1. 技能栏按 JD 顺序罗列，JD 里出现的名词原样保留（如 Spring Boot、Redis）；" +
                        "\n2. 经历按 STAR 写：背景 → 你的动作 → 量化结果，每条至少一个数字（QPS、耗时、百分比）；" +
                        "\n3. 个人简介控制在 3 行以内，一句话说清方向 + 2 个核心能力；" +
                        "\n4. 项目只留与目标岗位相关的 2-3 个，弱相关的删除。" +
                        "\n本平台的「AI简历 → 简历优化」会对照目标岗位逐条给出「原文 vs 优化后」，可直接回填。" +
                        resumeHint(ctx),
                List.of("简历完整度怎么提升？", "AI 简历分析的五维评分是什么？", "没有实习经历怎么写简历？")));

        topics.add(new Topic("简历分析",
                List.of("简历分析", "五维", "评分", "完整度", "分析报告", "简历得分"),
                ctx -> "AI 简历分析从五个维度打分，综合分 = 技能 30% + 经历 30% + 学历 20% + 完整度 20%：" +
                        "\n• 技能：技能数量 + Java/Spring/MySQL/Redis/Vue/Python/Docker 等热门技能命中；" +
                        "\n• 经历：有工作/实习经历 84、项目 ≥2 个 80、≥1 个 72；" +
                        "\n• 学历：博士 96 / 硕士 92 / 本科 86 / 大专 74；" +
                        "\n• 完整度：姓名/电话/邮箱/学校/学历/专业/技能/简介/经历/附件 十项各 10 分。" +
                        "\n报告会给出优势、不足、改进建议与适配岗位方向。" +
                        (ctx.getCompletionScore() == null ? "" : "\n你当前简历完整度：" + ctx.getCompletionScore() + " 分。"),
                List.of("简历怎么优化更容易过筛？", "AI 简历分析的五维评分是什么？", "投递后多久有回复？")));

        topics.add(new Topic("模拟面试",
                List.of("面试", "八股", "背题", "模拟", "提问", "自我介绍", "技术面", "hr面", "一面", "二面"),
                ctx -> "面试准备分三层，建议按顺序推进：" +
                        "\n1. 基础：Java 基础、集合与并发、JVM、MySQL 索引与事务、Redis 缓存与分布式锁、Spring 原理；" +
                        "\n2. 项目：每个项目准备「是什么、你负责什么、最难的点、怎么解决、量化结果」五连问；" +
                        "\n3. 表达：结论先行 → 分点说明 → 举例支撑，单个问题控制在 1-2 分钟。" +
                        "\n本平台的「AI面试」可按目标岗位出题（简单/中等/困难），逐题作答后给评分与点评，结束生成雷达图报告。" +
                        (ctx.getInterviewCount() > 0 ? "\n你已经历过 " + ctx.getInterviewCount() + " 次 AI 模拟面试，可以直接对比历史报告看进步。" : ""),
                List.of("面试自我介绍怎么说？", "项目经历怎么讲才有说服力？", "面试通过后怎么谈薪？")));

        topics.add(new Topic("自我介绍",
                List.of("自我介绍", "怎么介绍自己", "开场"),
                ctx -> "自我介绍用「1 + 3 + 1」结构，控制在一分钟内：" +
                        "\n1. 一句话身份：学校/专业或工作年限 + 目标岗位方向；" +
                        "\n2. 三个亮点：一个技术栈（" + firstSkill(ctx) + "）+ 一个项目成果 + 一个可迁移能力；" +
                        "\n3. 一句话收尾：说明为什么投这个岗位，把话题引向岗位。" +
                        "\n示例：“面试官好，我是" + (StringUtils.hasText(ctx.getResumeName()) ? ctx.getResumeName() : "XXX") +
                        "，" + (StringUtils.hasText(ctx.getEducation()) ? ctx.getEducation() : "本科") +
                        "，主攻后端开发，熟悉" + firstSkill(ctx) + "。最近做的项目把接口平均响应从 300ms 优化到 80ms，并沉淀为团队通用组件。我看好贵司的业务方向，希望在这个岗位上继续做深后端。”",
                List.of("项目经历怎么讲才有说服力？", "面试前应该准备什么？", "面试被问到不会的问题怎么办？")));

        topics.add(new Topic("项目经历", List.of("项目经历", "项目怎么讲", "难点", "亮点"),
                ctx -> "项目讲解的黄金公式：背景 → 目标 → 方案 → 结果，重点在你自己的贡献：" +
                        "\n• 背景：业务规模（用户量、数据量、QPS），一句话说清痛点；" +
                        "\n• 方案：为什么这么选（对比 1-2 个备选方案），体现技术判断；" +
                        "\n• 结果：量化指标，例如“P99 从 800ms 降到 120ms”“重复投递率下降 90%”；" +
                        "\n• 反思：如果重做会怎么改进，这句最能体现成长性。",
                List.of("面试自我介绍怎么说？", "面试前应该准备什么？", "面试通过后怎么谈薪？")));

        topics.add(new Topic("面试提问",
                List.of("反问", "问面试官", "最后问什么", "提问环节"),
                ctx -> "反问环节是加分项，建议问 2 个：" +
                        "\n• 业务与团队：“团队目前最核心的业务目标是什么？这个岗位在其中承担哪部分？”" +
                        "\n• 成长与流程：“新人入职后 3 个月通常要达成什么？团队的 Code Review / 发布节奏是怎样的？”" +
                        "\n不要一上来问薪资和加班，也不要问官网能查到的信息。",
                List.of("面试通过后怎么谈薪？", "怎么判断一家公司值不值得去？", "面试被问到不会的问题怎么办？")));

        topics.add(new Topic("沟通与反馈",
                List.of("已读", "没回复", "没消息", "多久", "回复", "跟进", "催", "hr不回", "石沉大海"),
                ctx -> "投递后的常规节奏：3 个工作日内简历初筛 → 1 周内约面 → 面试后 3-7 天出结果。" +
                        "\n超过 7 天没有动静，可以礼貌跟进一次（说明岗位 + 投递时间 + 一句话匹配亮点），仍然没有回复就当作不合适，把精力放到新岗位。" +
                        (ctx.getApplicationCount() > 0 ? "\n你当前共有 " + ctx.getApplicationCount() + " 条投递记录，可到「我的投递」查看每条状态。" : "") +
                        "\n提醒：状态变成「已查看」「面试中」说明进展正常；「待处理」停留较久属于正常现象，公司侧需要时间处理。",
                List.of("投递后多久有回复？", "同时拿到多个 offer 怎么选？", "怎么判断一家公司值不值得去？")));

        topics.add(new Topic("投递策略",
                List.of("投递", "海投", "投几家", "投递顺序", "什么时候投", "撤销"),
                ctx -> "投递建议分三档，别把简历撒胡椒面：" +
                        "\n• 冲刺档（匹配度 85+）：1-2 家，认真定制简历与自我介绍；" +
                        "\n• 主攻档（70-85）：4-6 家，是你的主战场，重点跟进；" +
                        "\n• 练手档（60-70）：2-3 家，用来熟悉面试节奏。" +
                        "\n投递时间建议周二到周四上午，HR 打开率更高；同一家公司只投一个最匹配的岗位。" +
                        "\n「待处理」状态的投递可以在「我的投递」里撤销，已查看之后的投递不能撤销。",
                List.of("先投大厂还是先投小公司？", "投递后多久有回复？", "帮我推荐几个岗位")));

        topics.add(new Topic("谈薪",
                List.of("薪资", "工资", "谈薪", "薪水", "多少钱", "待遇", "包"),
                ctx -> "谈薪三步走：" +
                        "\n1. 定锚点：先查市场分位（同城同岗 50 分位），把目标定在 50-75 分位之间；" +
                        "\n2. 等对方先报价：被问期望时给区间不给点值，例如“综合我的经历，期望在 X 到 Y 之间，具体看整体方案”；" +
                        "\n3. 谈总包：base、绩效、年终、补贴、公积金比例、期权都要问清，月薪差 1K 不如年终多一个月实在。" +
                        "\n注意：不要虚报当前薪资，背调会核对。" + salaryHint(ctx),
                List.of("同时拿到多个 offer 怎么选？", "怎么判断一家公司值不值得去？", "实习和正式岗位怎么选？")));

        topics.add(new Topic("流程认知",
                List.of("流程", "几个环节", "几轮", "笔试", "offer", "背调", "审批"),
                ctx -> "标准校招/社招流程：" +
                        "\n简历初筛 → 笔试/在线测评 → 技术一面 → 技术二面（或交叉面）→ HR 面 → 定薪审批 → 发放 offer → 背调/体检 → 入职。" +
                        "\n社招通常 2-3 轮技术面；校招多一轮群面或笔试。" +
                        "\n本平台的投递状态依次为：待处理 → 已查看 → 面试中 → 已录用 / 不合适，企业侧每改一次状态你在「我的投递」都能看到。",
                List.of("投递后多久有回复？", "面试前应该准备什么？", "面试通过后怎么谈薪？")));

        topics.add(new Topic("职业发展",
                List.of("职业规划", "发展方向", "转行", "提升", "学习路线", "怎么学", "进阶", "天花板"),
                ctx -> "技术路线大体三步：" +
                        "\n• 0-2 年：把一门语言 + 数据库 + 框架吃透，能独立交付模块，重点是工程规范与调试能力；" +
                        "\n• 2-5 年：深入一个方向（高并发、微服务、数据、中间件），能设计方案并带小项目；" +
                        "\n• 5 年以上：做技术选型与架构决策，或转向管理。" +
                        "\n结合你的技能画像（" + ctx.skillText() + "），最近的补强点是先把" + firstSkill(ctx) +
                        "做到能讲原理，再补一项中间件（Redis / MQ）与一项工程能力（Docker / CI）。",
                List.of("我缺哪些技能？", "先投大厂还是先投小公司？", "怎么判断一家公司值不值得去？")));

        topics.add(new Topic("技能补强",
                List.of("缺哪些技能", "补充技能", "技能不足", "不会", "补什么", "学什么"),
                ctx -> {
                    if (ctx.hasFocusJob() && !ctx.getFocusMissingSkills().isEmpty()) {
                        return "对照【" + ctx.getFocusJobName() + "】，你已命中：" +
                                String.join("、", ctx.getFocusMatchedSkills()) +
                                "；还缺：" + String.join("、", ctx.getFocusMissingSkills()) +
                                "。\n建议按“缺失项权重从高到低”补：先能说出原理，再用一个小 demo 落地（比如用 Redis 做缓存并压测），最后写进简历的技能栏与项目描述。" +
                                resumeHint(ctx);
                    }
                    return "补技能的顺序建议是：岗位 JD 高频项优先，其次是能串起你现有技能栈的中间件。" +
                            "\n以 Java 后端为例：Spring Boot（" + hasSkill(ctx, "spring") + "）→ MySQL 索引与事务 → Redis 缓存 → 消息队列 → Docker/CI。" +
                            "\n每项都用“原理 + 一个小项目 + 能讲清取舍”三步验收，不要只看视频。" +
                            resumeHint(ctx) + jobHint(ctx);
                },
                List.of("简历怎么优化更容易过筛？", "AI 岗位匹配是怎么算分的？", "帮我推荐几个岗位")));

        topics.add(new Topic("实习与校招",
                List.of("实习", "校招", "应届", "春招", "秋招", "没有经验", "无经验"),
                ctx -> "没有实习经历也能写好简历，重点是“等效经历”：" +
                        "\n1. 课程设计 / 竞赛 / 开源贡献按项目写，突出你的模块与技术选型；" +
                        "\n2. 技能栏与 JD 对齐，标注熟练度（熟悉 / 掌握 / 了解）；" +
                        "\n3. 投递时优先中小公司或实习转正岗，面试机会更多；" +
                        "\n4. 秋招（8-11 月）岗位最多，春招（次年 3-4 月）是补录，实习建议提前一个学期开始找。",
                List.of("简历怎么优化更容易过筛？", "投递后多久有回复？", "先投大厂还是先投小公司？")));

        topics.add(new Topic("企业选择",
                List.of("值不值得", "怎么选公司", "大厂", "小公司", "外包", "加班", "裁员", "稳定性"),
                ctx -> "判断一家公司，看四个信号：" +
                        "\n1. 业务：主营收入来自哪里、最近一年是否增长（看财报/融资/招聘量）；" +
                        "\n2. 团队：面试时反问技术栈、发布节奏、Code Review 是否常态化；" +
                        "\n3. 岗位：职责是否清晰，是不是长期“救火”岗；" +
                        "\n4. 待遇：base 之外看公积金比例、年终兑现历史、加班补贴。" +
                        "\n大厂给规范与背书，小公司给宽度与话语权；第一份工作优先选能让你“说清自己做了什么”的地方。",
                List.of("同时拿到多个 offer 怎么选？", "面试通过后怎么谈薪？", "职业发展方向怎么定？")));

        topics.add(new Topic("Offer 对比",
                List.of("多个offer", "怎么选offer", "offer比较", "两个offer", "选择offer", "违约"),
                ctx -> "多个 offer 用打分表决定，别凭感觉：" +
                        "\n• 业务前景 30%：赛道与公司增速；" +
                        "\n• 成长空间 25%：技术栈、带教、项目复杂度；" +
                        "\n• 薪资总包 25%：base + 年终 + 补贴 + 公积金；" +
                        "\n• 生活成本 20%：城市、通勤、加班强度。" +
                        "\n每项 1-5 分加权求和，差距 5 分以内选成长更高的那个。" +
                        "\n注意 offer 违约成本（校招三方违约一般有约定），决定后尽快礼貌告知其他公司。",
                List.of("面试通过后怎么谈薪？", "怎么判断一家公司值不值得去？", "职业发展方向怎么定？")));

        topics.add(new Topic("举报与安全",
                List.of("举报", "虚假", "骗", "收费", "诈骗", "违规", "投诉"),
                ctx -> "遇到以下情况请在岗位或企业详情页点「举报」：" +
                        "\n• 虚假岗位 / 与实际工作内容严重不符；" +
                        "\n• 以培训费、押金、服装费等名义收费；" +
                        "\n• 要求提供银行卡、验证码等敏感信息。" +
                        "\n举报后管理员会在后台处理（状态：待处理 → 已处理 / 已忽略），你也可以在「举报反馈」里查看历史记录。" +
                        "\n提醒：正规企业招聘不会向求职者收取任何费用。",
                List.of("投递后多久有回复？", "怎么判断一家公司值不值得去？", "同时拿到多个 offer 怎么选？")));

        topics.add(new Topic("我的数据",
                List.of("我的简历", "我的投递", "我的收藏", "我的面试", "投递记录", "收藏"),
                ctx -> "你的账户数据概览：" +
                        "\n• 简历：" + (ctx.hasResume()
                        ? "「" + ctx.getResumeName() + "」，完整度 " +
                        (ctx.getCompletionScore() == null ? "未知" : ctx.getCompletionScore()) +
                        " 分，经历 " + ctx.getExperienceCount() + " 段"
                        : "暂无，建议先到「AI简历」创建一份并上传附件") +
                        "\n• 投递：" + ctx.getApplicationCount() + " 条，其中面试中 " + ctx.getInterviewCount() + " 条" +
                        "\n• 推荐：" + ctx.topJobText() +
                        "\n对应入口：个人中心 / 我的投递 / 我的收藏 / AI面试 / 智能匹配。",
                List.of("帮我推荐几个岗位", "投递后多久有回复？", "简历怎么优化更容易过筛？")));

        return topics;
    }

    private static List<Topic> buildCompanyTopics() {
        List<Topic> topics = new ArrayList<>();

        topics.add(new Topic("企业-候选人筛选",
                List.of("候选人", "筛选", "筛简历", "怎么选人", "筛选标准", "投递处理"),
                ctx -> "企业端筛选候选人的建议顺序：" +
                        "\n1. 看候选人匹配度（岗位详情 → 候选人 AI 分析），优先 80 分以上；" +
                        "\n2. 看缺失技能是否属于“可培养项”，核心硬技能缺失直接淘汰；" +
                        "\n3. 看经历与岗位职责的重合度，重点看“量化结果”而不是职责罗列；" +
                        "\n4. 看投递时间，超过 3 天未处理的待处理投递先集中处理，避免候选人流失。" +
                        companyHint(ctx),
                List.of("怎么快速筛选合适的候选人？", "岗位发布有什么技巧？", "候选人 AI 分析看哪些指标？")));

        topics.add(new Topic("企业-岗位发布",
                List.of("发布岗位", "岗位优化", "招聘效果", "没人投", "岗位没人", "职位描述", "jd", "招聘文案"),
                ctx -> "岗位发布想拿到更多有效投递，注意四点：" +
                        "\n• 标题写清“岗位 + 方向 + 级别”，不要用内部代号；" +
                        "\n• 职责 3-5 条、要求 3-5 条，把必备技能与加分技能分开写；" +
                        "\n• 薪资区间如实填写，区间越窄投递质量越高；" +
                        "\n• 技能标签务必填全，系统正是按技能标签计算人岗匹配度并推送推荐。" +
                        companyHint(ctx),
                List.of("怎么快速筛选合适的候选人？", "怎么提高投递量？", "候选人 AI 分析看哪些指标？")));

        topics.add(new Topic("企业-招聘数据",
                List.of("投递量", "招聘数据", "转化率", "面试率", "招聘漏斗"),
                ctx -> "招聘漏斗建议按周复盘：曝光（浏览数）→ 投递（投递数）→ 初筛通过 → 面试 → 录用。" +
                        "\n• 浏览高但投递低：岗位要求或薪资区间有问题；" +
                        "\n• 投递高但面试率低：初筛标准过严或简历质量普遍偏低；" +
                        "\n• 面试到录用流失：面试体验或薪资竞争力需要调整。" +
                        companyHint(ctx),
                List.of("怎么提高投递量？", "岗位发布有什么技巧？", "怎么快速筛选合适的候选人？")));

        return topics;
    }

    private static List<Topic> buildAdminTopics() {
        List<Topic> topics = new ArrayList<>();

        topics.add(new Topic("管理员-平台数据",
                List.of("平台数据", "整体数据", "运营数据", "统计", "多少用户", "多少岗位", "数据怎么样"),
                ctx -> "后台数据看板的核心指标：" +
                        "\n• 规模：注册用户 " + ctx.getPlatformUserCount() + "、入驻企业 " + ctx.getPlatformCompanyCount()
                        + "、在线岗位 " + ctx.getPlatformOnlineJobCount() + "；" +
                        "\n• 活跃：近 7 天新增用户 / 岗位 / 投递趋势曲线；" +
                        "\n• 风险：待处理举报 " + ctx.getPlatformPendingReportCount() + " 条，建议 24 小时内清空；" +
                        "\n• 质量：岗位类型分布与企业岗位 Top 榜，用于判断平台供需结构。",
                List.of("举报处理流程是什么？", "平台数据看板看哪些指标？", "AI 功能支持哪些模式？")));

        topics.add(new Topic("管理员-举报处理",
                List.of("举报处理", "处理举报", "违规", "审核", "封禁", "下架"),
                ctx -> "举报处理流程：" +
                        "\n1. 在「举报管理」按状态筛选待处理记录，先看对象类型（岗位 / 企业）与举报原因；" +
                        "\n2. 核实：查看对应岗位或企业详情，确认是否存在虚假信息、收费等违规；" +
                        "\n3. 处置：属实则下架岗位或禁用企业账号，并填写处理备注；无关或证据不足标记为已忽略；" +
                        "\n4. 状态流转：待处理 → 已处理 / 已忽略，处理后用户可在「举报反馈」查看结果。" +
                        "\n当前待处理举报：" + ctx.getPlatformPendingReportCount() + " 条。",
                List.of("平台数据看板看哪些指标？", "举报处理流程是什么？", "AI 功能支持哪些模式？")));

        topics.add(new Topic("管理员-AI 配置",
                List.of("ai 模式", "ai配置", "mock", "真实模型", "apikey", "api key", "大模型", "连通性"),
                ctx -> "平台 AI 支持三种模式：" +
                        "\n• MOCK：默认，内置规则/题库/本地向量，无需 Key 与网络，适合演示；" +
                        "\n• REAL：配置 ai.enabled=true、ai.mock=false 与 api-key 后调用真实大模型（OpenAI 兼容）；" +
                        "\n• DISABLED：enabled=false 且 mock=false，不启用。" +
                        "\n后台「设置」页可查看脱敏配置（只返回 hasApiKey 布尔值，不回显密钥）并点「测试连通性」。" +
                        "\n接口调用失败会自动降级为规则引擎，页面不会报错。",
                List.of("AI 功能支持哪些模式？", "平台数据看板看哪些指标？", "AI 岗位匹配是怎么算分的？")));

        return topics;
    }

    /* ==================== 兜底与工具 ==================== */

    private MockAnswer fallback(ChatContext context) {
        String text = "这个问题我还没完全理解，先给你几条最常见的方向：" +
                "\n• 「AI 岗位匹配是怎么算分的？」——了解四维加权口径；" +
                "\n• 「简历怎么优化更容易过筛？」——按 JD 对齐的具体做法；" +
                "\n• 「面试前应该准备什么？」——基础 / 项目 / 表达三层清单。" +
                "\n也可以直接把岗位名称或你的疑问描述得更具体一些，例如“投递后一周没回复怎么办”。" +
                roleHint(context);
        return new MockAnswer(text, List.of(
                "AI 岗位匹配是怎么算分的？",
                "简历怎么优化更容易过筛？",
                "面试前应该准备什么？"));
    }

    private static String roleHint(ChatContext ctx) {
        if (RoleConstants.COMPANY.equals(ctx.getRole())) {
            return "\n你当前是企业账号" + (StringUtils.hasText(ctx.getCompanyName())
                    ? "（" + ctx.getCompanyName() + "）" : "") + "，我也可以帮你梳理岗位发布、候选人筛选与投递处理。";
        }
        if (RoleConstants.ADMIN.equals(ctx.getRole())) {
            return "\n你当前是管理员账号，我可以协助说明后台各模块与平台数据口径。";
        }
        return "";
    }

    private static String resumeHint(ChatContext ctx) {
        if (!ctx.hasResume()) {
            return "\n提示：你还没有简历，先到「AI简历」创建一份并上传附件，匹配与推荐会更准。";
        }
        return "\n你的简历「" + ctx.getResumeName() + "」当前技能：" + ctx.skillText() + "。";
    }

    private static String jobHint(ChatContext ctx) {
        if (!ctx.hasFocusJob()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n你正在看的岗位【" + ctx.getFocusJobName() + "】");
        if (ctx.getFocusJobCompany() != null) {
            sb.append("（").append(ctx.getFocusJobCompany()).append("）");
        }
        sb.append("，要求：").append(ctx.getFocusJobExperience()).append(" / ").append(ctx.getFocusJobEducation());
        if (!ctx.getFocusJobSkills().isEmpty()) {
            sb.append("，技能：").append(String.join("、", ctx.getFocusJobSkills()));
        }
        if (!ctx.getFocusMatchedSkills().isEmpty() || !ctx.getFocusMissingSkills().isEmpty()) {
            sb.append("\n你已命中：").append(ctx.getFocusMatchedSkills().isEmpty()
                    ? "无" : String.join("、", ctx.getFocusMatchedSkills()));
            sb.append("；缺失：").append(ctx.getFocusMissingSkills().isEmpty()
                    ? "无" : String.join("、", ctx.getFocusMissingSkills()));
        }
        sb.append("。");
        return sb.toString();
    }

    private static String salaryHint(ChatContext ctx) {
        if (!ctx.hasFocusJob() || !StringUtils.hasText(ctx.getFocusJobSalary())) {
            return "";
        }
        return "\n参考：你正在看的岗位薪资区间为 " + ctx.getFocusJobSalary() + "，谈薪时可以此为基准上浮 10%-15%。";
    }

    private static String companyHint(ChatContext ctx) {
        if (!StringUtils.hasText(ctx.getCompanyName())) {
            return "";
        }
        return "\n当前企业：" + ctx.getCompanyName() + "，在招岗位 " + ctx.getCompanyJobCount()
                + " 个（在线 " + ctx.getCompanyOnlineJobCount() + " 个），待处理投递 "
                + ctx.getCompanyPendingApplicationCount() + " 条。";
    }

    private static String firstSkill(ChatContext ctx) {
        List<String> skills = ctx.getSkills();
        return skills == null || skills.isEmpty() ? "Java / Spring Boot" : skills.get(0);
    }

    private static String hasSkill(ChatContext ctx, String keyword) {
        if (ctx.getSkills() == null) {
            return "待补充";
        }
        boolean hit = ctx.getSkills().stream().anyMatch(s -> s.toLowerCase(Locale.ROOT).contains(keyword));
        return hit ? "已具备" : "待补充";
    }

    /** 供服务层复用的默认快捷提问（按角色） */
    public List<String> defaultQuickQuestions(String role) {
        if (RoleConstants.COMPANY.equals(role)) {
            return List.of("怎么快速筛选合适的候选人？", "岗位发布有什么技巧？", "候选人 AI 分析看哪些指标？");
        }
        if (RoleConstants.ADMIN.equals(role)) {
            return List.of("平台当前整体数据怎么样？", "举报处理流程是什么？", "AI 功能支持哪些模式？");
        }
        return List.of("AI 岗位匹配是怎么算分的？", "简历怎么优化更容易过筛？", "面试前应该准备什么？");
    }

    /** 规则模式下按主题补充推荐追问（保证 3 条） */
    public List<String> completeSuggestions(List<String> suggestions, ChatContext ctx) {
        Map<String, Boolean> seen = new LinkedHashMap<>();
        List<String> result = new ArrayList<>();
        if (suggestions != null) {
            for (String s : suggestions) {
                if (StringUtils.hasText(s) && seen.putIfAbsent(s, Boolean.TRUE) == null) {
                    result.add(s);
                }
            }
        }
        for (String s : defaultQuickQuestions(ctx.getRole())) {
            if (result.size() >= 3) {
                break;
            }
            if (seen.putIfAbsent(s, Boolean.TRUE) == null) {
                result.add(s);
            }
        }
        return result.size() > 3 ? result.subList(0, 3) : result;
    }
}
