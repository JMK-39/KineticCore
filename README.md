# KineticCore

[![🇺🇸 English](https://img.shields.io/badge/%F0%9F%87%BA%F0%9F%87%B8_English-2F81F7?style=for-the-badge)](#english) [![🇨🇳 简体中文](https://img.shields.io/badge/%F0%9F%87%A8%F0%9F%87%B3_%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87-DC2828?style=for-the-badge)](#chinese) [![CurseForge](https://img.shields.io/badge/CurseForge-Open-F16436?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/kineticcore) [![Wiki](https://img.shields.io/badge/Wiki-Docs-2DA44E?style=for-the-badge&logo=github&logoColor=white)](https://github.com/JMK-39/KineticCore/wiki)

<a id="english"></a>

## English

KineticCore is the core mod of the Kinetic series. It adds a set of gameplay and client improvements, and gives addon mods one API for GUI, configuration, networking, input and more.

How to use each feature is described in the **[Wiki](https://github.com/JMK-39/KineticCore/wiki)**.

### Versions

| Minecraft | Loader | Java |
|---|---|---|
| 1.20.1 | Forge 47.4.x | 17 |
| 1.21.1 | NeoForge 21.1.x | 21 |
| 26.1.2 | NeoForge 26.1.2.x | 25 |

One JAR per Minecraft version: `kineticcore-<loader>-<minecraft>-<version>.jar`, from [Releases](https://github.com/JMK-39/KineticCore/releases). Install it on both client and server.

### Features

- **Config center (F6)**: every setting in one place, client and server settings separated, each feature can be switched off.
- **Flight**: flight permission, Superman-style high-speed flight, creative noclip, and crawling.
- **Custom world spawn**: pick the spawn of new worlds by dimension, biome or structure (villages by default).
- **First-join rewards**: items, equipment and commands for new players; **world init commands** for a new save.
- **Data pack loading recovery**: diagnose singleplayer loading errors, locate faulty files or ZIP packs, and temporarily retry without the identified local packs. Reopening the world restores the original pack selection. Dedicated servers report the diagnosis in their logs. See [Data pack recovery](https://github.com/JMK-39/KineticCore/wiki/Data-Pack-Recovery).
- **Client**: compact potion-effect display (JEI keeps clear of it), FPS and TPS/MSPT HUD, copy item IDs and details, log cleaning and filtering, default options for new installs.
- **Gameplay tweaks**: PVP protection, mobs that picked up items can still despawn, percentage void damage, creative void immunity, throwable spawn eggs, farmland protection, faster cobweb breaking, attribute range limits, larger network limits, recipe book removal, bee fixes.
- **Addon API**: page-based GUI, F6 config pages, networking and compression, key bindings and events, hooks, `/kt` command extensions, selectors and registry helpers. See [Addon development](https://github.com/JMK-39/KineticCore/wiki/附属开发入门).

Changes are listed in [CHANGELOG.md](CHANGELOG.md).

<a id="chinese"></a>

## 简体中文

KineticCore 是 Kinetic 系列的核心模组：自带一组玩法与客户端改进，同时为附属模组提供统一的 GUI、配置、网络、输入等开发接口。

各功能的具体用法见 **[Wiki](https://github.com/JMK-39/KineticCore/wiki)**。

### 支持的版本

| Minecraft | 加载器 | Java |
|---|---|---|
| 1.20.1 | Forge 47.4.x | 17 |
| 1.21.1 | NeoForge 21.1.x | 21 |
| 26.1.2 | NeoForge 26.1.2.x | 25 |

每个 Minecraft 版本一个 JAR：`kineticcore-<加载器>-<MC版本>-<版本号>.jar`，在 [Releases](https://github.com/JMK-39/KineticCore/releases) 下载。客户端和服务端都需要安装。

### 功能

- **配置中心（F6）**：所有设置集中在一处，客户端与服务端设置分开，每项功能都可以关闭。
- **数据包加载防护**：显示单人存档的加载错误，定位错误文件或 ZIP 包，并允许暂时跳过已定位的本地错误包重试；重新进入存档仍加载原来的包，专用服务器仅在日志中报告诊断。详见 [数据包加载防护](https://github.com/JMK-39/KineticCore/wiki/数据包加载防护)。
- **飞行**：飞行权限、超人式高速飞行、创造模式穿墙，以及主动爬行。
- **自定义出生点**：按维度、群系或结构选择新世界的出生点（默认村庄）。
- **首次进入奖励**：为新玩家发放物品、装备并执行指令；**世界初始化指令**在新存档首次加载时执行。
- **客户端**：紧凑状态效果显示（JEI 自动避开）、FPS 与 TPS/MSPT HUD、复制物品 ID 与详情、日志清理与过滤、新安装时的默认选项。
- **游戏机制**：PVP 保护、捡起物品的生物仍可自然消失、按比例的虚空伤害、创造模式虚空免疫、投掷刷怪蛋、耕地保护、快速破坏蜘蛛网、属性范围限制、更大的网络容量、移除配方书、蜜蜂修复。
- **附属开发接口**：页面式 GUI、F6 配置页、网络与压缩、按键与事件、Hook、`/kt` 命令扩展、选择器与注册表工具。见 [附属开发入门](https://github.com/JMK-39/KineticCore/wiki/附属开发入门)。

更新内容见 [CHANGELOG.md](CHANGELOG.md)。
