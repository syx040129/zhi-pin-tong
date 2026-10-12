package com.example.recruitment.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.recruitment.admin.dto.SysUserAdminVO;
import com.example.recruitment.common.PageResult;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.constant.RoleConstants;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 管理端用户服务
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public PageResult<SysUserAdminVO> page(String keyword, String role, String status,
                                           long page, long size) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        qw.eq(StringUtils.hasText(role), SysUser::getRole, role);
        qw.eq(StringUtils.hasText(status), SysUser::getStatus, status);
        qw.orderByDesc(SysUser::getId);
        Page<SysUser> result = userMapper.selectPage(new Page<>(page, size), qw);
        List<SysUserAdminVO> vos = result.getRecords().stream().map(this::toVO).toList();
        return PageResult.map(result, vos);
    }

    public void setStatus(Long id, String status) {
        SysUser user = getUser(id);
        if (!(CommonConstants.USER_ENABLED.equals(status) || CommonConstants.USER_DISABLED.equals(status))) {
            throw BusinessException.badRequest("状态参数不正确");
        }
        if (RoleConstants.ADMIN.equals(user.getRole())) {
            throw BusinessException.forbidden("不能修改管理员账号状态");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    public void resetPassword(Long id, String newPassword) {
        SysUser user = getUser(id);
        String password = StringUtils.hasText(newPassword) ? newPassword : "123456";
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
    }

    public void deleteUser(Long id) {
        SysUser user = getUser(id);
        if (Objects.equals(user.getId(), SecurityUtils.currentUserId())) {
            throw BusinessException.badRequest("不能删除当前登录账号");
        }
        if (RoleConstants.ADMIN.equals(user.getRole())) {
            throw BusinessException.forbidden("不能删除管理员账号");
        }
        userMapper.deleteById(id);
    }

    private SysUser getUser(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    private SysUserAdminVO toVO(SysUser user) {
        SysUserAdminVO vo = new SysUserAdminVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setGender(user.getGender());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
