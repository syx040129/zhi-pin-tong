package com.example.recruitment.company.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompanyQuery {
    @Size(max = 100)
    private String keyword;
    @Size(max = 50)
    private String industry;
    @Size(max = 50)
    private String scale;
    /** 默认仅展示正在招聘的企业。 */
    private boolean includeInactiveHiring = false;
    @Min(1)
    private long page = 1;
    @Min(1)
    @Max(100)
    private long size = 12;
}
