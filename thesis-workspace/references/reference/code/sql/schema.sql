-- =============================================================
-- 智能招聘网站 - 数据库初始化脚本
-- 数据库：smart_recruitment (MySQL 8.0 / utf8mb4)
-- 说明：业务关系统一在代码层维护，表之间仅建立索引
-- =============================================================

CREATE DATABASE IF NOT EXISTS smart_recruitment
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE smart_recruitment;

DROP TABLE IF EXISTS ai_chat_message;
DROP TABLE IF EXISTS ai_chat_session;
DROP TABLE IF EXISTS sys_log;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS ai_interview_question;
DROP TABLE IF EXISTS ai_interview;
DROP TABLE IF EXISTS ai_recommendation;
DROP TABLE IF EXISTS ai_resume_analysis;
DROP TABLE IF EXISTS favorite;
DROP TABLE IF EXISTS job_application;
DROP TABLE IF EXISTS job_skill;
DROP TABLE IF EXISTS resume_experience;
DROP TABLE IF EXISTS resume;
DROP TABLE IF EXISTS job;
DROP TABLE IF EXISTS company;
DROP TABLE IF EXISTS sys_user;

-- 1. 系统用户表
CREATE TABLE sys_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username    VARCHAR(50)  NOT NULL COMMENT '用户名',
  password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密密码',
  nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
  avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
  phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
  email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  gender      VARCHAR(10)  DEFAULT 'UNKNOWN' COMMENT '性别 MALE/FEMALE/UNKNOWN',
  role        VARCHAR(20)  NOT NULL COMMENT '角色 USER/COMPANY/ADMIN',
  status      VARCHAR(20)  DEFAULT 'ENABLED' COMMENT '状态 ENABLED/DISABLED',
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_role_status (role, status),
  KEY idx_user_list (deleted, role, status, id),
  KEY idx_user_trend (deleted, create_time),
  CONSTRAINT chk_sys_user_deleted CHECK (deleted IS NOT NULL AND deleted IN (0, 1))
) ENGINE = InnoDB COMMENT = '系统用户表';

-- 2. 企业表
CREATE TABLE company (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT       NOT NULL COMMENT '所属用户 id',
  company_name VARCHAR(100) DEFAULT NULL COMMENT '企业名称',
  logo         VARCHAR(255) DEFAULT NULL COMMENT '企业 Logo',
  industry     VARCHAR(50)  DEFAULT NULL COMMENT '所属行业',
  scale        VARCHAR(50)  DEFAULT NULL COMMENT '企业规模',
  address      VARCHAR(255) DEFAULT NULL COMMENT '办公地址',
  description  TEXT         COMMENT '企业简介',
  website      VARCHAR(255) DEFAULT NULL COMMENT '企业官网',
  status       VARCHAR(20)  DEFAULT 'ACTIVE' COMMENT '状态 ACTIVE/DISABLED',
  deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_id (user_id),
  KEY idx_status (status),
  KEY idx_company_list (deleted, status, id),
  CONSTRAINT chk_company_deleted CHECK (deleted IS NOT NULL AND deleted IN (0, 1))
) ENGINE = InnoDB COMMENT = '企业表';

-- 3. 岗位表
CREATE TABLE job (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  company_id   BIGINT       NOT NULL COMMENT '企业 id',
  job_name     VARCHAR(100) NOT NULL COMMENT '岗位名称',
  job_type     VARCHAR(50)  DEFAULT NULL COMMENT '岗位类型(分类)',
  salary_min   INT          DEFAULT NULL COMMENT '最低薪资(K/月)',
  salary_max   INT          DEFAULT NULL COMMENT '最高薪资(K/月)',
  city         VARCHAR(50)  DEFAULT NULL COMMENT '工作城市',
  education    VARCHAR(20)  DEFAULT NULL COMMENT '学历要求 不限/大专/本科/硕士/博士',
  experience   VARCHAR(20)  DEFAULT NULL COMMENT '经验要求 不限/在校生/应届/1-3年等',
  description  TEXT         COMMENT '岗位描述',
  requirements TEXT         COMMENT '岗位要求',
  status       VARCHAR(20)  DEFAULT 'ONLINE' COMMENT '状态 ONLINE/OFFLINE',
  view_count   INT          DEFAULT 0 COMMENT '浏览次数',
  deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_company (company_id),
  KEY idx_status_city (status, city),
  KEY idx_type (job_type),
  KEY idx_job_latest (deleted, status, create_time),
  KEY idx_job_hot (deleted, status, view_count),
  KEY idx_job_salary (deleted, status, salary_max),
  KEY idx_job_type_latest (deleted, status, job_type, create_time),
  KEY idx_company_jobs (company_id, deleted, create_time),
  KEY idx_job_trend (deleted, create_time),
  CONSTRAINT chk_job_salary CHECK ((salary_min IS NULL OR salary_min >= 0) AND (salary_max IS NULL OR salary_max >= 0) AND (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max)),
  CONSTRAINT chk_job_views CHECK (view_count IS NULL OR view_count >= 0),
  CONSTRAINT chk_job_deleted CHECK (deleted IS NOT NULL AND deleted IN (0, 1))
) ENGINE = InnoDB COMMENT = '岗位表';

