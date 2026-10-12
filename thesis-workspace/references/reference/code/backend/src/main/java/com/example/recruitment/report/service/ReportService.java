package com.example.recruitment.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.job.entity.Job;
import com.example.recruitment.job.mapper.JobMapper;
import com.example.recruitment.report.dto.ReportForm;
import com.example.recruitment.report.dto.ReportVO;
import com.example.recruitment.report.entity.Report;
import com.example.recruitment.report.mapper.ReportMapper;
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
 * 举报服务
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;
    private final JobMapper jobMapper;
    private final CompanyMapper companyMapper;
    private final SysUserMapper userMapper;

    public void create(ReportForm form, Long userId) {
        if ("JOB".equals(form.getTargetType()) && jobMapper.selectById(form.getTargetId()) == null) {
            throw BusinessException.notFound("举报的岗位不存在");
        }
        if ("COMPANY".equals(form.getTargetType()) && companyMapper.selectById(form.getTargetId()) == null) {
            throw BusinessException.notFound("举报的企业不存在");
        }
        Report report = new Report();
        report.setUserId(userId);
        report.setTargetType(form.getTargetType());
        report.setTargetId(form.getTargetId());
        report.setReason(form.getReason());
        report.setDetail(form.getDetail());
        report.setStatus(CommonConstants.REPORT_PENDING);
        reportMapper.insert(report);
    }

    public PageResult<ReportVO> myPage(Long userId, long page, long size) {
        Page<Report> result = reportMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getUserId, userId)
                        .orderByDesc(Report::getId));
        return toPage(result);
    }

    public PageResult<ReportVO> adminPage(String status, long page, long size) {
        Page<Report> result = reportMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Report>()
                        .eq(StringUtils.hasText(status), Report::getStatus, status)
                        .orderByDesc(Report::getId));
        return toPage(result);
    }

    public void setStatus(Long id, String status, String handleRemark) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw BusinessException.notFound("举报记录不存在");
        }
        if (!(CommonConstants.REPORT_HANDLED.equals(status)
                || CommonConstants.REPORT_IGNORED.equals(status))) {
            throw BusinessException.badRequest("状态参数不正确");
        }
        report.setStatus(status);
        report.setHandleRemark(handleRemark);
        reportMapper.updateById(report);
    }

    public void adminDelete(Long id) {
        if (reportMapper.selectById(id) == null) {
            throw BusinessException.notFound("举报记录不存在");
        }
        reportMapper.deleteById(id);
    }

    private PageResult<ReportVO> toPage(Page<Report> page) {
        List<Report> records = page.getRecords();
        List<Long> userIds = records.stream().map(Report::getUserId).distinct().toList();
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, Function.identity()));
        List<Long> jobIds = records.stream()
                .filter(r -> "JOB".equals(r.getTargetType())).map(Report::getTargetId).toList();
        List<Long> companyIds = records.stream()
                .filter(r -> "COMPANY".equals(r.getTargetType())).map(Report::getTargetId).toList();
        Map<Long, Job> jobs = jobIds.isEmpty() ? Map.of()
                : jobMapper.selectBatchIds(jobIds).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        Map<Long, Company> companies = companyIds.isEmpty() ? Map.of()
                : companyMapper.selectBatchIds(companyIds).stream()
                .collect(Collectors.toMap(Company::getId, Function.identity()));
        List<ReportVO> vos = records.stream().map(r -> {
            ReportVO vo = new ReportVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            SysUser user = users.get(r.getUserId());
            vo.setReporterName(user == null ? "" : user.getNickname());
            vo.setTargetType(r.getTargetType());
            vo.setTargetId(r.getTargetId());
            if ("JOB".equals(r.getTargetType())) {
                Job job = jobs.get(r.getTargetId());
                vo.setTargetName(job == null ? "（已删除岗位）" : job.getJobName());
            } else {
                Company company = companies.get(r.getTargetId());
                vo.setTargetName(company == null ? "（已删除企业）" : company.getCompanyName());
            }
            vo.setReason(r.getReason());
            vo.setDetail(r.getDetail());
            vo.setStatus(r.getStatus());
            vo.setHandleRemark(r.getHandleRemark());
            vo.setCreateTime(r.getCreateTime());
            return vo;
        }).toList();
        return PageResult.map(page, vos);
    }
}
