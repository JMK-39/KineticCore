# KineticCore

KineticCore 是面向 **Minecraft 1.20.1 / Forge 47.4.x / Java 17** 的核心基础模组与公共开发 API。它为 Kinetic 系列及其他附属提供统一的 GUI、配置、网络、压缩、输入、生命周期、Hook、命令扩展、选择器、Minecraft 桥接与运行时基础设施。

核心约束：**业务代码只调用公开 `dev.xyat.kineticcore.api.*`。** `internal/` 只负责实现，不属于附属调用面。

## 文档

- [API 架构规则](API_ARCHITECTURE.md)
- [API 参考](API_REFERENCE.md)
- [API 使用教程](API_TUTORIAL.md)

## 运行环境

- Minecraft 1.20.1
- Forge 47.4.x
- Java 17
- Gradle 8.8（源码构建固定版本）
- GitHub Release 仅手动发布；本地构建不会自动创建、上传或发布 GitHub Release
- Curios：可选兼容
- JEI：可选兼容

## 源码结构

```text
src/main/java/dev/xyat/kineticcore/
├─ api/         对附属公开的稳定能力
├─ internal/    API 的私有实现、Mixin、Bridge、具体 Screen 与运行时
├─ feature/     KineticCore 自身业务功能
└─ bootstrap/   核心启动与功能装配
```

依赖方向：

```text
feature ─┐
         ├──> api ──> internal ──> Minecraft / Forge
addon ───┘
```

KineticCore 自身的 Feature 与外部附属遵守相同原则。通用能力缺失时，应先把可复用能力补进 API，再由业务调用；明确属于某个模组自身的数据规则、玩法规则和业务含义留在该模组。

## 统一 GUI

Kinetic GUI 的虚拟画布固定为 **640×360**。API 负责实际分辨率适配、4K 缩放、鼠标坐标转换、Scissor、标准控件高度、Tooltip、右键菜单、确认框、焦点、层级和主题。

三类 Screen：

- `KineticScreen`：标准 640×360 虚拟画布界面。
- `KineticContainerScreen`：带容器菜单的 640×360 虚拟画布界面。
- `KineticNativeScreen`：原生屏幕坐标界面，用于原版界面注入或必须使用物理坐标的场景。

标准控件包括：

- 普通按钮、紧凑按钮。
- 高层按钮、紧凑高层按钮。
- Toggle。
- 单行文本、多行文本。
- Integer / Long / Double 数字输入框。
- 文本自动补全、整数/Long/小数自动补全。
- Dropdown。
- TabBar。
- RGB/HEX 颜色预览与颜色按钮。
- Placeholder。
- Tooltip、物品 Tooltip。
- 右键菜单、确认框。
- 平滑滚动、网格滚动控制器。
- 控件添加、移除、焦点管理。
- 坐标转换、UI Scissor。
- UI 重建与返回导航。

非 Screen 的 Panel、Helper、Editor 使用 `KineticWidgets.create...` detached factory。标准 Widget 的构造细节由 API 统一，不需要附属自己决定控件高度或样式。

## F6 配置 API

配置系统支持：

- 客户端本地配置。
- 本机安装级配置。
- 服务端权威配置。
- Boolean。
- Integer / Long / Double。
- String / Long Text。
- 固定值 Choice 与翻译后的 Choice。
- String List / Integer List。
- Entity List。
- Item List / Item Rule List。
- Tick ↔ Seconds 数值项。
- RGB/HEX Color。
- Action、Section、Description。
- 自定义 validator。
- 保存提示与应用时机。

客户端页面使用 `KTConfigPage` 描述“怎么编辑”；服务端权威数据使用 `KTServerConfigSpec` 描述“服务端最终允许什么”。数字、字符串、列表、Choice、颜色都可以由附属提供业务 validator，API 不擅自决定负数、最小值、最大值或业务格式。

主要入口：

```text
KTConfigApi
KTConfigPage
KTConfigEntry
KTConfigScope
KTClientConfigAdapter
KTServerConfigApi
KTServerConfigSpec
KTServerConfigClient
```

## 网络与压缩

公开网络层隐藏 Forge `SimpleChannel`、`FriendlyByteBuf`、`PacketDistributor` 与线程切换细节。

主要类型：

```text
KineticNetwork
NetworkChannel
NetworkCodec<T>
NetworkBuffer
ServerboundSender<T>
ClientboundSender<T>
ServerPacketContext
ServerboundPacketHandler<T>
NetworkProtocolLimits
NetworkTransportLimits
```

