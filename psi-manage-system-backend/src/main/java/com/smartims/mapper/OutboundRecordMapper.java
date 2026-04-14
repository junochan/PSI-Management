package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.OutboundRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 出库记录Mapper接口
 */
@Mapper
public interface OutboundRecordMapper extends BaseMapper<OutboundRecord> {

}