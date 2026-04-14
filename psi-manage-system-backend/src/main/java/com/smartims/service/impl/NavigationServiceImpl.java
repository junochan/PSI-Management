package com.smartims.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartims.config.registry.ApplicationPermissionRegistry;
import com.smartims.config.registry.ApplicationPermissionRegistry.UiRouteDefinition;
import com.smartims.entity.SysPermission;
import com.smartims.entity.SysUser;
import com.smartims.mapper.SysPermissionMapper;
import com.smartims.mapper.SysUserMapper;
import com.smartims.service.NavigationService;
import com.smartims.service.PermissionService;
import com.smartims.vo.MenuVO;
import com.smartims.vo.NavigationVO;
import com.smartims.vo.UiRouteVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NavigationServiceImpl implements NavigationService {

    /** 库中有菜单但暂无前端页面的权限编码（避免点进 404） */
    private static final Set<String> MENU_CODES_WITHOUT_PAGE = Set.of("reports");

    private final SysUserMapper sysUserMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final PermissionService permissionService;
    private final ApplicationPermissionRegistry permissionRegistry;

    @Override
    public NavigationVO buildNavigation(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            NavigationVO empty = new NavigationVO();
            empty.setMenus(List.of());
            empty.setPermissions(List.of());
            empty.setRoutes(List.of());
            empty.setHomePath("/dashboard");
            return empty;
        }

        boolean superAdmin = user.getRoleId() != null && user.getRoleId() == 1L;
        List<String> codes = permissionService.getPermissionCodesByUserId(userId);
        Set<String> codeSet = new HashSet<>(codes);

        List<MenuVO> menus = buildMenuTree(superAdmin, codeSet);
        List<UiRouteVO> routes = filterUiRoutes(superAdmin, codeSet);
        String homePath = resolveHomePath(menus);

        NavigationVO vo = new NavigationVO();
        vo.setMenus(menus);
        vo.setPermissions(codes);
        vo.setRoutes(routes);
        vo.setHomePath(homePath);
        return vo;
    }

    private List<MenuVO> buildMenuTree(boolean superAdmin, Set<String> codeSet) {
        LambdaQueryWrapper<SysPermission> q = new LambdaQueryWrapper<>();
        q.eq(SysPermission::getType, 1);
        q.eq(SysPermission::getStatus, 1);
        q.eq(SysPermission::getDeleted, 0);
        q.orderByAsc(SysPermission::getSort);
        List<SysPermission> all = sysPermissionMapper.selectList(q);

        List<SysPermission> visible = all.stream()
                .filter(m -> !MENU_CODES_WITHOUT_PAGE.contains(m.getCode()))
                .filter(m -> menuVisible(m, superAdmin, codeSet))
                .collect(Collectors.toList());

        Map<Long, MenuVO> idToVo = new HashMap<>();
        for (SysPermission p : visible) {
            MenuVO mv = new MenuVO();
            mv.setId(p.getId());
            mv.setParentId(p.getParentId() == null ? 0L : p.getParentId());
            mv.setName(p.getName());
            mv.setCode(p.getCode());
            mv.setPath(normalizeMenuPath(p.getPath()));
            mv.setIcon(p.getIcon());
            mv.setSort(p.getSort());
            idToVo.put(p.getId(), mv);
        }

        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO mv : idToVo.values()) {
            Long pid = mv.getParentId();
            if (pid == null || pid == 0L || !idToVo.containsKey(pid)) {
                roots.add(mv);
            } else {
                idToVo.get(pid).getChildren().add(mv);
            }
        }
        sortMenuTree(roots);
        return roots;
    }

    private void sortMenuTree(List<MenuVO> nodes) {
        nodes.sort(Comparator.comparing(MenuVO::getSort, Comparator.nullsLast(Integer::compareTo)));
        for (MenuVO n : nodes) {
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                sortMenuTree(n.getChildren());
            }
        }
    }

    private String normalizeMenuPath(String path) {
        if (path == null || path.isEmpty()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private boolean menuVisible(SysPermission m, boolean superAdmin, Set<String> codeSet) {
        if (superAdmin) {
            return true;
        }
        String c = m.getCode();
        if ("settings".equals(c)) {
            return codeSet.contains("settings")
                    || codeSet.stream().anyMatch(x -> x != null && x.startsWith("settings:"));
        }
        return codeSet.contains(c);
    }

    private List<UiRouteVO> filterUiRoutes(boolean superAdmin, Set<String> codeSet) {
        List<UiRouteVO> list = new ArrayList<>();
        for (UiRouteDefinition def : permissionRegistry.getUiRoutes()) {
            if (superAdmin || routeAllowed(def, codeSet)) {
                list.add(new UiRouteVO(
                        def.getPath(),
                        def.getRouteName(),
                        def.getTitle(),
                        def.getViewKey(),
                        def.getPermissionCode(),
                        def.isHidden(),
                        def.getPermissionMode()
                ));
            }
        }
        return list;
    }

    private boolean routeAllowed(UiRouteDefinition def, Set<String> codeSet) {
        if ("SETTINGS_ANY".equals(def.getPermissionMode())) {
            return codeSet.contains("settings")
                    || codeSet.stream().anyMatch(x -> x != null && x.startsWith("settings:"));
        }
        return codeSet.contains(def.getPermissionCode());
    }

    private String resolveHomePath(List<MenuVO> roots) {
        MenuVO first = findFirstMenuDfs(roots);
        if (first != null && first.getPath() != null && !first.getPath().isEmpty()) {
            return first.getPath();
        }
        return "/dashboard";
    }

    private MenuVO findFirstMenuDfs(List<MenuVO> nodes) {
        if (nodes == null) {
            return null;
        }
        List<MenuVO> sorted = new ArrayList<>(nodes);
        sorted.sort(Comparator.comparing(MenuVO::getSort, Comparator.nullsLast(Integer::compareTo)));
        for (MenuVO n : sorted) {
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                MenuVO sub = findFirstMenuDfs(n.getChildren());
                if (sub != null) {
                    return sub;
                }
            }
            if (n.getPath() != null && !n.getPath().isEmpty() && !"/".equals(n.getPath())) {
                return n;
            }
        }
        return null;
    }
}
