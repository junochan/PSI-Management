package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.OutboundRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 出库记录Mapper接口
 */
@Mapper
public interface OutboundRecordMapper extends BaseMapper<OutboundRecord> {

    /**
     * 指定日期前缀下最大出库单号（前缀形如 OUT260414）
     */
    @Select("SELECT MAX(order_no) FROM outbound_record WHERE order_no LIKE CONCAT(#{datePrefix}, '%')")
    String selectMaxOrderNoByDatePrefix(@Param("datePrefix") String datePrefix);
}