# KineticCore API Reference

开发前先读 [Kinetic 开发地图](KINETIC_API_GUIDE.md)：标准入口、实现位置、草稿生命周期和 API → internal 边界。

公开包根路径：

```text
dev.xyat.kineticcore.api
```

本文件按当前源码列出公开 API。方法存在多个重载时只概括参数差异，具体签名以源码为准。

## 1. Client / Screen

### `KineticScreen`

标准 640×360 虚拟画布 Screen 基类。

核心常量：

```text
STANDARD_CANVAS_WIDTH = 640
STANDARD_CANVAS_HEIGHT = 360
```

子类实现：

```java
protected abstract void buildUi();
```

布局与画布：

```text
canvasWidth
canvasHeight
canvasScale
canvasX
canvasY
safeArea
layout
layoutLevel
toVirtualX / toVirtualY
toScreenX / toScreenY / toScreenRight / toScreenBottom
enableUiScissor / disableUiScissor
```

标准控件：

```text
addButton
addCompactButton
addHighZButton
addCompactHighZButton
addTextField
addMultiLineTextField
addAutoCompleteField
addIntegerAutoCompleteField
addLongAutoCompleteField
addDecimalAutoCompleteField
addIntegerField
addLongField
addDecimalField
addToggleButton
addDropdown
addTabBar
addColorSwatchButton
addColorPreviewButton
addControl
removeControl
addEventListWidget
addScrollableWidget
```

交互：

```text
showTooltip
showFormattedTooltip
showItemTooltip
focusControl
blurControl
clearControlFocus
isControlFocused
openContextMenu
openDialog
rebuildUi
navigateBack
```

### `KineticContainerScreen<T>`

用于 `AbstractContainerMenu` 的标准 Kinetic 容器 Screen。虚拟 UI 同样固定 640×360。

提供与 `KineticScreen` 对齐的标准按钮、输入框、自动补全、数字框、Toggle、Dropdown、Tab、颜色、Tooltip、焦点、右键菜单、确认框、坐标转换与 Scissor API。

只读画布信息：

```text
uiWidth
uiHeight
uiScale
```

### `KineticNativeScreen`

使用真实屏幕坐标的 Kinetic Screen。适合原版界面注入或必须依赖物理像素坐标的界面。

同样提供标准控件、Tooltip、焦点、右键菜单、确认框、坐标转换和 Scissor API。其 `toVirtualX/Y` 与 `toScreenX/Y` 为原生坐标语义。

### `GuiSession`

统一 Screen 会话、父子 Screen、返回、草稿状态和放弃/提交状态。

主要入口：

```text
routeSelectionListWheel
handleScreenOpening
handlePlainScreenEscape
setParent
parentOf
back
isKineticScreen
reserveStandaloneOwner
configureDraft
isStandaloneOwner
enabled
isDirty
commitBaseline
discardToBaseline
```

### `GuiLayout`

响应式布局计算工具。

主要类型：

```text
Level
Aspect
Rect
SafeArea
Metrics
Split
```

主要方法：

```text
measure
rows
columns
splitHorizontal
splitVertical
weightedColumns
weightedRows
```

### `GuiTheme`

统一 Kinetic GUI 主题、标准颜色、边框与控件视觉参数。

### `KineticI18n`

通用 I18N 动态参数样式处理。读取语言文件中 `%s` 前的 `§` 格式并应用到动态参数，避免 `Component` 参数丢失颜色或粗体。可在客户端与服务端共同使用。

主要方法：

```text
translatable
translatableIn
```

### `KineticText`

客户端文本与滚动文本工具。`translatable(...)` 会委托给 `KineticI18n`，因此动态参数同样遵守语言文件中的 `§` 格式。

### `KineticSearch`

通用搜索与文本匹配辅助能力。注册表字典通过 `KineticRegistries` 读取，不要求附属直接接触 Forge 注册表。

### `KineticItemSearch`

统一物品搜索缓存。保留物品 ID、Tag、NBT 唯一键和缓存准备状态，供选择器、自动补全和附属编辑器共同使用。

