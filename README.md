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

## Which API to Use

Start here before writing addon code. Every row is an existing entry point; builders are called on the `KineticUi` passed to `build(KineticUi ui)`.

| I need | Use |
|---|---|
| A screen | `extends KineticPage` + `build(KineticUi ui)`; open it with `KineticGui.open(page)` or, from another page, `openChild(page)` |
| A container menu screen | `extends KineticContainerPage<M>` + `KineticClientMenus.register(...)`; the menu type comes from `KineticMenuTypes` |
| A button | `ui.button(x, y, w)`, `.compact()` for the narrow style; `ui.itemButton(...)` for an item icon button |
| An on/off switch | `ui.toggle(...)` |
| Text input | `ui.textField(...)`, multi-line `ui.textArea(...)` |
| Number input (integer or decimal, min/max, negatives) | `ui.numberField(...)` |
| Input with suggestions | `ui.autoComplete(...)`, for numbers `ui.numberAutoComplete(...)` |
| A choice | `ui.dropdown(...)`, `ui.cycleButton(...)` or `ui.slider(...)` |
| A colour | `ui.colorButton(...)` / `ui.colorSwatch(...)`; picker `KineticSelectors.openColorPicker(...)` |
| Tabs | `ui.tabBar(...)` (fixed) or `ui.tabStrip(...)` (scrollable) |
| A list of text rows | `ui.selectionList(...)` |
| Items as a list or a grid | `ui.itemSelectionList(...)` / `ui.itemGrid(...)` |
| Rows with trailing buttons or toggles | `ui.actionList`, `ui.itemActionList`, `ui.multiActionList`, `ui.toggleList`, `ui.toggleActionList`, `ui.multiToggleList` |
| Rows the page draws itself | `KineticRowList<T>`, added with `ui.add(...)` |
| Rows of real controls that scroll together | `ui.scrollViewport(...)` |
| A control the API does not have | first check whether it is generic and belongs in the API; otherwise `KineticCustomControl` + `ui.add(...)` |
| Confirm dialog, right-click menu, tooltip | page `openDialog(...)`, `openContextMenu(...)` with `KineticOverlays.MenuItem`, builder `.tooltip(...)` or page `showTooltip(...)` |
| Pick an item, entity, NBT or colour | `KineticSelectors.open*` |
| Let players move a HUD element | `KineticHudEditorPage` |
| Draw on the HUD | `KineticClientEvents.onHudRender(...)` |
| Draw icons or custom rows in an item tooltip | `KineticItemTooltips.registerComponentFactory(...)` with a `KineticTooltipComponent` |
| Add a button to a vanilla or third-party screen | `KineticClientEvents.onScreenInitAfter(...)` |
| Drawing, theme colours, text width | `KineticGraphics`, `KineticTheme`, `KineticText` |
| Labels that follow world positions (name tags, damage numbers) | `KineticWorldRender.beginScreenOverlay(context)` inside `KineticClientEvents.onLevelRender(...)`; never `RenderSystem` / `PoseStack` |
| Secondary (muted) text | `KineticTheme.muted(text)`; never the gray `§7` / `§8` codes |
| Input text cyan while unchanged, green once edited | builder `.defaultText(...)` / `.defaultValue(...)`, or `.firstShownTextAsDefault()` when the value is loaded after building |
| Player-visible text | `KineticI18n.translatable(...)` with keys in `zh_cn` and `en_us` |
| An F6 config page | `KTConfigPage.builder(...)` + `KTConfigApi.register(...)`; server-owned values `KTServerConfigSpec` |
| Network packets | `PacketChannel` (or `KineticNetwork.channel(...)`) |
| Commands | `KineticCommands`; clickable help lines `CommandText.suggest(...)` |
| Chat text that runs, suggests, copies or opens a link when clicked | `CommandText.clickToRun / clickToSuggest / clickToCopy / clickToOpenUrl`; never `new ClickEvent(...)` |
| Key bindings | `KineticKeyBindings` |
| Client or server side, config folder | `KineticPlatform`; config files `KineticPaths` |
| Registries and IDs | `KineticRegistries`, `KineticResourceIds` |
| Flight permission | `KineticFlightSources` |

Before adding API: first try a new parameter on an existing builder or interface, then a combination of existing controls. Add a new API type only for a new interaction model that an unrelated addon could use unchanged.

## Runtime Environment

- Minecraft 1.20.1
- Forge 47.4.x
- Java 17
- Gradle 8.8 (the pinned source-build version)
- GitHub Releases are published manually; local builds do not create, upload, or publish a release.
- No third-party mod integrations in the core. Curios, JEI and similar integrations belong in optional compat addons, which plug in through `KineticSelectors.registerInventorySource` and `KineticHoveredItems.register`.

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

