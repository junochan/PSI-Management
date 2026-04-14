package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 销售订单Mapper接口
 */
@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {

    /**
     * 指定日期前缀下最大销售单号（前缀形如 SO260414）
     */
    @Select("SELECT MAX(order_no) FROM sales_order WHERE order_no LIKE CONCAT(#{datePrefix}, '%')")
    String selectMaxOrderNoByDatePrefix(@Param("datePrefix") String datePrefix);
}