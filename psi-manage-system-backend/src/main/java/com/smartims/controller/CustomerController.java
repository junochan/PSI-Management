package com.smartims.controller;

import com.smartims.common.PageResult;
import com.smartims.common.Result;
import com.smartims.dto.CustomerDTO;
import com.smartims.dto.PageQuery;
import com.smartims.entity.Customer;
import com.smartims.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 客户管理控制器
 *
 * @author Smart IMS Team
 * @since 2026-04-12
 */
@Tag(name = "客户管理", description = "客户相关接口")
@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "查询客户列表")
    @GetMapping
    public Result<PageResult<Customer>> getCustomerList(PageQuery pageQuery) {
        PageResult<Customer> result = customerService.getCustomerList(pageQuery);
        return Result.success(result);
    }

    @Operation(summary = "查询客户详情")
    @GetMapping("/{id}")
    public Result<Customer> getCustomerById(@PathVariable Long id) {
        Customer customer = customerService.getCustomerById(id);
        return Result.success(customer);
    }

    @Operation(summary = "创建客户")
    @PostMapping
    public Result<Void> createCustomer(@Valid @RequestBody CustomerDTO dto) {
        customerService.createCustomer(dto);
        return Result.success("客户创建成功");
    }

    @Operation(summary = "更新客户")
    @PutMapping("/{id}")
    public Result<Void> updateCustomer(@PathVariable Long id, @Valid @RequestBody CustomerDTO dto) {
        customerService.updateCustomer(id, dto);
        return Result.success("客户更新成功");
    }

    @Operation(summary = "删除客户")
    @DeleteMapping("/{id}")
    public Result<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return Result.success("客户删除成功");
    }

    @Operation(summary = "批量删除客户")
    @DeleteMapping("/batch")
    public Result<Void> batchDeleteCustomer(@RequestBody List<Long> ids) {
        customerService.batchDeleteCustomer(ids);
        return Result.success("批量删除成功");
    }

}