GUI v2 is page-based. Addons extend `KineticPage` (a plain object, not a vanilla `Screen`); internal host screens handle canvas scaling, 4K, coordinate conversion, scissoring, tooltips, context menus, dialogs, focus, layers, drafts, themes and scrollbar hover/middle-click dispatch. Page code never touches `GuiGraphics`, `Font`, raw input callbacks or texture `ResourceLocation`s, so the upcoming MC 26.1 rendering and input changes stay inside KineticCore.

- `KineticPage`: `PageLayout.CANVAS` (640×360 virtual canvas, default) or `PageLayout.NATIVE` (GUI-scaled screen coordinates).
- `KineticContainerPage<M>`: container-menu page, registered with `KineticClientMenus.register(type, XPage::new)`.
- `KineticHudEditorPage`: HUD drag/scale/position editor.
- `KineticGui`: `open`, `openChild`, `closeScreen`, `currentPage`, plus `captureNavigationParent` for pages opened after an asynchronous server response.

Controls are created with builders in `build(KineticUi ui)`: buttons (compact, card, item), toggles, cycle buttons, colour buttons/swatches, sliders, text fields, text areas, number fields (`NumberType.INT/LONG/DECIMAL`), text and numeric autocomplete, dropdowns, tab bars, tab strips, selection/item/action/toggle lists and item grids. `.layer(n)` replaces the old high-z variants, and `ui.scrollViewport(...)` scrolls real controls with a custom list. Controls are exposed as interfaces (`KineticButton`, `KineticTextField`, `KineticSelectionList`, …).

- Drawing: `KineticGraphics` (shapes, text, scrolling text, items including translucent item icons, textures via `KineticTexture`, effect icons, transforms, layers, clipping) and `KineticTheme` (surfaces, panels, slots, outlines, flash, palette).
- Input: `MouseInput`, `MouseDragInput`, `ScrollInput`, `KeyInput`, `CharInput`; capture (`onMouseClickCapture`) runs before controls, bubble hooks after.
- Custom UI: `KineticCustomControl` for self-drawn controls, `KineticRowList<T>` for self-drawn lists with the core scrollbar, middle-click jump and selection flash, `KineticScrollController` for hand-written scroll areas, `KineticCommandAssist` for command input with suggestions.
- Text measuring: `KineticText`; translations: `KineticI18n`.

`KineticItemGrid` uses `ItemGridItem.outline` with `ItemGridOutline.SUCCESS` or `ItemGridOutline.WARNING` for common state outlines; the active theme supplies their colors. Grid states use full borders. An error red border takes priority over a hovered blue border, which takes priority over selection and business-state borders. Items with no state use a white border.

The full old→new mapping is in `docs/KineticCore-GUI-API-v2-Migration.md`.

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
- `KineticOverlays`: tooltips, context menus, confirmation dialogs, toasts, and high-z overlays.
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

All selectors use the current page or screen as their parent. `KineticCommandListEditor` provides reusable command-list editing (`open`, `action`). `KineticHudEditorPage` provides HUD dragging, scaling, and position editing.

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

Architecture rules run automatically during the build through `gradle/kinetic-architecture.gradle` and `gradle/kinetic-api-verification.gradle`. Addons get the same hard rules from `gradle/kinetic-addon-architecture.gradle`: copy it into the addon and `apply from` it, and `compileJava` fails when addon code bypasses the GUI API (details in `docs/KineticCore-GUI-API-v2-Migration.md`, section 12).

Regression checks come in four groups:

- No Minecraft needed: `checkKineticHeadlessRegressions`, runs in every build.
- Minecraft and Forge classes, no bootstrap: `checkKineticMinecraftRegressions`. Checks that keep static startup state run in their own JVM, in a working directory under `build/tmp`. Part of `check`, so it runs in every build.
- Minecraft bootstrap needed: `KineticScrollStateRegression`, `SmoothSelectionListLifecycleRegression`.
- Forge mod loading context needed: `SearchEnglishDisplayRegression`, `KTServerConfigGetterRegression`, `ServerConfigDecimalWireRegression`, `HookRegistrationIntegrationRegression`, `GuiDraftConfigureFailureRegression`, `GuiRootNavigationRegression`, `GuiSessionLifecycleRegression`, `GuiSessionNavigationRegression`.

The last two groups have no automated host yet. Do not make runtime code tolerate a missing game environment just to run them in a plain JVM.

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

## 该用哪个 API

写附属代码前先查这张表。每一行都是现有入口；控件都在 `build(KineticUi ui)` 收到的 `KineticUi` 上创建。

