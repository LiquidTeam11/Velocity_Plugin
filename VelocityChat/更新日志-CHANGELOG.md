# VelocityChat 更新日志 / Changelog

## v1.7.0
- 修复封禁玩家尝试连接时仍显示离开消息的问题（`[-]xxx离开了unknown`）
  - 若玩家从未连接到任何后端服务器（如封禁连接被拒），不再广播离开消息

- Fixed banned players still triggering disconnect messages (`[-]xxx left unknown`)
  - If the player never connected to a backend server (e.g. banned connection rejected), leave message is now suppressed

## v1.6.0
- `/br` Tab 补全优化：玩家输入内容后不再显示补全提示
- 代码内部优化

- `/br` tab completion optimization: no longer shows suggestions after player has typed
- Internal code improvements

## v1.5.0
- 修复断线时偶尔显示 `unknown` 的问题（缓存玩家最后所在服务器）
- 优化断线消息可靠性

- Fixed disconnect event occasionally showing `unknown` (cache last known server)
- Improved disconnect message reliability

## v1.4.0
- 控制台不再显示进服/切服/离服消息
- 权限细分：`velocitychat.admin.create` / `.group.join` / `.group.remove` / `.group.settitle` / `.group.delete` / `.group.list` / `.reload`
- 玩家进服自动注册权限节点到 LuckPerms

- Console no longer shows join/switch/leave messages
- Granular permissions: `velocitychat.admin.*` sub-nodes
- Permissions auto-register with LuckPerms on player join

## v1.3.0
- `/br` 冷却时间设置
- 管理员可绕过冷却（`velocitychat.admin`）

- `/br` cooldown config
- Admins can bypass cooldown

## v1.2.0
- 进服/切服/离服消息可见性控制：`all` / `admin` / `none`

- Join/switch/leave message visibility: `all` / `admin` / `none`

## v1.1.0
- 群组称号系统：`/vchat create group`、`/vchat group join`
- 称号支持 `&` 颜色代码
- `/vchat reload` 热重载

- Group title system: `/vchat create group`, `/vchat group join`
- `&` color codes in titles
- `/vchat reload` hot-reload

## v1.0.0
- `/br <消息>` 跨服聊天，支持 `&` 颜色代码
- 进服/切服/离服自动消息
- 服务器别名（如 lobby → 登录服）
- 中英双语

- `/br <message>` cross-server chat with `&` color codes
- Join/switch/leave announcements
- Server aliases (e.g. lobby → 登录服)
- Chinese & English
