# KineticCore API Tutorial

开发前先读 [Kinetic 开发地图](KINETIC_API_GUIDE.md)：标准入口、实现位置、草稿生命周期和 API → internal 边界。

本教程面向编写 Kinetic 附属的开发者。示例只使用公开 `dev.xyat.kineticcore.api.*`。

## 源码构建环境

- Minecraft 1.20.1
- Forge 47.4.x
- Java 17
- Gradle 8.8（固定版本）

工程通过 `gradle/wrapper/gradle-wrapper.properties` 固定 Gradle 8.8；不要使用 Gradle 9.x。

## 1. 基本原则

附属源码可以依赖：

```text
dev.xyat.kineticcore.api.*
```

不要直接 import：

```text
dev.xyat.kineticcore.internal.*
```

如果需要一个完全通用的 GUI、输入、配置、网络、Hook 或编辑器能力，而 API 还没有，应优先把该通用能力补进 KineticCore API。

明确属于附属自身业务的数据结构、公式和玩法逻辑留在附属。

## 2. 创建标准 640×360 Screen

```java
package example.client;

import dev.xyat.kineticcore.api.client.screen.KineticScreen;
import net.minecraft.network.chat.Component;

public final class ExampleScreen extends KineticScreen {
    public ExampleScreen() {
        super(Component.translatable("gui.example.title"));
    }

    @Override
    protected void buildUi() {
        addButton(
                24,
                24,
                120,
                Component.translatable("gui.example.save"),
                Component.translatable("gui.example.save.tip"),
                this::save
        );
    }

    private void save() {
    }
}
```

虚拟坐标永远按 640×360 编写。API 自动处理实际窗口分辨率、4K 缩放和居中。

打开 Screen：

```java
KineticClientRuntime.openScreen(ExampleScreen::new);
```

返回父界面：

```java
navigateBack();
```

不要直接调用 `Minecraft.setScreen(...)`。

## 3. 添加标准输入框

### 普通文本框

```java
var nameField = addTextField(
        24,
        60,
        160,
        Component.translatable("gui.example.name"),
        Component.translatable("gui.example.name.placeholder"),
        value -> !value.isBlank(),
        Component.translatable("gui.example.name.tip")
);
```

### 整数框

```java
var amountField = addIntegerField(
        24,
        90,
        120,
        Component.translatable("gui.example.amount"),
        true,
        -100,
        100,
        value -> value.intValue() != 13,
        Component.translatable("gui.example.amount.tip")
);
```

`allowNegative`、最小值、最大值和 validator 都由业务决定。API 不会自动把负数禁掉。

### Long

```java
var longField = addLongField(
        24,
        120,
        180,
        Component.translatable("gui.example.long_value"),
        true,
        Long.MIN_VALUE,
        Long.MAX_VALUE,
        value -> true,
        Component.translatable("gui.example.long_value.tip")
);
```

Long 范围判断使用 Long 精度。

### 自动补全

```java
var field = addAutoCompleteField(
        24,
        150,
        180,
        Component.translatable("gui.example.id"),
        Component.translatable("gui.example.id.placeholder"),
        () -> List.of("minecraft:stone", "minecraft:dirt"),
        Component.translatable("gui.example.id.tip")
);
```

数字自动补全分别使用：

```text
addIntegerAutoCompleteField
addLongAutoCompleteField
addDecimalAutoCompleteField
```

## 4. Toggle、Dropdown、Tab 和颜色

Toggle：

```java
var toggle = addToggleButton(
        24,
        24,
        140,
        true,
        Component.translatable("gui.example.enabled.on"),
        Component.translatable("gui.example.enabled.off"),
        Component.translatable("gui.example.enabled.tip"),
        value -> true,
        value -> onEnabledChanged(value)
);
```

Dropdown 推荐把“配置存储值”和“玩家显示值”分离。配置页面里优先使用 `translatedChoice(...)`。

颜色预览：

```java
var color = addColorPreviewButton(
        24,
        54,
        100,
        0xFF8800,
        Component.translatable("gui.example.color"),
        Component.translatable("gui.example.color.tip"),
        this::openColorEditor
);
```

