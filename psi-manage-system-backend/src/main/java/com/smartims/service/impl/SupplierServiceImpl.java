package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartims.common.PageResult;
import com.smartims.dto.PageQuery;
import com.smartims.dto.SupplierDTO;
import com.smartims.entity.InboundRecord;
import com.smartims.entity.PurchaseOrder;
import com.smartims.entity.Supplier;
import com.smartims.entity.SupplierIndustry;
import com.smartims.entity.SupplierIndustryLink;
import com.smartims.exception.BusinessException;
import com.smartims.mapper.InboundRecordMapper;
import com.smartims.mapper.PurchaseOrderMapper;
import com.smartims.mapper.SupplierIndustryLinkMapper;
import com.smartims.mapper.SupplierIndustryMapper;
import com.smartims.mapper.SupplierMapper;
import com.smartims.service.SupplierService;
import com.smartims.vo.SupplierPurchaseStatsRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 供应商服务实现类
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierMapper supplierMapper;
    private final SupplierIndustryMapper supplierIndustryMapper;
    private final SupplierIndustryLinkMapper supplierIndustryLinkMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final InboundRecordMapper inboundRecordMapper;

    @Override
    public PageResult<Supplier> getSupplierList(PageQuery pageQuery) {
        LambdaQueryWrapper<Supplier> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Supplier::getDeleted, 0);

        if (StringUtils.hasText(pageQuery.getKeyword())) {
            queryWrapper.and(wrapper -> wrapper
                    .like(Supplier::getName, pageQuery.getKeyword())
                    .or()
                    .like(Supplier::getCode, pageQuery.getKeyword())
                    .or()
                    .like(Supplier::getContact, pageQuery.getKeyword())
            );
        }

        if (StringUtils.hasText(pageQuery.getSort())) {
            if ("asc".equalsIgnoreCase(pageQuery.getOrder())) {
                queryWrapper.orderByAsc(Supplier::getCreateTime);
            } else {
                queryWrapper.orderByDesc(Supplier::getCreateTime);
            }
        } else {
            queryWrapper.orderByDesc(Supplier::getCreateTime);
        }

        Page<Supplier> page = new Page<>(pageQuery.getPage(), pageQuery.getSize());
        Page<Supplier> result = supplierMapper.selectPage(page, queryWrapper);

        fillIndustries(result.getRecords());
        fillPurchaseStats(result.getRecords());
        return PageResult.build(result.getTotal(), result.getCurrent(), result.getSize(), result.getRecords());
    }

    @Override
    public Supplier getSupplierById(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null || supplier.getDeleted() == 1) {
            throw new BusinessException("供应商不存在");
        }
        fillIndustries(List.of(supplier));
        fillPurchaseStats(List.of(supplier));
        return supplier;
    }

    @Override
    public void createSupplier(SupplierDTO dto) {
        if (StringUtils.hasText(dto.getCode())) {
            LambdaQueryWrapper<Supplier> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Supplier::getCode, dto.getCode());
            queryWrapper.eq(Supplier::getDeleted, 0);
            if (supplierMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("供应商编码已存在");
            }
        }

        Supplier supplier = new Supplier();
        supplier.setName(dto.getName());
        supplier.setCode(dto.getCode());
        supplier.setAddress(dto.getAddress());
        supplier.setPhone(dto.getContactPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setContact(dto.getContactPerson());
        supplier.setBankName(dto.getBankName());
        supplier.setBankAccount(dto.getBankAccount());
        supplier.setRemark(dto.getRemark());
        supplier.setStatus(1);

        supplierMapper.insert(supplier);
        replaceSupplierIndustries(supplier.getId(), dto.getIndustryIds());
        log.info("创建供应商成功：id={}, name={}", supplier.getId(), supplier.getName());
    }

    @Override
    public void updateSupplier(Long id, SupplierDTO dto) {
        Supplier existing = supplierMapper.selectById(id);
        if (existing == null || existing.getDeleted() == 1) {
            throw new BusinessException("供应商不存在");
        }

        if (StringUtils.hasText(dto.getCode()) && !dto.getCode().equals(existing.getCode())) {
            LambdaQueryWrapper<Supplier> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Supplier::getCode, dto.getCode());
            queryWrapper.eq(Supplier::getDeleted, 0);
            queryWrapper.ne(Supplier::getId, id);
            if (supplierMapper.selectCount(queryWrapper) > 0) {
                throw new BusinessException("供应商编码已存在");
            }
        }

        existing.setName(dto.getName());
        existing.setCode(dto.getCode());
        existing.setAddress(dto.getAddress());
        existing.setPhone(dto.getContactPhone());
        existing.setEmail(dto.getEmail());
        existing.setContact(dto.getContactPerson());
        existing.setBankName(dto.getBankName());
        existing.setBankAccount(dto.getBankAccount());
        existing.setRemark(dto.getRemark());

        supplierMapper.updateById(existing);
        replaceSupplierIndustries(id, dto.getIndustryIds());
        log.info("更新供应商成功：id={}, name={}", id, existing.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSupplier(Long id) {
        Supplier supplier = supplierMapper.selectById(id);
        if (supplier == null || supplier.getDeleted() == 1) {
            throw new BusinessException("供应商不存在");
        }
        assertNoRelatedPurchaseOrInbound(id);
        LambdaQueryWrapper<SupplierIndustryLink> lq = new LambdaQueryWrapper<>();
        lq.eq(SupplierIndustryLink::getSupplierId, id);
        supplierIndustryLinkMapper.delete(lq);
        supplierMapper.deleteById(id);
        log.info("删除供应商成功：id={}, name={}", id, supplier.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteSupplier(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException("请选择要删除的供应商");
        }
        for (Long id : ids) {
            Supplier supplier = supplierMapper.selectById(id);
            if (supplier == null || supplier.getDeleted() == 1) {
                continue;
            }
            assertNoRelatedPurchaseOrInbound(id);
        }
        LambdaQueryWrapper<SupplierIndustryLink> lq = new LambdaQueryWrapper<>();
        lq.in(SupplierIndustryLink::getSupplierId, ids);
        supplierIndustryLinkMapper.delete(lq);
        supplierMapper.deleteBatchIds(ids);
        log.info("批量删除供应商成功：ids={}", ids);
    }

    /**
     * 存在未清理的采购订单或入库记录时不可删除供应商（含数据库外键约束场景下的前置校验）。
     */
    private void assertNoRelatedPurchaseOrInbound(Long supplierId) {
        LambdaQueryWrapper<PurchaseOrder> poQ = new LambdaQueryWrapper<>();
        poQ.eq(PurchaseOrder::getSupplierId, supplierId);
        long purchaseCount = purchaseOrderMapper.selectCount(poQ);

        LambdaQueryWrapper<InboundRecord> irQ = new LambdaQueryWrapper<>();
        irQ.eq(InboundRecord::getSupplierId, supplierId);
        long inboundCount = inboundRecordMapper.selectCount(irQ);

        if (purchaseCount == 0 && inboundCount == 0) {
            return;
        }
        StringBuilder msg = new StringBuilder("无法删除该供应商：仍存在关联数据。");
        msg.append("请先删除关联的");
        if (purchaseCount > 0) {
            msg.append("采购订单（共 ").append(purchaseCount).append(" 条）");
        }
        if (purchaseCount > 0 && inboundCount > 0) {
            msg.append("、");
        }
        if (inboundCount > 0) {
            msg.append("入库记录（共 ").append(inboundCount).append(" 条）");
        }
        msg.append("后再试。");
        throw new BusinessException(msg.toString());
    }

    private void replaceSupplierIndustries(Long supplierId, List<Long> industryIds) {
        LambdaQueryWrapper<SupplierIndustryLink> q = new LambdaQueryWrapper<>();
        q.eq(SupplierIndustryLink::getSupplierId, supplierId);
        supplierIndustryLinkMapper.delete(q);
        if (industryIds == null || industryIds.isEmpty()) {
            return;
        }
        LinkedHashSet<Long> ordered = new LinkedHashSet<>();
        for (Long iid : industryIds) {
            if (iid != null) {
                ordered.add(iid);
            }
        }
        for (Long iid : ordered) {
            SupplierIndustryLink link = new SupplierIndustryLink();
            link.setSupplierId(supplierId);
            link.setIndustryId(iid);
            supplierIndustryLinkMapper.insert(link);
        }
    }

    private void fillIndustries(List<Supplier> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return;
        }
        List<Long> supplierIds = suppliers.stream().map(Supplier::getId).filter(Objects::nonNull).toList();
        if (supplierIds.isEmpty()) {
            return;
        }
        LambdaQueryWrapper<SupplierIndustryLink> lq = new LambdaQueryWrapper<>();
        lq.in(SupplierIndustryLink::getSupplierId, supplierIds);
        lq.orderByAsc(SupplierIndustryLink::getId);
        List<SupplierIndustryLink> links = supplierIndustryLinkMapper.selectList(lq);

        Map<Long, List<Long>> sidToIids = new HashMap<>();
        for (SupplierIndustryLink link : links) {
            sidToIids.computeIfAbsent(link.getSupplierId(), k -> new ArrayList<>()).add(link.getIndustryId());
        }

        if (links.isEmpty()) {
            for (Supplier s : suppliers) {
                s.setIndustryIds(Collections.emptyList());
                s.setIndustryNames("");
            }
            return;
        }

        Set<Long> allIndustryIds = links.stream().map(SupplierIndustryLink::getIndustryId).collect(Collectors.toSet());
        LambdaQueryWrapper<SupplierIndustry> iq = new LambdaQueryWrapper<>();
        iq.in(SupplierIndustry::getId, allIndustryIds);
        List<SupplierIndustry> industries = supplierIndustryMapper.selectList(iq);
        Map<Long, String> idToName = industries.stream()
                .collect(Collectors.toMap(SupplierIndustry::getId, SupplierIndustry::getName, (a, b) -> a));

        for (Supplier s : suppliers) {
            List<Long> iids = sidToIids.getOrDefault(s.getId(), Collections.emptyList());
            s.setIndustryIds(new ArrayList<>(iids));
            String names = iids.stream().map(idToName::get).filter(Objects::nonNull).collect(Collectors.joining("、"));
            s.setIndustryNames(names);
        }
    }

    /**
     * 填充当前页供应商在采购订单中的条数与金额合计（单次 IN + GROUP BY，避免 N+1）。
     */
    private void fillPurchaseStats(List<Supplier> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            return;
        }
        List<Long> ids = suppliers.stream()
                .map(Supplier::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return;
        }
        List<SupplierPurchaseStatsRow> rows = purchaseOrderMapper.selectPurchaseStatsBySupplierIds(ids);
        Map<Long, SupplierPurchaseStatsRow> byId = (rows == null || rows.isEmpty())
                ? Collections.emptyMap()
                : rows.stream().collect(Collectors.toMap(SupplierPurchaseStatsRow::getSupplierId, r -> r, (a, b) -> a));
        for (Supplier s : suppliers) {
            SupplierPurchaseStatsRow row = byId.get(s.getId());
            if (row == null) {
                s.setPurchaseOrderCount(0L);
                s.setTotalPurchaseAmount(BigDecimal.ZERO);
                continue;
            }
            s.setPurchaseOrderCount(row.getPurchaseOrderCount() != null ? row.getPurchaseOrderCount() : 0L);
            s.setTotalPurchaseAmount(row.getTotalPurchaseAmount() != null ? row.getTotalPurchaseAmount() : BigDecimal.ZERO);
        }
    }

}
