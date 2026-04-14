package com.smartims.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录后导航：菜单树 + 权限码 + 前端路由表 + 默认首页路径
 */
@Data
public class NavigationVO implements Serializable {

    private List<MenuVO> menus;
    private List<String> permissions;
    private List<UiRouteVO> routes;
    /** 首个可访问菜单 path，如 /dashboard */
    private String homePath;
}
