package com.smartims.controller;

import com.smartims.common.Result;
import com.smartims.entity.SupplierIndustry;
import com.smartims.service.SupplierIndustryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "供应商行业", description = "供应商所属行业字典")
@RestController
@RequestMapping("/v1/supplier-industries")
@RequiredArgsConstructor
public class SupplierIndustryController {

    private final SupplierIndustryService supplierIndustryService;

    @Operation(summary = "行业列表（启用，用于下拉）")
    @GetMapping
    public Result<List<SupplierIndustry>> list() {
        return Result.success(supplierIndustryService.listEnabled());
    }
}
