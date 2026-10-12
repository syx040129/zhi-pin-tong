-- CareerNova 数据库增量优化，MySQL >= 8.0.16
-- 不删除业务数据；重复运行跳过已有索引和约束。DDL 自动提交，失败后修复数据再重跑。
USE smart_recruitment;
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS cn_add_db_object;
DELIMITER $$
CREATE PROCEDURE cn_add_db_object(IN p_table VARCHAR(64), IN p_name VARCHAR(64), IN p_kind VARCHAR(20), IN p_definition TEXT)
BEGIN
  IF (p_kind='INDEX' AND NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=p_table AND INDEX_NAME=p_name))
    OR (p_kind='CHECK' AND NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME=p_table AND CONSTRAINT_NAME=p_name)) THEN
    SET @cn_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD ', IF(p_kind='INDEX', 'INDEX ', 'CONSTRAINT '), '`', p_name, '` ', p_definition);
    PREPARE cn_stmt FROM @cn_ddl;
    EXECUTE cn_stmt;
    DEALLOCATE PREPARE cn_stmt;
  END IF;
END$$
DELIMITER ;
CALL cn_add_db_object('sys_user', 'idx_user_list', 'INDEX', '(deleted, role, status, id)');
CALL cn_add_db_object('sys_user', 'idx_user_trend', 'INDEX', '(deleted, create_time)');
CALL cn_add_db_object('company', 'idx_company_list', 'INDEX', '(deleted, status, id)');
CALL cn_add_db_object('job', 'idx_job_latest', 'INDEX', '(deleted, status, create_time)');
CALL cn_add_db_object('job', 'idx_job_hot', 'INDEX', '(deleted, status, view_count)');
CALL cn_add_db_object('job', 'idx_job_salary', 'INDEX', '(deleted, status, salary_max)');
CALL cn_add_db_object('job', 'idx_job_type_latest', 'INDEX', '(deleted, status, job_type, create_time)');
CALL cn_add_db_object('job', 'idx_company_jobs', 'INDEX', '(company_id, deleted, create_time)');
CALL cn_add_db_object('job', 'idx_job_trend', 'INDEX', '(deleted, create_time)');
CALL cn_add_db_object('resume', 'idx_resume_owner', 'INDEX', '(user_id, deleted, id)');
CALL cn_add_db_object('resume_experience', 'idx_experience_sort', 'INDEX', '(resume_id, sort)');
CALL cn_add_db_object('job_application', 'idx_application_user', 'INDEX', '(user_id, deleted, id)');
CALL cn_add_db_object('job_application', 'idx_application_job', 'INDEX', '(job_id, deleted, status, id)');
CALL cn_add_db_object('job_application', 'idx_application_admin', 'INDEX', '(deleted, status, id)');
CALL cn_add_db_object('job_application', 'idx_application_trend', 'INDEX', '(deleted, create_time)');
CALL cn_add_db_object('favorite', 'idx_favorite_user', 'INDEX', '(user_id, id)');
CALL cn_add_db_object('ai_recommendation', 'idx_recommendation_rank', 'INDEX', '(user_id, match_score)');
CALL cn_add_db_object('ai_interview', 'idx_interview_history', 'INDEX', '(user_id, id)');
CALL cn_add_db_object('ai_interview_question', 'idx_question_sort', 'INDEX', '(interview_id, sort)');
CALL cn_add_db_object('report', 'idx_report_list', 'INDEX', '(deleted, status, id)');
CALL cn_add_db_object('sys_log', 'idx_log_time', 'INDEX', '(create_time)');
CALL cn_add_db_object('sys_log', 'idx_log_type_time', 'INDEX', '(type, create_time)');
CALL cn_add_db_object('ai_chat_session', 'idx_chat_history', 'INDEX', '(user_id, id)');
CALL cn_add_db_object('ai_chat_message', 'idx_chat_messages', 'INDEX', '(session_id, id)');
CALL cn_add_db_object('job', 'chk_job_salary', 'CHECK', 'CHECK ((salary_min IS NULL OR salary_min >= 0) AND (salary_max IS NULL OR salary_max >= 0) AND (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max))');
CALL cn_add_db_object('job', 'chk_job_views', 'CHECK', 'CHECK (view_count IS NULL OR view_count >= 0)');
CALL cn_add_db_object('job', 'chk_job_deleted', 'CHECK', 'CHECK (deleted IS NOT NULL AND deleted IN (0, 1))');
CALL cn_add_db_object('job_skill', 'chk_skill_level', 'CHECK', 'CHECK (required_level IS NULL OR required_level BETWEEN 1 AND 5)');
CALL cn_add_db_object('job_skill', 'chk_skill_weight', 'CHECK', 'CHECK (weight IS NULL OR weight > 0)');
CALL cn_add_db_object('resume', 'chk_resume_completion', 'CHECK', 'CHECK (completion_score IS NULL OR completion_score BETWEEN 0 AND 100)');
CALL cn_add_db_object('resume', 'chk_resume_deleted', 'CHECK', 'CHECK (deleted IS NOT NULL AND deleted IN (0, 1))');
CALL cn_add_db_object('job_application', 'chk_application_score', 'CHECK', 'CHECK (match_score IS NULL OR match_score BETWEEN 0 AND 100)');
CALL cn_add_db_object('job_application', 'chk_job_application_deleted', 'CHECK', 'CHECK (deleted IS NOT NULL AND deleted IN (0, 1))');
CALL cn_add_db_object('ai_resume_analysis', 'chk_analysis_scores', 'CHECK', 'CHECK ((overall_score IS NULL OR overall_score BETWEEN 0 AND 100) AND (skill_score IS NULL OR skill_score BETWEEN 0 AND 100) AND (experience_score IS NULL OR experience_score BETWEEN 0 AND 100) AND (education_score IS NULL OR education_score BETWEEN 0 AND 100) AND (completeness_score IS NULL OR completeness_score BETWEEN 0 AND 100))');
CALL cn_add_db_object('ai_recommendation', 'chk_recommendation_scores', 'CHECK', 'CHECK ((match_score IS NULL OR match_score BETWEEN 0 AND 100) AND (skill_score IS NULL OR skill_score BETWEEN 0 AND 100) AND (experience_score IS NULL OR experience_score BETWEEN 0 AND 100) AND (education_score IS NULL OR education_score BETWEEN 0 AND 100) AND (semantic_score IS NULL OR semantic_score BETWEEN 0 AND 100))');
CALL cn_add_db_object('ai_interview', 'chk_interview_score', 'CHECK', 'CHECK (total_score IS NULL OR total_score BETWEEN 0 AND 100)');
CALL cn_add_db_object('ai_interview', 'chk_interview_count', 'CHECK', 'CHECK (question_count IS NULL OR question_count >= 0)');
CALL cn_add_db_object('ai_interview_question', 'chk_question_score', 'CHECK', 'CHECK (score IS NULL OR score BETWEEN 0 AND 100)');
CALL cn_add_db_object('ai_chat_session', 'chk_chat_count', 'CHECK', 'CHECK (message_count IS NULL OR message_count >= 0)');
CALL cn_add_db_object('sys_user', 'chk_sys_user_deleted', 'CHECK', 'CHECK (deleted IS NOT NULL AND deleted IN (0, 1))');
CALL cn_add_db_object('company', 'chk_company_deleted', 'CHECK', 'CHECK (deleted IS NOT NULL AND deleted IN (0, 1))');
CALL cn_add_db_object('report', 'chk_report_deleted', 'CHECK', 'CHECK (deleted IS NOT NULL AND deleted IN (0, 1))');
DROP PROCEDURE cn_add_db_object;
