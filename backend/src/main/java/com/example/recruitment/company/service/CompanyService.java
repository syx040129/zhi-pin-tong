package com.example.recruitment.company.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.company.dto.CompanyForm;
import com.example.recruitment.company.dto.CompanyVO;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 企业服务
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyMapper companyMapper;
    private final JobMapper jobMapper;
    private final SysUserMapper sysUserMapper;

    /**
     * 企业账号注册时自动创建企业档案
     */
    public Company createCompanyForUser(Long userId, String companyName) {
        Company company = new Company();
        company.setUserId(userId);
        company.setCompanyName(StringUtils.hasText(companyName) ? companyName : "未命名企业");
        company.setStatus(CommonConstants.COMPANY_ACTIVE);
        companyMapper.insert(company);
        return company;
    }

    public Company getByUserId(Long userId) {
        return companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                .eq(Company::getUserId, userId).last("limit 1"));
    }

    /**
     * 获取当前登录企业的档案
     */
    public Company currentCompany() {
        Company company = getByUserId(SecurityUtils.currentUserId());
        if (company == null) {
            throw BusinessException.notFound("企业档案不存在，请联系管理员");
        }
        if (!CommonConstants.COMPANY_ACTIVE.equals(company.getStatus())) {
            throw BusinessException.forbidden("企业账号已被禁用");
        }
        return company;
    }

    public CompanyVO currentProfile() {
        return toVO(currentCompany(), true);
    }

    public CompanyVO updateProfile(CompanyForm form) {
        Company company = currentCompany();
        company.setCompanyName(form.getCompanyName());
        company.setIndustry(form.getIndustry());
        company.setScale(form.getScale());
        company.setAddress(form.getAddress());
        company.setDescription(form.getDescription());
        company.setWebsite(form.getWebsite());
        companyMapper.updateById(company);
        return toVO(company, true);
    }

    public void updateLogo(String logoUrl) {
        Company company = currentCompany();
        company.setLogo(logoUrl);
        companyMapper.updateById(company);
    }

    public CompanyVO updateLogoByCompanyId(Long companyId, String logoUrl) {
        Company company = companyMapper.selectById(companyId);
        if (company == null) {
            throw BusinessException.notFound("企业不存在");
        }
        company.setLogo(logoUrl);
        companyMapper.updateById(company);
        return toVO(company, false);
    }

    /**
     * 公共企业详情
     */
    public CompanyVO publicDetail(Long id) {
        Company company = companyMapper.selectById(id);
        if (company == null || CommonConstants.COMPANY_DISABLED.equals(company.getStatus())) {
            throw BusinessException.notFound("企业不存在");
        }
        return toVO(company, true);
    }

    public CompanyVO toVO(Company company, boolean withJobCount) {
        CompanyVO vo = new CompanyVO();
        vo.setId(company.getId());
        vo.setUserId(company.getUserId());
        vo.setCompanyName(company.getCompanyName());
        vo.setLogo(company.getLogo());
        vo.setIndustry(company.getIndustry());
        vo.setScale(company.getScale());
        vo.setAddress(company.getAddress());
        vo.setDescription(company.getDescription());
        vo.setWebsite(company.getWebsite());
        vo.setStatus(company.getStatus());
        vo.setCreateTime(company.getCreateTime());
        if (withJobCount) {
            vo.setJobCount(jobMapper.selectCount(new LambdaQueryWrapper<Job>()
                    .eq(Job::getCompanyId, company.getId())
                    .eq(Job::getStatus, CommonConstants.JOB_ONLINE)));
        }
        return vo;
    }

    public List<CompanyVO> toVOList(List<Company> companies) {
        if (companies.isEmpty()) {
            return List.of();
        }
        List<Long> companyIds = companies.stream().map(Company::getId).toList();
        Map<Long, Long> jobCounts = jobMapper.selectList(new LambdaQueryWrapper<Job>()
                        .in(Job::getCompanyId, companyIds))
                .stream().collect(Collectors.groupingBy(Job::getCompanyId, Collectors.counting()));
        List<Long> userIds = companies.stream().map(Company::getUserId).toList();
        Map<Long, SysUser> users = sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .in(SysUser::getId, userIds))
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));
        return companies.stream().map(c -> {
            CompanyVO vo = toVO(c, false);
            vo.setJobCount(jobCounts.getOrDefault(c.getId(), 0L));
            SysUser user = users.get(c.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setContactPhone(user.getPhone());
            }
            return vo;
        }).toList();
    }

    public PageResult<CompanyVO> adminPage(long page, long size, String keyword, String status) {
        LambdaQueryWrapper<Company> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Company::getCompanyName, keyword)
                    .or().like(Company::getIndustry, keyword));
        }
        if (StringUtils.hasText(status)) {
            qw.eq(Company::getStatus, status);
        }
        qw.orderByDesc(Company::getId);
        Page<Company> result = companyMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.map(result, toVOList(result.getRecords()));
    }

    public void setStatus(Long id, String status) {
        Company company = companyMapper.selectById(id);
        if (company == null) {
            throw BusinessException.notFound("企业不存在");
        }
        if (!(CommonConstants.COMPANY_ACTIVE.equals(status) || CommonConstants.COMPANY_DISABLED.equals(status))) {
            throw BusinessException.badRequest("状态参数不正确");
        }
        company.setStatus(status);
        companyMapper.updateById(company);
        // 同步禁用/启用企业对应账号
        SysUser user = sysUserMapper.selectById(company.getUserId());
        if (user != null) {
            user.setStatus(CommonConstants.COMPANY_ACTIVE.equals(status)
                    ? CommonConstants.USER_ENABLED : CommonConstants.USER_DISABLED);
            sysUserMapper.updateById(user);
        }
    }

    public void deleteCompany(Long id) {
        Company company = companyMapper.selectById(id);
        if (company == null) {
            throw BusinessException.notFound("企业不存在");
        }
        companyMapper.deleteById(id);
        SysUser user = sysUserMapper.selectById(company.getUserId());
        if (user != null) {
            sysUserMapper.deleteById(user.getId());
        }
    }
}
