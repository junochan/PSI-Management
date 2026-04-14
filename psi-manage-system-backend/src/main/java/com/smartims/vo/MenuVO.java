package com.smartims.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点（来自 sys_permission type=1）
 */
@Data
public class MenuVO implements Serializable {

    private Long id;
    private Long parentId;
    private String name;
    private String code;
    /** 前端路由路径，如 /dashboard */
    private String path;
    private String icon;
    private Integer sort;
    private List<MenuVO> children = new ArrayList<>();
}