Tab 使用 `addTabBar(...)`；每个 Tab 可以有独立 Tooltip。

## 5. Panel / Helper 中创建控件

Helper 不是 Screen 时，不要直接 `new EditBox` 或 `Button.builder`。

使用 detached factory：

```java
Button button = KineticWidgets.createButton(
        20,
        20,
        100,
        Component.translatable("gui.example.action"),
        Component.translatable("gui.example.action.tip"),
        ignored -> runAction()
);

KineticWidgets.setButtonSelected(button, true);
```

数字框、Toggle、Dropdown、颜色按钮、Tab、高层按钮都有对应 `KineticWidgets.create...`。

## 6. Scissor 与坐标转换

普通 Kinetic Screen：

```java
enableUiScissor(graphics, 20, 40, 300, 250);
renderContent(graphics);
disableUiScissor(graphics);
```

如果 Helper 接收到屏幕鼠标坐标：

```java
double virtualX = toVirtualX(mouseX);
double virtualY = toVirtualY(mouseY);
```

不要直接调用 `GuiGraphics.enableScissor(...)`。

## 7. Tooltip、右键菜单和确认框

Tooltip：

```java
showTooltip(Component.translatable("tip.example.value"));
```

物品 Tooltip：

```java
showItemTooltip(stack);
```

右键菜单：

```java
openContextMenu(
        mouseX,
        mouseY,
        List.of(
                GuiOverlay.MenuItem.action(
                        Component.translatable("gui.example.edit"),
                        this::edit
                ),
                GuiOverlay.MenuItem.danger(
                        Component.translatable("gui.example.remove"),
                        this::remove
                )
        )
);
```

确认框使用 Screen 的 `openDialog(...)` 或 `GuiOverlay.openCurrentDialog(...)`。

菜单/对话框打开时 Overlay 会阻断底层输入与底层 Tooltip。

## 8. 注册客户端配置页面

```java
private static boolean enabled = true;
private static int amount = 10;
private static String mode = "AUTO";

public static void registerConfigPage() {
    KTConfigPage page = KTConfigPage.builder(
                    "example:general",
                    Component.translatable("cfg.example.general")
            )
            .scope(KTConfigScope.LOCAL_INSTALLATION)
            .applyTiming(KTConfigPage.ApplyTiming.IMMEDIATE)
            .section(Component.translatable("cfg.example.section.general"))
            .booleanValue(
                    "enabled",
                    Component.translatable("cfg.example.enabled"),
                    () -> enabled,
                    value -> enabled = value,
                    true,
                    Component.translatable("cfg.example.enabled.tip")
            )
            .intValue(
                    "amount",
                    Component.translatable("cfg.example.amount"),
                    () -> amount,
                    value -> amount = value,
                    10,
                    -100,
                    100,
                    value -> value != 13,
                    Component.translatable("cfg.example.amount.tip")
            )
            .translatedChoice(
                    "mode",
                    Component.translatable("cfg.example.mode"),
                    () -> mode,
                    value -> mode = value,
                    "AUTO",
                    value -> true,
                    Component.translatable("cfg.example.mode.tip"),
                    "cfg.example.mode",
                    "AUTO",
                    "SINGLE",
                    "BURST"
            )
            .onSave(ExampleConfig::save)
            .build();

    KTConfigApi.register(page);
}
```

Choice 的实际配置仍保存 `AUTO / SINGLE / BURST`，界面显示翻译结果。

配置入口安装到模组配置按钮：

```java
KTConfigApi.installConfigScreen("example");
```

如果希望打开统一配置 Hub：

```java
KTConfigApi.installConfigHub("example");
```

如果附属需要本地 CLIENT 配置文件，不要自己声明 `ForgeConfigSpec`。使用 `KTClientConfigSpec`：

