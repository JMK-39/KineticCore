# KineticCore API Architecture

本文档定义 KineticCore 当前源码必须遵守的长期架构边界。目标是让公共 API 可以被核心自身和外部附属共同使用，同时把 Forge/Mixin/具体 Screen/网络实现隐藏在私有实现层。

## 1. 包职责

### `api/`

公开开发接口。附属与 KineticCore Feature 都可以调用。

API 负责：

- GUI 怎么显示、怎么操作、怎么保持统一体验。
- 配置页面与服务端配置协议。
- 网络协议注册与安全 Buffer。
- 压缩。
- 输入与生命周期。
- Tooltip、Overlay、选择器、编辑器。
- 通用 Hook。
- 命令扩展。
- Minecraft 私有能力桥接。
- 通用注册与运行时入口。

### `internal/`

API 的私有实现层。

可以包含：

- Forge Event Bridge。
- Mixin / Accessor。
- Forge 网络 Channel。
- 具体选择器 Screen。
- 配置同步 Packet。
- Overlay Runtime。
- Widget 底层渲染实现。
- 注册表与加载期桥接。

外部附属和 Feature 不直接 import `internal`。

### `feature/`

KineticCore 自身业务功能。

Feature 只依赖：

- `dev.xyat.kineticcore.api.*`
- 自己所属的 Feature 包。
- Minecraft / Forge / 第三方业务类型。

不同 Feature 不通过 Java import 互相耦合。需要共享的数据或能力应通过公开 API、公共协议或稳定数据入口连接。

### `bootstrap/`

负责装配核心功能。它不承担可复用业务能力，也不作为附属 API。

## 2. 依赖方向

```text
feature ─┐
         ├──> api ──> internal ──> Minecraft / Forge
addon ───┘
```

禁止：

```text
feature -> internal
internal -> feature
api -> feature
feature A -> feature B
addon -> internal
```

`api -> internal` 只允许作为私有实现调用。任何 `public` / `protected` API 签名都不能出现 internal 类型。

## 3. 判断通用能力是否应该进入 API

只问一个问题：

> 把这个能力放到另一个完全无关的 Kinetic 附属里，能不能原样使用？

能原样使用的能力应进入 API，例如：

- 按钮、输入框、Toggle、Dropdown。
- RGB/HEX 颜色选择器。
- Tooltip、右键菜单、确认框。
- 滚动列表、TabBar、焦点。
- 屏幕坐标转换、Scissor。
- 数字范围与 validator。
- 自动补全。
- 网络消息注册、Buffer、压缩。
- 客户端/服务端通用生命周期。
- 通用 Hook。

不能原样复用的能力留在业务，例如：

- 某种枪械伤害公式。
- TACZ 特定配方规则。
- 某个 NPC 的任务阶段。
- 某种生物生成公式。
- 某个模组专用的 NBT 业务结构。

API 负责“怎么显示、怎么操作”；业务负责“这个值代表什么”。

## 4. GUI 是强统一边界

GUI 是 KineticCore 最严格的公共层。

### 4.1 固定画布

`KineticScreen` 与 `KineticContainerScreen` 的虚拟画布固定为：

```text
640 × 360
```

API 负责：

- 实际窗口缩放。
- 4K / 高 DPI 适配。
- 画布居中。
- 鼠标坐标转换。
- Scissor 转换。

业务不能创建另一套虚拟画布规格。

`KineticNativeScreen` 使用真实屏幕坐标，只用于原版界面注入或确实需要原生坐标的界面。

### 4.2 标准控件

标准控件必须通过：

```text
KineticScreen.add...
KineticNativeScreen.add...
KineticContainerScreen.add...
KineticWidgets.create...
```

Screen 子类使用 `add...`；Panel/Tab/Helper/外部原版界面注入使用 `KineticWidgets.create...` detached factory。

不要在业务层直接使用：

```text
Button.builder
new EditBox
new MultiLineEditBox
Tooltip.create
new ConfirmScreen
```

### 4.3 控件规则

API 统一决定：

- 普通按钮高度。
- 紧凑按钮高度。
- 输入框高度。
- 边框。
- Hover / Selected / Error / Disabled 状态。
- Placeholder。
- Tooltip。
- Dropdown。
- TabBar。
- 右键菜单。
- 确认框。
- Scissor。
- 焦点。
- 控件添加/移除。
- UI 重建。

业务只传位置、宽度、文本、数据状态、validator 和回调。

### 4.4 Helper 同样遵守 API

Panel、Tab、Editor、Entry、Helper 即使不是 Screen，也不能因此绕过标准控件。使用 `KineticWidgets` 或由父 Kinetic Screen 创建控件。

## 5. 数字与业务验证

API 不擅自决定业务数值规则。

数字控件与配置条目支持：

```text
allowNegative
minValue
maxValue
validator
```

