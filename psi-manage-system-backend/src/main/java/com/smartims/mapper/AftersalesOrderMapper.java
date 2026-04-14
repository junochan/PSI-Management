package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.AftersalesOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 售后工单Mapper接口
 */
@Mapper
public interface AftersalesOrderMapper extends BaseMapper<AftersalesOrder> {

    /**
     * 指定日期前缀下最大售后单号（前缀形如 AS260414）
     */
    @Select("SELECT MAX(order_no) FROM aftersales_order WHERE order_no LIKE CONCAT(#{datePrefix}, '%')")
    String selectMaxOrderNoByDatePrefix(@Param("datePrefix") String datePrefix);
}