```java
private static final KTClientConfigSpec.BooleanValue ENABLED;
private static final KTClientConfigSpec.IntValue RANGE;
private static final KTClientConfigSpec SPEC;

static {
    KTClientConfigSpec.Builder builder = KTClientConfigSpec.builder();
    builder.push("general");
    ENABLED = builder
            .translation("cfg.example.enabled")
            .defineBoolean("enabled", true);
    RANGE = builder
            .translation("cfg.example.range")
            .defineInt("range", 16, 1, 128);
    builder.pop();
    SPEC = builder.build();

    KTClientConfigAdapter.registerSpec(SPEC, "example-client.toml");
}
```

范围与 validator 会同时用于底层持久化和 Kinetic 配置 GUI，附属不需要接触具体配置加载器。

## 9. 服务端权威配置

服务端必须定义最终校验规则。

```java
private static boolean enabled = true;
private static long limit = 1000L;
private static String mode = "AUTO";

public static void registerServerConfig() {
    KTServerConfigSpec spec = KTServerConfigSpec.builder("example:server")
            .booleanValue(
                    "enabled",
                    () -> enabled,
                    value -> enabled = value
            )
            .longValue(
                    "limit",
                    () -> limit,
                    value -> limit = value,
                    -1_000_000L,
                    1_000_000L,
                    value -> value != 13L
            )
            .choiceValue(
                    "mode",
                    () -> mode,
                    value -> mode = value,
                    value -> true,
                    "AUTO",
                    "SINGLE",
                    "BURST"
            )
            .onSave(ExampleServerConfig::save)
            .build();

    KTServerConfigApi.register(spec);
}
```

客户端对应页面：

```java
KTConfigPage.builder(
        "example:server",
        Component.translatable("cfg.example.server")
)
.scope(KTConfigScope.SERVER_AUTHORITATIVE)
.serverManaged()
```

读取服务端权威值：

```java
boolean enabled = KTServerConfigApi.getBoolean(
        "example:server",
        "enabled",
        true
);
```

客户端读取镜像：

```java
long limit = KTServerConfigClient.getLong(
        "example:server",
        "limit",
        1000L
);
```

不要把客户端 validator 当成服务端安全校验的替代品。

## 10. 注册网络协议

定义消息：

```java
public record ExampleMessage(int value, String id) {
}
```

定义 Codec：

```java
private static final NetworkCodec<ExampleMessage> CODEC = NetworkCodec.of(
        (buffer, message) -> {
            buffer.writeVarInt(message.value());
            buffer.writeUtf(message.id());
        },
        buffer -> new ExampleMessage(
                buffer.readVarInt(),
                buffer.readUtf()
        )
);
```

注册 Channel：

```java
private static final NetworkChannel CHANNEL = KineticNetwork.channel(
        new ResourceLocation("example", "main"),
        "1"
);
```

客户端发送到服务端：

```java
private static final ServerboundSender<ExampleMessage> SEND_TO_SERVER =
        CHANNEL.registerServerbound(
                ExampleMessage.class,
                CODEC,
                (message, context) -> handleServer(message, context.sender())
        );
```

发送：

```java
SEND_TO_SERVER.send(new ExampleMessage(5, "minecraft:stone"));
```

服务端发送到指定玩家：

```java
private static final ClientboundSender<ExampleMessage> SEND_TO_CLIENT =
        CHANNEL.registerClientbound(
                ExampleMessage.class,
                CODEC,
                ExampleNetwork::handleClient
        );
```

```java
SEND_TO_CLIENT.send(player, new ExampleMessage(5, "minecraft:stone"));
```

业务不直接操作 `SimpleChannel`、`FriendlyByteBuf` 或 `NetworkEvent.Context`。

需要按消息类型自动保存 Sender 时可以使用更高层的 `PacketChannel`：

```java
private static final PacketChannel PACKETS = PacketChannel.create(
        new ResourceLocation("example", "main"),
        "1"
);

PACKETS.registerServerbound(ExampleMessage.class, CODEC, ExampleNetwork::handleServer);
PACKETS.sendToServer(new ExampleMessage(5, "minecraft:stone"));
```

