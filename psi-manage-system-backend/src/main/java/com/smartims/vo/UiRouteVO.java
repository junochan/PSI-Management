package com.smartims.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 前端动态路由元数据（与 ApplicationPermissionRegistry.uiRoutes 一致）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UiRouteVO implements Serializable {

    /** 主布局下相对 path，如 products/view/:id */
    private String path;
    /** Vue Router name，全局唯一 */
    private String routeName;
    private String title;
    /** 与前端 viewLoaders 注册键一致 */
    private String viewKey;
    private String permissionCode;
    private boolean hidden;
    /** NORMAL 或 SETTINGS_ANY */
    private String permissionMode;
}