-- 4. 岗位技能表
CREATE TABLE job_skill (
  id             BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  job_id         BIGINT      NOT NULL COMMENT '岗位 id',
  skill_name     VARCHAR(50) NOT NULL COMMENT '技能名称',
  required_level INT         DEFAULT 3 COMMENT '要求等级 1-5',
  weight         INT         DEFAULT 1 COMMENT '技能权重',
  PRIMARY KEY (id),
  KEY idx_job_id (job_id),
  CONSTRAINT chk_skill_level CHECK (required_level IS NULL OR required_level BETWEEN 1 AND 5),
  CONSTRAINT chk_skill_weight CHECK (weight IS NULL OR weight > 0)
) ENGINE = InnoDB COMMENT = '岗位技能表';

-- 5. 简历表
CREATE TABLE resume (
  id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id          BIGINT       NOT NULL COMMENT '用户 id',
  name             VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
  phone            VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
  email            VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  education        VARCHAR(20)  DEFAULT NULL COMMENT '最高学历',
  school           VARCHAR(100) DEFAULT NULL COMMENT '毕业院校',
  major            VARCHAR(100) DEFAULT NULL COMMENT '专业',
  skills           VARCHAR(1000) DEFAULT NULL COMMENT '技能（逗号分隔）',
  summary          TEXT         COMMENT '个人简介',
  resume_file_url  VARCHAR(255) DEFAULT NULL COMMENT '简历附件地址',
  completion_score INT          DEFAULT 0 COMMENT '简历完整度 0-100',
  deleted          TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
  create_time      DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time      DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_resume_owner (user_id, deleted, id),
  CONSTRAINT chk_resume_completion CHECK (completion_score IS NULL OR completion_score BETWEEN 0 AND 100),
  CONSTRAINT chk_resume_deleted CHECK (deleted IS NOT NULL AND deleted IN (0, 1))
) ENGINE = InnoDB COMMENT = '简历表';

-- 6. 简历经历表
CREATE TABLE resume_experience (
  id         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  resume_id  BIGINT      NOT NULL COMMENT '简历 id',
  type       VARCHAR(20) NOT NULL COMMENT '类型 EDUCATION/WORK/PROJECT/INTERNSHIP',
  title      VARCHAR(100) DEFAULT NULL COMMENT '经历标题(岗位/项目/学位)',
  company    VARCHAR(100) DEFAULT NULL COMMENT '公司/学校',
  content    TEXT        COMMENT '经历描述',
  start_date VARCHAR(20) DEFAULT NULL COMMENT '开始时间(如 2022-09)',
  end_date   VARCHAR(20) DEFAULT NULL COMMENT '结束时间(如 2026-06)',
  sort       INT         DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id),
  KEY idx_resume_id (resume_id),
  KEY idx_experience_sort (resume_id, sort)
) ENGINE = InnoDB COMMENT = '简历经历表';

-- 7. 职位投递表
CREATE TABLE job_application (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  job_id       BIGINT       NOT NULL COMMENT '岗位 id',
  user_id      BIGINT       NOT NULL COMMENT '用户 id',
  resume_id    BIGINT       DEFAULT NULL COMMENT '简历 id',
  status       VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态 PENDING/VIEWED/INTERVIEW/REJECTED/HIRED',
  cover_letter VARCHAR(1000) DEFAULT NULL COMMENT '求职信',
  match_score  INT          DEFAULT NULL COMMENT 'AI 匹配度',
  deleted      TINYINT      DEFAULT 0 COMMENT '逻辑删除 0否1是',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间',
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '处理时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_job (user_id, job_id),
  KEY idx_job (job_id),
  KEY idx_status (status),
  KEY idx_application_user (user_id, deleted, id),
  KEY idx_application_job (job_id, deleted, status, id),
  KEY idx_application_admin (deleted, status, id),
  KEY idx_application_trend (deleted, create_time),
  CONSTRAINT chk_application_score CHECK (match_score IS NULL OR match_score BETWEEN 0 AND 100),
  CONSTRAINT chk_job_application_deleted CHECK (deleted IS NOT NULL AND deleted IN (0, 1))
) ENGINE = InnoDB COMMENT = '职位投递表';

