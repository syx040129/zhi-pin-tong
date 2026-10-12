package com.example.recruitment.common.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 统计查询 Mapper（日期趋势、分布等）
 */
@Mapper
public interface StatsMapper {

    @Select("SELECT DATE(create_time) AS d, COUNT(*) AS cnt FROM sys_user " +
            "WHERE role = 'USER' AND deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(create_time) ORDER BY d")
    List<Map<String, Object>> userTrend(int days);

    @Select("SELECT DATE(create_time) AS d, COUNT(*) AS cnt FROM job " +
            "WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(create_time) ORDER BY d")
    List<Map<String, Object>> jobTrend(int days);

    @Select("SELECT DATE(create_time) AS d, COUNT(*) AS cnt FROM job_application " +
            "WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE(create_time) ORDER BY d")
    List<Map<String, Object>> applicationTrend(int days);

    @Select("SELECT job_type AS name, COUNT(*) AS value FROM job " +
            "WHERE deleted = 0 AND status = 'ONLINE' GROUP BY job_type ORDER BY value DESC")
    List<Map<String, Object>> jobTypeDistribution();

    @Select("SELECT c.company_name AS name, COUNT(j.id) AS value FROM job j " +
            "JOIN company c ON c.id = j.company_id AND c.deleted = 0 " +
            "WHERE j.deleted = 0 GROUP BY c.id, c.company_name ORDER BY value DESC LIMIT 6")
    List<Map<String, Object>> companyJobTop();

    @Select("SELECT a.status AS name, COUNT(*) AS value FROM job_application a " +
            "JOIN job j ON j.id = a.job_id AND j.deleted = 0 " +
            "WHERE j.company_id = #{companyId} AND a.deleted = 0 GROUP BY a.status")
    List<Map<String, Object>> companyApplicationStatus(long companyId);
}