需要把一段临时协议数据编码成 `byte[]` 时使用 `NetworkBuffers.encode(...) / decode(...)`，不要自己创建 `FriendlyByteBuf`。

## 11. 压缩

```java
byte[] compressed = KineticCompression.compressUtf8(json);
String json = KineticCompression.decompressUtf8(
        compressed,
        4 * 1024 * 1024
);
```

最大解压大小必须按业务数据上限明确给出。

## 12. 注册按键

```java
private static final KineticKeyBindings.Binding OPEN_KEY =
        KineticKeyBindings.builder("key.example.open")
                .category("key.categories.example")
                .context(KineticKeyBindings.Context.IN_GAME)
                .modifier(KineticKeyBindings.Modifier.NONE)
                .keyboardKey(GLFW.GLFW_KEY_G)
                .enabledWhen(() -> true)
                .onPressed(() -> {
                    KineticClientRuntime.openScreen(ExampleScreen::new);
                    return true;
                })
                .register();
```

鼠标键使用：

```text
mouseButton
```

需要精确修饰键匹配时使用：

```text
exactModifiers(true)
```

需要在 Tooltip 中显示当前实际按键名称时：

```java
Component keyName = OPEN_KEY.translatedKeyMessage();
```

## 13. 客户端生命周期

```java
HookRegistration tickRegistration = KineticClientEvents.onTick(
        KineticClientEvents.TickPhase.END,
        ExampleClient::tick
);
```

Screen 初始化：

```java
KineticClientEvents.onScreenInitAfter(screen -> {
});
```

需要安全读取、添加或移除原版/第三方 Screen 控件时：

```java
KineticClientEvents.onScreenInitAfterWithControls(context -> {
    context.listeners().stream()
            .filter(this::shouldRemove)
            .findFirst()
            .ifPresent(context::removeListener);
});
```

HUD：

```java
KineticClientEvents.onHudRender(
        KineticClientEvents.HudStage.END,
        (graphics, partialTick) -> renderHud(graphics)
);
```

鼠标按键预处理和世界渲染同样走 API：

```java
KineticClientEvents.onMouseButtonBefore(context -> {
    if (context.button() == 1 && shouldBlockRightClick()) context.cancel();
});

KineticClientEvents.onLevelRender(
        KineticClientEvents.LevelRenderStage.AFTER_ENTITIES,
        context -> renderWorld(context.poseStack(), context.camera())
);
```

不再需要为这些通用阶段单独监听 Forge Event。

## 14. 服务端生命周期

```java
KineticServerEvents.onPlayerLogin(player -> onLogin(player));

KineticServerEvents.onTick(
        KineticServerEvents.TickPhase.END,
        server -> onServerTick(server)
);
```

完整生命周期包括：

```text
onAboutToStart
onStarted
onStopping
onStopped
onPlayerTick
onPlayerLogin
onPlayerLogout
onPlayerClone
onPlayerRespawn
onPlayerChangedDimension
onDatapackSync
onChat
```

需要保留 Forge 原事件执行顺序时使用 `Priority` 重载：

```java
KineticServerEvents.onPlayerLogout(
        KineticServerEvents.Priority.LOWEST,
        ExampleServer::onLogout
);
```

聊天拦截使用 `ChatContext.cancel()`，附属不需要拿到 `ServerChatEvent`。

## 15. 物品 Tooltip

```java
KineticItemTooltips.onBuild((stack, lines) -> {
    if (isSpecial(stack)) {
        lines.add(Component.translatable("tip.example.special"));
    }
});
```

需要观察当前 Tooltip 渲染时使用 `onRender(...)`。

需要修改高级 Tooltip 组件列表时使用 `onGather(...)`；自定义 `TooltipComponent` 的客户端渲染器通过 `registerComponentFactory(...)` 注册，不需要附属直接监听 Forge Tooltip 事件。

## 16. Hook

注册 Hook：

```java
HookRegistration registration = CommonHooks.onRecipeBookRemoval(
        () -> true
);
```

不再需要时：

```java
registration.close();
```

