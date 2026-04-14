package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.SupplierDTO;
import com.smartims.entity.Supplier;

import java.util.List;

/**
 * 供应商服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface SupplierService {

    /**
     * 分页查询供应商列表
     */
    PageResult<Supplier> getSupplierList(PageQuery pageQuery);

    /**
     * 根据ID查询供应商详情
     */
    Supplier getSupplierById(Long id);

    /**
     * 创建供应商
     */
    void createSupplier(SupplierDTO dto);

    /**
     * 更新供应商
     */
    void updateSupplier(Long id, SupplierDTO dto);

    /**
     * 删除供应商
     */
    void deleteSupplier(Long id);

    /**
     * 批量删除供应商
     */
    void batchDeleteSupplier(List<Long> ids);

}