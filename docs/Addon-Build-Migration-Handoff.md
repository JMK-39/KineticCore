# 附属构建架构迁移交接文档（对齐 KineticCore 多版本架构）

本文档交给负责迁移附属模组构建的代理执行。目标：把所有 KineticCore 附属的构建方式统一到 KineticCore 现在的架构，即 Java 21 编译、Gradle 9.8、Stonecutter 多版本节点、ModDevGradle（MDG），并采用新的 JAR 命名。玩法代码不在本次范围内。

## 1. 范围

需要迁移的附属都在 `D:\IDEAWork\` 下，每个都是独立的 git 仓库：

AdventureSystems、CombatSystems、ContentStudio、EnchantWorks、EntityControl、ItemControl、KineticArmory、MobAscension、ModRefinery、RealmControl、TACZWorkshop、TextStudio

`LangExporter` 不依赖 KineticCore，也不是同类构建，不在本次范围内。

这 12 个附属现在的构建方式相同：ForgeGradle 6.0.24 + MixinGradle 0.7.38，`java.toolchain` 为 Java 17，Gradle 8.8，Forge 1.20.1（47.4.x）。每个附属都 `apply` 了 `gradle/kinetic-addon-architecture.gradle` 和 `gradle/kinetic-addon-reobf-verification.gradle`。

参考实现是 `D:\IDEAWork\KineticCore`，以它的文件为准。本文档描述要做什么以及原因；具体写法请直接读核心的文件，不要凭记忆重写。

## 2. 分两个阶段

**阶段一（本次必须完成）：** 构建架构迁移，只启用 `1.20.1-forge` 一个节点。
- 产物仍是 Forge 1.20.1 的 JAR，但改为 Java 21 字节码，并使用新命名。
- 游戏内行为必须与迁移前完全一致。

**阶段二（不在本次范围，单独立项）：** 给附属启用 `1.21.1-neoforge` 节点，并移植代码。需要的前提见第 9 节。阶段一要把多版本结构搭好，阶段二只需在 `settings.gradle` 加一行并移植代码。

每个附属单独完成、单独验证、单独提交，不要批量修改后统一构建。建议先做 AdventureSystems，跑通后再把同样的改动套到其余附属。

## 3. 目标结构（对照核心的文件）

| 核心文件 | 作用 | 附属中的做法 |
|---|---|---|
| `settings.gradle` | 声明 Stonecutter 0.9.8、foojay 1.0.0，以及版本节点 `version('1.20.1-forge','1.20.1').buildscript('build.forge.gradle')`，`vcsVersion = '1.20.1-forge'` | 照搬，只保留 1.20.1 节点，`rootProject.name` 改为附属名 |
| `stonecutter.gradle` | 声明 MDG 插件（`apply false`），`stonecutter.active`，`constants.match(loader,'forge','neoforge')`，Forge→NeoForge 的包名替换，`buildAll` 任务 | 照搬（替换规则阶段一用不到，但保留，阶段二直接可用） |
| `build.forge.gradle` | `net.neoforged.moddev.legacyforge`、`legacyForge {}`、`mixin {}`、Mixin 注解处理器、`kineticConfigureReleaseJar(tasks.named('reobfJar', Jar))` | 按附属的依赖改写 `dependencies` |
| `build.neoforge.gradle` | NeoForge 节点构建逻辑 | 阶段一可以先照搬（不启用节点就不会被执行） |
| `gradle/kinetic-node.gradle` | 所有节点共用：版本号、toolchain/`release`、资源展开、JAR 命名与输出目录、检查任务 | 以它为模板，去掉核心专用的检查（API 面检查、屏幕一致性等），保留通用部分 |
| `versions/1.20.1-forge/gradle.properties` | `loader`、MC 与 Forge 版本、`java_version=21`、`pack_format=15` | 新建，值取自附属现有的 `gradle.properties` |
| `gradle/gradle-daemon-jvm.properties` | 让 Gradle 守护进程运行在 Java 21 | 照搬 |
| `gradle/wrapper/` | Gradle 9.8.0 | 运行 `gradlew wrapper --gradle-version 9.8.0`，或照搬核心的 wrapper |
| `.github/workflows/*.yml`（如附属有） | Java 21、Gradle 9.8.0、`gradle buildAll`、上传 `<mod_id>-*-<version>.jar` | 参照核心修改 |

原来根目录的 `build.gradle` 要删除：Stonecutter 的控制脚本是 `stonecutter.gradle`，各节点用 `build.<loader>.gradle`。

## 4. 必须保持的规则（核心里已经踩过的坑）

1. **Java 版本**：toolchain 取 `max(21, java_version)`，编译参数 `options.release = java_version`。1.20.1 的 `java_version=21`，产物是 Java 21 字节码（class version 65），整合包统一在 Java 21 上运行。
2. **Mixin 兼容级别**：Forge 1.20.1 自带的 Mixin 0.8.5 不认识 `JAVA_21`，所以 Forge 节点的 mixin JSON 必须保持 `"compatibilityLevel": "JAVA_17"`。核心在 `kinetic-node.gradle` 的 `processResources` 里只对非 Forge 节点上调这个值，照抄这个逻辑。
3. **refmap 与清单**：Forge 节点需要 `mixin { add sourceSets.main, "<modid>.mixins.refmap.json"; config ... }`、注解处理器 `org.spongepowered:mixin:0.8.5:processor`，以及 JAR 清单里的 `MixinConfigs`。发布的 JAR 是 `reobfJar` 的输出，不是 `jar`。
4. **依赖重映射**：ForgeGradle 的 `fg.deobf(...)` 在 MDG Legacy 中改为 `modImplementation` / `modCompileOnly` / `modRuntimeOnly`。Modrinth、Curios、FTB 等依赖都按这个方式换。`annotationProcessor` 和普通 `implementation` 不变。
5. **log4j 约束**：加上 `constraints { implementation 'org.apache.logging.log4j:log4j-core:2.19.0' }`，否则会按 2.8.1 编译。
6. **测试**：如附属有 `src/test`，需要 `legacyForge { addModdingDependenciesTo sourceSets.test }`。Gradle 9 没发现测试时会报错，要设 `failOnNoDiscoveredTests = false`。
7. **路径**：构建脚本在 `versions/<节点>/` 子项目中执行，所有指向仓库根目录的路径必须写 `rootProject.file(...)` / `rootDir`，否则会解析到节点目录下。原有的 `adventure-text.gradle`、`kinetic-addon-*.gradle` 也都要检查这一点。
8. **运行目录**：Forge 节点 `gameDirectory = rootProject.file('run')`，NeoForge 节点用 `run-<节点>`。`.gitignore` 加上 `run-*`。
9. **Stonecutter 注释**：版本差异代码写成 `//? if >=1.21 {` … `//?} else {` … `//?}`；被禁用块内的嵌套注释写成 `/^* ... ^/`。`gradle.properties` 里保留 `dev.kikugie.stonecutter.hard_mode=true`。
10. **Mixin 包**：任何代码都不能调用 Mixin 包（mixin JSON 的 `package`）里的非 Mixin 类，包括外层容器类上的静态方法，否则运行时会抛 `IllegalClassLoadError`。辅助方法要放在 Mixin 包之外。

## 5. JAR 命名与输出

- 命名：`<mod_id>-<loader>-<minecraft>-<version>.jar`，例如 `adventuresystems-forge-1.20.1-26.10.3.jar`。由 `kinetic-node.gradle` 的 `kineticConfigureReleaseJar` 统一设置。
- 输出：`gradle.properties` 设 `output_mods_dir=D:/NEWMODS` 时直接输出到该目录（相对路径按仓库根目录解析），未设置时输出到根目录的 `build/libs`。
- 版本号沿用现有的 `use_auto_version` 规则（`yy.M.d`）。

## 6. 依赖解析：优先使用本地库（必须修改，否则会构建失败）

### 6.1 规则：本地优先，官方有新版才下载

所有第三方模组依赖（Curios、Refined Storage、FTB、Architectury、JEI 等）和 KineticCore 都要**优先使用本地库 `D:\IDEA_Caches\libs`**，只有官方来源有更新的版本时才下载。

- 本地库的位置是 Gradle 用户目录的上一级加 `libs`（附属现有代码 `new File(gradle.gradleUserHomeDir.canonicalFile.parentFile, 'libs')` 就是这个目录，保留这种写法，不要写死盘符）。
- 本地库中的文件按 `<artifact>-<version>.jar` 命名，与依赖坐标对应。例如 `maven.modrinth:curios:IPQlZkz1` 对应 `curios-IPQlZkz1.jar`，`dev.ftb.mods:ftb-library-forge:2001.2.12` 对应 `ftb-library-forge-2001.2.12.jar`。
- **第三方模组**：坐标都固定了版本（Modrinth 的版本 ID、Maven 的版本号），不存在"官方新版"的判断。把本地库声明为第一个仓库（`flatDir`，不加 `content` 过滤），本地有对应文件就用本地文件，没有才去 Modrinth、FTB、Architectury 等远程仓库下载。`flatDir` 不带依赖元数据，所以这些依赖不会传递引入其他依赖；这和现在 `fg.deobf` 模组依赖的用法一致。如果某个依赖确实需要传递依赖，把它单独列出来汇报。
- **KineticCore**：版本不固定，保留现有的比较逻辑。比较本地库（以及第 2 点列出的其他本地目录）中最高的本地版本与 GitHub `releases/latest`：本地版本不低于官方版本时用本地；官方版本更新时下载官方版本；无法联网时用本地版本。`-Pkineticcore_version=<版本>` 仍可强制指定版本。

### 6.2 KineticCore 解析需要的具体修改

核心已改为新命名，旧的解析逻辑会失效。以 AdventureSystems 的 `build.gradle` 为例（其余附属相同），涉及以下几处：

1. **本地 JAR 正则**：`^kineticcore-(\d+(?:\.\d+){2,})\.jar$` 匹配不到新文件名。改为同时识别新旧两种命名，例如 `^kineticcore-(?:(forge|neoforge)-([\d.]+)-)?(\d+(?:\.\d+){2,})\.jar$`；在新命名中，只接受与当前节点 `loader` 和 `minecraft_version` 相同的 JAR。
2. **本地目录**（按顺序搜索）：本地库 `D:\IDEA_Caches\libs`（目前里面是旧命名的 `kineticcore-26.10.2.jar`），然后是核心的输出目录 `D:/NEWMODS`（核心 `gradle.properties` 中的 `output_mods_dir`；可以读取 `../KineticCore/gradle.properties`，或者给附属配置同名属性），最后是 `../KineticCore/build/libs`（核心不再输出到这里，仅作备选保留）。
3. **GitHub Release 资源检查**：`asset.name == "kineticcore-${version}.jar"` 改为优先匹配 `kineticcore-<loader>-<mc>-${version}.jar`，旧命名作为兼容回退（旧版本的 Release 仍是旧命名）。
4. **Ivy 下载模式**：`'v[revision]/[artifact]-[revision].[ext]'` 改为可以带上 loader 和 MC 版本，例如 `'v[revision]/[artifact]-[classifier]-[revision].[ext]'`，classifier 设为 `forge-1.20.1`；或者为两种命名各声明一个 ivy 仓库。
5. **依赖声明**：`fg.deobf("dev.xyat.kineticcore:kineticcore:${v}")` 改为 MDG Legacy 的 `modImplementation`，`kineticCoreReobf` 配置也要相应调整（它供 reobf 校验使用，见第 7 节）。
6. **发布顺序**：核心已推送到 master，带新命名的 Release（`kineticcore-forge-1.20.1-<版本>.jar`、`kineticcore-neoforge-1.21.1-<版本>.jar`）会由下一次成功的自动构建发布。之后 `releases/latest` 不再有旧命名的资源，旧的解析逻辑会报 "has no matching JAR asset"。在新解析逻辑完成之前，附属只能使用本地 JAR，或用 `-Pkineticcore_version=26.10.2` 构建。

### 6.3 核心自带的 JEI 适配

核心内置了一项第三方适配：安装 JEI 时，核心会把紧凑状态效果的区域告知 JEI，让物品列表避开这块区域（`MiniEffectsMixins$InventoryEffectRendererGuiHandlerMixin`，带 `@Pseudo`，未安装 JEI 时跳过）。这是有意保留的功能。附属不要重复实现，也不要因为"核心不做第三方联动"的原则去掉它。

## 7. 检查任务

- `kinetic-addon-architecture.gradle`：从核心 `gradle/kinetic-addon-architecture.gradle` 复制最新版本覆盖到附属，所有路径改为基于 `rootProject`。附属在 `build.gradle` 中登记的例外规则迁到 `build.forge.gradle`，或放到共用的节点脚本里。
- `kinetic-addon-reobf-verification.gradle` 与 `KineticReobfCheck.java`：改为校验 `reobfJar` 的输出，路径同样基于 `rootProject`。参照核心的 `gradle/kinetic-reobf-verification.gradle`。
- **建议加上 Mixin 目标检查**：把核心的 `gradle/KineticMixinTargetCheck.java`，以及 `kinetic-node.gradle` 中的 `kineticMixinCheck` 配置和 `checkKineticMixinTargets` 任务一起移植过来，并挂到 `check` 上。Mixin 在找不到目标方法时会静默跳过注入（mixin 配置里没有 `defaultRequire`）；核心就因此有 4 个注入在 1.20.1 上从未生效，用这个检查才发现。对附属第一次运行时出现的 PROBLEM，先登记到 `known_mixin_issues` 并单独汇报，不要在本次迁移中顺手修改玩法代码。

## 8. 每个附属的验收清单

1. `gradlew :1.20.1-forge:build` 通过，包括架构检查、reobf 校验，以及 Mixin 目标检查（如已加入）。在 `D:\IDEA_Caches\libs` 已有全部依赖的前提下，`--offline` 构建也必须通过，说明依赖确实来自本地库。
2. 产物文件名符合第 5 节，并出现在预期目录中。
3. JAR 内容：
   - class 版本为 65（`javap -v` 中 `major version: 65`）；
   - 包含 refmap；
   - mixin JSON 为 `JAVA_17`；
   - 清单中有 `MixinConfigs`；
   - `mods.toml` 中依赖 KineticCore 的版本范围没有变化。
4. 开发运行：`gradlew :1.20.1-forge:runClient` 能进入主菜单，日志中没有 Mixin 报错。可参照核心，加上 `-Pkinetic_quick_play=<存档>` 直接进入世界。
5. 与迁移前的 JAR 对比：类列表、资源和语言文件一致（只允许字节码版本、refmap 和清单变化）。
6. 整合包验证由用户自己完成：用户在 `F:\game\异界战斗幻想\.minecraft\versions\OTHERWORLD CLASH` 中替换 JAR 后启动测试。不要自己启动或结束用户的游戏进程。

## 9. 阶段二的前提（仅供规划）

附属启用 `1.21.1-neoforge` 节点时，需要处理核心 API 在 1.21.1 上的这些差异：
- 物品文本使用 `id[components]` 写法；1.21.1 仍能读取 1.20.1 的 `id{NBT}` 写法，并自动升级为组件。
- `KineticClientAdvancements.all()` 返回 `List<AdvancementHolder>`。
- 不支持 `KineticEnchantments.register`：附魔改为数据驱动，影响 EnchantWorks。
- 物品 NBT 改为 Data Components，`Attribute`/`MobEffect` 改用 `Holder`，网络改用 NeoForge payload。

迁移套路参见核心 README 的 "Multi-version build / Differences on 1.21.1" 一节，以及核心源码里的 `//? if` 用法。

## 10. 工作约定

- 不修改玩法逻辑、配置格式、语言文本和玩家可见行为。
- 不推送、不发布 Release；每个附属完成后只在本地提交，由用户决定何时推送。
- 构建遇到网络问题时可用 `--offline` 重试。JAR 被占用时（例如被压缩软件或游戏打开），先告知用户，不要强行结束进程。
- 每个附属完成后汇报：改了哪些文件、验收清单每项的结果，以及新发现的 Mixin 问题。