业务专用 Mixin 仍然允许保留。只有完全通用、可被其他附属原样复用的注入能力才应该提升为公共 Hook/API。

## 17. 扩展 `/kt` 命令

```java
KineticCommands.registerExtension("example", new CommandExtension() {
    @Override
    public void registerCommands(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("example")
                .executes(context -> run(context.getSource())));
    }

    @Override
    public void reload(CommandSourceStack source) {
        ExampleConfig.reload();
    }
});
```

附属不需要直接监听 `RegisterCommandsEvent`。

如果必须保留独立顶层命令路径，例如 `/example reload`，使用：

```java
KineticCommands.registerTopLevel("example", dispatcher -> {
    dispatcher.register(Commands.literal("example")
            .then(Commands.literal("reload")
                    .executes(context -> reload(context.getSource()))));
});
```

## 18. 选择器

物品选择器、实体选择器、NBT、颜色等都从 `KineticSelectors` 打开。调用方只处理业务回调，不直接实例化 internal Screen。

例如颜色：

```java
KineticSelectors.openColorPicker(
        KineticClientRuntime.currentScreen(),
        Component.translatable("gui.example.color"),
        currentRgb,
        rgb -> setRgb(rgb)
);
```

具体重载以 `KineticSelectors` 源码签名为准。

## 19. 功能开关

```java
KineticFeatureSwitches.register(
        "example.feature",
        "example.section",
        true,
        "cfg.example.section",
        "cfg.example.feature",
        "cfg.example.feature.tip"
);
```

运行时读取：

```java
if (KineticFeatureSwitches.isEnabled("example.feature")) {
    runFeature();
}
```

功能 ID 描述实际业务能力，不使用具体 Mixin 类名作为玩家可见功能名。

附属自己的 `IMixinConfigPlugin` 也应使用同一功能 ID：在插件 `onLoad` 中注册 Descriptor，在 `shouldApplyMixin` 内把内部 Mixin 类映射到稳定功能 ID，再调用 `KineticFeatureSwitches.isEnabled(...)`。不要再生成以 Mixin 类名为键的独立配置文件。

## 20. 注册表、物品、菜单与创造模式 Tab

实体类型：

```java
KineticRegistryHandle<EntityType<MyEntity>> MY_ENTITY =
        KineticEntityTypes.register(
                "example",
                "my_entity",
                () -> EntityType.Builder
                        .of(MyEntity::new, MobCategory.MISC)
                        .sized(0.6F, 1.8F)
                        .build("example:my_entity")
        );
```

客户端渲染器：

```java
KineticClientRenderers.registerEntityRenderer(
        MY_ENTITY,
        MyEntityRenderer::new
);
```

物品注册：

```java
KineticRegistryHandle<Item> MY_ITEM = KineticItems.register(
        "example",
        "my_item",
        () -> new Item(new Item.Properties())
);
```

MenuType 注册和客户端 Screen 注册分别使用 `KineticMenuTypes`、`KineticClientMenus`；附加数据通过 `NetworkBuffer` 读取。

只读注册表查询：

```java
Item item = KineticRegistries.items().get(new ResourceLocation("minecraft", "stone"));
ResourceLocation id = KineticRegistries.items().id(item);
boolean tagged = KineticRegistries.items().isInTag(item, someTag);
KineticRegistryView<MyType> custom = KineticRegistries.<MyType>custom(registryId).orElseThrow();
```

创造模式 Tab 不直接访问 `BuiltInRegistries.CREATIVE_MODE_TAB`：

```java
for (KineticCreativeTabs.TabEntry entry : KineticCreativeTabs.entries()) {
    ResourceLocation tabId = entry.key().location();
    CreativeModeTab tab = entry.tab();
}

KineticCreativeTabs.refreshSearch(searchableItems);
```

## 21. 资源包 / 数据包源

```java
KineticPackSources.register(
        PackType.SERVER_DATA,
        () -> source
);
```

## 22. 模组生命周期、运行环境与路径

