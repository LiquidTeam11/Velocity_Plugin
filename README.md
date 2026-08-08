<div align="center">

# Velocity_Plugin

**A collection of Velocity proxy plugin source code**

[![简体中文](https://img.shields.io/badge/简体中文-Read_in_Chinese-blue?style=for-the-badge&logo=github)](README_zh_CN.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)]()
[![Velocity](https://img.shields.io/badge/Velocity-3.x-blueviolet.svg)]()

</div>

Minecraft plugins that run **only on the Velocity proxy** — no backend server plugins required.

## Plugins

| Plugin | Version | Description | Source |
|---|---|---|---|
| [VelocityReport](Velocity-Report/) | 2.2.0 | Cross-server report plugin | `Velocity-Report/` |
| [VelocityChat](VelocityChat/) | 1.9.1 | Cross-server chat & broadcast plugin | `VelocityChat/` |

## Requirements

- **Java** 17+
- **Proxy** Velocity 3.x
- **Maven** 3.8+ (only to build from source)

## Build

```bash
git clone https://github.com/LiquidTeamYHC/Velocity_Plugin.git
cd Velocity-Report && mvn package
cd ../VelocityChat && mvn package
```

## Install

1. Put the built JAR into your Velocity proxy's `plugins/` folder
2. Restart the proxy
3. A `config.yml` is generated on first run — review and restart if you change it

## Author

**YuHongChen (LiquidTeam)** · QQ: `1464670605`

## License

[MIT License](LICENSE)