是否允许负数、具体上下限、业务合法性由附属决定。

Long 使用 Long 语义进行精确范围判断；网络配置链也保持整数为 Long，不通过 Double 处理大整数。

字符串、列表、Choice、颜色同样可以提供 validator。

## 6. 配置边界

客户端页面由 `KTConfigPage` 描述。

服务端权威规则由 `KTServerConfigSpec` 描述。

服务器负责：

- 最终校验。
- 最终保存。
- 权限判断。
- 快照同步。

客户端负责：

- 显示镜像。
- 输入即时验证。
- 提交变更。
- 显示服务端拒绝原因。

客户端验证不能替代服务端验证。

Forge CLIENT Config 的互操作集中在 `KTClientConfigAdapter`；普通配置页面不需要知道 `ModContainer`、`ConfigScreenHandler` 或 Forge Event。

## 7. 网络边界

业务使用：

```text
KineticNetwork
NetworkChannel
NetworkCodec
NetworkBuffer
ServerboundSender
ClientboundSender
```

业务不直接处理：

```text
SimpleChannel
FriendlyByteBuf
PacketDistributor
NetworkEvent.Context
NetworkRegistry
```

协议规则：

- 同一 Channel ID 只有一个协议所有者。
- 同 ID 使用不同协议版本属于错误。
- 客户端和服务端协议版本严格匹配。
- 读取与写入都应用显式长度限制。
- 解压必须限制最大输出。

## 8. 生命周期与输入

通用客户端生命周期走 `KineticClientEvents`。

通用服务端生命周期走 `KineticServerEvents`。

按键走 `KineticKeyBindings`。

如果 API 已经覆盖对应能力，Feature 和附属不要再各自注册一套相同 Forge 生命周期监听器或按键注册器。

业务专用事件，例如特定实体受伤、特定方块交互、特定玩法判定，可以继续留在业务层。

## 9. Hook 与 Mixin

Mixin 可以使用，不要求附属放弃业务 Mixin。

规则：

1. 同一模组中不要为同一能力维护重复 Mixin。
2. 能被无关附属原样复用的底层注入能力，应由 KineticCore 提供公开 API/Hook。
3. 明确业务专用的注入可以留在业务模组。
4. 已经存在通用 API 的能力不要再通过另一套 Mixin 重复实现。
5. 普通运行时代码不得直接 import 或强转 `internal.mixin` 包中的类型；需要暴露访问能力时，由正常 API/Bridge 接口定义契约，Mixin 仅负责实现。

公开 Hook 注册使用 `HookRegistration`，调用方在不再需要时可以 `close()` 取消注册。

多个注册者的组合语义由对应 Hook Runtime 明确定义；调用方不能假设“注册成功就独占整个 Hook”。

## 10. 玩家可见文本

玩家可见固定文本必须使用 I18N。

Java 负责：

- 选择语言键。
- 传动态参数。
- 渲染组件。

语言文件负责：

- 中文/英文显示内容。
- 文本语义颜色。

项目禁止使用灰色与深灰色两种原生文本颜色码。

Java 不硬编码玩家文本 `§` 颜色，也不通过 `ChatFormatting` 决定固定玩家文本颜色。

带动态参数的彩色 I18N 文本使用 `KineticI18n` / `KineticText.translatable`，由语言文件中 `%s` 前的 `§` 格式决定参数样式。

GUI 主题颜色、边框、背景、遮罩属于渲染主题，不等同于玩家文本颜色。

## 11. 初始化规则

调用方只注册自己的能力，不管理 KineticCore 内部初始化顺序。

例如：

- `KineticClientRuntime` 首次使用时自行准备客户端运行时。
- `KTServerConfigApi.register(...)` 负责确保服务端配置网络可用。
- `KineticCommands.registerExtension(...)` 负责确保命令 Runtime 可用。

公开 API 不提供“先手动 initialize 某个 internal runtime”这一类调用负担。

## 12. 自动架构检查

`gradle/kinetic-architecture.gradle` 提供 `checkKineticArchitecture`。

它检查：

- 业务代码直接访问 `internal`。
- `internal` 依赖 Feature。
- API 依赖 Feature。
- Feature 跨 Feature import。
- API 对外泄漏 internal 类型。
- API 直接暴露 Forge Event 类型。
- 业务绕过标准 GUI。
- 业务绕过统一网络层。
- 业务重复注册已经统一的按键、生命周期、Tooltip、命令事件。

Mixin/Hook 是否属于真正重复业务能力需要结合语义判断，不用简单“禁止 Mixin”的方式处理。

## 13. 公共 API 文档

- [API_REFERENCE.md](API_REFERENCE.md)：类与主要方法参考。
- [API_TUTORIAL.md](API_TUTORIAL.md)：从零开始的附属接入示例。