```text
getUniqueKey
getRegistryTagIds
getItems
isReady
clear
prepareCache
```

## 2. Client / Widgets

### `KineticWidgets`

非 Screen Helper/Panel 使用的 detached factory 与通用控件工具。

标准工厂：

```text
createButton
createCompactButton
createMenuButton
createTextureButton
renderTextureButtonIcon
createTextField
createCompactTextField
createValidatingCompactTextField
createMultiLineTextField
createAutoCompleteField
createIntegerField
createLongField
createDecimalField
createIntegerAutoCompleteField
createLongAutoCompleteField
createDecimalAutoCompleteField
createToggleButton
createColorSwatchButton
createColorPreviewButton
createHighZButton
createCompactHighZButton
createDropdown
createTabBar
attachTooltip
```

按钮状态：

```text
setButtonSelected
setButtonError
isButtonSelected
isButtonError
```

主要公开控件/Helper：

```text
KineticEditBox
ValidationEditBox
NumericEditBox
AutoCompleteBox
NumericAutoCompleteBox
ToggleButton
ColorSwatchButton
ColorPreviewButton
HighZButton
Dropdown
TabBar
Scroll.State
SmoothSelectionList
GridScrollController
EntityPreviewRenderer
LayerState
DragStateController
EditedEntryTracker
```

数字控件支持：

```text
allowNegative
minValue
maxValue
validator
```

Long 类型保持 Long 精度。

## 3. Client / Overlay

### `GuiOverlay`

统一高层覆盖、Tooltip、右键菜单、确认框和 Toast。

右键菜单项：

```text
MenuItem.action
MenuItem.toggle
MenuItem.danger
MenuItem.disabled
MenuItem.separator
```

Tooltip：

```text
tooltip
formattedTooltip
itemTooltip
requestTooltip
requestFormattedTooltip
requestItemTooltip
```

菜单与对话框：

```text
openMenu
closeMenu
openDialog
openCurrentDialog
closeDialog
blocksInput
```

Toast：

```text
toast
removeToast
clearToasts
clearAllToasts
```

## 4. Client / Selectors and Editors

### `KineticSelectors`

统一选择器入口：

```text
prepareItems
itemsReady
openItemSelector
openEntitySelector
openItemListEditor
openNbtEditor
openColorPicker
openPalette
```

### `KineticCommandListEditor`

可复用命令列表编辑器。

入口：

```text
create
```

显示文本通过 `KineticCommandListEditor.Text` 提供。

### `HudPositionEditor`

HUD 拖拽、缩放、重置、快照、恢复以及鼠标/键盘交互辅助。

### `KineticEffectDisplay`

状态效果布局策略：

```text
configure
leftSide
holdTabToExpand
potionItemIcon
compact
```

### `EffectAreaProvider`

向状态效果布局提供可用显示区域。

## 5. Client / Input and Lifecycle

### `KineticKeyBindings`

按键定义与注册。

枚举：

```text
Context: IN_GAME / GUI / UNIVERSAL
Modifier: NONE / SHIFT / CONTROL / ALT
Device: KEYBOARD / MOUSE
```

入口：

```text
builder
register
```

Builder：

```text
category
context
modifier
keyboardKey
mouseButton
registerWhen
enabledWhen
onPressed
exactModifiers
build
register
```

`Binding`：

```text
translationKey
isRegistered
isDown
translatedKeyMessage
```

### `KineticClientEvents`

客户端生命周期。

```text
onTick(START/END)
onLogin
onLogout
onScreenInitBefore
onScreenInitAfter
onScreenInitAfterWithControls
onScreenRenderAfter
onMouseButtonBefore
onLevelRender
onHudRender(HOTBAR/AFTER_CHAT/END)
```

`ScreenInitContext` 可以读取、添加和移除原版/第三方 Screen 的现有 `GuiEventListener`；附属不需要直接调用 Screen 的原生控件管理方法。

所有注册返回 `HookRegistration`。

### `KineticItemTooltips`

物品 Tooltip 生命周期：

