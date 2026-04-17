package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.AftersalesDTO;
import com.smartims.dto.AftersalesHandleDTO;
import com.smartims.dto.PageQuery;
import com.smartims.entity.AftersalesOrder;
import com.smartims.entity.Customer;
import com.smartims.entity.SalesOrder;
import com.smartims.entity.SysUser;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.AftersalesOrderMapper;
import com.smartims.mapper.CustomerMapper;
import com.smartims.mapper.SalesOrderMapper;
import com.smartims.mapper.SysUserMapper;
import com.smartims.security.UserContext;
import com.smartims.service.AftersalesService;
import com.smartims.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 售后服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AftersalesServiceImpl implements AftersalesService {

    private static final DateTimeFormatter AFTERSALES_ORDER_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final int MAX_AFTERSALES_ORDER_NO_ATTEMPTS = 5;

    private final AftersalesOrderMapper aftersalesOrderMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final CustomerMapper customerMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<AftersalesOrder> getAftersalesList(PageQuery pageQuery) {
        LambdaQueryWrapper<AftersalesOrder> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AftersalesOrder::getDeleted, 0);

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(AftersalesOrder::getOrderNo, pageQuery.getKeyword())
                    .or()
                    .like(AftersalesOrder::getCustomerName, pageQuery.getKeyword())
                    .or()
                    .like(AftersalesOrder::getContent, pageQuery.getKeyword())
            );
        }

        if (StringUtils.hasText(pageQuery.getAftersalesStatus())) {
            queryWrapper.eq(AftersalesOrder::getStatus, pageQuery.getAftersalesStatus());
        }

        queryWrapper.orderByDesc(AftersalesOrder::getCreateTime);

        Page<AftersalesOrder> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<AftersalesOrder> result = aftersalesOrderMapper.selectPage(page, queryWrapper);

        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public AftersalesOrder getAftersalesById(Long id) {
        AftersalesOrder order = aftersalesOrderMapper.selectById(id);
        if (order == null || order.getDeleted() == 1) {
            throw new BusinessException("售后工单不存在");
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAftersales(AftersalesDTO dto) {
        // 验证销售订单
        SalesOrder salesOrder = salesOrderMapper.selectById(dto.getSalesOrderId());
        if (salesOrder == null || salesOrder.getDeleted() == 1) {
            throw new BusinessException("销售订单不存在");
        }

        // 验证客户
        Customer customer = customerMapper.selectById(salesOrder.getCustomerId());
        if (customer == null) {
            throw new BusinessException("客户不存在");
        }

        // 创建售后工单
        AftersalesOrder order = new AftersalesOrder();
        order.setSalesOrderId(dto.getSalesOrderId());
        order.setSalesOrderNo(salesOrder.getOrderNo());
        order.setCustomerId(salesOrder.getCustomerId());
        order.setCustomerName(customer.getName());
        order.setType(dto.getType());
        order.setContent(dto.getContent());
        order.setExpectHandle(dto.getExpectHandle());
        order.setRefundAmount(dto.getRefundAmount());
        order.setStatus("待处理"); // 待处理
        order.setRemark(dto.getRemark());

        for (int attempt = 0; attempt < MAX_AFTERSALES_ORDER_NO_ATTEMPTS; attempt++) {
            order.setOrderNo(allocateAftersalesOrderNo());
            try {
                aftersalesOrderMapper.insert(order);
                log.info("创建售后工单成功：orderNo={}", order.getOrderNo());
                return;
            } catch (DuplicateKeyException e) {
                log.warn("售后单号冲突，重新分配 attempt={} orderNo={}", attempt + 1, order.getOrderNo());
                if (attempt == MAX_AFTERSALES_ORDER_NO_ATTEMPTS - 1) {
                    throw new BusinessException("售后单号生成失败，请稍后重试");
                }
            }
        }
    }

    /**
     * 生成当日唯一售后单号，与 {@link CodeGenerator#generateAftersalesOrderNo()} 格式一致。
     */
    private String allocateAftersalesOrderNo() {
        String dateStr = LocalDate.now().format(AFTERSALES_ORDER_DATE);
        String prefix = CodeGenerator.AFTERSALES_PREFIX + dateStr;
        String maxNo = aftersalesOrderMapper.selectMaxOrderNoByDatePrefix(prefix);
        int nextSeq = 1;
        if (maxNo != null && maxNo.length() >= prefix.length() + 4) {
            try {
                String tail = maxNo.substring(maxNo.length() - 4);
                nextSeq = Integer.parseInt(tail) + 1;
            } catch (NumberFormatException ignored) {
                nextSeq = 1;
            }
        }
        if (nextSeq < 1 || nextSeq > 9999) {
            throw new BusinessException("当日售后单序号已用尽，请联系管理员");
        }
        return prefix + String.format("%04d", nextSeq);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleAftersales(Long id, AftersalesHandleDTO dto) {
        AftersalesOrder order = getAftersalesById(id);

        if (!"待处理".equals(order.getStatus())) {
            throw new BusinessException("只有待处理状态的工单可以处理");
        }

        order.setStatus("处理中"); // 处理中
        order.setHandleResult(dto.getHandleResult());
        order.setRefundAmount(dto.getRefundAmount());
        order.setHandleTime(LocalDateTime.now());
        order.setRemark(dto.getRemark());

        Long handlerId = UserContext.getCurrentUserId();
        order.setHandlerId(handlerId);
        String handlerDisplay = UserContext.getCurrentUsername();
        if (handlerId != null) {
            SysUser handler = sysUserMapper.selectById(handlerId);
            if (handler != null) {
                if (StringUtils.hasText(handler.getName())) {
                    handlerDisplay = handler.getName();
                } else if (StringUtils.hasText(handler.getUsername())) {
                    handlerDisplay = handler.getUsername();
                }
            }
        }
        order.setHandlerName(handlerDisplay);

        aftersalesOrderMapper.updateById(order);
        log.info("处理售后工单成功：id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeAftersales(Long id) {
        AftersalesOrder order = getAftersalesById(id);

        if ("待处理".equals(order.getStatus())) {
            throw new BusinessException("待处理状态的工单不能直接关闭");
        }

        order.setStatus("已完成"); // 已完成
        aftersalesOrderMapper.updateById(order);
        log.info("关闭售后工单成功：id={}", id);
    }

}