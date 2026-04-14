package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.UserDTO;
import com.smartims.entity.SysUser;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.SysUserMapper;
import com.smartims.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public PageResult<SysUser> getUserList(PageQuery pageQuery) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getDeleted, 0);

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(SysUser::getName, pageQuery.getKeyword())
                    .or()
                    .like(SysUser::getEmail, pageQuery.getKeyword())
                    .or()
                    .like(SysUser::getUsername, pageQuery.getKeyword())
            );
        }

        queryWrapper.orderByDesc(SysUser::getCreateTime);

        Page<SysUser> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<SysUser> result = sysUserMapper.selectPage(page, queryWrapper);

        return PageResult.build(result.getTotal(), pageQuery.getPage(), pageQuery.getSize(), result.getRecords());
    }

    @Override
    public SysUser getUserById(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(UserDTO dto) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, dto.getUsername().trim());
        queryWrapper.eq(SysUser::getDeleted, 0);
        if (sysUserMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException("登录名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername().trim());
        user.setName(dto.getName().trim());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRoleId(dto.getRoleId() != null ? dto.getRoleId() : 3L); // 默认销售专员
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);

        // 设置密码
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        } else {
            // 默认密码
            user.setPassword(passwordEncoder.encode("123456"));
        }

        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }

        sysUserMapper.insert(user);
        log.info("添加用户成功：userId={}, username={}, name={}", user.getId(), user.getUsername(), user.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(Long id, UserDTO dto) {
        SysUser user = getUserById(id);

        String newUsername = dto.getUsername().trim();
        if (!newUsername.equals(user.getUsername())) {
            LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(SysUser::getUsername, newUsername);
            queryWrapper.eq(SysUser::getDeleted, 0);
            queryWrapper.ne(SysUser::getId, id);
            if (sysUserMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("登录名已存在");
            }
        }

        user.setUsername(newUsername);
        user.setName(dto.getName().trim());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRoleId(dto.getRoleId() != null ? dto.getRoleId() : user.getRoleId());
        user.setStatus(dto.getStatus() != null ? dto.getStatus() : user.getStatus());

        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }

        // 更新密码（如果提供了新密码）
        if (StringUtils.hasText(dto.getPassword())) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        sysUserMapper.updateById(user);
        log.info("更新用户成功：userId={}, name={}", id, user.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        SysUser user = getUserById(id);
        sysUserMapper.deleteById(id);
        log.info("删除用户成功：userId={}, name={}", id, user.getName());
    }

    @Override
    public List<SysUser> getAllUsers() {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getDeleted, 0);
        queryWrapper.orderByDesc(SysUser::getCreateTime);
        return sysUserMapper.selectList(queryWrapper);
    }

}