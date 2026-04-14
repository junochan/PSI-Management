package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.InboundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 入库记录Mapper接口
 */
@Mapper
public interface InboundRecordMapper extends BaseMapper<InboundRecord> {

    /**
     * 指定日期前缀下最大入库单号（前缀形如 IN260414）
     */
    @Select("SELECT MAX(order_no) FROM inbound_record WHERE order_no LIKE CONCAT(#{datePrefix}, '%')")
    String selectMaxOrderNoByDatePrefix(@Param("datePrefix") String datePrefix);
}