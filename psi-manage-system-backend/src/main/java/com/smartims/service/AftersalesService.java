package com.smartims.service;

import com.smartims.common.PageResult;
import com.smartims.dto.AftersalesDTO;
import com.smartims.dto.AftersalesHandleDTO;
import com.smartims.dto.PageQuery;
import com.smartims.entity.AftersalesOrder;

/**
 * 售后服务接口
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
public interface AftersalesService {

    /**
     * 分页查询售后工单列表
     */
    PageResult<AftersalesOrder> getAftersalesList(PageQuery pageQuery);

    /**
     * 根据ID查询售后工单详情
     */
    AftersalesOrder getAftersalesById(Long id);

    /**
     * 创建售后工单
     */
    void createAftersales(AftersalesDTO dto);

    /**
     * 处理售后工单
     */
    void handleAftersales(Long id, AftersalesHandleDTO dto);

    /**
     * 关闭售后工单
     */
    void closeAftersales(Long id);

}