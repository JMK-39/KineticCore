# Kinetic 开发地图

先按使用场景找公开入口，再沿 JavaDoc 查实现。所有附属统一使用当前正式 API；旧名称、旧嵌套类型和转发兼容壳不再保留。

| 场景 | 标准入口 |
| --- | --- |
| 普通界面，640×360 虚拟画布 | `KineticScreen`，实现 `buildUi()` |
| 原生屏幕坐标界面 | `KineticNativeScreen` |
| 容器界面 | `KineticContainerScreen` |
| Screen 内创建控件 | `addButton`、`addTextField`、`addIntegerField` 等 `addXxx` |
| Helper / Tab / Panel / 原版界面注入 | `KineticWidgets.createXxx` |
| 注册已创建的控件 | 宿主 Screen 的 `addControl(widget, tooltip)` |
| 滚动条计算与绘制 | `KineticScroll.xxx()` |
| 滚动状态与列表 | `KineticScroll.State`、`KineticScroll.GridScrollController`、`KineticScroll.SmoothSelectionList` |
| Tooltip / 菜单 / 对话框 | Screen 的 `showTooltip`、`openContextMenu`、`openDialog` |
| 焦点 / 移除 / 重建 | `focusControl`、`removeControl`、`rebuildUi` |
| 打开 / 刷新界面、客户端任务 | `KineticClientRuntime.openScreen` / `refreshScreen` / `execute` |
| 客户端配置规格与配置页 | `KTClientConfigSpec` + `KTClientConfigAdapter` + `KTConfigApi` |
| 服务端权威配置 | `KTServerConfigApi` + `KTServerConfigSpec` |
| 网络与压缩 | `PacketChannel` / `NetworkChannel` / `NetworkBuffers` / `KineticCompression` |
| 客户端 / 服务端生命周期 | `KineticClientEvents` / `KineticServerEvents` / `KineticModLifecycle` |
| 世界 / Living 事件 | `KineticWorldEvents` / `KineticLivingEvents` |
| 按键 | `KineticKeyBindings` |
| 物品 / 菜单 / 实体注册 | `KineticItems` / `KineticMenuTypes` / `KineticEntityTypes` |
| 注册表只读访问 | `KineticRegistries` / `KineticRegistryView` |
| 客户端菜单与物品属性 | `KineticClientMenus` / `KineticItemProperties` |
| 创造模式 Tab | `KineticCreativeTabs` |
| 模组检测 / 物理端 / 配置路径 | `KineticPlatform` / `KineticEnvironment` / `KineticPaths` |
| 功能开关 | `KineticFeatureSwitches`，配置使用稳定功能 ID，不暴露 Mixin 类名 |
| 强加载区块 / 玩家背包槽判断 | `KineticChunkLoading` / `KineticInventorySlots` |
| 物品搜索 / 选择器 | `KineticItemSearch` / `KineticSelectors` |
| Tooltip 高级组件 | `KineticItemTooltips.onGather` / `registerComponentFactory` |
| `/kt` 子命令与独立顶层命令 | `KineticCommands.register` / `registerTopLevel` |

## add 与 create

`addXxx` 创建并注册控件，Screen 统一管理输入、渲染和 Overlay Tooltip。

`createXxx` 只创建，不注册，也不自动接入宿主的生命周期。它的 Tooltip 通过控件自身显示。
把 detached 控件加入 Kinetic Screen 时，建议工厂的 Tooltip 传 `null`，再通过
`addControl(widget, tooltip)` 登记 Screen Tooltip，避免两套 Tooltip 同时显示。
`registerWidgetTooltip(widget, null)` 只移除 Screen 的登记，不清除控件自身 Tooltip。

自动补全控件的建议列表仍需接入 `AutoCompleteBoxGroup` 的渲染、鼠标和键盘路由；工厂不负责绑定这些生命周期。
`addEventListWidget` 只登记事件，列表渲染由调用方安排。

## 重建、草稿与返回

- 在 `buildUi()` 创建控件，内容变化后调用 `rebuildUi()`。重建会清理旧控件和 Tooltip，不重新配置草稿基线。
- 用 `enableUiScissor` / `disableUiScissor`，传当前 Screen 的 UI 坐标，在 `finally` 中结束裁剪。
- `configureDraft(capture, restore)` 记录基线。快照应独立、不可随编辑原地改变，并支持有意义的 `equals`。
- 普通子界面可以共享父界面草稿；共享边界内导航不丢弃草稿。只有所有者能用 `commitDraft()` 更新保存基线。
- 保存顺序：业务校验与持久化成功 → `commitDraft()` → 按业务需要返回。`commitDraft()` 本身不写文件、不发包。
- 取消可调用 `discardDraft()` 恢复基线，然后 `navigateBack()`；ESC 通过 `onClose()` 返回，离开共享会话时回滚未提交修改。
- 需要独立保存边界时用 `configureStandaloneDraft`；原生界面可在构造时用 `reserveStandaloneDraft` 防止打开时继承父草稿。
- `openDialog` 只管理模态交互，保存与回滚由传入的确认/取消回调决定。

## 依赖边界

```text
附属 / KineticCore feature
          ↓
      公开 API
          ↓
       internal
          ↓
   Minecraft / Forge
```

