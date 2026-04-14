package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 商品Mapper接口
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 查询指定日期前缀下已存在的最大商品编码（用于生成不重复编码）
     * 前缀形如 PROD260414（PROD + yyMMdd）
     */
    @Select("SELECT MAX(code) FROM product WHERE code LIKE CONCAT(#{datePrefix}, '%')")
    String selectMaxCodeByDatePrefix(@Param("datePrefix") String datePrefix);
}