```text
onBuild
onRender
onGather
registerComponentFactory
```

`onGather` 可修改高级 Tooltip 元素列表；`registerComponentFactory` 用于把自定义 `TooltipComponent` 映射为客户端渲染组件。

## 6. Configuration

### `KTConfigApi`

客户端配置页注册与界面入口。

```text
register
unregister
find
pages
notifySaved
notifyModuleSaved
canEdit
unavailableReason
createScreen
createScreenForOwner
refreshScreenFromSource
installConfigHub
installConfigScreen
screenAction
```

### `KTConfigPage`

配置页面声明。

页面元数据：

```text
pageDescription
scope
serverManaged
applyTiming
applyNotice
onSave
```

结构项：

```text
section
description
```

值类型：

```text
booleanValue
intValue
longValue
doubleValue
stringValue
longTextValue
choice
translatedChoice
stringList
entityList
itemList
itemRuleList
tickSecondsValue
intList
color
action
```

多数值类型都有可选 validator 重载。

页面 ID 格式：

```text
namespace:path
```

普通 entry ID 使用小写标识形式。

### `KTConfigEntry<T>`

配置页中的一个结构项、值项或操作项。配置 Screen 通过它读取、验证、写入快照并生成对应控件。

通用编辑器可使用 `readSnapshot()`、`defaultSnapshot()`、`snapshot(Object)` 与 `writeSnapshot(Object)` 处理不知道具体泛型类型的配置值；所有写入仍会经过该 Entry 自身的解码、范围与 validator 校验。

### `KTConfigScope`

```text
CLIENT_LOCAL
LOCAL_INSTALLATION
SERVER_AUTHORITATIVE
```

提供：

```text
shortTranslationKey
detailTranslationKey
```

### `KTClientConfigSpec`

公开的客户端本地配置规格。附属只声明配置结构、范围和 validator；底层配置加载器、持久化与 GUI 适配由 KineticCore internal 负责。公开签名不暴露 `ForgeConfigSpec`。

Builder：

```text
comment
translation
push / pop
defineBoolean
defineInt
defineLong
defineDouble
defineString
defineEnum
build
```

值对象：

```text
get
set
defaultValue
```

### `KTClientConfigAdapter`

`KTClientConfigSpec` 与 `KTConfigPage` 的统一适配层。

主要能力：

```text
registerSpec
pageBuilder
appendEntries
inferApplyTiming
```

范围、枚举和业务 validator 会继续传递到实际配置存储与 Kinetic 配置 GUI。

### `KTServerConfigSpec`

服务端权威配置定义。

Builder：

```text
booleanValue
intValue
longValue
doubleValue
colorValue
stringValue
choiceValue
stringList
intList
onSave
afterSave
build
```

值类型支持 validator 重载。

实例：

```text
pageId
snapshot
value
apply
save
applyAndSave
```

### `KTServerConfigApi`

服务端配置注册与运行时读取：

```text
register
registerActionPage
find
isRegistered
getBoolean
getInt
getLong
getDouble
getString
getStringList
```

### `KTServerConfigClient`

客户端服务端配置镜像：

```text
isLoaded
canEdit
revision
loadFailureKey
request
save
savePartial
getBoolean
getInt
getLong
getDouble
getString
getStringList
getIntegerList
```

## 7. Network

### `KineticNetwork`

```text
channel(ResourceLocation)
channel(ResourceLocation, protocolVersion)
transportLimits
configureTransportLimits
```

同一 Channel ID 共享一个统一 Channel；协议版本要求一致。

### `NetworkChannel`

```text
id
registerServerbound
registerClientbound
```

### `PacketChannel`

按消息类型保存发送器的高层 Channel。适合不希望在业务层长期保存多个 Sender 的附属。

```text
create
id
registerServerbound
registerClientbound
sendToServer
sendToPlayer
broadcast
```

### `NetworkCodec<T>`

```text
encode
decode
of
```

### `NetworkBuffers`

在内存 byte[] 与 `NetworkBuffer` 之间编码/解码：

```text
encode
decode
```

### `NetworkBuffer`

