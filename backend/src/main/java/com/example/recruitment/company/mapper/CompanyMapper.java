package com.example.recruitment.company.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.dto.CompanyJobCount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 企业 Mapper
 */
@Mapper
public interface CompanyMapper extends BaseMapper<Company> {
    @Select("""
            <script>
            SELECT company_id, COUNT(*) AS job_count FROM job
            WHERE deleted = 0 AND status = 'ONLINE' AND company_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY company_id
            </script>
            """)
    List<CompanyJobCount> countOnlineJobs(@Param("ids") List<Long> ids);
}
