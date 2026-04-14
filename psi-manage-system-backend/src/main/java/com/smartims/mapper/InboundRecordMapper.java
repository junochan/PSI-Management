package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.InboundRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 入库记录Mapper接口
 */
@Mapper
public interface InboundRecordMapper extends BaseMapper<InboundRecord> {

}