基础值：

```text
write/readBoolean
write/readByte
write/readInt
write/readVarInt
write/readLong
write/readFloat
write/readDouble
```

字符串与数组：

```text
writeUtf / readUtf
writeByteArray / readByteArray
writeStringList / readStringList
```

均有默认安全上限，相关类型提供显式上限重载。

Minecraft 类型：

```text
ResourceLocation
UUID
BlockPos
CompoundTag
ItemStack
```

### `ServerboundSender<T>`

```text
send(message)
```

### `ClientboundSender<T>`

```text
send(ServerPlayer, message)
```

### `ServerboundPacketHandler<T>`

```text
handle(message, ServerPacketContext)
```

### `ServerPacketContext`

```text
sender
```

### `NetworkProtocolLimits`

网络字段默认协议限制记录。

### `NetworkTransportLimits`

底层传输层限制记录。

## 8. Compression

### `KineticCompression`

```text
compressUtf8
compressBytes
decompressUtf8
decompressBytes
```

解压方法要求最大解压后大小。

## 9. Hooks

### `HookRegistration`

继承 `AutoCloseable`，`close()` 取消注册。

### `ClientHooks`

```text
onOptionsLoading
onResourceReloadUi
```

### `CommonHooks`

```text
onCrawlPose
onMobPersistence
onRecipeBookRemoval
```

### `ServerHooks`

```text
onSpawnOverride
onWorldDeletion
onDataPackOrder
```

`SpawnOverride` 提供出生点、首次登录放置、重生放置与默认出生点变化等细分回调。

## 10. Commands

### `KineticCommands`

```text
registerExtension
unregisterExtension
registerTopLevel
unregisterTopLevel
```

`registerExtension` 统一维护 `/kt` 根命令、帮助和 reload 聚合；`registerTopLevel` 用于必须保持原独立命令路径的业务命令，不需要附属直接监听 Forge `RegisterCommandsEvent`。

### `CommandExtension`

可实现：

```text
registerCommands
appendHelpItems
reload
```

### `CommandText`

统一 `/kt` 帮助文本、可点击命令和建议文本构建。

## 11. Registry and Resources

### `KineticEntityTypes`

实体类型注册：

```text
register(ResourceLocation, Supplier<EntityType<T>>)
register(namespace, path, Supplier<EntityType<T>>)
```

返回 `KineticRegistryHandle<EntityType<T>>`。

### `KineticItems`

通用 Item 注册，返回 `KineticRegistryHandle<T>`。

```text
register(ResourceLocation, Supplier<T>)
register(namespace, path, Supplier<T>)
```

### `KineticMenuTypes`

通用 `MenuType` 注册。附加打开数据通过公开 `NetworkBuffer` 传入，不向附属暴露 `FriendlyByteBuf` / `IForgeMenuType`。

```text
register(ResourceLocation, Factory<T>)
register(namespace, path, Factory<T>)
```

### `KineticRegistryHandle<T>`

注册句柄，同时是 `Supplier<T>`；提供稳定注册 ID 与最终对象访问。

### `KineticRegistries` / `KineticRegistryView<T>`

只读注册表访问层。当前提供：

```text
items
entityTypes
blocks
mobEffects
attributes
enchantments
custom
```

每个 View 提供：

```text
get
id
values
ids
contains
tagIds
valuesInTag
isInTag
holder
```

附属不再直接依赖 `ForgeRegistries`；第三方自定义 Forge 注册表通过 `custom(registryId)` 获取类型安全视图。

### `KineticClientRenderers`

```text
registerEntityRenderer
```

### `KineticClientMenus`

客户端容器 Screen 注册：

```text
register(menuTypeSupplier, screenFactory)
```

### `KineticItemProperties`

客户端物品模型属性注册：

```text
register(itemSupplier, propertyId, propertyFunction)
```

### `KineticPackSources`

```text
register(PackType, RepositorySource)
register(PackType, Supplier<? extends RepositorySource>)
```

## 12. Runtime

### `KineticClientRuntime`

