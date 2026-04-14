package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.CustomerDTO;
import com.smartims.dto.PageQuery;
import com.smartims.entity.Customer;

import java.util.List;

/**
 * 客户服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface CustomerService {

    /**
     * 分页查询客户列表
     */
    PageResult<Customer> getCustomerList(PageQuery pageQuery);

    /**
     * 根据ID查询客户详情
     */
    Customer getCustomerById(Long id);

    /**
     * 创建客户
     */
    void createCustomer(CustomerDTO dto);

    /**
     * 更新客户
     */
    void updateCustomer(Long id, CustomerDTO dto);

    /**
     * 删除客户
     */
    void deleteCustomer(Long id);

    /**
     * 批量删除客户
     */
    void batchDeleteCustomer(List<Long> ids);

}