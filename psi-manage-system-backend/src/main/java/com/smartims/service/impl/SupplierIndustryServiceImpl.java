package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartims.entity.SupplierIndustry;
import com.smartims.mapper.SupplierIndustryMapper;
import com.smartims.service.SupplierIndustryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierIndustryServiceImpl implements SupplierIndustryService {

    private final SupplierIndustryMapper supplierIndustryMapper;

    @Override
    public List<SupplierIndustry> listEnabled() {
        LambdaQueryWrapper<SupplierIndustry> q = new LambdaQueryWrapper<>();
        q.eq(SupplierIndustry::getStatus, 1);
        q.orderByAsc(SupplierIndustry::getSort).orderByAsc(SupplierIndustry::getId);
        return supplierIndustryMapper.selectList(q);
    }
}
