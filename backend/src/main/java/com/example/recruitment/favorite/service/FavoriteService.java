package com.example.recruitment.favorite.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.favorite.dto.FavoriteVO;
import com.example.recruitment.favorite.entity.Favorite;
import com.example.recruitment.favorite.mapper.FavoriteMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 收藏服务
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;

    public void add(Long jobId, Long userId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            throw BusinessException.notFound("岗位不存在");
        }
        boolean exists = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getJobId, jobId)) > 0;
        if (!exists) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setJobId(jobId);
            favoriteMapper.insert(favorite);
        }
    }

    public void remove(Long jobId, Long userId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getJobId, jobId));
    }

    public boolean isFavorite(Long jobId, Long userId) {
        return favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId).eq(Favorite::getJobId, jobId)) > 0;
    }

    public PageResult<FavoriteVO> page(Long userId, long page, long size) {
        Page<Favorite> result = favoriteMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getId));
        List<Favorite> records = result.getRecords();
        List<FavoriteVO> vos = List.of();
        if (!records.isEmpty()) {
            List<Long> jobIds = records.stream().map(Favorite::getJobId).toList();
            Map<Long, Job> jobs = jobMapper.selectBatchIds(jobIds).stream()
                    .collect(Collectors.toMap(Job::getId, Function.identity()));
            List<Long> companyIds = jobs.values().stream().map(Job::getCompanyId)
                    .distinct().toList();
            Map<Long, Company> companies = companyIds.isEmpty() ? Map.of()
                    : companyMapper.selectBatchIds(companyIds).stream()
                    .collect(Collectors.toMap(Company::getId, Function.identity()));
            vos = records.stream().map(f -> {
                FavoriteVO vo = new FavoriteVO();
                vo.setId(f.getId());
                vo.setCreateTime(f.getCreateTime());
                Job job = jobs.get(f.getJobId());
                if (job != null) {
                    vo.setJobId(job.getId());
                    vo.setJobName(job.getJobName());
                    vo.setJobType(job.getJobType());
                    vo.setCity(job.getCity());
                    vo.setSalaryMin(job.getSalaryMin());
                    vo.setSalaryMax(job.getSalaryMax());
                    vo.setEducation(job.getEducation());
                    vo.setExperience(job.getExperience());
                    vo.setJobStatus(job.getStatus());
                    Company company = companies.get(job.getCompanyId());
                    if (company != null) {
                        vo.setCompanyId(company.getId());
                        vo.setCompanyName(company.getCompanyName());
                        vo.setCompanyLogo(company.getLogo());
                    }
                } else {
                    vo.setJobId(f.getJobId());
                }
                return vo;
            }).toList();
        }
        return PageResult.map(result, vos);
    }
}