特性：

- 同一 Channel ID 只有一个统一协议所有者。
- 协议版本严格匹配。
- UTF、byte[]、字符串列表都有默认安全上限和显式上限重载。
- 服务端配置 JSON 整数按 `Long` 精确保留，不通过 `Double` 丢失大整数精度。

压缩统一使用 `KineticCompression`：

```text
compressUtf8
compressBytes
decompressUtf8
decompressBytes
```

解压必须给出最大输出大小，避免无界解压。

## 输入与生命周期

`KineticKeyBindings` 统一按键注册、默认键、鼠标键、修饰键、上下文、启用条件、按下回调和持续按住状态。

`KineticClientEvents` 提供：

- Client Tick START / END。
- 登录、退出。
- Screen Init Before / After。
- Screen Render After。
- HUD AFTER_CHAT / END。

`KineticServerEvents` 提供：

- Server Tick START / END。
- Server Started。
- 玩家登录、退出、重生、切换维度。

附属不需要直接监听这些对应的 Forge 生命周期事件。

## Tooltip、Overlay 与状态效果

- `KineticItemTooltips`：统一物品 Tooltip 构建与渲染观察入口。
- `GuiOverlay`：Tooltip、右键菜单、确认框、Toast 和高层覆盖。
- `KineticEffectDisplay`：状态效果区域、紧凑布局、Tab 展开和图标策略。

Overlay 打开时会阻断底层输入和底层 Tooltip，避免菜单/确认框与下层控件互相穿透。

## 选择器与编辑器

`KineticSelectors` 提供统一入口：

- 物品选择器。
- 实体选择器。
- 物品列表/物品规则编辑器。
- NBT 编辑器。
- RGB/HEX 调色器。
- 调色板编辑器。

`KineticCommandListEditor` 提供可复用的命令列表编辑能力；`HudPositionEditor` 提供 HUD 拖拽、缩放和位置编辑能力。

## Hook 与 Mixin

Mixin 是允许的扩展手段。规则是：

- 同一个模组不要为同一能力重复创建多套 Mixin。
- 如果某项注入能力能被完全无关的附属原样复用，应放入 KineticCore 的公开 API/Hook。
- 明确属于某个附属自身业务的 Mixin 可以留在附属。
- 已经由公开 API 提供的通用 GUI、输入、配置、网络等能力不要再重复实现。

公开 Hook：

```text
ClientHooks
CommonHooks
ServerHooks
HookRegistration
```

## 命令扩展

`KineticCommands` 维护统一 `/kt` 根命令。附属通过 `CommandExtension` 注册自己的子命令、帮助项和 reload 回调，不需要自己接管 Forge 命令注册事件。

## 注册与运行时

通用注册入口包括：

```text
KineticEntityTypes
KineticRegistryHandle
KineticClientRenderers
KineticPackSources
KineticModLifecycle
KineticClientRuntime
KineticFeatureSwitches
KineticRuntime
```

`KineticClientRuntime` 负责统一客户端线程执行和 Screen 导航；附属不需要管理 KC 内部初始化顺序。

## Minecraft 桥接

公开桥接：

```text
MinecraftAttributes
MinecraftContainers
MinecraftKeys
MinecraftScreens
```

这些 API 用于隐藏 Accessor/Mixin 或原版私有实现细节。

## 飞行与性能监控

飞行：

```text
KineticFlight
KineticFlightClient
```

性能：

```text
KineticServerPerformance
ServerTickTracker
```

## 架构检查

工程提供 `checkKineticArchitecture` 静态架构任务，用于阻止：

- Feature/业务代码直接依赖 `internal`。
- `internal` 反向依赖 Feature。
- API 依赖 Feature。
- Feature 跨 Feature 直接 import。
- API 公共签名泄漏 internal 类型。
- API 暴露 Forge Event 实现类型。
- 业务绕过已有统一 GUI、输入、生命周期、网络、Tooltip、命令能力。

完整规则见 [API_ARCHITECTURE.md](API_ARCHITECTURE.md)。

## API 源码包

工程提供 `apiSourceZip`，内容包括公开 `api/` 源码和：

```text
README.md
API_ARCHITECTURE.md
API_REFERENCE.md
API_TUTORIAL.md
```

## 许可证

Copyright (C) 2024-2026 XYAT.

本项目基于 **GNU Lesser General Public License v3.0 (LGPLv3)** 开源。
