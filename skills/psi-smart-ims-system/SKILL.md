---
name: psi-smart-ims-system
description: >-
  智链进销存系统管理：通过 psims 管理用户、角色与权限、查看操作日志，以及 util-encode 生成 BCrypt；对应 HTTP /users、/roles、/logs、/util。
  当用户开户改权、审计操作记录、或初始化测试账号密码哈希时使用。
---

# 系统设置（用户 / 角色 / 日志 / 工具）

## 功能与作用

- **用户**：后台账号的增删改查；`all` 用于下拉选择等不分页场景。
- **角色与权限**：维护角色，及 **角色-权限** 绑定（权限 id 数组）。
- **操作日志**：按条件分页查询审计日志。
- **util-encode**：后端提供的 **BCrypt 明文转哈希**（仅测试/运维，返回 **纯文本** 非 JSON Result）。

对应前端：`/settings`、角色权限页。

## CLI 调用

### 用户 `psims users <子命令>`

```bash
psims users list -q "{}"
psims users all
psims users get 1
psims users create -f ./user.json
psims users update 1 -d "{\"phone\":\"...\"}"
psims users delete 1
```

### 角色 `psims roles <子命令>`

```bash
psims roles list
psims roles get 1
psims roles create -f ./role.json
psims roles update 1 -d "{...}"
psims roles delete 1
psims roles permissions 1
psims roles permissions-all
psims roles set-permissions 1 -d "[1,2,3]"
```

### 操作日志

```bash
psims logs -q "{\"current\":1,\"size\":20}"
```

### 密码哈希（非 JSON 输出）

```bash
psims util-encode -p "plain-password"
```

该命令直接打印服务端返回的 **多行文本**（明文 + BCrypt），与统一 `Result` JSON 不同。

## HTTP 对照

| 区域 | 路径 |
|------|------|
| 用户 | `/users`、`/users/all`、`/users/{id}` |
| 角色 | `/roles`、`/roles/{id}`、`/roles/{id}/permissions`、`/roles/permissions` |
| 日志 | `GET /logs` |
| 工具 | `GET /util/encode?password=`（纯文本响应） |
