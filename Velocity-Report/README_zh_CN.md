<div align="center">

# VelocityReport

**一款 Velocity 代理端跨服举报插件**

[![English](https://img.shields.io/badge/English-Read_in_English-blue?style=for-the-badge&logo=github)](README.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](../../LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)]()
[![Velocity](https://img.shields.io/badge/Velocity-3.x-blueviolet.svg)]()

</div>

玩家可以在你服务器的**任意子服**举报其他玩家，举报信息会被集中存储，并且所有子服的管理员都能看到通知——无论被举报的玩家在哪台服务器上。

## ✨ 功能特性

- **跨服举报** — 玩家可以举报整个代理网络里的任意玩家，管理端在任何服务器上都能收到通知
- **点击选择原因菜单** — 不带原因执行 `/report <玩家>` 会弹出一个可点击的预设原因菜单
- **自定义原因** — `/report <玩家> <原因>` 支持自由填写原因，最长 256 字
- **两种存储后端** — SQLite（默认，零配置）或 MySQL（基于 HikariCP 连接池）
- **双语言** — 内置简体中文 `zh_CN` 和英文 `en_US` 语言文件，运行时可切换
- **完整的管理流程** — 查看举报列表与详情、标记已解决、关闭、重新开启、填写处理备注
- **举报历史** — 按玩家分页浏览所有历史举报记录
- **冷却时间** — 可配置的防刷屏冷却，支持 `velocityreport.bypasscooldown` 权限绕过
- **MiniMessage 格式** — 所有消息均可使用 [Adventure MiniMessage](https://docs.advntr.dev/minimessage/format.html) 自定义
- **管理端通知** — 收到举报时、管理员上线时提示有待处理举报

## ✅ 环境要求

| 要求 | 版本 |
|---|---|
| Java | 17+ |
| 代理端 | Velocity 3.x（所有现代版本） |
| 可选 | 权限插件（如 [LuckPerms](https://luckperms.net)）用于细分管理权限 |

## 📦 安装方法

1. 下载插件 JAR（自行编译或下载发行版）
2. 放入 Velocity 代理端的 `plugins/` 文件夹
3. 重启代理（或使用插件热重载工具执行 `/velocity reload`）
4. 首次运行会自动生成 `config.yml`——请检查配置，修改后重启生效

## 🎮 命令

### 玩家命令

| 命令 | 说明 | 权限 |
|---|---|---|
| `/report <玩家> [原因]`（别名 `/rep`） | 举报玩家。不带原因时弹出可点击的原因菜单 | `velocityreport.report`（默认所有玩家） |

### 管理命令

| 命令 | 说明 | 权限 |
|---|---|---|
| `/reports`（别名 `/reportslist`、`/reportlist`） | 分页列出所有待处理的举报 | `velocityreport.staff` |
| `/reportview <编号>` | 查看单条举报的完整详情 | `velocityreport.staff` |
| `/reportclose <编号> [处理备注]`（别名 `/closereport`、`/resolvereport`） | 关闭/解决举报，可附处理备注 | `velocityreport.staff` |
| `/reportcd view <编号>` | 打开交互式举报详情菜单 | `velocityreport.staff` |
| `/reportcd resolve <编号>` | 标记举报为已解决 | `velocityreport.staff` |
| `/reportcd reopen <编号>` | 重新开启已解决的举报 | `velocityreport.staff` |
| `/reportreload` | 不重启即可热重载配置 | `velocityreport.staff` |

> **提示：** `/reportcd` 详情菜单自带可点击按钮，无需手动输入子命令。

## 🔑 权限

| 权限 | 默认 | 说明 |
|---|---|---|
| `velocityreport.report` | 所有玩家 | 允许提交举报 |
| `velocityreport.staff` | 仅管理 | 访问所有管理命令及接收举报通知 |
| `velocityreport.bypasscooldown` | false | 无视举报冷却时间 |
| `velocityreport.notify` | false | 接收游戏内管理通知（`staff-users` 配置同样生效） |

## ⚙️ 配置说明（`config.yml`）

生成的配置文件中所有选项都带注释，主要设置如下：

| 设置项 | 默认值 | 说明 |
|---|---|---|
| `language` | `zh_CN` | 语言文件：`zh_CN` 或 `en_US` |
| `database.type` | `sqlite` | 存储类型：`sqlite` 或 `mysql` |
| `database.sqlite.filename` | `reports.db` | SQLite 数据库文件名 |
| `database.mysql.*` | — | MySQL 主机 / 端口 / 库名 / 用户 / 密码及 HikariCP 连接池调优 |
| `cooldown-seconds` | `60` | 两次举报的最小间隔（设为 `0` 关闭冷却） |
| `report-reasons` | 5 个预设 | 点击菜单中显示的原因标识与图标（`hacking`、`chat`、`griefing`、`admin-abuse`、`other`） |
| `reports-per-page` | `10` | 每页显示的举报条数 |
| `staff-users` | — | 未安装权限插件时的备用管理名单（不区分大小写） |
| `server-aliases` | — | 需要显示本地化名称的服务器列表 |
| `messages` | — | 覆盖语言文件中的指定消息（优先级更高） |

## 🔨 从源码构建

需要 JDK 17+ 和 Maven 3.8+。

```bash
git clone https://github.com/LiquidTeam11/Velocity_Plugin.git
cd Velocity_Plugin/Velocity-Report
mvn package
```

构建产物位于 `target/Velocity-Report-2.1.1.jar`。

## 📜 更新日志

版本历史请见 [历史更新.md](历史更新.md)。

## 👤 作者

- **YuHongChen（LiquidTeam）** — 主要开发者
- QQ：`1464670605`

## 📄 开源协议

本项目基于 [MIT License](../../LICENSE) 开源。
