<div align="center">

# Velocity_Plugin

**Velocity 代理端插件源代码集 · Velocity Proxy Plugins**

[![License: GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-blue.svg)](LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)]()
[![Velocity](https://img.shields.io/badge/Velocity-3.x-blueviolet.svg)]()

</div>

本仓库收录基于 [Velocity](https://papermc.io/software/velocity) 代理端开发的 Minecraft 插件**源代码**，目前包含 **跨服举报插件 VelocityReport** 与 **跨服聊天插件 VelocityChat**。所有插件均只需安装在 Velocity 代理端即可运行，子服务端零侵入、无需安装任何额外插件。

## 📦 插件一览

| 插件 | 最新版本 | 简介 | 源码目录 |
|---|---|---|---|
| [VelocityReport](Velocity-Report/) | 2.2.0 | 跨服举报 —— 全服集中存储举报，管理端任意子服可见 | `Velocity-Report/` |
| [VelocityChat](VelocityChat/) | 1.9.1 | 跨服聊天 —— 全服广播、进服/切换/离开提示、群组头衔 | `VelocityChat/` |

> 每个插件目录内均含有独立的中英双语 README 与更新日志，点击上方插件名可跳转。

---

## ✨ VelocityReport — 跨服举报插件

[![English](https://img.shields.io/badge/English-Read_in_English-blue?style=flat-square&logo=github)](Velocity-Report/README.md)
[![简体中文](https://img.shields.io/badge/简体中文-中文文档-blue?style=flat-square&logo=github)](Velocity-Report/README_zh_CN.md)

玩家可以在**任意子服**举报整个代理网络中的任意玩家，举报信息会被集中存储，且所有子服的管理员都能看到通知——无论被举报的玩家在哪台服务器上。

### 核心特性

- **跨服举报** — 举报全代理网络内任意玩家，管理端任意服务器可收到通知
- **点击选择原因菜单** — 不带原因执行 `/report <玩家>` 弹出可点击的预设原因菜单
- **两种存储后端** — SQLite（默认，零配置）或 MySQL（HikariCP 连接池）
- **完整管理流程** — 查看列表与详情、标记已解决、关闭、重新开启、填写处理备注
- **举报历史** — 按玩家分页浏览所有历史举报记录
- **中英双语** + MiniMessage 消息格式 + 可配置防刷屏冷却

### 主要命令

| 命令 | 说明 |
|---|---|
| `/report <玩家> [原因]`（别名 `/rep`） | 举报玩家（玩家命令） |
| `/reports` | 分页列出所有待处理举报（管理） |
| `/reportview <编号>` | 查看单条举报详情（管理） |
| `/reportclose <编号> [备注]` | 关闭/解决举报（管理） |
| `/reportreload` | 热重载配置（管理） |

[查看完整文档 →](Velocity-Report/README_zh_CN.md)

---

## ✨ VelocityChat — 跨服聊天插件

[![English](https://img.shields.io/badge/English-Read_in_English-blue?style=flat-square&logo=github)](VelocityChat/README.md)
[![简体中文](https://img.shields.io/badge/简体中文-中文文档-blue?style=flat-square&logo=github)](VelocityChat/README_zh_CN.md)

在代理网络内实现跨服聊天，**仅安装在 Velocity 代理端**，无需任何子服务端插件。

### 核心特性

- **跨服广播** — `/br <消息>` 向所有子服的全部玩家发送聊天消息
- **服务器邀请** — `/yq [消息]` 广播邀请，其他玩家点击即可加入邀请者所在子服
- **进服 / 切换 / 离开提示** — 自动广播玩家上下线、切换服务器消息，可限制为仅管理员可见
- **服务器别名** — 显示 "登录服" 等友好名称，而非原始服务器 ID
- **群组头衔** — 创建群组并为成员设置聊天名字前缀
- **完整 `&` 颜色代码** + 广播/邀请冷却 + 精细权限（LuckPerms 可选）

### 主要命令

| 命令 | 说明 |
|---|---|
| `/br <消息>`（别名 `/broadcast`） | 跨服广播（玩家命令） |
| `/yq [消息]` | 广播服务器邀请（玩家命令） |
| `/vchat create group <名称> [头衔]` | 创建群组（管理） |
| `/vchat group <名称> join/remove/settitle/delete/list <参数>` | 群组管理（管理） |
| `/vchat reload` | 热重载配置与语言文件（管理） |

[查看完整文档 →](VelocityChat/README_zh_CN.md)

---

## ✅ 环境要求

| 要求 | 版本 |
|---|---|
| Java | 17+ |
| 代理端 | Velocity 3.x（所有现代版本） |
| 构建工具 | Maven 3.8+（仅编译源码时需要） |
| 可选 | 权限插件（如 [LuckPerms](https://luckperms.net)）用于细分管理权限 |

## 🔨 从源码构建

```bash
git clone https://github.com/LiquidTeamYHC/Velocity_Plugin.git
cd Velocity_Plugin

# 构建 VelocityReport（产物: Velocity-Report/target/Velocity-Report-2.2.0.jar）
cd Velocity-Report
mvn package

# 构建 VelocityChat（产物: VelocityChat/target/VelocityChat-1.9.1.jar）
cd ../VelocityChat
mvn package
```

## 📥 安装

1. 将构建出的 JAR 放入 Velocity 代理端的 `plugins/` 文件夹
2. 重启代理端
3. 首次启动自动生成 `config.yml`，按需修改后再次重启生效

## 📂 目录结构

```
Velocity_Plugin/
├── LICENSE                  # 开源协议（GPL-3.0）
├── README.md                # 本说明文件
├── Velocity-Report/         # 跨服举报插件源码
│   ├── src/main/java/       # Java 源码
│   ├── src/main/resources/  # 配置与语言文件
│   ├── README.md            # 英文文档
│   └── README_zh_CN.md      # 中文文档
└── VelocityChat/            # 跨服聊天插件源码
    ├── src/main/java/       # Java 源码
    ├── src/main/resources/  # 配置与语言文件
    ├── README.md            # 英文文档
    └── README_zh_CN.md      # 中文文档
```

## 👤 作者

- **YuHongChen（LiquidTeam）** — 主要开发者
- QQ：`1464670605`

## 📄 开源协议

本项目基于 [GPL-3.0 License](LICENSE) 开源。
