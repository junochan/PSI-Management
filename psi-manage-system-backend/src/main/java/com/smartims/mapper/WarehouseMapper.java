package com.smartims.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smartims.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Delete;

/**
 * 仓库Mapper接口
 */
@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {

    /**
     * 物理删除（绕过逻辑删除）.
     */
    @Delete("DELETE FROM warehouse WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);

    /**
     * 批量物理删除（绕过逻辑删除）.
     */
    @Delete({
            "<script>",
            "DELETE FROM warehouse WHERE id IN ",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "#{id}",
            "</foreach>",
            "</script>"
    })
    int hardDeleteBatchByIds(@Param("ids") java.util.List<Long> ids);

    /**
     * 清理同名的逻辑删除仓库，释放可能的唯一键占用.
     */
    @Delete("DELETE FROM warehouse WHERE name = #{name} AND deleted = 1")
    int hardDeleteDeletedByName(@Param("name") String name);

    /**
     * 清理同编码的逻辑删除仓库，释放可能的唯一键占用.
     */
    @Delete("DELETE FROM warehouse WHERE code = #{code} AND deleted = 1")
    int hardDeleteDeletedByCode(@Param("code") String code);
}