# KineticCore

[![🇺🇸 English](https://img.shields.io/badge/%F0%9F%87%BA%F0%9F%87%B8_English-2F81F7?style=for-the-badge)](#english) [![🇨🇳 简体中文](https://img.shields.io/badge/%F0%9F%87%A8%F0%9F%87%B3_%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87-DC2828?style=for-the-badge)](#chinese) [![CurseForge](https://img.shields.io/badge/CurseForge-Open-F16436?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/kineticcore)

<a id="english"></a>

## English

KineticCore is the shared base mod and public development API for **Minecraft 1.20.1 / Forge 47.4.x / Java 17**. It gives the Kinetic mod family and other addons common GUI, configuration, networking, compression, input, lifecycle, hook, command-extension, selector, registry-access, Minecraft-helper, and runtime infrastructure.

Core rule: **feature code calls only the public `dev.xyat.kineticcore.api.*` API.** `internal/` contains implementation details and is not an addon-facing API.

## API Documentation

Public API documentation is maintained as Javadoc in the `dev.xyat.kineticcore.api.*` source tree; a separate hand-maintained API Markdown is not used. Regular builds do not generate Javadoc, keeping compilation faster.

To generate method documentation for an AI or another reader, select `Tasks → build → buildJavaDOC` in IDEA's Gradle panel, or run `gradlew buildJavaDOC` from the project root on Windows. Only this task generates the documentation and its shareable archive:

- Browser documentation: `javadoc/index.html` (the `javadoc/` directory is under the project root).
- Shareable AI package: `KineticCore-javadoc-<build-version>.zip` under the project root; `javadoc/index.html` is its entry page.

To package only the API source and documentation, run `apiSourceZip`. It also generates Javadoc when needed, but a regular `build` does not invoke it.

## Runtime Environment

- Minecraft 1.20.1
- Forge 47.4.x
- Java 17
- Gradle 8.8 (the pinned source-build version)
- GitHub Releases are published manually; local builds do not create, upload, or publish a release.
- Curios: optional compatibility
- JEI: optional compatibility

## Source Layout

```text
src/main/java/dev/xyat/kineticcore/
├─ api/         Stable capabilities exposed to addons
├─ internal/    Private API implementations, Mixins, concrete screens, and runtime
├─ feature/     KineticCore's own product features
└─ bootstrap/   Core startup and feature wiring
```

Dependency direction:

```text
feature ─┐
         ├──> api ──> internal ──> Minecraft / Forge
addon ───┘
```

KineticCore's own features follow the same rule as external addons. When a reusable capability is missing, add it to the public API first and call it from feature code. Data rules, gameplay rules, and meanings that belong to one mod stay in that mod.

## Shared GUI

The Kinetic GUI uses a fixed **640×360** virtual canvas. The API handles resolution adaptation, 4K scaling, mouse-coordinate conversion, scissoring, standard control heights, tooltips, context menus, confirmation dialogs, focus, z-order, and themes.

Three screen bases are available:

- `KineticScreen`: standard screen on the 640×360 virtual canvas.
- `KineticContainerScreen`: container-menu screen on the 640×360 virtual canvas.
- `KineticNativeScreen`: native physical screen coordinates, for vanilla-screen integration or cases that require physical coordinates.

Standard controls include:

- Regular and compact buttons.
- High-z and compact high-z buttons.
- Toggle buttons.
- Single-line and multiline text fields.
- Integer, Long, and Double numeric fields.
- Text autocomplete and integer/Long/decimal autocomplete.
- Dropdowns and tab bars.
- RGB/HEX color previews and color buttons.
- Placeholders, tooltips, and item tooltips.
- Context menus and confirmation dialogs.
- Smooth scrolling and grid-scroll controllers.
- Control registration/removal and focus management.
- Coordinate conversion and UI scissoring.
- UI rebuilding and return navigation.

`KineticTabs.ScrollableItemGrid` uses `ItemGridItem.outline` with `ItemGridOutline.SUCCESS` or `ItemGridOutline.WARNING` for common state outlines; the active theme supplies their colors. Grid states use full borders. An error red border takes priority over a hovered blue border, which takes priority over selection and business-state borders. Items with no state use a white border.

Non-screen panels, helpers, and editors should use the detached `KineticWidgets.create...` factories. The API owns standard widget construction details, so addons do not need to choose control heights or styles themselves.

## F6 Configuration API

The configuration system supports:

- Client-local configuration.
- Installation-wide local configuration.
- Server-authoritative configuration.
- Boolean values.
- Integer, Long, and Double values.
- Strings and long text.
- Fixed choices and translated choices.
- String lists and integer lists.
- Entity lists.
- Item lists and item-rule lists.
- Tick-to-seconds numeric entries.
- RGB/HEX colors.
- Actions, sections, and descriptions.
- Custom validators.
- Save notices and application timing.

Client pages use `KTConfigPage` to describe **how a value is edited**. Server-authoritative data uses `KTServerConfigSpec` to describe **what the server ultimately permits**. Addons may provide business validators for numbers, strings, lists, choices, and colors; the API does not impose business-specific rules for negative values, minimums, maximums, or formats.

Main entry points:

```text
KTConfigApi
KTConfigPage
KTConfigEntry
KTConfigScope
KTClientConfigSpec
KTClientConfigAdapter
KTServerConfigApi
KTServerConfigSpec
KTServerConfigClient
```

## Networking and Compression

The public networking layer hides Forge `SimpleChannel`, `FriendlyByteBuf`, `PacketDistributor`, and thread-switching details.

Main types:

```text
KineticNetwork
PacketChannel
NetworkChannel
NetworkCodec<T>
NetworkBuffer
NetworkBuffers
ServerboundSender<T>
ClientboundSender<T>
ServerPacketContext
ServerboundPacketHandler<T>
NetworkProtocolLimits
NetworkTransportLimits
```

Features:

- Each Channel ID has one unified protocol owner.
- Protocol versions must match exactly.
- UTF, `byte[]`, and string lists have safe default limits and overloads for explicit limits.
- Server configuration JSON integers are preserved exactly as `Long` values instead of losing large-integer precision through `Double`.

Use `KineticCompression` for shared compression:

```text
compressUtf8
compressBytes
decompressUtf8
decompressBytes
```

Decompression requires a maximum output size to prevent unbounded expansion.

## Input and Lifecycle

`KineticKeyBindings` unifies key registration, default keys, mouse buttons, modifiers, contexts, enable conditions, press callbacks, and held-state tracking.

`KineticInputGestures` provides reusable input-gesture recognition. It currently includes `DoubleTap`, which detects two presses within a specified tick window so each feature or addon does not need its own edge detection and timing state. Superman Flight's double-tap Space toggle uses this shared capability.

`KineticClientEvents` provides:

- Client Tick START / END.
- Login and logout.
- Screen Init Before / After, including inspection, addition, and removal of controls on external screens.
- Screen Render After.
- Mouse Button Before.
- Level Render Stage.
- HUD AFTER_CHAT / HOTBAR / END.

`KineticServerEvents` provides:

- Server Tick START / END.
- Player Tick START / END.
- Server AboutToStart / Started / Stopping / Stopped.
- Player login, logout, clone, respawn, and dimension changes.
- Datapack Sync.
- Cancellable server chat events.
- Stable priorities from `HIGHEST` to `LOWEST`.

`KineticWorldEvents` provides world load/unload, entity join/leave, chunk load/unload, block break/place, item pickup, Mob Finalize Spawn, and baby-entity spawn events.

`KineticLivingEvents` provides Living Tick, equipment changes, death, Hurt, Damage, Attack, and potion-applicability events. Dedicated contexts are available when a handler needs to modify values or cancel an event.

Addons do not need to listen directly to the corresponding generic Forge lifecycle and world events. Event contracts specific to third-party mods remain in the addon.

## Tooltips, Overlays, and Status Effects

- `KineticItemTooltips`: shared item-tooltip construction, GatherComponents, client factories and render observation for custom `TooltipComponent` implementations.
- `GuiOverlay`: tooltips, context menus, confirmation dialogs, toasts, and high-z overlays.
- `KineticEffectDisplay`: status-effect areas, compact layouts, expanded tabs, and icon policies.

An open overlay blocks input and tooltips from the underlying screen, preventing menus or confirmation dialogs from leaking interaction to controls below them.

## Selectors and Editors

`KineticSelectors` provides shared entry points for:

- Item selection.
- Entity selection.
- Item-list and item-rule editors.
- NBT editing.
- RGB/HEX color picking.
- Palette editing.

`KineticCommandListEditor` provides reusable command-list editing. `HudPositionEditor` provides HUD dragging, scaling, and position editing.

## Hooks and Mixins

Mixins are allowed extension tools. Follow these rules:

- A mod should not create multiple Mixin implementations for the same capability.
- If an injection capability can be reused unchanged by unrelated addons, put it in KineticCore's public API/Hook layer.
- A Mixin that clearly belongs to one addon's own feature may stay in that addon.
- Do not reimplement common GUI, input, configuration, networking, or other capabilities already provided by the public API.

Public hooks:

```text
ClientHooks
CommonHooks
ServerHooks
HookRegistration
```

## Command Extensions

`KineticCommands` owns the shared `/kt` root command. Addons register subcommands, help entries, and reload callbacks through `CommandExtension`. Use `registerTopLevel(...)` when a command needs to keep an independent root path. Neither approach requires an addon to take over Forge command-registration events.

## Registration and Runtime

Shared registration and runtime entry points include:

```text
KineticItems
KineticMenuTypes
KineticEntityTypes
KineticRegistryHandle
KineticRegistries
KineticRegistryView
KineticClientMenus
KineticItemProperties
KineticClientRenderers
KineticPackSources
KineticCreativeTabs
KineticModLifecycle
KineticClientRuntime
KineticServerRuntime
KineticEnvironment
KineticPlatform
KineticPaths
KineticFeatureSwitches
KineticRuntime
```

`KineticRegistries` supports ID-to-object lookup, enumeration, and tag queries for items, entity types, blocks, potion effects, attributes, and enchantments. Use `custom(...)` to access third-party registries.

`KineticItems` and `KineticMenuTypes` provide shared registration. Client menu bindings and item properties use `KineticClientMenus` and `KineticItemProperties`, respectively.

`KineticCreativeTabs` provides creative-tab enumeration and lookup, BuildContents callbacks, and client search-tree refresh.

`KineticEnvironment`, `KineticPlatform`, and `KineticPaths` unify physical-side checks, mod detection, and configuration-directory access.

`KineticClientRuntime` provides shared client-thread execution, screen opening, and screen refresh. Addons do not need to manage KineticCore's internal initialization order.

`KineticFeatureSwitches` manages feature toggles through stable feature IDs, names, and descriptions. Mixin class names are implementation mappings only; they must not become player-facing configuration keys or GUI labels. Every toggle must have its own name and description in both `zh_cn` and `en_us`; hovering shows the description and the restart-after-save notice.

## World, Player Pose, and Inventory Foundations

```text
KineticChunkLoading
KineticInventorySlots
KineticItemSearch
KineticSelectors
KineticPlayerPose
KineticCrawling
```

`KineticChunkLoading` unifies forced chunk loading and release. `KineticInventorySlots` checks player-inventory slots and handlers. `KineticItemSearch` provides snapshots of the shared item-search index and lets `CachedItem.matches(query, ItemCategory)` combine name searches with Combat, Tool, Food, General, and Block category filters. For items outside the shared index, use `KineticItemSearch.matchesCategory(stack, category)`; selectors should be opened through `KineticSelectors`.

`KineticPlayerPose` provides shared player-pose application and real-dimension refresh. Features that temporarily change a player's real collision pose should use this API instead of overriding `getDimensions()` independently. Core crawling and Superman Flight's horizontal pose share this path so multiple features do not compete over the player's pose or collision size.

`KineticCrawling` provides crawling-state queries and APIs to take over or release crawling. Horizontal Superman Flight has higher priority than crawling: starting horizontal flight releases crawling, crawling cannot take over while horizontal flight is active, and crawling may take over again after horizontal flight ends.

## Minecraft Helpers

Public helper entry points:

```text
MinecraftAttributes
MinecraftContainers
MinecraftKeys
MinecraftScreens
```

These APIs hide Accessor/Mixin and vanilla private-implementation details.

## Flight and Performance Monitoring

Flight APIs:

```text
KineticFlight
KineticFlightClient
KineticSuperFlight
```

Superman Flight uses shared server/client flight state and network synchronization, working together with the player-pose, crawling, and input-gesture APIs.

Controls:

- **Double-tap Space**: toggle Superman Flight.
- When Superman Flight is available, Kinetic handles double-tap Space before vanilla Creative Flight. If the ability is unavailable, vanilla behavior is not intercepted.
- **Tap Space**: exit the current horizontal pose during horizontal high-speed flight without disabling the overall Superman Flight toggle.
- **W / S**: move forward / backward.
- **A / D**: control roll; they do not strafe.
- **Shift**: accelerate smoothly without triggering sneak.
- **Ctrl + W**: immediately reach the currently saved target top speed.
- **Shift + Mouse Wheel**: adjust the target top speed, up to 100x.
- **Alt**: free-look camera.
- Horizontal mouse movement controls the camera only; it does not directly control flight direction or roll.

Superman Flight FOV is calculated from the player's **actual current movement speed**, not only from the target speed. Smooth interpolation expands and restores the view, making the effect more noticeable at 100x speed. Horizontal flight uses the crawling-compatible `KineticPlayerPose` path to maintain a genuinely low collision profile and cooperates with temporary-state cleanup on respawn, dimension changes, and re-login.

Performance APIs:

```text
KineticServerPerformance
ServerTickTracker
```

## Architecture Checks

The project provides the static `checkKineticArchitecture` task to prevent:

- Feature/business code from depending directly on `internal`.
- `internal` from depending on Feature.
- API from depending on Feature.
- Direct cross-Feature imports.
- Public API signatures from exposing `internal` types.
- API from exposing Forge Event implementation types.
- Business code from bypassing existing shared GUI, input, lifecycle, networking, tooltip, or command capabilities.

Architecture rules run automatically during the build through `gradle/kinetic-architecture.gradle` and `gradle/kinetic-api-verification.gradle`.

## API Source Archive

The project provides `apiSourceZip`, containing the public `api/` source, this `README.md`, and Javadoc HTML generated during the task.

<a id="chinese"></a>

## 简体中文

KineticCore 是面向 **Minecraft 1.20.1 / Forge 47.4.x / Java 17** 的核心基础模组与公共开发 API。它为 Kinetic 系列及其他附属提供统一的 GUI、配置、网络、压缩、输入、生命周期、Hook、命令扩展、选择器、注册表访问、Minecraft 辅助能力与运行时基础设施。

核心约束：**业务代码只调用公开 `dev.xyat.kineticcore.api.*`。** `internal/` 只负责实现，不属于附属调用面。

## API 文档

公开 API 的说明直接维护在 `dev.xyat.kineticcore.api.*` 源码 Javadoc 中，不再手工维护独立 API Markdown。正常构建不会生成 Javadoc，以免拖慢编译。

需要给 AI 提供方法文档时，在 IDEA 的 Gradle 面板选择 `Tasks → build → buildJavaDOC`，或在项目根目录运行 `gradlew buildJavaDOC`（Windows）。仅执行此任务时才生成对应文档及便于分享的压缩包：

- 浏览器文档：`javadoc/index.html`（文档目录：项目根目录下的 `javadoc/`）。
- 发给 AI 的文件：项目根目录下的 `KineticCore-javadoc-<构建版本号>.zip`，其中 `javadoc/index.html` 是文档首页。

仅需包含源码和文档的完整 API 源码包时，仍可单独运行 `apiSourceZip`；它同样会按需生成 Javadoc，但不会由普通 `build` 调用。

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
├─ internal/    API 的私有实现、Mixin、具体 Screen 与运行时
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

`KineticTabs.ScrollableItemGrid` 的 `ItemGridItem.outline` 使用 `ItemGridOutline.SUCCESS` 或
`ItemGridOutline.WARNING` 表示通用业务状态描边，并由当前主题提供颜色。网格状态统一用完整边框，
错误红框优先于悬停蓝框，悬停蓝框优先于选中框和业务状态框，没有状态时显示白框。

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
KTClientConfigSpec
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
PacketChannel
NetworkChannel
NetworkCodec<T>
NetworkBuffer
NetworkBuffers
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

`KineticInputGestures` 提供可复用的输入手势识别能力。当前包含 `DoubleTap`，用于识别指定 tick 窗口内的双击/双按操作，避免各 Feature 或附属重复维护边沿检测与计时状态。超人飞行的双击空格开关即通过该公共能力实现。

`KineticClientEvents` 提供：

- Client Tick START / END。
- 登录、退出。
- Screen Init Before / After，以及外部 Screen 控件的查看、添加、移除。
- Screen Render After。
- Mouse Button Before。
- Level Render Stage。
- HUD AFTER_CHAT / HOTBAR / END。

`KineticServerEvents` 提供：

- Server Tick START / END。
- Player Tick START / END。
- Server AboutToStart / Started / Stopping / Stopped。
- 玩家登录、退出、Clone、重生、切换维度。
- Datapack Sync。
- 可取消的服务端聊天事件。
- `HIGHEST` 到 `LOWEST` 的稳定优先级。

`KineticWorldEvents` 提供世界加载/卸载、实体加入/离开、区块加载/卸载、方块破坏/放置、物品拾取、Mob Finalize Spawn 与幼体生成。

`KineticLivingEvents` 提供 Living Tick、装备变化、死亡、Hurt、Damage、Attack 与药水适用性事件，并为需要修改数值或取消事件的场景提供专用上下文。

附属不需要直接监听这些对应的 Forge 通用生命周期和世界事件。第三方模组自己的事件契约仍留在附属。

## Tooltip、Overlay 与状态效果

- `KineticItemTooltips`：统一物品 Tooltip 构建、GatherComponents、自定义 `TooltipComponent` 客户端工厂与渲染观察入口。
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

`KineticCommands` 维护统一 `/kt` 根命令。附属通过 `CommandExtension` 注册自己的子命令、帮助项和 reload 回调；需要保持独立命令路径时使用 `registerTopLevel(...)`。两种方式都不需要附属自己接管 Forge 命令注册事件。

## 注册与运行时

通用注册与运行时入口包括：

```text
KineticItems
KineticMenuTypes
KineticEntityTypes
KineticRegistryHandle
KineticRegistries
KineticRegistryView
KineticClientMenus
KineticItemProperties
KineticClientRenderers
KineticPackSources
KineticCreativeTabs
KineticModLifecycle
KineticClientRuntime
KineticServerRuntime
KineticEnvironment
KineticPlatform
KineticPaths
KineticFeatureSwitches
KineticRuntime
```

`KineticRegistries` 提供物品、实体类型、方块、药水效果、属性和附魔的 ID ↔ 对象查询、枚举与 Tag 查询，也可通过 `custom(...)` 访问第三方自定义注册表。
`KineticItems` / `KineticMenuTypes` 负责通用注册；客户端菜单绑定和物品属性分别通过 `KineticClientMenus` / `KineticItemProperties`。
`KineticCreativeTabs` 负责创造模式 Tab 枚举、查询、BuildContents 回调与客户端搜索树刷新。
`KineticEnvironment`、`KineticPlatform`、`KineticPaths` 统一物理端判断、模组检测和配置目录。
`KineticClientRuntime` 负责统一客户端线程执行、Screen 打开与刷新；附属不需要管理 KC 内部初始化顺序。

`KineticFeatureSwitches` 使用稳定的功能 ID、名称和说明管理功能开关。Mixin 类名只允许作为实现层映射，不能成为玩家配置键或 GUI 文案。每个开关必须在 `zh_cn` 和 `en_us` 提供独立的名称与功能说明，鼠标悬停显示说明及保存后重启提示。

## 世界、玩家姿态与背包基础能力

```text
KineticChunkLoading
KineticInventorySlots
KineticItemSearch
KineticSelectors
KineticPlayerPose
KineticCrawling
```

`KineticChunkLoading` 统一强加载/释放区块；`KineticInventorySlots` 提供玩家背包 Slot/Handler 判定；`KineticItemSearch` 提供共享物品搜索索引快照，并可通过 `CachedItem.matches(query, ItemCategory)` 结合名称搜索与战斗、工具、食物、通用、方块分类筛选。未进入共享索引的物品可用 `KineticItemSearch.matchesCategory(stack, category)` 判断；选择器统一通过 `KineticSelectors` 打开。

`KineticPlayerPose` 提供统一的玩家 Pose 应用与真实尺寸刷新能力。需要临时改变玩家真实碰撞姿态的功能应复用该 API，不要各自直接改写 `getDimensions()`。核心爬行与超人横向飞行共用这条姿态链，以避免多个功能同时争夺玩家 Pose/碰撞尺寸。

`KineticCrawling` 提供爬行状态查询、接管与释放入口。横向超人飞行的优先级高于爬行：开始横向飞行时会释放爬行接管，横向飞行期间不允许重新进入爬行；退出横向飞行后，爬行功能才可再次接管。

## Minecraft 辅助能力

公开辅助入口：

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
KineticSuperFlight
```

超人飞行使用统一的服务端/客户端飞行状态与网络同步，并与玩家姿态、爬行和输入手势 API 协同工作。

控制方式：

- **双击空格**：开启/关闭超人飞行。
- 当玩家具备超人飞行能力时，Kinetic 会优先接管双击空格，优先级高于原版创造模式双击空格飞行；未具备该能力时不拦截原版行为。
- **单击空格**：横向高速飞行时退出当前横向姿态，但不会关闭超人飞行总开关。
- **W / S**：前进 / 后退。
- **A / D**：控制 Roll，不作为左右平移。
- **Shift**：平滑加速，不触发潜行。
- **Ctrl + W**：立即达到当前保存的目标极速。
- **Shift + 鼠标滚轮**：调整目标极速，范围最高 100x。
- **Alt**：自由视角。
- 鼠标左右移动只控制视角，不直接控制飞行方向或 Roll。

超人飞行的 FOV 根据**玩家当前实际移动速度**实时计算，而不是只根据目标速度设置；视觉层使用平滑插值放大/缩小，100x 高速时会显著增强 FOV 效果。横向飞行期间使用与爬行共用的 `KineticPlayerPose` 姿态链来维持真实低矮碰撞尺寸，并与重生、跨维度、重新登录时的临时状态清理配套。

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

架构规则由 `gradle/kinetic-architecture.gradle` 和 `gradle/kinetic-api-verification.gradle` 在构建阶段自动检查。

## API 源码包

工程提供 `apiSourceZip`，包含公开 `api/` 源码、`README.md` 与构建时自动生成的 Javadoc HTML。
