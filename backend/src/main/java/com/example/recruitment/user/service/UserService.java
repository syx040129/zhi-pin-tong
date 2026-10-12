package com.example.recruitment.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.recruitment.common.constant.CommonConstants;
import com.example.recruitment.common.exception.BusinessException;
import com.example.recruitment.common.utils.SecurityUtils;
import com.example.recruitment.company.entity.Company;
import com.example.recruitment.company.mapper.CompanyMapper;
import com.example.recruitment.user.dto.ProfileUpdateRequest;
import com.example.recruitment.user.dto.UserProfileVO;
import com.example.recruitment.user.entity.SysUser;
import com.example.recruitment.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final SysUserMapper sysUserMapper;
    private final CompanyMapper companyMapper;
    private final PasswordEncoder passwordEncoder;

    public SysUser getById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    public SysUser getByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username).last("limit 1"));
    }

    public SysUser createUser(String username, String rawPassword, String nickname, String phone,
                              String email, String role) {
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username);
        user.setPhone(phone);
        user.setEmail(email);
        user.setGender("UNKNOWN");
        user.setRole(role);
        user.setStatus(CommonConstants.USER_ENABLED);
        sysUserMapper.insert(user);
        return user;
    }

    /**
     * 获取当前登录用户完整信息
     */
    public UserProfileVO currentProfile() {
        return toProfileVO(getById(SecurityUtils.currentUserId()));
    }

    public UserProfileVO toProfileVO(SysUser user) {
        UserProfileVO vo = new UserProfileVO();
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
        if (user.getRole().equals("COMPANY")) {
            Company company = companyMapper.selectOne(new LambdaQueryWrapper<Company>()
                    .eq(Company::getUserId, user.getId()).last("limit 1"));
            if (company != null) {
                vo.setCompanyId(company.getId());
                vo.setCompanyName(company.getCompanyName());
            }
        }
        return vo;
    }

    public UserProfileVO updateProfile(ProfileUpdateRequest request) {
        SysUser user = getById(SecurityUtils.currentUserId());
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getGender() != null && !request.getGender().isBlank()) {
            user.setGender(request.getGender());
        }
        sysUserMapper.updateById(user);
        return toProfileVO(user);
    }

    public void changePassword(String oldPassword, String newPassword) {
        SysUser user = getById(SecurityUtils.currentUserId());
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BusinessException.badRequest("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    public void updateAvatar(String avatarUrl) {
        SysUser user = getById(SecurityUtils.currentUserId());
        user.setAvatar(avatarUrl);
        sysUserMapper.updateById(user);
    }

    public void updateAvatarByUserId(Long userId, String avatarUrl) {
        SysUser user = getById(userId);
        user.setAvatar(avatarUrl);
        sysUserMapper.updateById(user);
    }
}
