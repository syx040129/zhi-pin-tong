-- =============================================================
-- AI 智能助手（页面右下角悬浮对话框）增量脚本
-- 适用：已按旧版 schema.sql 初始化过的数据库（不删除任何既有数据）
-- 全新初始化无需执行本文件，schema.sql 已包含这两张表
-- 执行：mysql -uroot -p < sql/upgrade-ai-chat.sql
-- =============================================================

USE smart_recruitment;

-- 15. AI 智能助手会话表
CREATE TABLE IF NOT EXISTS ai_chat_session (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT       NOT NULL COMMENT '用户 id',
  title         VARCHAR(100) DEFAULT '新对话' COMMENT '会话标题(取首条提问)',
  message_count INT          DEFAULT 0 COMMENT '消息条数',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = 'AI 智能助手会话表';

-- 16. AI 智能助手消息表
CREATE TABLE IF NOT EXISTS ai_chat_message (
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
  KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = 'AI 智能助手消息表';