这是 Kinetic 实现依赖方向；公开签名仍可使用 Minecraft 的 `Component`、`Screen` 等数据类型。

业务不得直接引用 `internal`。公开 API 自身可以直接连接 internal 私有实现，但公开方法、字段和类型签名不得泄露 internal 类型。客户端 API 只连接客户端 internal，实现层仍需遵守物理端隔离，避免服务端提前触碰客户端类。
Screen 初始化只调用 `KineticClientRuntime.ensureReady()`；业务通常无需手动初始化。

`internal.mixin` 是最底层注入实现。沿用项目更严格的规则：连普通 internal runtime 也不直接引用 Mixin 类型；
通过正常 API 或 internal 访问接口定义契约，由 Mixin 实现。附属专用 Mixin 可留在附属，但已有通用 API 的能力不要重复注入。

业务禁止直接使用 `Button.builder`、`new EditBox`、`new NumericEditBox`、`Tooltip.create`、原生 Scissor、
`clearWidgets`、`this.init()`、原生焦点操作，以及直接引用 `internal` / `internal.mixin`。
已有公开能力时，也不要再直接使用 `SimpleChannel`、`FriendlyByteBuf`、`PacketDistributor`、`DeferredRegister`、
`ForgeRegistries`、`BuiltInRegistries`、`RegisterCommandsEvent`、`FMLJavaModLoadingContext`、`DistExecutor`、
`FMLEnvironment`、`FMLPaths`、`ForgeChunkManager` 或 Forge 的通用生命周期事件。第三方模组自身的事件、能力接口、
Mixin 目标方法签名属于第三方契约，可以保留在附属，禁止为了“清零 Forge import”让 KineticCore 反向依赖具体第三方模组。
缺少可通用复用的能力先扩展 API；业务专用规则留在附属或自己的 feature。

## 查实现的位置

| 能力 | 实现文件（`api/client/` 下） |
| --- | --- |
| 按钮、Toggle、颜色按钮 | `widget/button/KineticButtons.java` |
| 文本、验证、多行文本 | `widget/input/KineticTextFields.java` |
| 整数、长整数、小数 | `widget/input/KineticNumericFields.java` |
| 文本与数字自动补全 | `widget/input/KineticAutoComplete.java` |
| 滚动条、平滑滚动、滚动列表 | `widget/scroll/KineticScroll.java` |
| 下拉框、Tab | `widget/selection/KineticDropdowns.java`、`KineticTabs.java` |
| 实体预览 | `widget/render/KineticEntityPreview.java` |
| 拖拽、编辑排序、图层状态 | `widget/state/` |
| 三个 Screen 的控件与焦点 | `screen/KineticScreenControls.java`、`KineticScreenFocus.java` |
| Overlay 与草稿生命周期 | `overlay/GuiOverlay.java`、`screen/GuiSession.java` 的 `DraftSession` |
| 客户端配置规格 | `config/client/KTClientConfigSpec.java`、`KTClientConfigAdapter.java` |
| 网络、缓冲、压缩 | `network/PacketChannel.java`、`NetworkBuffers.java`、`KineticCompression.java` |
| 客户端事件与高级 Tooltip | `client/event/KineticClientEvents.java`、`client/tooltip/KineticItemTooltips.java` |
| 服务端 / 世界 / Living 事件 | `server/event/KineticServerEvents.java`、`world/event/KineticWorldEvents.java`、`entity/event/KineticLivingEvents.java` |
| 注册与注册表 | `registry/KineticItems.java`、`KineticMenuTypes.java`、`KineticRegistries.java` |
| 创造模式 Tab、环境、路径、功能开关 | `runtime/KineticCreativeTabs.java`、`KineticEnvironment.java`、`KineticPaths.java`、`KineticFeatureSwitches.java` |
| 区块加载与背包槽 | `world/chunk/KineticChunkLoading.java`、`inventory/KineticInventorySlots.java` |
| 物品搜索与选择器 | `client/search/KineticItemSearch.java`、`client/selector/KineticSelectors.java` |

这些分组就是当前正式公开类型的位置。Screen 内优先使用 `addXxx`，Helper / Tab / Panel / 原版界面注入使用 `KineticWidgets.createXxx`；需要保存控件类型或调用类型专属能力时，直接引用对应分包中的正式类型。
API 不提供旧嵌套类型、旧包名、旧方法别名或转发兼容壳。

## 验证与分发

运行 Gradle 8.8 的 `check`：包含架构检查和当前正式 API 的状态回归检查。
API 发生重构时同步迁移核心与所有附属，不维护旧 API 二进制兼容基线。
这些检查不代替 Minecraft 客户端内的渲染、Tooltip、焦点与容器交互验收。

`apiJar` / `apiSourceZip` 只发布公开 `api/`；internal 不进入公开源码包。它们用于附属编译和阅读，运行时仍安装完整 KineticCore。
详细方法见 [API_REFERENCE.md](API_REFERENCE.md)，示例见 [API_TUTORIAL.md](API_TUTORIAL.md)，架构说明见 [API_ARCHITECTURE.md](API_ARCHITECTURE.md)。
