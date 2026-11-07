# UltiSideBar

[![UltiTools](https://img.shields.io/badge/UltiTools--API-6.3.0%2B-blue.svg)](https://github.com/UltiKits/UltiTools-Reborn)

Requires **UltiTools-API 6.3.0 or later**; an older framework refuses to load this module (it declares `api-version: 630`).

需要 **UltiTools-API 6.3.0 或更高版本**；更早的框架会拒绝加载本模块（本模块声明 `api-version: 630`）。

一个功能强大的 Minecraft 服务器侧边栏插件，基于 UltiTools-API 框架开发，支持 PlaceholderAPI 变量和玩家偏好持久化。

## ✨ 功能特性

- 🎨 **自定义标题和内容** - 完全可配置的侧边栏标题和多行内容
- 🔄 **PlaceholderAPI 支持** - 支持所有 PAPI 变量，实现动态数据显示
- 💾 **偏好持久化** - 玩家的侧边栏开关状态自动保存到数据库
- ⚡ **性能优化** - 内容缓存机制，仅在内容变化时更新，减少网络开销
- 🌍 **世界黑名单** - 可配置在特定世界禁用侧边栏
- 🔥 **热重载** - 支持 `/sidebar reload` 实时重载配置，无需重启服务器
- 🌐 **多语言支持** - 内置中英文语言文件，支持自定义翻译

## 📦 安装

### 前置依赖

- **UltiTools-API** 6.3.0 或更高版本（本模块声明 `api-version: 630`，更早的框架会拒绝加载它）
- **PlaceholderAPI** (可选，但强烈推荐)

### 安装步骤

1. 下载 `UltiSideBar.jar`
2. 将文件放入服务器的 `plugins/UltiTools/plugins/` 目录
3. 重启服务器（`/ul reload` 不会加载新的模块 JAR）
4. 编辑 `plugins/UltiTools/pluginConfig/UltiSideBar/config/sidebar.yml` 配置文件
5. 执行 `/sidebar reload` 应用配置

## ⚙️ 配置

配置文件位于 `plugins/UltiTools/pluginConfig/UltiSideBar/config/sidebar.yml`

```yaml
# 启用侧边栏
enabled: true

# 侧边栏标题（支持颜色代码和变量）
title: "&6&l我的服务器"

# 更新间隔（tick，20 tick = 1秒）
update-interval: 20

# 侧边栏内容（支持 PlaceholderAPI 变量）
lines:
  - "&7欢迎, &f%player_name%"
  - ""
  - "&e在线人数: &f%server_online%/%server_max_players%"
  - "&e世界: &f%player_world%"
  - ""
  - "&e金币: &f%vault_eco_balance_formatted%"
  - "&ePing: &f%player_ping%ms"
  - ""
  - "&7服务器时间"
  - "&f%server_time_HH:mm:ss%"
  - ""
  - "&6play.example.com"

# 禁用侧边栏的世界
world-blacklist:
  - "world_event"

# 玩家默认启用侧边栏
default-enabled: true
```

`lines` 最多 15 行。写得更多时，UltiTools 会在加载时拒绝加载本模块，并在控制台指出字段 `lines`、实际行数和上限 `[1, 15]`，
配置文件不会被改写；删减到 15 行以内后重启即可。这是按设计的行为：多出的行既不会被悄悄丢弃，也不会被自动截断。

`lines` takes at most 15 lines. If more are written, UltiTools refuses to load this module at start-up and the console names the
field `lines`, the number of lines written and the limit `[1, 15]`; the file is not rewritten. Reduce it to 15 lines or fewer and
restart. This is by design: extra lines are neither dropped silently nor cut automatically.

### Custom translations / 自定义翻译

UltiSideBar's language files are in `plugins/UltiTools/pluginConfig/UltiSideBar/lang/`.
To customise command and status messages:

1. Copy `lang/zh.yml` to `lang/zh-myserver.yml` **in that same module folder**, keeping the `.yml`
   extension (or copy `lang/en.yml` to `lang/en-myserver.yml` for an English base).
2. Edit the copy, keeping message keys and formatting placeholders unchanged.
3. Set `language: zh-myserver` (or `language: en-myserver`) in `plugins/UltiTools/config.yml`, then run
   a bare `/ul reload` or restart the server. This is the framework-wide language setting, not a
   per-module setting; `/sidebar reload` and `/ul reload UltiSideBar` do not apply a changed setting.

Do not edit the official `lang/zh.yml` or `lang/en.yml` in place: UltiTools restores differing official
files from the bundled version at every start, and restores the official language in use on module
reload. An edited file is kept as a `.bak` backup (then `.1.bak`, `.2.bak`, etc.) with a warning; use
that backup to recover edits into a custom copy. Custom-named copies are never overwritten by the framework.

Missing messages in `zh-myserver` come from official `zh`; those in `en-myserver` come from official
`en`. A safe custom name without an official-code-and-hyphen prefix, such as `ja`, is still read, but
missing messages fall back to English with one warning per module. Names must start with an ASCII
letter or digit and contain only ASCII letters, digits, `_` or `-`; unsafe names are not read.

Sidebar `title` and `lines` are customised in `config/sidebar.yml`, not in a language file: their
built-in defaults come from the module JAR, not the custom copy.

语言文件位于 `plugins/UltiTools/pluginConfig/UltiSideBar/lang/`。自定义命令和状态提示时：

1. 在**本模块的同一目录**内，将 `lang/zh.yml` 复制为 `lang/zh-myserver.yml`，保留 `.yml` 扩展名；
   英文可将 `lang/en.yml` 复制为 `lang/en-myserver.yml`。
2. 只编辑副本中的译文，保留消息键和格式占位符。
3. 在 `plugins/UltiTools/config.yml` 设置 `language: zh-myserver`（或 `language: en-myserver`），
   再执行不带模块名的 `/ul reload` 或重启服务器。这是框架全局语言设置，不是单模块设置；
   `/sidebar reload` 和 `/ul reload UltiSideBar` 不会应用已修改的语言设置。

不要直接编辑官方 `lang/zh.yml` 或 `lang/en.yml`：每次启动时，与 JAR 不同的官方文件都会恢复为内置版本，
模块重载时也会恢复当前使用的官方语言文件。编辑过的内容会保留在 `.bak`（之后为 `.1.bak`、`.2.bak` 等）
备份中，并有警告提示；可从备份将修改恢复到自定义副本。框架不会覆盖自定义命名的副本。

`zh-myserver` 缺失的消息回退到官方 `zh`，`en-myserver` 回退到官方 `en`。`ja` 这类安全但不以官方语言代码
加连字符开头的名称仍会读取，但缺失消息回退到英文，每个模块记录一次警告。名称必须以 ASCII 字母或数字开头，
且只能包含 ASCII 字母、数字、`_` 或 `-`；不安全的名称不会读取。

侧边栏的 `title` 和 `lines` 请在 `config/sidebar.yml` 中自定义，不要在语言文件中修改；
它们的内置默认值来自模块 JAR，不来自自定义副本。

### 颜色代码

使用 `&` 符号加颜色代码：

| 代码 | 颜色 | 代码 | 样式 |
|------|------|------|------|
| &0 | 黑色 | &l | 粗体 |
| &1 | 深蓝 | &m | 删除线 |
| &2 | 深绿 | &n | 下划线 |
| &3 | 深青 | &o | 斜体 |
| &4 | 深红 | &r | 重置 |
| &5 | 紫色 | | |
| &6 | 金色 | | |
| &7 | 灰色 | | |
| &8 | 深灰 | | |
| &9 | 蓝色 | | |
| &a | 浅绿 | | |
| &b | 青色 | | |
| &c | 红色 | | |
| &d | 粉色 | | |
| &e | 黄色 | | |
| &f | 白色 | | |

### 常用 PlaceholderAPI 变量

| 变量 | 描述 |
|------|------|
| `%player_name%` | 玩家名称 |
| `%player_ping%` | 玩家延迟 |
| `%player_health%` | 玩家生命值 |
| `%player_food_level%` | 玩家饥饿值 |
| `%server_online%` | 在线人数 |
| `%server_max_players%` | 最大人数 |
| `%server_tps%` | 服务器 TPS |
| `%player_world%` | 当前世界名 |
| `%vault_eco_balance%` | 玩家余额 |
| `%vault_eco_balance_formatted%` | 格式化余额 |
| `%server_time_HH:mm:ss%` | 服务器时间（24 小时制） |

更多变量请参考 [PlaceholderAPI Wiki](https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/Placeholders)

## ⚠️ Known limitations / 已知限制

**Deleting `title` or `lines` and then reloading.** Under `language: en` (any language whose built-in text differs from
the Chinese Java default), if you delete the `title` or `lines` key from `config/sidebar.yml` and run `/ul reload
UltiSideBar`, the log shows an extra "the module's changes to '...' were not written" WARNING for each reload, and the
server-stop report names the key again. Nothing is lost and the file is never overwritten: the framework binds the
default for the missing key but only inserts missing keys at start. Restart the server instead of reloading after
deleting a key, and the extra lines stop (UltiKits/UltiSideBar#38).

**删除 `title` 或 `lines` 后重载。** 在 `language: en`（内置文本与默认的中文 Java 默认值不同的任何语言）下，若从
`config/sidebar.yml` 删除 `title` 或 `lines` 键并执行 `/ul reload UltiSideBar`，每次重载日志都会多出一条
“模块对 '...' 的更改未写入”的 WARNING，服务器停止时的报告也会再次提到该键。不会丢失任何内容，文件也不会被覆盖：
框架会为缺失的键绑定默认值，但只在启动时插入缺失的键。删除键后请重启服务器而不是重载，多余的日志行即会消失
（UltiKits/UltiSideBar#38）。

## 📝 命令

| 命令 | 描述 | 权限 |
|------|------|------|
| `/sidebar` | 显示帮助信息 | `ultisidebar.toggle` |
| `/sidebar toggle` | 切换侧边栏开关 | `ultisidebar.toggle` |
| `/sidebar on` | 开启侧边栏 | `ultisidebar.toggle` |
| `/sidebar off` | 关闭侧边栏 | `ultisidebar.toggle` |
| `/sidebar reload` | 重载配置文件 | `ultisidebar.admin` |

**命令别名:** `/sb`

## 🔐 权限

| 权限节点 | 描述 | 默认 |
|----------|------|------|
| `ultisidebar.toggle` | 允许玩家切换自己的侧边栏 | 所有玩家 |
| `ultisidebar.admin` | 允许重载插件配置 | OP |

## 📁 数据存储

玩家的侧边栏偏好设置会自动保存到 UltiTools 配置的数据源中（JSON/SQLite/MySQL），无需额外配置。

数据表结构：
- **表名:** `sidebar_preferences`
- **字段:** `player_uuid` (玩家UUID), `enabled` (是否启用)

## 🔧 技术细节

### 性能优化

- **内容缓存:** 每个玩家的侧边栏内容会被缓存，只有当 PlaceholderAPI 变量解析结果发生变化时才会更新 Scoreboard
- **异步检测:** PlaceholderAPI 在主线程执行，但内容比较在内存中完成，开销极小

### 配置热重载

执行 `/sidebar reload` 时会：

1. 重新加载 YAML 配置文件
2. 清空所有玩家的内容缓存
3. 强制刷新所有在线玩家的侧边栏

### 世界切换处理

- 玩家进入黑名单世界时，侧边栏自动隐藏
- 玩家离开黑名单世界时，侧边栏自动恢复（如果玩家未手动关闭）

### 侧边栏计分板的归属 / Who owns the sidebar's scoreboard

The sidebar is drawn on a private scoreboard this module assigns to each player who has it on. That scoreboard belongs to this module alone: another plugin must not write onto it (register its own objective named `sidebar`, or put a player into its own team on it). Such writes are not supported and may be overwritten. The module copies the main scoreboard's teams onto it, so name prefixes stay visible. If another plugin's private scoreboard is already on screen, this sidebar waits until the player is back on the main scoreboard.

**This module replaces a sidebar set with vanilla commands.** A sidebar shown on the server's main scoreboard (`/scoreboard objectives setdisplay sidebar …`, or a plugin that draws on the main scoreboard) counts as a free slot, and this sidebar takes it for every player who has the sidebar on, as every earlier version did.

侧边栏画在本模块为每位开启侧边栏的玩家换上的私有计分板上。这块计分板只归本模块所有：其他插件不应往上面写东西（注册名为 `sidebar` 的目标，或把玩家放进自己在这块计分板上的队伍），这类写入不受支持，可能被覆盖。本模块会把主计分板的队伍复制过去，让名字前缀保持可见。若玩家屏幕上已经是其他插件的私有计分板，本侧边栏会等到玩家回到主计分板后再显示。

**本模块会盖掉用原版命令设置的侧边栏。** 显示在服务器主计分板上的侧边栏（`/scoreboard objectives setdisplay sidebar …`，或把侧边栏画在主计分板上的插件）视为空闲位置，开启侧边栏的玩家都会看到本模块的侧边栏，与以往所有版本一致。

## 📋 更新日志

### v1.0.0

- 🎉 初始版本发布
- ✅ PlaceholderAPI 变量支持
- ✅ 玩家偏好持久化存储
- ✅ 内容缓存性能优化
- ✅ 世界黑名单功能
- ✅ 配置热重载命令
- ✅ 多语言支持 (中/英)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

本项目基于 [MIT License](LICENSE) 开源。

## 🔗 相关链接

- [UltiTools-API 文档](https://doc.ultikits.com/)
- [UltiKits 官网](https://www.ultikits.com/)
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
