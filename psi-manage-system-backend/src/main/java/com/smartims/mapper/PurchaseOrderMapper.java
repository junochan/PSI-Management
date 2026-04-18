package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.PurchaseOrder;
import com.smartims.vo.SupplierPurchaseStatsRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 采购订单Mapper接口
 */
@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    /**
     * 查询指定日期前缀下已存在的最大采购单号（用于生成不重复单号）
     * 前缀形如 PO260414（PO + yyMMdd）
     */
    @Select("SELECT MAX(order_no) FROM purchase_order WHERE order_no LIKE CONCAT(#{datePrefix}, '%')")
    String selectMaxOrderNoByDatePrefix(@Param("datePrefix") String datePrefix);

    /**
     * 按供应商 ID 批量统计未删除采购单的条数与金额合计（用于供应商分页/详情）
     */
    @Select("<script>"
            + "SELECT supplier_id AS supplierId, COUNT(*) AS purchaseOrderCount, COALESCE(SUM(amount), 0) AS totalPurchaseAmount "
            + "FROM purchase_order WHERE deleted = 0 AND supplier_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY supplier_id"
            + "</script>")
    List<SupplierPurchaseStatsRow> selectPurchaseStatsBySupplierIds(@Param("ids") List<Long> ids);
}