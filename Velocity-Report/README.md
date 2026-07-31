<div align="center">

# VelocityReport

**A cross-server report plugin for the Velocity proxy**

[![简体中文](https://img.shields.io/badge/简体中文-Read_in_Chinese-blue?style=for-the-badge&logo=github)](README_zh_CN.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](../../LICENSE)
[![Java 17](https://img.shields.io/badge/Java-17-orange.svg)]()
[![Velocity](https://img.shields.io/badge/Velocity-3.x-blueviolet.svg)]()

</div>

Let players report others from **any server** on your network — the report is stored centrally and shown to staff on every server, no matter which server the player was on.

## ✨ Features

- **Cross-server reports** — players can report anyone across your whole proxy network; staff see a notification no matter which server they're on
- **Clickable reason menu** — `/report <player>` without a reason opens a clickable menu of predefined reasons
- **Custom reasons** — `/report <player> <reason>` supports free-text reasons with a 256-character limit
- **Two storage backends** — SQLite (zero-config, default) or MySQL (via HikariCP connection pool)
- **Bilingual** — built-in `zh_CN` (Simplified Chinese) and `en_US` (English) language files, switchable at runtime
- **Full staff workflow** — view report lists & details, resolve, close, reopen, per-report resolution notes
- **Report history** — browse every past report by player with pagination
- **Cooldown** — configurable anti-spam cooldown with a `velocityreport.bypasscooldown` permission
- **MiniMessage formatting** — all messages are fully customizable using [Adventure MiniMessage](https://docs.advntr.dev/minimessage/format.html)
- **Staff notifications** — in-game notification on submit and on join for pending reports

## ✅ Requirements

| Requirement | Version |
|---|---|
| Java | 17+ |
| Proxy | Velocity 3.x (all modern versions) |
| Optional | A permissions plugin (e.g. [LuckPerms](https://luckperms.net)) for granular staff permissions |

## 📦 Installation

1. Download the plugin JAR (build it yourself or grab a release)
2. Drop it into the `plugins/` folder of your Velocity proxy
3. Restart the proxy (or run `/velocity reload` with the plugin reloader installed)
4. A `config.yml` is generated on first run — review it and restart if you change anything

## 🎮 Commands

### Player commands

| Command | Description | Permission |
|---|---|---|
| `/report <player> [reason]` (alias `/rep`) | Report a player. Without a reason, opens a clickable reason menu | `velocityreport.report` (default: everyone) |

### Staff commands

| Command | Description | Permission |
|---|---|---|
| `/reports` (aliases `/reportslist`, `/reportlist`) | List all pending reports with pagination | `velocityreport.staff` |
| `/reportview <id>` | View full details of a single report | `velocityreport.staff` |
| `/reportclose <id> [resolution]` (aliases `/closereport`, `/resolvereport`) | Close/resolve a report, optionally with a resolution note | `velocityreport.staff` |
| `/reportcd view <id>` | Open the interactive report-detail menu | `velocityreport.staff` |
| `/reportcd resolve <id>` | Mark a report as resolved | `velocityreport.staff` |
| `/reportcd reopen <id>` | Reopen a resolved report | `velocityreport.staff` |
| `/reportreload` | Reload the configuration without restarting | `velocityreport.staff` |

> **Tip:** The `/reportcd` detail menu has built-in clickable buttons — no need to type the subcommands.

## 🔑 Permissions

| Permission | Default | Description |
|---|---|---|
| `velocityreport.report` | true (all players) | Allows submitting reports |
| `velocityreport.staff` | staff only | Access to all staff commands & staff report notifications |
| `velocityreport.bypasscooldown` | false | Ignore the report cooldown |
| `velocityreport.notify` | false | Receive in-game staff notifications (staff-users in config also apply) |

## ⚙️ Configuration (`config.yml`)

Key settings (all commented in the generated file):

| Setting | Default | Description |
|---|---|---|
| `language` | `zh_CN` | Language file: `zh_CN` or `en_US` |
| `database.type` | `sqlite` | `sqlite` or `mysql` |
| `database.sqlite.filename` | `reports.db` | SQLite database file |
| `database.mysql.*` | — | MySQL host / port / database / user / password + HikariCP pool tuning |
| `cooldown-seconds` | `60` | Seconds between reports (set `0` to disable) |
| `report-reasons` | 5 presets | Reason IDs + icons shown in the clickable menu (`hacking`, `chat`, `griefing`, `admin-abuse`, `other`) |
| `reports-per-page` | `10` | Reports per list page |
| `staff-users` | — | Fallback staff list when no permissions plugin is installed (case-insensitive) |
| `server-aliases` | — | Servers that show a localized display name |
| `messages` | — | Override individual language-file messages (takes priority) |

## 🔨 Building from source

JDK 17+ and Maven 3.8+ are required.

```bash
git clone https://github.com/LiquidTeam11/Velocity_Plugin.git
cd Velocity_Plugin/Velocity-Report
mvn package
```

The built plugin is at `target/Velocity-Report-2.1.1.jar`.

## 📜 Changelog

See [历史更新.md](历史更新.md) for the version history.

## 👤 Author

- **YuHongChen (LiquidTeam)** — main developer
- QQ: `1464670605`

## 📄 License

This project is licensed under the [MIT License](../../LICENSE).
