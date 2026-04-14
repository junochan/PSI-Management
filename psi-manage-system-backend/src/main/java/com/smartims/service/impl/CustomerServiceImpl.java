package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.CustomerDTO;
import com.smartims.dto.PageQuery;
import com.smartims.entity.Customer;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.CustomerMapper;
import com.smartims.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 客户服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerMapper customerMapper;

    @Override
    public PageResult<Customer> getCustomerList(PageQuery pageQuery) {
        LambdaQueryWrapper<Customer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Customer::getDeleted, 0);

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Customer::getName, pageQuery.getKeyword())
                    .or()
                    .like(Customer::getCode, pageQuery.getKeyword())
                    .or()
                    .like(Customer::getContact, pageQuery.getKeyword())
            );
        }

        if (StringUtils.hasText(pageQuery.getSort())) {
            if ("asc".equalsIgnoreCase(pageQuery.getOrder())) {
                queryWrapper.orderByAsc(Customer::getCreateTime);
            } else {
                queryWrapper.orderByDesc(Customer::getCreateTime);
            }
        } else {
            queryWrapper.orderByDesc(Customer::getCreateTime);
        }

        Page<Customer> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<Customer> result = customerMapper.selectPage(page, queryWrapper);

        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Customer getCustomerById(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null || customer.getDeleted() == 1) {
            throw new BusinessException("客户不存在");
        }
        return customer;
    }

    @Override
    public void createCustomer(CustomerDTO dto) {
        if (!StringUtils.hasText(dto.getName())) {
            throw new BusinessException("客户名称不能为空");
        }
        // 检查编码是否重复
        if (StringUtils.hasText(dto.getCode())) {
            LambdaQueryWrapper<Customer> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Customer::getCode, dto.getCode());
            queryWrapper.eq(Customer::getDeleted, 0);
            if (customerMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("客户编码已存在");
            }
        }

        Customer customer = new Customer();
        customer.setName(dto.getName());
        customer.setCode(dto.getCode());
        customer.setType(dto.getType());
        customer.setContact(dto.getContactPerson());
        customer.setPhone(dto.getContactPhone());
        customer.setEmail(dto.getEmail());
        customer.setAddress(dto.getAddress());
        customer.setVipLevel(dto.getVipLevel());
        customer.setRemark(dto.getRemark());
        customer.setStatus(1); // active

        customerMapper.insert(customer);
        log.info("创建客户成功：id={}, name={}", customer.getId(), customer.getName());
    }

    @Override
    public void updateCustomer(Long id, CustomerDTO dto) {
        Customer existing = getCustomerById(id);

        // 检查编码是否重复（排除自身）
        if (StringUtils.hasText(dto.getCode()) && !dto.getCode().equals(existing.getCode())) {
            LambdaQueryWrapper<Customer> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Customer::getCode, dto.getCode());
            queryWrapper.eq(Customer::getDeleted, 0);
            queryWrapper.ne(Customer::getId, id);
            if (customerMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("客户编码已存在");
            }
        }

        // 仅更新请求体中出现的字段，避免前端用 contact/phone 等别名时未传联系人导致被清空
        if (dto.getName() != null) {
            existing.setName(dto.getName());
        }
        if (dto.getCode() != null) {
            existing.setCode(dto.getCode());
        }
        if (dto.getType() != null) {
            existing.setType(dto.getType());
        }
        if (dto.getContactPerson() != null) {
            existing.setContact(dto.getContactPerson());
        }
        if (dto.getContactPhone() != null) {
            existing.setPhone(dto.getContactPhone());
        }
        if (dto.getEmail() != null) {
            existing.setEmail(dto.getEmail());
        }
        if (dto.getAddress() != null) {
            existing.setAddress(dto.getAddress());
        }
        if (dto.getVipLevel() != null) {
            existing.setVipLevel(dto.getVipLevel());
        }
        if (dto.getRemark() != null) {
            existing.setRemark(dto.getRemark());
        }

        customerMapper.updateById(existing);
        log.info("更新客户成功：id={}, name={}", id, existing.getName());
    }

    @Override
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        customerMapper.deleteById(id);
        log.info("删除客户成功：id={}, name={}", id, customer.getName());
    }

    @Override
    public void batchDeleteCustomer(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的客户");
        }
        customerMapper.deleteBatchIds(ids);
        log.info("批量删除客户成功：ids={}", ids);
    }

}