```java
KineticModLifecycle.onCommonSetup(ExampleAddon::commonSetup);
KineticModLifecycle.onClientSetup(ExampleClient::clientSetup);
KineticModLifecycle.onLoadComplete(ExampleAddon::finishSetup);
```

侧别判断和延迟执行：

```java
if (KineticEnvironment.isClient()) {
}

KineticEnvironment.runOnClient(() -> ExampleClient::init);
```

模组检测与配置目录：

```java
boolean loaded = KineticPlatform.isModLoaded("example_dependency");
Path configDir = KineticPaths.configDirectory().resolve("example");
```

调用方不需要直接持有 Forge Mod Event Bus、`DistExecutor`、`FMLEnvironment`、`ModList` 或 `FMLPaths`。

## 23. 世界、实体和背包事件

世界事件：

```java
KineticWorldEvents.onEntityJoin(context -> {
    if (shouldReject(context.entity())) context.cancel();
});

KineticWorldEvents.onChunkLoad(context -> handleChunk(context.level(), context.chunk()));
```

Living 事件：

```java
KineticLivingEvents.onHurt(context -> {
    if (shouldReduce(context.entity())) context.amount(context.amount() * 0.5F);
});

KineticLivingEvents.onPotionApplicable(context -> {
    if (immune(context.entity(), context.effectInstance())) {
        context.applicability(KineticLivingEvents.Applicability.DENY);
    }
});
```

强制区块加载使用 `KineticChunkLoading.setForced(...)`；判断某个 Slot/Handler 是否属于玩家背包使用 `KineticInventorySlots`。

## 24. I18N

所有固定玩家文本使用语言键：

```java
Component.translatable("gui.example.title")
```

颜色放在 `zh_cn.json` / `en_us.json` 的值中，用 Minecraft `§` 颜色码控制。不要在 Java 中硬编码玩家文本颜色。

当翻译文本包含动态参数，并且动态参数本身需要继承语言文件中的颜色或粗体时，使用 `KineticI18n`：

```java
KineticI18n.translatable(
        "msg.example.account",
        Component.literal(accountId)
);
```

语言文件：

```text
"msg.example.account": "§bAccount: §6%s"
```

这样 `Account:` 与动态的 `accountId` 都完全由语言文件决定颜色。普通 `Component.translatable(..., Component)` 不应被用于依赖 `%s` 前 `§` 格式的动态参数。

客户端界面也可以使用：

```java
KineticText.translatable("msg.example.account", Component.literal(accountId))
```

`KineticText.translatable(...)` 内部使用同一套 I18N 参数样式规则。

如果翻译键的第二段不是模组 ID，可以使用显式命名空间入口：

```java
KineticI18n.translatableIn("examplemod", "custom.translation.key", value)
```

中英文语言文件中，同一动态参数前的颜色/格式必须保持一致。不要使用灰色与深灰色两种原生文本颜色码。

配置中存储的固定值可以继续保持英文业务值，GUI 使用 `translatedChoice(...)` 显示翻译。

## 25. 开发检查清单

提交一个附属功能前检查：

1. 是否直接 import 了 `internal`。
2. 是否自己 `Button.builder` / `new EditBox`。
3. 是否自己写了已经由 API 提供的 Scissor、Tooltip、右键菜单或确认框。
4. 数字输入是否保留原本的负数、最小值、最大值规则。
5. 服务端权威配置是否在 `KTServerConfigSpec` 再次校验。
6. 玩家可见文本是否全部 I18N。
7. 是否在 Java 里硬编码玩家文本颜色。
8. 是否为同一能力重复创建了 Mixin。
9. 完全通用的新能力是否应该先进入 KineticCore API。
10. 是否把业务专用规则错误塞进了通用 API。
11. 是否还直接使用 `ForgeRegistries` / `BuiltInRegistries` / `DeferredRegister`，而核心已经提供对应注册或查询 API。
12. 是否还直接使用 `DistExecutor`、`ModList`、`FMLPaths` 或通用 Forge 生命周期事件。
13. 功能开关是否使用稳定业务 ID，而不是把 Mixin 类名暴露给玩家。