-- 8. 职位收藏表
CREATE TABLE favorite (
  id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  job_id      BIGINT   NOT NULL COMMENT '岗位 id',
  user_id     BIGINT   NOT NULL COMMENT '用户 id',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_job (user_id, job_id),
  KEY idx_job (job_id),
  KEY idx_favorite_user (user_id, id)
) ENGINE = InnoDB COMMENT = '职位收藏表';

-- 9. AI 简历分析表
CREATE TABLE ai_resume_analysis (
  id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  resume_id           BIGINT       NOT NULL COMMENT '简历 id',
  overall_score       INT          DEFAULT NULL COMMENT '综合评分',
  skill_score         INT          DEFAULT NULL COMMENT '技能评分',
  experience_score    INT          DEFAULT NULL COMMENT '经历评分',
  education_score     INT          DEFAULT NULL COMMENT '教育评分',
  completeness_score  INT          DEFAULT NULL COMMENT '完整度评分',
  strengths           VARCHAR(2000) DEFAULT NULL COMMENT '优势(JSON数组)',
  weaknesses          VARCHAR(2000) DEFAULT NULL COMMENT '不足(JSON数组)',
  suggestions         VARCHAR(3000) DEFAULT NULL COMMENT '建议(JSON数组)',
  analysis_result     TEXT         COMMENT '完整分析(适配方向)',
  create_time         DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_resume_id (resume_id),
  CONSTRAINT chk_analysis_scores CHECK ((overall_score IS NULL OR overall_score BETWEEN 0 AND 100) AND (skill_score IS NULL OR skill_score BETWEEN 0 AND 100) AND (experience_score IS NULL OR experience_score BETWEEN 0 AND 100) AND (education_score IS NULL OR education_score BETWEEN 0 AND 100) AND (completeness_score IS NULL OR completeness_score BETWEEN 0 AND 100))
) ENGINE = InnoDB COMMENT = 'AI 简历分析结果表';

-- 10. AI 岗位推荐表
CREATE TABLE ai_recommendation (
  id               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id          BIGINT        NOT NULL COMMENT '用户 id',
  job_id           BIGINT        NOT NULL COMMENT '岗位 id',
  match_score      INT           DEFAULT NULL COMMENT '综合匹配度',
  skill_score      INT           DEFAULT NULL COMMENT '技能匹配度',
  experience_score INT           DEFAULT NULL COMMENT '经验匹配度',
  education_score  INT           DEFAULT NULL COMMENT '学历匹配度',
  semantic_score   INT           DEFAULT NULL COMMENT '语义匹配度',
  recommend_reason VARCHAR(1000) DEFAULT NULL COMMENT '推荐理由',
  create_time      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_job (user_id, job_id),
  KEY idx_user (user_id),
  KEY idx_recommendation_rank (user_id, match_score),
  CONSTRAINT chk_recommendation_scores CHECK ((match_score IS NULL OR match_score BETWEEN 0 AND 100) AND (skill_score IS NULL OR skill_score BETWEEN 0 AND 100) AND (experience_score IS NULL OR experience_score BETWEEN 0 AND 100) AND (education_score IS NULL OR education_score BETWEEN 0 AND 100) AND (semantic_score IS NULL OR semantic_score BETWEEN 0 AND 100))
) ENGINE = InnoDB COMMENT = 'AI 岗位推荐结果表';

-- 11. AI 模拟面试表
CREATE TABLE ai_interview (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id         BIGINT       NOT NULL COMMENT '用户 id',
  job_id          BIGINT       DEFAULT NULL COMMENT '目标岗位 id',
  title           VARCHAR(100) DEFAULT NULL COMMENT '面试标题',
  difficulty      VARCHAR(20)  DEFAULT '中等' COMMENT '难度 简单/中等/困难',
  question_count  INT          DEFAULT 0 COMMENT '题目数量',
  total_score     INT          DEFAULT NULL COMMENT '总分',
  status          VARCHAR(20)  DEFAULT 'ONGOING' COMMENT '状态 ONGOING/COMPLETED',
  report          TEXT         COMMENT '面试报告(JSON)',
  create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_interview_history (user_id, id),
  CONSTRAINT chk_interview_score CHECK (total_score IS NULL OR total_score BETWEEN 0 AND 100),
  CONSTRAINT chk_interview_count CHECK (question_count IS NULL OR question_count >= 0)
) ENGINE = InnoDB COMMENT = 'AI 模拟面试表';