```text
ensureReady
execute
currentScreen
currentScreen(Class<T>)
openScreen(Screen)
openScreen(Supplier<? extends Screen>)
refreshScreen
refreshCurrentScreen
```

### `KineticModLifecycle`

```text
onCommonSetup
onClientSetup
onLoadComplete
```

### `KineticEnvironment`

```text
isClient
isDedicatedServer
runOnClient
runOnDedicatedServer
```

统一替代附属中的 `DistExecutor` / `FMLEnvironment` 侧别判断。

### `KineticPlatform`

```text
isModLoaded
loadedMods
```

### `KineticPaths`

```text
configDirectory
```

### `KineticCreativeTabs`

创造模式 Tab 的构建事件、只读查询与客户端搜索索引刷新：

```text
onBuildContents
entries
values
get
id
contains
refreshSearch
```

### `KineticServerRuntime`

```text
currentServer
```

### `KineticRuntime`

```text
logger
id
```

并提供核心 `MOD_ID` 常量。

### `KineticFeatureSwitches`

```text
register
isEnabled
configuredEnabled
setConfiguredEnabled
saveConfigured
descriptors
```

功能开关使用稳定业务 ID 和语言键描述，不向用户暴露具体 Mixin 类名。

## 13. Server Lifecycle

### `KineticServerEvents`

```text
onTick(START/END)
onPlayerTick(START/END)
onAboutToStart
onStarted
onStopping
onStopped
onPlayerLogin
onPlayerLogout
onPlayerClone
onPlayerRespawn
onPlayerChangedDimension
onDatapackSync
onChat
```

所有入口都支持 `Priority` 重载；聊天上下文支持取消消息。注册返回 `HookRegistration`。

## 14. World / Living / Inventory

### `KineticWorldEvents`

通用世界事件，均支持 `Priority`：

```text
onLevelLoad
onLevelUnload
onEntityJoin
onEntityLeave
onChunkLoad
onChunkUnload
onBlockBreak
onBlockPlace
onItemPickup
onMobFinalizeSpawn
onBabySpawn
```

可取消的上下文只暴露业务需要的数据与 `cancel()`，不把 Forge Event 对象泄漏给附属。

### `KineticLivingEvents`

通用 Living 生命周期与数值事件：

```text
onTick
onEquipmentChange
onDeath
onHurt
onDamage
onAttack
onPotionApplicable
```

支持优先级、取消、伤害数值修改，以及药水 `DEFAULT / ALLOW / DENY` 结果。

### `KineticChunkLoading`

```text
setForced
```

统一处理模组拥有者的强制区块加载/释放。

### `KineticInventorySlots`

```text
isPlayerInventorySlot
isPlayerInventoryHandler
```

用于业务判断玩家背包 Slot/Handler，不要求附属识别 Forge wrapper 实现类。

## 15. Minecraft Helpers

### `MinecraftAttributes`

```text
setRange
```

### `MinecraftContainers`

```text
hoveredSlot
left
top
```

### `MinecraftKeys`

```text
setDefault
defaultModifier
modifier
```

修饰键使用 `KineticKeyBindings.Modifier`。

### `MinecraftScreens`

```text
parent
```

## 16. Flight

### `KineticFlight`

服务端/公共飞行状态：

```text
installNoclipSyncSender
isDebouncing
setDebouncing
sources
addSource
removeSource
setLastKnownFlying
lastKnownFlying
isFlightAllowed
refresh
noclipEnabled
copyPersistentState
serverNoclipEnabled
applyServerNoclip
syncServerNoclip
```

### `KineticFlightClient`

客户端飞行状态：

```text
installSpeedModifierState
installNoclipRequestHandler
isSpeedModifierDown
noclipEnabled
requestNoclip
applyServerNoclip
applyLocalNoclip
flightSpeedMultiplier
setFlightSpeedMultiplier
inertiaEnabled
setInertiaEnabled
```

## 17. Monitoring

### `KineticServerPerformance`

```text
tracker
tps
```

### `ServerTickTracker`

```text
addTick
getStats
getLatestMspt
tps
```
