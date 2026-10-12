package com.example.recruitment.job.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 岗位技能表
 */
@Data
@TableName("job_skill")
public class JobSkill {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long jobId;
    private String skillName;
    /** 要求等级 1-5 */
    private Integer requiredLevel;
    /** 技能权重 */
    private Integer weight;
}