-- 12. AI 面试题目表
CREATE TABLE ai_interview_question (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  interview_id  BIGINT       NOT NULL COMMENT '面试 id',
  question      TEXT         NOT NULL COMMENT '题目',
  question_type VARCHAR(30)  DEFAULT NULL COMMENT '题目类型',
  difficulty    VARCHAR(10)  DEFAULT NULL COMMENT '难度',
  answer        TEXT         COMMENT '用户回答',
  score         INT          DEFAULT NULL COMMENT 'AI 评分',
  feedback      TEXT         COMMENT 'AI 点评',
  sort          INT          DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (id),
  KEY idx_interview_id (interview_id),
  KEY idx_question_sort (interview_id, sort),
  CONSTRAINT chk_question_score CHECK (score IS NULL OR score BETWEEN 0 AND 100)
) ENGINE = InnoDB COMMENT = 'AI 面试题目表';

-- 13. 举报表
CREATE TABLE report (
  id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT        NOT NULL COMMENT '举报人 id',
  target_type   VARCHAR(20)   NOT NULL COMMENT '对象类型 JOB/COMPANY',
  target_id     BIGINT        NOT NULL COMMENT '对象 id',
  reason        VARCHAR(100)  NOT NULL COMMENT '举报原因',
  detail        VARCHAR(1000) DEFAULT NULL COMMENT '补充说明',
  status        VARCHAR(20)   DEFAULT 'PENDING' COMMENT '状态 PENDING/HANDLED/IGNORED',
  handle_remark VARCHAR(500)  DEFAULT NULL COMMENT '处理备注',
  deleted       TINYINT       DEFAULT 0 COMMENT '逻辑删除 0否1是',
  create_time   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
  PRIMARY KEY (id),
  KEY idx_status (status),
  KEY idx_report_list (deleted, status, id),
  CONSTRAINT chk_report_deleted CHECK (deleted IS NOT NULL AND deleted IN (0, 1))
) ENGINE = InnoDB COMMENT = '举报表';

-- 14. 系统日志表
CREATE TABLE sys_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       DEFAULT NULL COMMENT '操作用户 id',
  username    VARCHAR(50)  DEFAULT NULL COMMENT '用户名',
  type        VARCHAR(20)  DEFAULT NULL COMMENT '类型 LOGIN/OPERATE/AI/ERROR',
  module      VARCHAR(50)  DEFAULT NULL COMMENT '模块',
  action      VARCHAR(100) DEFAULT NULL COMMENT '动作',
  detail      VARCHAR(2000) DEFAULT NULL COMMENT '详情',
  ip          VARCHAR(50)  DEFAULT NULL COMMENT 'IP',
  cost_ms     BIGINT       DEFAULT NULL COMMENT '耗时(毫秒)',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (id),
  KEY idx_type (type),
  KEY idx_user (user_id),
  KEY idx_log_time (create_time),
  KEY idx_log_type_time (type, create_time)
) ENGINE = InnoDB COMMENT = '系统日志表';

-- 15. AI 智能助手会话表（页面右下角悬浮助手）
CREATE TABLE ai_chat_session (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT       NOT NULL COMMENT '用户 id',
  title         VARCHAR(100) DEFAULT '新对话' COMMENT '会话标题(取首条提问)',
  message_count INT          DEFAULT 0 COMMENT '消息条数',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id),
  KEY idx_chat_history (user_id, id),
  CONSTRAINT chk_chat_count CHECK (message_count IS NULL OR message_count >= 0)
) ENGINE = InnoDB COMMENT = 'AI 智能助手会话表';

-- 16. AI 智能助手消息表
CREATE TABLE ai_chat_message (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  session_id  BIGINT      NOT NULL COMMENT '会话 id',
  user_id     BIGINT      DEFAULT NULL COMMENT '用户 id',
  role        VARCHAR(20) NOT NULL COMMENT '角色 USER/ASSISTANT',
  content     TEXT        NOT NULL COMMENT '消息内容',
  suggestions VARCHAR(1000) DEFAULT NULL COMMENT '推荐追问(JSON数组)',
  source      VARCHAR(10) DEFAULT NULL COMMENT '回答来源 LLM/RULE',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '时间',
  PRIMARY KEY (id),
  KEY idx_session (session_id),
  KEY idx_user (user_id),
  KEY idx_chat_messages (session_id, id)
) ENGINE = InnoDB COMMENT = 'AI 智能助手消息表';
