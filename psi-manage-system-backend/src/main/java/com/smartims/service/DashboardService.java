package com.smartims.service;

import com.smartims.vo.DashboardOverviewVO;

/**
 * 仪表盘
 *
 * @author Smart IMS Team
 * @since 2026-04-14
 */
public interface DashboardService {

    /**
     * 仪表盘聚合数据
     *
     * @param days 销售趋势/分类/排名的统计区间天数：7、30、90
     */
    DashboardOverviewVO getOverview(int days);
}
