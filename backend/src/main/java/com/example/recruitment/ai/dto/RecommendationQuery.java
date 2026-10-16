package com.example.recruitment.ai.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.util.StringUtils;

/** 选填意向仅用于当前查询，不覆盖默认推荐缓存。薪资单位：K/月。 */
@Data
public class RecommendationQuery {
    @Positive
    private Long resumeId;
    @Size(max = 50)
    private String city;
    @Size(max = 50)
    private String jobType;
    @Min(0)
    private Integer salaryMin;
    @Min(0)
    private Integer salaryMax;

    public boolean personalized() {
        return resumeId != null || StringUtils.hasText(city) || StringUtils.hasText(jobType)
                || salaryMin != null || salaryMax != null;
    }

    @AssertTrue(message = "最高薪资不能低于最低薪资")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMax >= salaryMin;
    }
}