| 我要 | 用 |
|---|---|
| 一个界面 | `extends KineticPage` + `build(KineticUi ui)`；用 `KineticGui.open(page)` 打开，从另一个页面打开用 `openChild(page)` |
| 容器（Menu）界面 | `extends KineticContainerPage<M>` + `KineticClientMenus.register(...)`；Menu 类型用 `KineticMenuTypes` |
| 按钮 | `ui.button(x, y, w)`，紧凑样式加 `.compact()`；物品图标按钮 `ui.itemButton(...)` |
| 开关 | `ui.toggle(...)` |
| 文本输入 | `ui.textField(...)`，多行 `ui.textArea(...)` |
| 数字输入（整数或小数、最小/最大值、负数） | `ui.numberField(...)` |
| 带补全的输入 | `ui.autoComplete(...)`，数字用 `ui.numberAutoComplete(...)` |
| 选项 | `ui.dropdown(...)`、`ui.cycleButton(...)` 或 `ui.slider(...)` |
| 颜色 | `ui.colorButton(...)` / `ui.colorSwatch(...)`；取色器 `KineticSelectors.openColorPicker(...)` |
| 标签页 | `ui.tabBar(...)`（固定）或 `ui.tabStrip(...)`（可滚动） |
| 文字列表 | `ui.selectionList(...)` |
| 物品列表或网格 | `ui.itemSelectionList(...)` / `ui.itemGrid(...)` |
| 行尾带按钮或开关的列表 | `ui.actionList`、`ui.itemActionList`、`ui.multiActionList`、`ui.toggleList`、`ui.toggleActionList`、`ui.multiToggleList` |
| 页面自绘的行 | `KineticRowList<T>`，用 `ui.add(...)` 加入 |
| 一起滚动的多行真实控件 | `ui.scrollViewport(...)` |
| API 没有的控件 | 先判断是不是通用能力、该不该进 API；不是才用 `KineticCustomControl` + `ui.add(...)` |
| 确认框、右键菜单、Tooltip | 页面 `openDialog(...)`、`openContextMenu(...)`（配 `KineticOverlays.MenuItem`）、Builder 的 `.tooltip(...)` 或页面 `showTooltip(...)` |
| 选择物品、实体、NBT 或颜色 | `KineticSelectors.open*` |
| 让玩家拖动 HUD 位置 | `KineticHudEditorPage` |
| 在 HUD 上绘制 | `KineticClientEvents.onHudRender(...)` |
| 在物品提示框里画图标或自定义行 | `KineticItemTooltips.registerComponentFactory(...)` + `KineticTooltipComponent` |
| 给原版或第三方界面加按钮 | `KineticClientEvents.onScreenInitAfter(...)` |
| 绘制、主题颜色、文字宽度 | `KineticGraphics`、`KineticTheme`、`KineticText` |
| 跟随世界位置的标签（名牌、伤害数字） | 在 `KineticClientEvents.onLevelRender(...)` 里用 `KineticWorldRender.beginScreenOverlay(context)`；不要直接用 `RenderSystem` / `PoseStack` |
| 次要（灰色）文字 | `KineticTheme.muted(text)`；不要用 `§7` / `§8` |
| 输入框未改动时青色、改动后绿色 | 构建器 `.defaultText(...)` / `.defaultValue(...)`；数值在构建后才载入时用 `.firstShownTextAsDefault()` |
| 玩家可见文字 | `KineticI18n.translatable(...)`，语言键同时写 `zh_cn` 和 `en_us` |
| F6 配置页 | `KTConfigPage.builder(...)` + `KTConfigApi.register(...)`；服务器端数值用 `KTServerConfigSpec` |
| 网络包 | `PacketChannel`（或 `KineticNetwork.channel(...)`） |
| 命令 | `KineticCommands`；可点击的帮助行 `CommandText.suggest(...)` |
| 聊天里点击后执行、填入、复制或打开链接的文字 | `CommandText.clickToRun / clickToSuggest / clickToCopy / clickToOpenUrl`；不要自己 `new ClickEvent(...)` |
| 按键 | `KineticKeyBindings` |
| 判断客户端/服务端、配置目录 | `KineticPlatform`；配置文件读写 `KineticPaths` |
| 注册表和 ID | `KineticRegistries`、`KineticResourceIds` |
| 飞行权限 | `KineticFlightSources` |

新增 API 之前：先尝试给现有 Builder 或接口加一个参数，再尝试组合现有控件。只有出现一个完全无关的附属也能原样使用的新交互方式时，才新增 API 类型。

## 运行环境

- Minecraft 1.20.1
- Forge 47.4.x
- Java 17
- Gradle 8.8（源码构建固定版本）
- GitHub Release 仅手动发布；本地构建不会自动创建、上传或发布 GitHub Release
- 核心不内置任何第三方模组联动。Curios、JEI 等联动放在可选兼容附属中，通过 `KineticSelectors.registerInventorySource` 与 `KineticHoveredItems.register` 接入。

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

