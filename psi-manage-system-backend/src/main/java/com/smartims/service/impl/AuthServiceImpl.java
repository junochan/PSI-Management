package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartims.dto.ChangePasswordDTO;
import com.smartims.dto.LoginDTO;
import com.smartims.entity.SysRole;
import com.smartims.entity.SysUser;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.SysRoleMapper;
import com.smartims.mapper.SysUserMapper;
import com.smartims.security.JwtUtil;
import com.smartims.security.UserContext;
import com.smartims.service.AuthService;
import com.smartims.service.PermissionService;
import com.smartims.vo.LoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 查询用户
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, loginDTO.getUsername());
        SysUser user = sysUserMapper.selectOne(queryWrapper);

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证用户状态
        if (user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        // 更新登录时间
        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        // 查询角色信息
        SysRole role = null;
        if (user.getRoleId() != null) {
            role = sysRoleMapper.selectById(user.getRoleId());
        }

        // 生成JWT令牌
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        // 构建登录响应
        LoginVO loginVO = new LoginVO();
        loginVO.setUserId(user.getId());
        loginVO.setUsername(user.getUsername());
        loginVO.setName(user.getName());
        loginVO.setEmail(user.getEmail());
        loginVO.setPhone(user.getPhone());
        loginVO.setAvatar(user.getAvatar());
        loginVO.setRoleId(user.getRoleId());
        loginVO.setRoleName(role != null ? role.getName() : null);
        loginVO.setToken(token);

        List<String> permissionCodes = permissionService.getPermissionCodesByUserId(user.getId());
        loginVO.setPermissions(permissionCodes);

        log.info("用户登录成功：userId={}, username={}", user.getId(), user.getUsername());
        return loginVO;
    }

    @Override
    public void logout() {
        log.info("用户登出成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = UserContext.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("未登录或登录已过期");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("当前密码错误");
        }
        String newPwd = dto.getNewPassword() != null ? dto.getNewPassword().trim() : "";
        if (!StringUtils.hasText(newPwd) || newPwd.length() < 6) {
            throw new BusinessException("新密码长度至少6位");
        }
        if (passwordEncoder.matches(newPwd, user.getPassword())) {
            throw new BusinessException("新密码不能与当前密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPwd));
        sysUserMapper.updateById(user);
        log.info("用户修改密码成功：userId={}", userId);
    }

}