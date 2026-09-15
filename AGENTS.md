# KineticCore 开发规则

先阅读 [KINETIC_API_GUIDE.md](KINETIC_API_GUIDE.md)。公开 API 只保留当前正式入口，不保留旧名称、旧嵌套类型、转发壳或兼容别名。

- 普通 GUI 用 `KineticScreen`；原生坐标用 `KineticNativeScreen`；容器用 `KineticContainerScreen`。
- Screen 内用 `addXxx`；Helper/Tab/Panel/注入用 `KineticWidgets.createXxx`。滚动统一用 `KineticScroll`。
- 三个 Screen 的公共控件行为只改 `KineticScreenControls`，焦点只改 `KineticScreenFocus`。草稿和导航遵循 `GuiSession`。
- 业务不得使用 `Button.builder`、原生输入框构造器、Kinetic 输入框构造器、`Tooltip.create`、原生 Scissor、`clearWidgets`、`this.init()`、原生焦点操作。
- 业务 → 公开 API → `internal`。只有公开 API 实现层可以引用 `internal`；Feature、Bootstrap 业务和附属都不得直接访问 `internal`。
- `internal.mixin` 只能由 Mixin 实现自身引用；普通 runtime 通过访问接口使用注入能力。
- 缺少通用能力先扩展 API；业务专用逻辑留在附属或所属 feature。禁止跨 feature 依赖。
- 给关键 API 写简短中文 JavaDoc，说明注册、Tooltip、坐标、生命周期或返回值约束。
- GUI 改动后用 Gradle 8.8 执行 `check`。检查只验证当前正式 API、架构边界和状态行为，不维护旧 API 二进制兼容基线。
- 玩家固定文本使用 I18N；遵循现有主题和语言文件规则。