GUI v2 采用页面模型。附属继承 `KineticPage`（普通对象，不是原版 `Screen`），由内部宿主界面负责画布缩放、4K、坐标转换、Scissor、Tooltip、右键菜单、确认框、焦点、层级、草稿、主题以及滚动条悬停提示/中键跳转。页面代码不接触 `GuiGraphics`、`Font`、原始输入回调与贴图 `ResourceLocation`，为 MC 26.1 的渲染与输入改动预留隔离层。

- `KineticPage`：`PageLayout.CANVAS`（640×360 虚拟画布，默认）或 `PageLayout.NATIVE`（GUI 缩放后的屏幕坐标）。
- `KineticContainerPage<M>`：容器菜单页面，通过 `KineticClientMenus.register(type, XPage::new)` 注册。
- `KineticHudEditorPage`：HUD 拖拽、缩放与位置编辑页面。
- `KineticGui`：`open`、`openChild`、`closeScreen`、`currentPage`，以及用于异步服务端回包后打开子页的 `captureNavigationParent`。

控件在 `build(KineticUi ui)` 中以构建器创建：按钮（紧凑、卡片、物品）、开关、循环按钮、颜色按钮/色块、滑块、文本框、多行文本、数字框（`NumberType.INT/LONG/DECIMAL`）、文本与数字自动补全、下拉框、标签栏、可滚动标签条、选择/物品/操作/开关列表与物品网格。`.layer(n)` 取代旧的高层（HighZ）变体，`ui.scrollViewport(...)` 让真实控件随自绘列表滚动。控件以接口形式公开（`KineticButton`、`KineticTextField`、`KineticSelectionList` 等）。

- 绘制：`KineticGraphics`（图形、文字、滚动文字、物品（含透明物品图标）、`KineticTexture` 贴图、效果图标、变换、层级、裁剪）与 `KineticTheme`（表面、面板、槽位、描边、闪烁、调色板）。
- 输入：`MouseInput`、`MouseDragInput`、`ScrollInput`、`KeyInput`、`CharInput`；`onMouseClickCapture` 在控件之前执行，其余鼠标钩子在控件之后执行。
- 自定义界面：`KineticCustomControl` 自绘控件；`KineticRowList<T>` 自绘行列表（自带核心滚动条、中键跳转与选中闪烁）；`KineticScrollController` 手写滚动区域；`KineticCommandAssist` 命令输入补全。
- 文本测量用 `KineticText`，翻译用 `KineticI18n`。

`KineticItemGrid` 的 `ItemGridItem.outline` 使用 `ItemGridOutline.SUCCESS` 或 `ItemGridOutline.WARNING` 表示通用业务状态描边，并由当前主题提供颜色。网格状态统一用完整边框，
错误红框优先于悬停蓝框，悬停蓝框优先于选中框和业务状态框，没有状态时显示白框。

完整的新旧 API 对照见 `docs/KineticCore-GUI-API-v2-Migration.md`。

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
- `KineticOverlays`：Tooltip、右键菜单、确认框、Toast 和高层覆盖。
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

所有选择器都以当前页面或界面作为父界面。`KineticCommandListEditor` 提供可复用的命令列表编辑能力（`open`、`action`）；`KineticHudEditorPage` 提供 HUD 拖拽、缩放和位置编辑能力。

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

架构规则由 `gradle/kinetic-architecture.gradle` 和 `gradle/kinetic-api-verification.gradle` 在构建阶段自动检查。附属使用 `gradle/kinetic-addon-architecture.gradle` 获得同样的硬性规则：把它复制到附属并 `apply from`，附属代码绕过 GUI API 时 `compileJava` 直接失败（详见 `docs/KineticCore-GUI-API-v2-Migration.md` 第 12 节）。

回归检查分四组：

- 不需要 Minecraft：`checkKineticHeadlessRegressions`，每次构建都会运行。
- 需要 Minecraft 和 Forge 类，但不需要 bootstrap：`checkKineticMinecraftRegressions`。会保留静态启动状态的检查在独立 JVM 中运行，工作目录位于 `build/tmp` 下。已接入 `check`，每次构建都会运行。
- 需要 Minecraft bootstrap：`KineticScrollStateRegression`、`SmoothSelectionListLifecycleRegression`。
- 需要 Forge 模组加载环境：`SearchEnglishDisplayRegression`、`KTServerConfigGetterRegression`、`ServerConfigDecimalWireRegression`、`HookRegistrationIntegrationRegression`、`GuiDraftConfigureFailureRegression`、`GuiRootNavigationRegression`、`GuiSessionLifecycleRegression`、`GuiSessionNavigationRegression`。

后两组暂时没有自动运行的宿主。不要为了让它们在普通 JVM 里通过而让运行时代码容忍缺失的游戏环境。

## API 源码包

工程提供 `apiSourceZip`，包含公开 `api/` 源码、`README.md` 与构建时自动生成的 Javadoc HTML。
