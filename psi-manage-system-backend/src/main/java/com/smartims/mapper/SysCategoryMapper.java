package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.SysCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商品分类Mapper
 *
 * @author Smart IMS Team
 * @since 2026-04-13
 */
@Mapper
public interface SysCategoryMapper extends BaseMapper<SysCategory> {

    /**
     * 指定日期前缀下最大分类编码（前缀形如 CAT260414）
     */
    @Select("SELECT MAX(code) FROM sys_category WHERE code LIKE CONCAT(#{datePrefix}, '%')")
    String selectMaxCodeByDatePrefix(@Param("datePrefix") String datePrefix);
}