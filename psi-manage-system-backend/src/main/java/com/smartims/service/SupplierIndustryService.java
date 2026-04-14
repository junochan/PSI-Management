package com.smartims.service;

import com.smartims.entity.SupplierIndustry;

import java.util.List;

public interface SupplierIndustryService {

    /**
     * 下拉用：全部启用行业，按 sort、id 排序
     */
    List<SupplierIndustry> listEnabled();
}
