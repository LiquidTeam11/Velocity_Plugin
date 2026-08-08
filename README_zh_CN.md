<div align="center">

# Velocity_Plugin

**Velocity 代理端插件源代码集**

[![English](https://img.shields.io/badge/English-Read_in_English-blue?style=for-the-badge&logo=github)](README.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)]()
[![Velocity](https://img.shields.io/badge/Velocity-3.x-blueviolet.svg)]()

</div>

本仓库收录运行在 **Velocity 代理端** 的 Minecraft 插件源代码，所有插件仅在代理端安装即可，子服务端零侵入。

## 插件一览

| 插件 | 版本 | 简介 | 源码目录 |
|---|---|---|---|
| [VelocityReport](Velocity-Report/) | 2.2.0 | 跨服举报插件 | `Velocity-Report/` |
| [VelocityChat](VelocityChat/) | 1.9.1 | 跨服聊天与广播插件 | `VelocityChat/` |

## 环境要求

- **Java** 17+
- **代理端** Velocity 3.x
- **Maven** 3.8+（仅编译源码时需要）

## 构建

```bash
git clone https://github.com/LiquidTeamYHC/Velocity_Plugin.git
cd Velocity-Report && mvn package
cd ../VelocityChat && mvn package
```

## 安装

1. 将构建出的 JAR 放入 Velocity 代理端 `plugins/` 目录
2. 重启代理端
3. 首次启动自动生成 `config.yml`，修改后重启生效

## 作者

**YuHongChen（LiquidTeam）** · QQ：`1464670605`

## 开源协议

[MIT License](LICENSE)
