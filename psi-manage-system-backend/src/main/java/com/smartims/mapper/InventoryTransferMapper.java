package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.InventoryTransfer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 调拨记录Mapper接口
 */
@Mapper
public interface InventoryTransferMapper extends BaseMapper<InventoryTransfer> {

    /**
     * 查询指定日期前缀下已存在的最大调拨单号（用于生成不重复单号）
     * 前缀形如 TR260414（TR + yyMMdd）
     */
    @Select("SELECT MAX(order_no) FROM inventory_transfer WHERE order_no LIKE CONCAT(#{datePrefix}, '%')")
    String selectMaxOrderNoByDatePrefix(@Param("datePrefix") String datePrefix);
}