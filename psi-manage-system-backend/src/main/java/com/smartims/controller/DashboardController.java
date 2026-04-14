package com.smartims.controller;

import com.smartims.common.Result;
import com.smartims.service.DashboardService;
import com.smartims.vo.DashboardOverviewVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘
 *
 * @author Smart IMS Team
 * @since 2026-04-14
 */
@Tag(name = "仪表盘", description = "仪表盘聚合数据")
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "仪表盘聚合数据（销售趋势/分类/排名/库存等）")
    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview(@RequestParam(defaultValue = "7") int days) {
        if (days != 7 && days != 30 && days != 90) {
            days = 7;
        }
        return Result.success(dashboardService.getOverview(days));
    }
}
