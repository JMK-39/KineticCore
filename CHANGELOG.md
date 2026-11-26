2026年11月26日 — 26.11.26.1 — Picking blocks in a structure preview / 结构预览方块点选

**English**

- A 3D structure editor can now identify the visible block under the mouse at the moment of the press. On a reduced-detail preview the API returns the selected cell and its scale, so an editor can open a full-detail view before making a change. Containers, spawners and entity markers still take priority when visible. The existing marker-click listener continues to work.

**简体中文**

- 3D 结构编辑器现在能取得按下鼠标时指向的可见方块。缩略视图会返回选中格及其缩放比例，方便编辑器打开原始精度的局部视图后再修改。可见的容器、刷怪笼和实体标记优先选中，原有标记点击监听器继续可用。

2026年11月26日 — 26.11.26 — First-join reward save fix / 首次进入奖励存档修复

**English**

- Fixed an issue when a player received enchanted first-join equipment in a second world opened without restarting Minecraft. On NeoForge 1.21.1 the inventory save could crash. On NeoForge 26.1.2 the player could disconnect and the equipment could fail to save. KineticCore now reads reward items using the current world's registries each time it grants them.

**简体中文**

- 修复同一次游戏会话中进入第二个存档并领取附魔新手装备时的问题。NeoForge 1.21.1 可能在保存物品栏时崩溃，NeoForge 26.1.2 可能断开连接并无法保存装备。KineticCore 现在每次发放奖励都从当前存档的注册表解析物品。

2026年11月24日 — 26.11.24 — Config folders / 配置归档
- Core settings live under `config/kineticcore/core`. Updated add-ons can group settings under their mod ID and migrate earlier paths before loading. Existing destination files take precedence, and conflicts retain the previous file for inspection.
- 核心配置迁入 `config/kineticcore/core`。更新后的附属按模组 ID 归档配置，并在读取之前自动迁移老路径。新位置已有配置时优先使用它，冲突的旧文件留待核对。
- Log Cleaner keeps game lifecycle messages visible in error-only mode so PCL can recognize a normal exit.
- 日志清理的仅错误模式会保留游戏生命周期消息，便于 PCL 识别正常退出。
- Runtime attribute APIs support applying and removing newly attached attribute instances and respect registered attribute ranges.
- 运行时属性 API 支持添加、移除新的属性实例，并遵循已注册属性的取值范围。

2026年11月14日 — 26.11.14 — Structure preview camera / 结构预览镜头
- The 3D structure preview handles like a modelling program: drag to turn it all the way round, over the top and from below; right- or middle-drag to move it; the wheel zooms toward the pointer and, zoomed on, goes into the structure through a perspective camera, so rooms can be seen from inside. Zooming eases smoothly; double-click resets.
- 3D 结构预览的操作改得像建模软件：拖拽可 360° 自由旋转，能翻过顶部、从下方看；右键或中键拖拽平移；滚轮朝鼠标处缩放，继续推进会以透视镜头进入结构内部，可以看到房间里面。缩放过渡平滑，双击复位。
- Dragging a 3D preview in a real screen (RealmControl's structure panel, rules page and structure editor) turned it only for the first instant and then stopped responding: the preview takes no keyboard focus, so the screen dropped it as the drag target within a tick. A drag now stays with the control it started on until the button is released, for every Kinetic custom control and every mouse button.
- 在实际界面（RealmControl 的结构面板、规则页与结构编辑器）中拖拽 3D 预览时，只有刚按下的一瞬间会转动，之后就不再响应：预览不接受键盘焦点，界面在一刻内就把它从拖拽目标中移除了。现在拖拽会一直交给开始拖拽的控件，直到松开按键；所有 Kinetic 自绘控件与所有鼠标按键都是如此。
- The controls hint of a 3D preview no longer covers the structure whenever the pointer is over it: it moved to a small "?" badge in the preview's top-right corner and shows only while the pointer rests on the badge, one short line per control.
- 3D 预览的操作说明不再在鼠标移到预览上时一直遮挡结构：说明移到预览右上角的小“?”图标上，只有鼠标停在图标上时才显示，每种操作一行短句。
- On 26.1, a Kinetic custom control (list scroll bars, entity grids, 3D previews) never learned that the mouse button came up when it was released over the control, since 26.1 tells only the focused control. Every control now gets the release of a press it took, exactly once, on all versions.
- 26.1 上在 Kinetic 自绘控件（列表滚动条、实体网格、3D 预览）上松开鼠标时，控件收不到松开事件，因为 26.1 只通知获得焦点的控件。现在所有版本中，控件都会收到自己那次按下对应的松开，且只收一次。
- The 3D structure preview draws the structure's entities as their models, and names the block under the pointer, with its position, in its bottom-left corner. Public API: `KineticStructureView` takes markers (boxes such as containers and entities) that the pointer picks, blocks in front hiding them; the one under the pointer is outlined and named, the current one outlined yellow and other selected ones green, visible through walls. A `PickListener` hears clicks without dragging and double-clicks on markers; a double-click on empty space still resets the view. `lookAt` eases the camera to a point and `setEntities` draws changed entity data.
- 3D 结构预览会画出结构里的生物模型，并在左下角显示鼠标所指方块的名称和坐标。公开 API：`KineticStructureView` 可以设置标记（箱子、生物等的包围盒），鼠标可以拾取，前方的方块会挡住标记；鼠标所指的标记会描边并显示名称，当前标记为黄色描边，其他选中的为绿色描边，隔着墙也能看到。`PickListener` 接收不拖动的单击和对标记的双击；双击空白处仍然复位视角。`lookAt` 让镜头平滑移到某一点，`setEntities` 用修改后的数据显示生物。
- On 26.1, structure previews left out chests, beds, signs, banners and other blocks drawn by their block entity: their models have no faces of their own. They are drawn by their block entity's model now, as in a minecart.
- 26.1 的结构预览里不显示箱子、床、告示牌、旗帜等由方块实体绘制的方块，因为它们自身的模型没有面。现在改由方块实体的模型绘制，与矿车里显示方块的方式相同。
- The 3D structure preview has a backdrop that is lighter at the top, a fine grid on the floor around the structure with its footprint outlined, and an orientation gizmo in the bottom-right corner showing where X (east), Y (up) and Z (south) point as the view turns. Clicking a letter of the gizmo looks straight along that axis (X from the east, Y from above, Z from the south), and clicking it again looks from the other side. `KineticStructureView.setGridShown` and `setAxesShown` switch the grid and the gizmo off.
- 3D 结构预览加了上亮下暗的背景、结构周围地面上的细网格（结构占地范围有描边），以及右下角的方向指示：旋转视角时显示 X（东）、Y（上）、Z（南）各自的朝向。点击方向指示上的字母会正对该轴（X 从东面看，Y 从上方看，Z 从南面看），再点一次从另一面看。`KineticStructureView.setGridShown` 和 `setAxesShown` 可以关闭网格和方向指示。
- Public API: `KineticTheme.tag(graphics, text, x, y, indicator)` draws a status tag, a word or two in the indicator's colour on a faint ground of that colour (`TAG_HEIGHT` high, `tagWidth` wide), for states such as draft, modified or published next to a name.
- 公开 API：`KineticTheme.tag(graphics, text, x, y, indicator)` 绘制状态标签：一两个字，用该状态的颜色，底色是同色的淡色块（高 `TAG_HEIGHT`，宽 `tagWidth`），用于名称旁的“草稿”“已修改”“已发布”等状态。
- On 1.21.1, labels drawn at world positions through `KineticWorldRender.beginScreenOverlay` (name tags, damage numbers) were placed as if the camera never turned, so most of them were not drawn at all: from 1.20.5 on the level-render pose stack starts empty and the camera's rotation is in the stage's model-view matrix. `project` now uses that rotation. Public API: `LevelRenderContext.viewMatrix()` returns it on every version, and `KineticWorldRender.captureProjection(context)` keeps a frame's camera so a HUD render callback can project world points, which is how such labels are drawn on 26.1.
- 1.21.1 上通过 `KineticWorldRender.beginScreenOverlay` 画在世界位置上的标签（名牌、伤害数字）是按镜头从不转动来计算位置的，所以大多数根本不显示：从 1.20.5 起，世界渲染的姿态栈一开始是空的，镜头的旋转在该阶段的模型视图矩阵里。`project` 现在使用这个旋转。公开 API：`LevelRenderContext.viewMatrix()` 在所有版本上返回它，`KineticWorldRender.captureProjection(context)` 保存一帧的镜头，供 HUD 渲染回调把世界坐标投影到屏幕上；26.1 上这类标签就是这样绘制的。
- On 1.21.1 and 26.1, a Kinetic custom control a page had just hidden was still drawn for one more frame, so a list that a page swaps for other controls flashed over them. It is no longer drawn once hidden.
- 1.21.1 与 26.1 上，页面刚隐藏的 Kinetic 自绘控件还会再绘制一帧，页面用其他控件替换列表时，列表会在它们上面闪一下。现在隐藏后立即不再绘制。
- With `log_cleaner.errors_only` on, the few lines launchers read to follow a game stay on the console and in the log: the user and LWJGL lines, "OpenAL initialized", the texture atlas lines, "Stopping!" and the lines that say where a crash report was saved. PCL counts them to tell that a game has finished loading and reports a game that ends before that as crashed; with errors only the count stopped at 3 of 5, so that then rested on PCL finding the game window alone. "Stopping!" also shows, in a log sent for help, that the game was closed normally. The option's description now says to turn it off before looking into a crash.
- 开启 `log_cleaner.errors_only` 时，启动器用来跟踪游戏状态的几行日志仍会输出到控制台和日志文件：用户与 LWJGL 两行、“OpenAL initialized”、材质图集的几行、“Stopping!”以及说明崩溃报告保存位置的几行。PCL 靠这些行判断游戏是否加载完成，游戏在此之前结束会被报告为崩溃；仅保留错误日志时这个计数停在 3/5，之后就只能靠 PCL 找到游戏窗口来判断。“Stopping!”还能让求助时发出的日志看得出游戏是正常关闭的。该选项的说明里补充了：排查崩溃前请先关闭它。
- The structure preview on 26.1 keeps its mesh on the graphics card and builds it a slice per frame, like the other versions: turning a large structure no longer re-tessellates every block each frame, and opening one no longer stalls the game.
- 26.1 的结构预览与其他版本一样把网格保留在显卡上、每帧分片构建：旋转大型结构时不再每帧重新生成全部方块，打开时也不再卡住游戏。

2026年11月7日 — 26.11.7 — Layered attribute ranges / 属性范围优先级
- Public API: owner-scoped, prioritized attribute range overrides with atomic replacement. Existing range setters update the base layer; removing an override restores the latest base. Addons can invalidate native attribute-instance caches to apply new bounds immediately without changing entity base values or modifiers.
- 公开 API 新增按附属与优先级管理的属性范围覆盖，整组替换前完成校验；原有范围设置更新基础层，移除覆盖时恢复最新基础范围。附属可刷新原版属性实例缓存，使新范围立即作用于已有实体，同时保留基础值和修饰符。

2026年11月5日 — 26.11.5 — Line breaks in Chinese text / 中文换行
- Wrapped text in Kinetic GUIs (dialogs, descriptions, tooltips, toasts and field placeholders) no longer starts a line with a comma, full stop or closing bracket, or ends one with an opening bracket or quotation mark. Vanilla breaks Chinese text after any character; the break now moves back a character or two. Breaks between English words are unchanged.
- Kinetic 界面中自动换行的文字（对话框、说明、悬浮提示、通知与输入框占位文字）不再把逗号、句号、右括号等放在行首，也不再把左括号、左引号放在行尾：原版会在任意汉字后换行，现在换行位置会往前挪一两个字。英文单词之间的换行不变。

2026年11月4日 — 26.11.4 — Search boxes keep what you type, spawn egg editor fixes / 搜索框保留已输入文字与刷怪蛋编辑器修复
- Typing in the search box of a Kinetic menu (the F6 module pages, the config list, the potion and spawn egg editors) no longer selects what was typed so far, so the next letter adds to it instead of replacing it. A field that a screen focuses for you now puts the caret after its text with nothing selected.
- 在 Kinetic 菜单的搜索框（F6 模块页、配置列表、药水与刷怪蛋编辑器）中输入时，已输入的文字不再被反复选中，后输入的字会接在后面而不是覆盖前面的内容。界面自动聚焦的输入框现在把光标放在文字末尾，不选中任何内容。
- Spawn egg editor: switching an attribute from Addition to Final Multiplier crashed the game when its value was 0 or less, which is the case for every newly ticked attribute. The value now becomes the multiplier 1 (no change) and a message says that a multiplier cannot be 0; typing 0 (or less) as a multiplier is refused with "Cannot multiply by 0".
- 刷怪蛋编辑器：属性数值为 0 或负数时（新勾选的属性都是 0），把“加法”切换为“最终倍率”会导致游戏崩溃。现在会自动改为倍数 1（不改变数值），并提示最终倍率不能为 0；在最终倍率下手动输入 0（或负数）会提示“不能乘 0”，该值不会生效。
- Special spawn eggs now apply their attributes, effects, equipment and name however the mob is spawned: placed on a block or in water by right-clicking, from a dispenser, or thrown. Before, only thrown eggs and spawners did, so a hand-placed egg spawned a plain mob (for example a pig whose max health stayed at 10). A mob from an egg that has no name in the editor no longer carries the egg's label ("Special Spawn Egg: Pig") as its name; a name given in an anvil stays.
- 特殊刷怪蛋无论以何种方式生成生物都会应用其属性、效果、装备与名称：右键放在方块上或水中、发射器、投掷均可。此前只有投掷和刷怪笼会应用，手动放置的刷怪蛋生成的是普通生物（例如最大生命值仍为 10 的猪）。编辑器中未命名的刷怪蛋生成的生物不再把刷怪蛋的标签（“特殊刷怪蛋：猪”）当作名字；用铁砧改的名字保留。
- Fast cobweb breaking now covers pickaxes, shovels and hoes as well as axes, as its description says. They break a cobweb as fast as a sword and it drops string; Mining Fatigue, Haste, being underwater and being in the air change the speed as they do for a sword (before, axes always got the same speed).
- 快速破网现在和描述一致，除斧外镐、锹和锄也适用：破坏蜘蛛网和剑一样快，并掉落线；挖掘疲劳、急迫、水下与空中对速度的影响与用剑时相同（此前斧头的速度固定不变）。
- Farmland protection works with Feather Falling boots, but its descriptions (the F6 tooltip, the general.toml comment and the wiki) said Protection, which never kept farmland. They now say Feather Falling, and the line under Feather Falling on items says that jumping onto farmland won't trample it.
- 耕地保护对“摔落保护”靴子生效，但其说明（F6 提示、general.toml 注释与 wiki）写的是“保护”，而“保护”从未生效。说明已改为“摔落保护”，物品上“摔落保护”下方的提示也改为“穿在脚上时，跳到耕地上不会踩坏它”。
- Structures and structure sets in Kinetic data packs whose type no installed mod registers (for example after the mod that adds it was removed) are left out of the pack, with a warning in the log, so the world still loads; vanilla would refuse to load the world's data packs at all.
- Kinetic 数据包中类型未被任何已安装模组注册的结构和结构集合（例如提供该类型的模组被移除后）会被跳过并在日志中警告，世界照常加载；原版会因此拒绝加载整个世界的数据包。
- Add-ons can register structure types, structure placement types and structure piece types (KineticWorldgenTypes).
- 附属可以注册结构类型、结构放置类型与结构部件类型（KineticWorldgenTypes）。

2026年11月3日 — 26.11.3 — 3D structure previews for add-ons / 附属可用的结构 3D 预览
- Add-ons can show a block structure in 3D in any screen (KineticStructureView): drag to turn, right-drag or middle-drag to move, scroll to zoom, double-click to reset, and a cross-section height that hides everything above it. Large structures are built a slice per frame with a percentage shown, so the game never stalls; chests, beds and banners are drawn with their own models and blocks unknown to the game show as red glass. It looks the same on 1.20.1, 1.21.1 and 26.1.2.
- 附属可以在任意界面中用 3D 显示方块结构（KineticStructureView）：左键拖拽旋转，右键或中键拖拽平移，滚轮缩放，双击复位，并可设置剖面高度隐藏其上方的方块。大型结构逐帧分段构建并显示百分比，不会卡顿；箱子、床、旗帜用自身模型绘制，游戏中不存在的方块显示为红色玻璃。1.20.1、1.21.1 与 26.1.2 上显示效果一致。
- Block snapshots (KineticBlockSnapshot) hold a structure's blocks, block entity data and entities, are encoded compactly and keep the names of blocks the reading side does not know. Large data is sent in parts with progress (KineticBlobTransfer).
- 方块快照（KineticBlockSnapshot）保存结构的方块、方块实体数据与实体，编码紧凑，读取端不认识的方块保留其名称。大数据可分段发送并报告进度（KineticBlobTransfer）。
- Small pictures of snapshots can be drawn without a game window, also on a server, and saved as PNG (KineticSnapshotThumbnail); PNG data can be shown in screens as a texture (KineticRuntimeTextures).
- 快照的小图可以在没有游戏窗口的情况下绘制（服务端也可以），并保存为 PNG（KineticSnapshotThumbnail）；PNG 数据可作为贴图显示在界面中（KineticRuntimeTextures）。
- Add-ons can use the mouse wheel in game (KineticClientEvents.onMouseScroll) with a priority: a handler that takes a turn keeps it from the hotbar and from Kinetic's own wheel uses. The creative flight speed (Shift + wheel) now listens the same way at normal priority, so a held tool's wheel controls come first.
- 附属可以在游戏中使用鼠标滚轮（KineticClientEvents.onMouseScroll），并指定优先级：处理了滚动的监听器会阻止快捷栏切换和 Kinetic 自身的滚轮功能。创造飞行调速（Shift + 滚轮）也改用同一事件、普通优先级，手持工具的滚轮操作优先。
- World line boxes can stay visible behind blocks (LineBatch.boxThroughBlocks): the edges in the open are drawn in full colour, the hidden ones faintly, as selection boxes in building tools are.
- 世界线框可以透过方块显示（LineBatch.boxThroughBlocks）：露在外面的边用正常颜色，被挡住的边淡色显示，与建筑工具的选区框一样。
- Loot tables in Kinetic data packs (config/kineticcore/datapack) load without the entries whose item is missing, for example after a mod was removed, and the rest of the table keeps working; vanilla would drop the whole table. The log names the entries left out, once per table.
- Kinetic 数据包（config/kineticcore/datapack）中的战利品表在加载时去掉物品缺失的条目（例如移除了某个模组），表的其余部分照常生效；原版会让整张表失效。日志按表列出被去掉的条目，每张表一行。

2026年11月2日 — 26.11.2 — Spawn platform switch, tooltips beside the hovered slot, Actually Subtractions crash / 出生平台开关、提示框紧贴悬停格与实用拓展崩溃
- Custom spawn: ground at any height counts as a place to stand, down to the bottom layer of the world (a flat world's bedrock floor), so no obsidian platform is laid in the sky above it. A new F6 switch, Obsidian Safety Platform (on by default), decides whether a 3x3 platform is laid when no ground is found at all; when off, no blocks are placed.
- 自定义出生点：任何高度的地面都可作为落脚点，最低到世界底层（如超平坦世界的基岩层），不再在其上方的空中铺黑曜石平台。F6 新增“黑曜石保护平台”开关（默认开启），决定完全找不到地面时是否铺 3x3 平台；关闭后不放置任何方块。
- Item grids and entity card grids open a tooltip beside the hovered slot or card instead of beside the whole row, so it stays next to the cursor and only keeps that one item uncovered.
- 物品网格与实体卡片网格的提示框改为紧贴悬停的格子或卡片显示，不再推到整行的外侧；提示框只避开被悬停的那一项。
- Fixed a crash when entering a world with Actually Subtractions (the 1.20.1 port of Actually Additions) and ContentStudio installed. That mod locks its engineer villager's trade lists; the trade lists Kinetic hands to add-ons can now always be changed.
- 修复同时安装 Actually Subtractions（实用拓展的 1.20.1 移植版）与 ContentStudio 时进入世界崩溃的问题。该模组把工程师村民的交易列表设为不可修改；Kinetic 交给附属的交易列表现在始终可以修改。

2026年11月1日 — 26.11.1 — Driven flight brakes sooner / 附属驱动的飞行刹得更快
- A flight driven by an add-on brakes harder when the keys are let go: from top speed it comes to an upright hover in about half a second.
- 附属驱动的超人飞行松开按键后刹得更快：从全速到直立悬停约半秒。

2026年10月31日 — 26.10.31 — Add-ons drive super flight, Kinetic's own flight sounds / 附属驱动超人飞行与自制飞行音效
- Add-ons can grant super flight from an item or effect (KineticSuperFlight.grant) and drive it for the local player (KineticFlightClient.overrideSuperFlight): their own speed, eased in and out, and the flight pose while moving; letting go glides on, slows and then hovers upright, so tapping the keys never flips the pose. In super flight the legs hang still instead of freezing mid-stride.
- 附属可以由物品或效果授予超人飞行（KineticSuperFlight.grant），并为本人接管飞行（KineticFlightClient.overrideSuperFlight）：使用附属给定的速度并平滑加减速，移动时进入飞行姿势；松开按键会继续滑行、减速后直立悬停，点按方向键不会来回切换姿势。超人飞行中双腿自然下垂，不再定格在迈步的姿势。
- Super flight's wind, sonic boom and takeoff blast are mixed from public-domain (CC0) recordings instead of vanilla sounds (see sounds/flight/CREDITS.txt), and the vanilla elytra rush is silenced while super flight is on. KineticClientSounds plays sounds that exist only in a resource pack's sounds.json.
- 超人飞行的风声、音爆与起飞爆鸣改为由公有领域（CC0）录音混制的音效，不再使用原版音效（见 sounds/flight/CREDITS.txt）；超人飞行期间不再播放原版鞘翅风声。KineticClientSounds 可以播放只在资源包 sounds.json 中定义的音效。
- Glow and tint batches draw at once and in order, after clouds and rain, so a mod that defers the level's drawing no longer puts the glow under the tint; on 26.1 overlapping glows no longer cut each other off. On 1.20.1 and 1.21.1 they draw with the view recorded after entities, so effects drawn after weather stay on what they belong to while the camera turns (they used to drift or turn twice).
- 发光与着色批次改为立即按顺序绘制，并放在云和雨雪之后：推迟世界绘制的模组不会再让发光层被着色层盖住；26.1 上重叠的发光面不再互相遮挡。1.20.1 与 1.21.1 上改用实体渲染后记录的视角绘制，雨雪之后绘制的特效在转动镜头时不再偏离（以前会漂移或被转两次）。
- In the potion and spawn egg editors, the fields and buttons of each row sit as far from the row's right edge as from its top and bottom.
- 药水与刷怪蛋编辑器中，每行的输入框与按钮离行右边框的距离与上下一致。

2026年10月27日 — 26.10.27 — Super flight effects, page views for screenshots / 超人飞行特效与页面视图截图
- Custom spawn eggs can specify main-hand, off-hand and four armor-slot items, including their NBT or components. The compact item grid shows vanilla equipment-slot silhouettes; click to search and replace, right-click to edit data or clear. Equipment is saved with presets, shown in the mob preview and applied to ordinary, thrown and spawner-generated mobs. Missing attribute/effect translations no longer repeat raw language keys, and row controls are vertically centered in both editors.
- 自定义刷怪蛋新增主手、副手和四个盔甲槽位，保留装备的 NBT／组件；紧凑物品格使用原版装备空槽图案，点击搜索更换，右键编辑数据或清空。支持记录保存、生物预览、普通使用、投掷和刷怪笼。属性与药水效果缺失翻译时仅显示注册 ID，两种编辑器的行内输入框和按钮改为垂直居中。
- Superman flight has effects and sounds that follow its speed: rushing wind that grows louder and higher, cloud streaks and speed lines, a vapour cone as the flyer nears the sound barrier (320 m/s by default), a sonic boom with a shock ring and a flash when crossing it, a blast on fast takeoffs, and a white-hot shield with plasma flames streaming back at twice the barrier speed. Nearby players who fly that fast show the same effects. Everything, including the barrier and takeoff speeds, volumes and colours, is set on the new Super-Flight Effects page in F6.
- 超人飞行加入随速度变化的特效与音效：风声随速度变响变尖，云气拖尾与速度线；接近音障（默认 320 m/s）时出现音锥，突破时有音爆、冲击环与闪光；极速起飞时有爆鸣；达到两倍音障速度时身前出现白热护盾，等离子火舌向后翻卷。附近高速飞行的玩家也会显示同样的效果。所有内容（包括音障与起飞速度、音量和颜色）都在 F6 新增的“超人飞行特效”页设置。
- Add-ons can draw quads in the world in one batch, either glowing with additive blending (KineticWorldRender.beginGlowBatch) or tinted with ordinary blending so colours keep their hue against a bright sky (beginTintBatch), and play positioned or looping client sounds by id (KineticClientSounds).
- 附属可以批量绘制世界中的面片：叠加混合的发光面（KineticWorldRender.beginGlowBatch），或普通混合、在明亮天空下也保持本色的着色面（beginTintBatch）；并可按 ID 播放定点或循环的客户端音效（KineticClientSounds）。
- Pages can list their tabs and categories (KineticPage.reviewViews) so screenshot tools capture every one of them; the spawn egg editor lists its attribute and effect lists.
- 页面可以列出自己的标签页和分类（KineticPage.reviewViews），截图工具会逐个截图；刷怪蛋编辑器列出了属性与药水效果两个列表。

2026年10月26日 — 26.10.26 — Opaque canvas, complete item bans / 不透明画布与彻底封禁
- Right-click menus show eight rows at most and scroll the rest with a scrollbar, so long menus no longer run off the screen.
- 右键菜单最多显示 8 行，其余用滚动条滚动，长菜单不再超出屏幕。
- The datapack and resource pack priority lists show pack names as fixed text: they can be reordered and removed, never renamed, and have no Add button.
- 数据包与资源包优先级列表的名称改为固定文字，只能排序和移除，不能修改名称，也不再有“添加条目”。
- Config lists can now show item rows: an item slot (click to change it), the item's name in colour and a number beside it; Add opens the item picker (KTConfigPage.itemValueList).
- 配置列表新增物品行样式：物品格（点击更换）、彩色物品名和右侧数值，“添加”直接打开物品选择器（KTConfigPage.itemValueList）。
- The spawn location editor follows the canvas layout (Back, tabs, centred title and Save in the header; search and switches on one row; the list fills the rest), and Back returns to the settings page instead of closing everything.
- 出生地编辑器按画布重新排版（顶栏为返回、分类、居中标题和保存；搜索与开关在同一行；列表铺满下方），返回改为回到设置页，不再直接关闭界面。
- The potion and spawn egg editors are rebuilt around saved records. The left column lists every saved potion or egg of this client (newest first, the one being edited outlined in yellow; right-click to duplicate or delete). On the right, tick as many effects (or attributes) as needed straight in the list; a ticked row shows its level, seconds and Forever switch (or operation and value) in place, and right-click sets particles and icon. New, Save and Generate sit at the top right: Save stores the record in the client config and moves the ticked entries to the top of the list, vanilla entries come before modded ones, and switching records or leaving with unsaved changes asks first. The egg editor shows the mob above its records.
- 药水与刷怪蛋编辑器按“记录”重做：左侧列出本机保存的所有药水或刷怪蛋记录（最新的在最上面，正在编辑的为黄色边框；右键可复制或删除）。右侧列表直接勾选多个药水效果（或属性），勾选的行内就能设置等级、秒数和永久（或运算与数值），右键设置粒子和图标。右上角为新建、保存与生成：保存会把记录写入客户端配置，并把勾选过的条目置顶；原版条目排在模组条目之前；有未保存的修改时切换记录或离开会先询问。刷怪蛋编辑器在记录上方显示生物模型。
- The NBT and data component editor has Back at the top left like every page; Clear and Save are at the top right and the separate Close button is gone.
- NBT 与数据组件编辑器左上角改为“返回”，与其他页面一致；清空与保存在右上角，去掉单独的“关闭”按钮。
- The canvas of every Kinetic screen is opaque in the modern style, and around it the game stays visible and live, only dimmed. The background blur is gone on every version, which also removes its frame-rate cost on 1.20.1.
- 现代风格下所有 Kinetic 界面的画布改为不透明；画布外仍能看到实时变化的游戏画面，只加一层暗色。所有版本都不再模糊背景，1.20.1 也因此不再有模糊带来的掉帧。
- The first-join reward editor has Back, Import Inventory and Save in its header instead of at the bottom.
- 首次加入奖励编辑页的返回、导入背包与保存移到顶栏，不再放在底部。

2026年10月24日 — 26.10.24 — Buttons at the top, standard slots / 按钮置顶与统一物品格
- Screens with many icons stay smooth on 1.20.1 and 1.21.1: rounded shapes, slot outlines and item icons are sent to the GPU in batches instead of one draw call per fill or icon (a full item grid went from thousands of draw calls a frame to a few). Kinetic screens never limited the frame rate; the drop was drawing cost. Add-ons can batch their own icon grids with KineticGraphics.batchItems.
- 物品较多的界面在 1.20.1 与 1.21.1 上保持流畅：圆角形状、格子边框与物品图标改为批量提交，不再每次填充或每个图标单独提交一次（满屏物品网格每帧从数千次绘制降到几次）。Kinetic 界面本身从未限制帧率，掉帧来自绘制开销。附属可用 KineticGraphics.batchItems 批量绘制自己的图标网格。
- One item slot size and one 3D mob card size everywhere: KineticSizes (22 px slots 24 px apart, 50 px mob cards) and the new ItemGridDensity.COMPACT_FLUSH (standard slots without inner padding) for add-on grids.
- 统一物品格与生物卡片尺寸：新增 KineticSizes（物品格 22 像素、间距 24，生物卡片 50 像素）与 ItemGridDensity.COMPACT_FLUSH（无内边距的标准格子），供各附属网格使用。
- Colour swatches are flat right-angled squares of their colour (white outline on hover) instead of small buttons.
- 颜色选择块改为直角纯色方块（悬停白色描边），不再是小按钮样式。
- Command editor: Save at the top right and the command box at the bottom, so its suggestions open over the whole page. Command and value lists: Add (and Done) in the header, the description on one line and the rows right under it.
- 指令编辑：保存移到右上角，指令框放在最底部，补全列表向上展开占用整页；指令列表与数值列表：新增（与完成）放在顶栏，说明压成一行，条目紧接其下。
- The NBT/text editor wraps at the real line width: TTF fonts with fractional glyph widths no longer wrap long before the right edge.
- NBT/文本编辑器按真实宽度换行：使用小数字宽的 TTF 字体时不再离右边很远就换行。

2026年10月23日 — 26.10.23 — Modern style, movable content areas and a real command line / 现代风格、可拖动的内容区域与真正的命令行

- Pages can declare named content regions (region(...)); the layout editor shows them as orange areas that can be moved and resized like controls. The page is rebuilt in the new rectangle, so grids show as many whole cells as fit and leave the rest blank. Dragging the edge between two neighbouring areas resizes both (hold Ctrl to resize one side only).
- Controls can carry a fixed layout name (layoutId(...)), so a saved position no longer depends on the label text or build order. Saved layouts remember the defaults they were made against; when a mod update changes a page's defaults, the old positions are dropped with a notice instead of misplacing controls. Per-row buttons the page positions itself are no longer offered for editing, and Ctrl+Z skips steps that would change nothing.
- The in-page command line is a vanilla text field with the vanilla chat command suggestions: Tab completes, arrow keys choose, copy, paste and selection work, and Enter runs the command (it was swallowed before). Besides /ktlayout, any client or server command can be run from it. While it is open it receives every key first, so Backspace edits the text instead of deleting a hovered entry.
- Back buttons show a left triangle before their label, and buttons that open a menu or list (dropdowns and buttons marked with expands()) show a down triangle after it, the same in every add-on. Cancel and Close at the top-left now read Back.
- A modern style for every Kinetic screen, on by default: rounded corners drawn smoothly at any GUI scale, translucent frosted-glass panels over the blurred game (Minecraft's own menu blur on 1.21.1 and 26.1.2, the same blur added on 1.20.1), glass buttons, dark rounded input fields, pill scrollbars and sliders, rounded menu rows and notices. Item slots keep the vanilla look. modernStyle in config/kineticcore/ui_client.toml (UI & Interaction settings) switches back to the classic square style.
- The layout editor selects several items: Shift+click adds or removes one, dragging a box on empty space selects everything inside it, Ctrl+A selects every control. Dragging or the arrow keys move the whole selection, and right-click restores all of it. Double-clicking an item opens the command line with its numbers (/ktlayout set x y width height) to type exact values.
- New /ktlayout sub-commands, listed by the vanilla suggestions: reset (the whole page back to its defaults), set x y [width height], align left|right|top|bottom|centerx|centery (several items to their common edge, one item to the canvas margin or centre), same width|height|size (as the item clicked last) and distribute horizontal|vertical (equal gaps). Each is one undo step.
- Hover and selection are easier to see: the outline breathes towards white and back (instead of a faint dimming), the current choice has a full-pixel outline, and in the modern style a soft halo breathes around the item, with a faint tint inside cards, rows and fields. Autocomplete lists use the frosted rounded look.
- An empty input field is no longer marked red before anyone has clicked into it; it shows the error once it was visited and is still empty or invalid. Confirmation dialogs say Back instead of Cancel.
- While editing, problems are marked red: moved controls that overlap another control, items outside the 4 px canvas margin and buttons too narrow for their label; the hover label names the problem, and the save question says how many remain.
- Text too long for its space shows its start with an ellipsis and scrolls through in full only while the mouse is over it, starting from the beginning each time.
- Tooltips are placed beside what they describe and always stay on screen: a button's or field's tooltip opens below it (above when there is no room), an item's to its right or left, a menu entry's beside the menu. Tooltips of buttons at the top of a screen no longer reach past its edge. A list row's or grid cell's tooltip opens beside or below that row, never on top of it; pages that draw their own rows or cards place theirs with tooltipAnchor(x, y, width, height).
- Buttons in dropdown menus have 2 px between them.
- 1.20.1: a page opened from the chat screen (TextStudio's chat history) no longer stays invisible in the modern style; the frosted background left depth behind that hid the page.
- 页面可声明命名内容区域（region(...)）；布局编辑器中显示为橙色区域，可像控件一样移动和调整大小。页面按新矩形重建，网格按能放下的整格数显示，剩余部分留白。拖动两个相邻区域之间的边会同时调整两边（按住 Ctrl 只改一边）。
- 控件可设置固定布局名（layoutId(...)），保存的位置不再受按钮文字或创建顺序影响。保存的布局会记录当时的默认位置；模组更新改变页面默认布局后，旧位置会被停用并提示，不会错位。页面自行定位的每行按钮不再出现在编辑中，Ctrl+Z 会跳过不产生变化的步骤。
- 页面内命令行改为原版输入框并带原版聊天命令补全：Tab 补全、方向键选择，支持复制、粘贴和选择，回车可执行（此前回车被吞掉）。除 /ktlayout 外也可执行任意客户端或服务器命令。命令行打开时优先接收所有按键，退格只编辑文字，不会删除鼠标下的条目。
- 返回按钮在文字前显示向左三角，打开菜单或列表的按钮（下拉框及标记 expands() 的按钮）在文字后显示向下三角，所有附属外观一致。左上角的“取消”“关闭”统一改为“返回”。
- 所有 Kinetic 界面新增现代风格（默认开启）：任意界面缩放下都平滑的圆角、模糊游戏画面上的半透明毛玻璃面板（1.21.1 与 26.1.2 使用原版菜单模糊，1.20.1 补上同样的模糊）、玻璃质感按钮、深色圆角输入框、胶囊形滚动条与滑块、圆角菜单行与提示。物品格保持原版样式。config/kineticcore/ui_client.toml 中的 modernStyle（“界面与交互”设置）可切回经典直角风格。
- 布局编辑器支持多选：Shift+点击加选或取消，在空白处拖框选中框内全部项，Ctrl+A 全选控件。拖动或方向键会移动全部选中项，右键会把它们全部恢复默认。双击某项会打开命令行并填好它的数值（/ktlayout set x y 宽 高），可直接输入精确数值。
- 新增 /ktlayout 子命令（原版补全会列出）：reset（本页全部恢复默认）、set x y [宽 高]、align left|right|top|bottom|centerx|centery（多项对齐到共同边缘，单项对齐到画布边距或中线）、same width|height|size（与最后点击的项同宽/同高/同大小）、distribute horizontal|vertical（等距排列）。每条命令都可一次撤销。
- 悬浮与选中提示更明显：描边的呼吸改为向白色提亮再回落（原来只是轻微变暗），当前选中项描边加粗到 1 个界面像素；现代风格下控件外圈还有随呼吸变化的柔和光晕，卡片、列表行和输入框内部带淡淡的同色底。自动补全列表也改为毛玻璃圆角样式。
- 空输入框在没被点过之前不再显示红框；点过之后仍为空或无效才显示错误。确认弹窗里的“取消”改为“返回”。
- 编辑时用红色标出问题：移动后与其他控件重叠、超出画布 4 像素边距、按钮宽度放不下文字；悬停标签会写明问题，保存询问中会提示还剩几处。
- 超出空间的文字平时显示开头加省略号，只在鼠标悬停时从头开始来回滚动显示全文。
- 悬浮提示放在所描述内容的旁边，并始终完整显示在屏幕内：按钮和输入框的提示显示在其下方（放不下时在上方），物品的提示在其左右两侧，菜单项的提示在菜单旁边。屏幕顶部按钮的提示不再超出屏幕边缘。列表行和物品格的提示显示在该行旁边或下方，不再盖住它；自绘行或卡片的页面用 tooltipAnchor(x, y, 宽, 高) 指定位置。
- 下拉菜单中的按钮之间留 2 像素间距。
- 1.20.1：从聊天界面打开的页面（TextStudio 的抓取历史）在现代风格下不再看不见；原因是毛玻璃背景残留的深度把页面挡住了。

---

2026年10月22日 — 26.10.22 — In-page layout editor / 页面内布局编辑器

- One Core command edits the layout of any Kinetic page: type /ktlayout in chat, or press / on an open page and enter ktlayout. Drag a control to move it, drag its edge or corner to resize, turn the mouse wheel to scale it; the arrow keys nudge it by 1 px (Shift: 4 px), or resize it after an edge drag. Controls snap to the canvas margins and centre and to the edges, centres and 4 px gaps of neighbouring controls, with guide lines like an image editor; hold Alt to place freely. Right-click restores a control's default, Ctrl+Z undoes. A label shows the position, size and scale of the selected control.
- Entering the command again asks whether to save. Saved layouts go to config/kineticcore/gui-layouts/<modid>.json, one file per mod, and only hold the controls that differ from their defaults; deleting an entry or the file restores the original layout. Every position stays inside the 640×360 canvas.
- The item search and item list editor start their content 6 px higher (header divider at 24, content at 28). The item list editor's Rules button shows 规则 again in Chinese (it showed ??), and add-ons can handle clicks on item cards themselves (for example to open a settings dialog or the NBT editor) and give #tag / @mod picks their own rule form.
- The entity selector's confirm button reads Apply again (it was labelled Back, the same as the real Back button); Clear Selection is shortened to Clear so it fits its button.
- Autocomplete suggestion lists on canvas pages stay inside the canvas: a field near the right edge no longer pushes its list past the frame; longer suggestions are clipped inside their row.
- 核心提供统一的页面布局编辑命令：在聊天栏输入 /ktlayout，或在打开的页面中按 / 后输入 ktlayout 即可进入编辑模式。按住拖动控件移动位置，拖动边缘或角调整大小，滚轮按比例缩放；方向键每次微调 1 像素（Shift 为 4 像素），拖动过边缘后方向键改为微调大小。控件会自动吸附画布边距、中线以及相邻控件的边缘、中心和 4 像素间距，并显示参考线（类似 PS），按住 Alt 可自由放置。右键恢复控件默认位置，Ctrl+Z 撤销；选中控件时显示位置、尺寸与缩放比例。
- 再次输入命令时询问是否保存。布局保存在 config/kineticcore/gui-layouts/<模组ID>.json，每个模组一个文件，只记录与默认不同的控件；删除对应条目或文件即可恢复原布局。所有位置都限制在 640×360 画布内。
- 物品搜索与物品列表编辑器的内容上移 6 像素（顶栏分隔线在 24，内容从 28 开始）；列表编辑器的“规则”按钮恢复中文（原显示为 ??）；附属可自行处理物品卡片点击（例如打开设置弹窗或 NBT 编辑器），并为 #标签 / @模组 选择指定自己的规则格式。
- 生物选择器的确认按钮恢复为“应用”（此前误写为“返回”，与左上角返回按钮同名）；“清空选择”缩短为“清空”，完整显示在按钮内。
- 画布页面中的自动补全候选列表保持在画布内：靠近右边缘的输入框不再让候选列表超出外框，过长的候选在行内截断。

---

2026年10月20日 — 26.10.20 — Consistent canvas and dedicated list editors / 统一画布与专用列表编辑

- Core configuration screens and add-on pages share the four-point outer margin, so the same 640×360 canvas has matching visible bounds.
- Editors keep one white outer canvas frame; content areas and grid backgrounds use open surfaces and gray-white divider lines, while controls, cards and selection states retain their outlines.
- Hovered controls and current selections use distinct breathing outlines. Entity cards keep green membership and red errors visible independently from the current editing item.
- Integer-list settings support dedicated visual editors while preserving integer validation, unsaved drafts and server-controlled saving.
- Scrolling controls share one clipping region for drawing, hovering, clicking and tooltips. FTB inventory sidebars are hidden only on Core-owned screens, including container editors.
- 核心配置界面与附属页面统一使用四点外边距，相同的 640×360 画布显示为一致的边框大小。
- 编辑界面仅保留白色画布外框，内容区与网格背景改用开放区域和灰白色分割线；控件、物品格及选中状态保留对应描边。
- 悬浮与当前选择使用不同颜色的呼吸描边；生物卡片可分别显示绿色已配置状态、红色错误与当前编辑项，悬浮时也不会遮掉错误颜色。
- 整数列表配置支持附属提供专用可视化编辑器，保留整数校验、草稿及服务端保存权限。
- 滚动控件的绘制、悬停、点击及提示统一遵循视口裁剪；仅在核心托管界面中隐藏 FTB 背包侧栏，容器编辑页也保持一致。

- Native item tooltips can append page instructions without losing structured item components; unsaved draft attributes are captured while the preview is active.
- Generic lists use compact rows; single-color dialogs are centered, and NBT editing uses the four-point margin with wheel input limited to its body.

- Enter and keypad Enter confirm dialogs without activating focused list rows or action buttons; text input and completion keep their normal behavior.
- Layout validation also records container editors, so recipe workstations receive the same frame and control checks as ordinary pages.
- Notifications stack with a two-point gap, including wrapped messages and entrance animations, without overlapping their borders.
- Add-ons can query the reserved title-screen account and startup-information area through the overlay API, keeping their own notices below it without jumping when the timer fades.

---

2026年10月16日 — 26.10.16 — Native spawner templates and shared editor controls / 原生刷怪笼模板与通用编辑控件

- Custom spawn eggs configure vanilla spawners with the selected name, attributes and effects through native SpawnData, both when used directly and when thrown at a spawner. Later waves keep the template, and the spawner retains its timing, range and other settings.
- 自定义刷怪蛋直接使用或投掷命中原生刷怪笼时，会通过原生 SpawnData 写入指定名称、属性与效果；后续波次保持模板，并保留刷怪笼原有的延迟、范围等设置。

- Lists calculate their scroll limit from the actual clipped viewport: the final entry is fully visible at the end without trailing blank rows, including draggable scrollbars and partial rows.
- Entity cards use a shared 50×50 size with 2 px gaps; full-width selectors fit twelve columns. Selected borders breathe while retaining their state color.
- Add-ons can bind Delete to their existing deletion action. Text fields keep normal text deletion; Enter and keypad Enter confirm modal dialogs, and Escape cancels them.
- Item rule editors open the shared item multiselector directly, supporting tags, mod namespaces and caller-owned rule registries. Saving and cancelling return to the originating screen.
- Large configuration pages use the 640×360 canvas, compact top controls and less left indentation. The index shows the installed add-on count and a Close button; client entries are white and server entries cyan. Reset All applies immediately after confirmation.
- 列表按实际裁剪区域计算滚动末端，最后一项完整显示后准确停止，不再多出空白行，支持拖动滑块与非整行视口。
- 生物格统一为50×50、间距2点，完整宽度的选择器可显示12列；选中描边保留状态颜色并加入呼吸明暗效果。
- 附属可将Delete绑定到已有删除操作；文本输入保留正常字符删除，确认框支持回车与小键盘回车确认、Esc取消。
- 物品规则直接进入核心多选搜索器，支持标签、模组与调用方注册表规则；保存及取消返回原界面。
- 大配置页统一640×360画布，压紧顶部按钮与左侧缩进；首页显示实际附属数量及关闭按钮，客户端配置白色、服务端配置青色；全部重置经二次确认后立即应用。

---

2026年10月15日 — 26.10.15 — Dropdown labels and entity cards / 下拉选项与生物卡片

- Dropdown menus show an add-on's label for each option instead of the stored value next to it, so options such as "add" no longer appear twice in two languages. Lists of registry IDs (biomes, attributes and so on) keep the ID with its translated name.
- The entity selector puts Back, Clear and Apply in the top row and the selected count beside a shorter search field, so the grid shows six rows. Each entity card is an item slot with a 2 px gap to its neighbours; selected entities have a single green outline instead of a double one that ran into the next card.
- Entity cards in every screen use the item slot background instead of a near-white checkerboard.

- 下拉菜单的每个选项只显示附属提供的名称，不再在旁边重复显示存储值，例如“add”不会再以两种语言出现两次；生物群系、属性等注册 ID 列表仍显示 ID 与译名。
- 生物选择器把返回、清空与应用放在顶部一行，已选数量放在缩短的搜索框右侧，网格可显示六行。每个生物卡片都是带 2 像素间距的物品格；已选生物使用单层绿色描边，不再用会与相邻卡片连在一起的双层描边。
- 所有界面的生物卡片改用物品格背景，不再使用接近纯白的棋盘格。

---

2026年10月14日 — 26.10.14 — Uniform entity model cards / 统一的生物模型卡片

- The entity selector uses the same small model cards as the other entity grids, so it shows twelve columns and five rows of entities at once instead of eight by three. Its search field and Filter button line up with the grid.
- Entity previews leave room for models that turn and for legs, wings and other parts that reach past the hit box, so wide mobs such as spiders and camels keep a gap to the left and right frame as well as above and below.
- The item selector's category buttons are slightly wider and the item grid moves right accordingly; the grid is exactly as wide as its twenty columns, so the gap before the scrollbar matches the gap after the category list.
- The spawn egg and potion editors use one panel: name, mob preview or potion form and Generate on the left; on the right, Attributes and Potion Effects tabs with a search field, a list of IDs (with translated names in other languages than English), and an editor under the list for the clicked entry with Apply and Remove. Entries on the item have a green outline and their values at the end of the row; the entry being edited has a yellow outline. Saved drafts and New draft sit in the header.
- Clicking a list row right after scrolling the list now always hits that row. Before, a click while the list was still gliding to its new position could do nothing, for example when unticking an accessory slot just scrolled into view.
- While a list scrolls, the part-visible row at its bottom stays drawn and clickable. Before, it disappeared during the last part of each scroll in lists whose height is not a whole number of rows.

- 生物选择器使用与其他生物网格相同的小号模型卡片，一屏可显示 12 列 5 行生物，不再是 8 列 3 行；搜索框与筛选按钮与网格对齐。
- 生物预览为旋转的模型以及超出碰撞箱的腿、翅膀等部位预留空间，蜘蛛、骆驼等较宽的生物左右与边框也保持间距，不再贴边。
- 物品选择器左侧分类按钮略微加长，物品网格相应右移；网格宽度正好容纳 20 列，滚动条前的间距与分类列表后的间距一致，右侧不再多出空隙。
- 刷怪蛋与药水编辑器合并为一个面板：左侧为名称、生物预览或药水形态以及生成按钮；右侧为属性与药水效果标签页和搜索框、显示 ID 的列表（非英文时附带译名），以及列表下方针对所点条目的编辑区（应用、移除）。已加入物品的条目显示绿色边框并在行末显示数值，正在编辑的条目显示黄色边框；已存方案与新建方案位于顶部。
- 滚动列表后立即点击某一行，现在总能命中该行；此前列表仍在平滑滚动时点击可能无效，例如取消勾选刚滚入视野的饰品槽位。
- 列表滚动时，底部只露出一部分的那一行会保持显示并可点击；此前在高度不是整行数的列表中，每次滚动的最后阶段该行会消失。

---

2026年10月13日 — 26.10.13 — Spawn egg and potion editors / 生物刷怪蛋与药水编辑器

- Add core editors for special spawn eggs and normal, splash and lingering potions. Edit attributes and multiple effects directly in outlined text lists with small Edit buttons and Reset settings, including modded effects and their icons; saved selected entries move to the top, with vanilla entries first in each group. Spawn eggs use a square checkerboard model preview and a single-entity picker; potions switch forms through three expanded buttons.
- Custom names and separate saved drafts let players switch between mobs and potions, generate items and keep adjusting each draft. Deselected entries retain their edited parameters, and runtime-added attributes survive world reloads.
- Special spawn eggs support player throwing and redstone dispensers. Templates apply synchronously at LOWEST completion priority after entity insertion and extra data, without a tick delay; normal dispenser items keep their existing behavior. Add reusable item-creation and spawn-completion APIs.
- Raise the default network NBT allowance to 16 MiB. Existing configured values are preserved, and larger values remain configurable.

- 核心新增特殊刷怪蛋及普通、喷溅、滞留药水编辑器。右侧文字描边列表直接多选属性和多重效果，每项带小编辑按钮，子页支持重置和保存，包含模组效果与效果图标；保存后的已选项置顶，同组原版项优先。生物预览使用正方形棋盘格与单选生物选择器；药水形态通过展开的三项按钮切换。
- 支持自定义名称与独立保存的编辑方案，可在多个生物或药水之间切换、生成并继续微调。取消勾选保留参数，运行时新增属性可在重进世界后恢复。
- 特殊刷怪蛋支持玩家投掷和红石发射器。实体加入世界、附加数据处理完成后，以 LOWEST 完成优先级同步应用模板，不使用 tick 延迟；普通发射器物品保留原行为。新增可复用的物品生成与刷怪完成 API。
- 网络 NBT 容量默认值提高至 16 MiB，保留已有配置值，并允许继续配置更大的容量。

---

2026年10月13日 — 26.10.13 — Full-screen shade and aligned titles / 全屏遮罩与标题对齐

- The background shade of a canvas page covers the whole screen instead of leaving a darker canvas-sized box.
- Titles of the configuration, list, selector and command editor screens line up with their Back buttons, and the Back button keeps a two-pixel gap to the frame.

- 画布页面的背景遮罩覆盖整个屏幕，不再留下画布大小的暗框。
- 配置、列表、选择器和指令编辑页的标题与返回按钮对齐，返回按钮与边框保持两像素间距。

---

2026年10月12日 — 26.10.12 — Searchable selection menus / 可搜索的选择列表

- Multi-select menus and searchable lists use framed text rows with two-pixel gaps. IDs are white and translated names are gold; long text has separate clipping regions. Selection changes only the border; switched-on rows of a multi-selection are green. Lists with ten or more options have a search field. Short single-choice menus, such as dropdowns that switch pages, modes or categories, open as buttons with the current option in yellow.
- Add-ons can show item-grid stack counts and durability while keeping the original icon size and grid spacing.
- Item-editing labels use data components on 1.21.1 and 26.1.2; 1.20.1 keeps item NBT terminology.
- On 26.1.2, unhandled clicks over custom drawing regions reach the page, allowing recipe slots to be selected while preserving scrollbar input.

- 多选菜单和可搜索的列表使用描边文本行，间隔两像素；ID 白色、译名金色，长文本分区显示，选中只改变边框，多选中已开启的行为绿色。十项及以上的列表自动提供搜索框。切换页面、模式或分类这类较短的单选下拉菜单以按钮展开，当前项为黄色。
- 附属可在物品网格中显示数量与耐久覆盖层，同时保持原有图标比例和格子间距。
- 1.21.1 和 26.1.2 的物品编辑文字改为数据组件；1.20.1 保留物品 NBT 称呼。
- 修复 26.1.2 自绘区域吞掉未处理点击的问题，配方槽位可以正常选中，滑块输入继续由控件处理。

---

---

2026年10月10日 — 26.10.10.4 — Item browser and editor navigation / 物品搜索器与编辑页导航

- The item browser has a narrower category column, an extra item column and category row, and thirteen complete item rows. Scrolling to the bottom keeps the final row visible without an extra blank row.
- Numeric configuration fields and list-edit buttons are shorter. Add-ons can open their visual rule editors from both individual configuration pages and the mod's combined page.
- Use the item browser's spare horizontal space for wider category buttons while retaining twenty item columns. Back buttons are at the upper left of core editors; the inventory-style first-join editor uses the lower left.
- Item search indexing updates the percentage at the end of one notification without flashing between messages. Completion replaces the progress with the indexed item count.
- Fix a startup crash when an add-on checks client world, player or connection state before Minecraft has initialized.
- Item-card button icons now use the API checkerboard slot background at their original icon size.
- Identifier selection rows have thin outlines and two-pixel gaps; selection changes only the border color. Back controls use white text.

- 物品搜索器缩短左侧分类，增加一列物品和一行分类按钮，完整显示十三行物品；滚动到底部时保留完整的最后一行，不再多出空行。
- 缩短数字配置输入框和列表编辑按钮；附属可在单独配置页和模组总配置页中打开自己的可视化规则编辑器。
- 利用物品搜索器的横向余量加宽分类按钮，同时保持二十列物品；核心编辑页的返回按钮统一放在左上角，首次加入奖励的背包式编辑页放在左下角。
- 物品搜索索引在同一条提示的末尾持续更新百分比，不再反复闪烁；构建完成后改为显示完成提示与物品数量，不再显示百分比。
- 修复附属在 Minecraft 初始化前查询客户端世界、玩家或连接状态时可能导致的启动崩溃。
- 物品卡片按钮的图标统一使用 API 棋盘格背景，保持原有图标大小。
- ID 选择条增加细描边，条目间保留两像素间距，选中时只改变边框颜色；返回入口使用白色文字。

---

2026年10月10日 — Data pack loading recovery / 数据包加载防护

- Singleplayer data pack loading errors now show the failing pack, resource file and cause. Locate the file or archive, open the data pack directories, or temporarily skip identified faulty local packs and retry. Skipping applies to whole packs for this world-opening session; leaving and reopening the world loads the original packs again. Saved pack choices and pack files are preserved. Dedicated servers report the diagnostics in their logs.

- 单人存档加载数据包失败时，显示错误数据包、资源文件和原因。可定位文件或压缩包、打开数据包目录，或暂时跳过已定位的错误本地包并重试。跳过整个数据包，仅在本次打开存档期间生效；退出后重新进入仍加载原来的包，保留存档的数据包选择和包内文件。专用服务器在日志中记录诊断信息。

---

2026年10月09日 — Dismiss flight tutorials / 关闭飞行教程

- Creative flight tutorials include a clickable dismiss action. Your choice is remembered for this player in the current world, including after respawning or rejoining; other players and worlds keep their tutorials.

- 创造飞行教程新增“点击关闭提示”，当前存档会记住这个玩家的选择，重生或重新进入存档后仍然生效；其他玩家和其他存档不受影响。

---

2026年10月08日 — Identifier list colors / ID 列表颜色

- Plain text lists preserve their text colors when selected; selection changes the row outline only.

- 纯文本列表选中时保留原有文字颜色，仅改变行边框颜色。

---

2026年10月08日 — Identifier list spacing / ID 列表间距

- Plain text identifier lists keep 2 px between their striped rows in both single-selection and multi-selection views. Clicking the gaps does not select a neighbouring row.

- 单选、多选的纯文本 ID 列表统一在深浅条纹行之间保留 2 像素间隔，点击间隔不会误选相邻条目。

---

2026年10月08日 — Checkerboard item backgrounds / 棋盘格物品背景

- Item backgrounds use one complete 8×8-pixel texture containing 8 rows and 8 columns of alternating colour. It fills square slot interiors with equal horizontal and vertical scaling, without an extra black inset. Rectangular areas repeat complete tiles; borders and item positions are preserved.

- 物品背景使用一张完整的 8×8 像素贴图，包含 8 行、8 列交替色块，等比铺满正方形格子的内部，不再保留额外黑色内衬。长方形区域重复拼接完整贴图，保留边框及物品位置。

---

2026年10月08日 01时41分 — Item selector grid / 物品选择器网格

- The item selector repeats a single-slot texture for each cell, with 2 px between cells and 2 px of inner padding. The existing panel shows 19 columns and 12 visible rows; scroll to browse the remaining items. Forge 1.20.1, NeoForge 1.21.1 and NeoForge 26.1.2 use the same layout.

- 物品选择器重复使用单格贴图，格子之间以及网格内边距均为 2 像素。原有面板内显示 19 列、12 行，剩余物品通过滚动浏览。Forge 1.20.1、NeoForge 1.21.1 与 NeoForge 26.1.2 的排版相同。

---

2026年10月08日 00时55分

- Removed log deduplication and repeat-count summaries. Repeated messages are logged individually; keyword filtering, errors-only mode and old-log cleanup are preserved. The obsolete `log_cleaner.deduplication` setting and its configuration control are removed automatically.

- 移除日志去重与重复次数汇总，重复日志逐条输出；保留关键词过滤、仅错误日志模式和旧日志清理。自动移除旧的 `log_cleaner.deduplication` 配置项及对应界面开关。

---

2026年10月07日 17时30分

- Horizontal tab bars keep 2 px between their tabs, the same spacing as vertical tab bars, instead of tabs touching each other.

- 横向标签栏的标签之间保持 2 像素，与纵向标签栏的间距一致，不再相互紧贴。

---

2026年10月07日 00时40分

- Screens keep the same layout in every language: config controls have one width per type (numbers, text, choices) instead of growing with their value or option names, tab strip tabs have one width instead of growing with their labels, and the item selector's filter label no longer moves with the length of the item count before it. Text that does not fit scrolls.

- 所有语言下界面排版相同：配置页的控件按类型（数字、文字、选项）使用固定宽度，不再随数值或选项名称变宽；标签栏中的标签使用统一宽度，不再随文字变宽；物品选择器的筛选标签不再随前面物品数量文字的长度移动。放不下的文字滚动显示。

---

2026年10月06日 23时27分

- Text that is too long for its space scrolls at about three characters a second (18 GUI pixels a second) instead of about one, still pausing half a second at each end.

- 放不下的文字滚动速度约为每秒 3 个字符（每秒 18 个界面像素），之前约为每秒 1 个；两端仍各停留半秒。

---

2026年10月06日 07时17分

- A server-managed config page can load its server values on the client when the player joins, so a client that shows or uses those values never runs on its own local file. Only the joining player is answered; nothing is sent to other players, and later admin edits reach other players when they next join.
- A button too narrow for its label with the normal 4 px padding (an icon button such as ▶) keeps the label whole with 2 px of space instead of scrolling it. A label that has to scroll anyway keeps the normal 4 px from the frame.
- Number fields can be optional: an empty field means "not set" and is not marked with a red error border.
- Text shortened with an ellipsis now really fits its space. With resource-pack fonts that have fractional widths, the shortened text came out 1–3 px too wide, so the "..." could touch the text or control next to it.
- On 26.1.2, lists and other clipped areas inside a scaled page cut off up to 2 px at their right and bottom edges, so row frames lost their right border. The clip area now rounds outwards, as on 1.20.1 and 1.21.1.
- The text editor's save button says "Save" instead of "Save NBT", since on 1.20.5+ it also edits item components.
- The shared item search index (used by item pickers, recipe pages and searches) reads the game's items a few milliseconds per tick instead of all at once, so in large modpacks opening such a page no longer freezes the game, which on a server could get the player dropped for timing out.
- An item button whose label would reach its icon centres the label in the space after the icon and scrolls it there, instead of running the text under the icon. Labels that fit stay centred on the whole button.
- Preformatted tooltips now wrap lines that would run past the screen edge, keeping their styles, like text and item tooltips already did. Only a tooltip taller than the screen may exceed it, as in vanilla.

- 由服务端管理的配置页面可以在玩家加入时把服务端数值加载到客户端，显示或使用这些数值的客户端不再依赖自己的本地文件。只回复加入的玩家，不向其他玩家发送任何内容；管理员之后的修改会在其他玩家下次加入时生效。
- 按钮宽度不足以按 4 像素内边距放下文字时（例如 ▶ 这样的图标按钮），文字改为保留 2 像素间距完整显示，而不是滚动。本来就需要滚动的文字仍与边框保持 4 像素。
- 数字输入框可以设为可选：留空表示"不设置"，不再显示红色错误边框。
- 用省略号缩短的文字现在确实放得进自己的区域。使用宽度带小数的资源包字体时，缩短后的文字会宽出 1–3 像素，"..." 可能碰到旁边的文字或控件。
- 26.1.2 上，缩放页面中的列表和其他裁剪区域会在右边和下边多裁掉最多 2 像素，导致行边框缺少右边线。现在裁剪区域向外取整，与 1.20.1 和 1.21.1 一致。
- 文本编辑器的保存按钮显示为"保存"而不是"保存NBT"，因为在 1.20.5+ 上它也用于编辑物品数据组件。
- 共用的物品搜索索引（用于物品选择器、配方页面和搜索）改为每个刻读取几毫秒的物品，而不是一次读完；在大型整合包中打开这类页面不再卡住游戏，在服务器上也不会因此超时被断开。
- 物品按钮的文字会碰到图标时，改为在图标之后的空间内居中并在其中滚动，不再从图标下面穿过。放得下的文字仍在整个按钮上居中。
- 预格式化的悬浮提示现在也会把超出屏幕边缘的行保留样式换行，与文字和物品提示一致。只有比屏幕还高的提示才允许超出，与原版相同。

---

2026年10月05日 13时02分

- On 1.20.5+, item component text (`[damage=5]`) can be edited in the same standard editor used for NBT on Forge, so the screen looks the same on every version.
- On 26.1.2, container pages (for example a trash bin or a recipe editor drawn on a vanilla container texture) drew their background in screen space while the slots and items were drawn in the page canvas, so the background was enlarged and offset from the slots. The background is now drawn in the canvas with the slots, as on 1.20.1 and 1.21.1.
- On 26.1.2, item slots and item grids (for example a Curios slot or an item selector) showed only a plain frame instead of the checkerboard slot texture, and buttons drawn from a texture were blank. They now draw the same textures as on 1.20.1 and 1.21.1.
- The NBT editor now checks the text it opens with, so invalid saved text is marked in red with its error straight away instead of showing a green check until the first edit.
- Lists of identifiers can use plain text rows instead of buttons: striped 14 px rows packed without gaps, with vertically centered text that keeps its padding and scrolls when too long. The current choice is yellow, the hovered row is outlined and, in toggle lists (multi-select), chosen rows are green.
- Toggle lists now mark chosen rows green instead of yellow, in both row styles, and buttons in multi-select groups can show a green "picked" state. Yellow stays the current choice.
- Search suggestion popups no longer draw a box around every row; only the hovered row and the keyboard choice are outlined, and suggestion text keeps 4 px from the row edges.
- Text that does not fit its space is never cut off: up to twice the space it scrolls back and forth, and longer text shows its start with an ellipsis and scrolls through in full while the mouse is over it. This applies to every scrolling text drawn by the core and by addons.
- Button labels keep 4 px from the button edge, clear of the frame, instead of vanilla's 2 px on the bevel, and follow the same scrolling rule.
- A text field that is not being edited shows its text from the first character. A long value no longer stays scrolled to its end after it was set or picked from the suggestions, which made it look shifted against the frame.
- 3D mob previews keep the model 3 px inside their cell (the 1 px frame plus 2 px of space) instead of drawing up to the frame.
- Context menus tell single choices from on/off switches: the current option is yellow, and enabled switches (a multi-selection) are now green instead of yellow.

- 1.20.5+ 的物品数据组件（`[damage=5]`）可在与 Forge 编辑 NBT 相同的标准编辑器中编辑，各版本界面一致。
- 26.1.2 上的容器页面（例如垃圾桶或基于原版容器贴图的配方编辑器）背景按屏幕坐标绘制，而槽位与物品按页面画布绘制，导致背景放大并与槽位错位。现在背景与槽位一起在画布中绘制，与 1.20.1 和 1.21.1 一致。
- 26.1.2 上的物品槽和物品网格（例如饰品槽或物品选择器）只显示一个空边框而没有棋盘格槽位贴图，使用贴图的按钮也显示为空白。现在与 1.20.1 和 1.21.1 绘制相同的贴图。
- NBT 编辑器打开时即检查初始文本，已保存的无效文本会立即以红色标出并显示错误，不再在首次编辑前显示绿色对勾。
- ID 列表可以用纯文字行代替按钮显示。行高 14 像素、条纹背景、行间无空隙；文字垂直居中并保留内边距，过长时滚动。当前选择为黄色，光标所在行加边框，开关列表（多选）中已选的行为绿色。
- 开关列表中已选的行在两种行样式下都改为绿色（原为黄色）。多选组的按钮可以显示绿色"已选"状态。黄色继续表示当前选择。
- 搜索候选弹窗不再给每一行画边框，只有光标所在行和键盘选中项带边框；候选文字与行边缘保持 4 像素。
- 放不下的文字不再被截断：超出不到一倍时来回滚动；更长的文字显示开头加省略号，鼠标悬停时滚动显示完整内容。核心与附属绘制的所有滚动文字均适用。
- 按钮文字与按钮边缘保持 4 像素、不压边框（原版为 2 像素，压在斜面上），并遵循相同的滚动规则。
- 不在编辑中的输入框从第一个字符开始显示。设置或从候选中选取较长的值后，不再停留在末尾，因此不会看起来贴着边框偏移。
- 生物 3D 预览中的模型与格子边缘保持 3 像素（1 像素边框加 2 像素间距），不再画到边框上。
- 右键菜单区分单选与开关：当前选项为黄色；开启的开关行（多选）改为绿色（原为黄色）。

---

2026年10月04日 13时42分

- Item and text tooltips near the screen edge no longer run off the screen: long lines wrap to the room left on the wider side of the cursor, on every supported version.
- Text that is too long for its space scrolls back and forth at a steady speed, twice as fast as before, pausing for half a second at the start and at the end instead of slowing down near them, and glides smoothly instead of moving a whole pixel at a time.

- 屏幕边缘的物品和文字悬浮提示不再超出屏幕：过长的行会按光标较宽一侧剩余的空间自动换行，所有支持的版本均生效。
- 超出可用空间的文字以匀速左右往返滚动，速度比之前快一倍，到开头和结尾时各停顿半秒，不再在接近两端时减速，并且平滑移动，不再一次跳动一个像素。

---

2026年10月03日 20时11分

- Added Minecraft 26.1.2 (NeoForge 26.1.2.112, Java 25). Its JAR is kineticcore-neoforge-26.1.2-<version>.jar.
- On 26.1.2 everything follows 26.1's own formats: item text uses 26.1's /give syntax, for example enchantments={"minecraft:protection":2}, and the default first-join rewards are written in it.
- First-join rewards are marked as received only after every reward was granted. If a reward item in the config fails to parse, nothing is cleared, granted or marked, and the error is logged; after the config is fixed, the player still gets the rewards on the next join within the first minute.
- The compact potion-effect display works on 26.1.2, including JEI keeping clear of it.
- On 26.1.2 new players start at the custom spawn through 26.1's new login flow.
- The mod's pack.mcmeta follows each version's format, so 26.1.2 no longer logs a pack metadata warning.

- 新增 Minecraft 26.1.2 支持（NeoForge 26.1.2.112，Java 25），JAR 为 kineticcore-neoforge-26.1.2-<版本>.jar。
- 26.1.2 完全使用 26.1 自己的格式：物品文本使用 26.1 的 /give 写法，例如 enchantments={"minecraft:protection":2}，默认的首次进入奖励也按此写法生成。
- 首次进入奖励只有全部发放成功后才打上已领取标记。配置中有奖励物品解析失败时，既不清理快捷栏、不发放也不标记，并在日志中报错；修好配置后，玩家在游玩 1 分钟内再次进入仍可领取。
- 紧凑的药水效果显示支持 26.1.2，JEI 同样会避开它。
- 26.1.2 中新玩家通过 26.1 新的登录流程出生在自定义出生点。
- 模组的 pack.mcmeta 按各版本自己的格式生成，26.1.2 不再出现资源包元数据警告。

---

2026年10月03日 16时21分

- Items as text strictly follow each version's `/give` syntax again: `id{NBT}` on 1.20.1, `id[components]` on 1.21.1. The conversion of 1.20.1 `{NBT}` text on 1.21.1 is removed; such entries are reported as an error in the log and not given. 1.21.1 configs that still use `{NBT}` must be rewritten, for example `1x minecraft:leather_helmet[enchantments={levels:{"minecraft:protection":2}}]`.

- 以文本表示的物品重新严格遵循各版本的 `/give` 写法：1.20.1 为 `物品ID{NBT}`，1.21.1 为 `物品ID[组件]`。移除在 1.21.1 中转换 1.20.1 `{NBT}` 写法的功能；这类条目会在日志中报错且不会发放。仍使用 `{NBT}` 的 1.21.1 配置需要改写，例如 `1x minecraft:leather_helmet[enchantments={levels:{"minecraft:protection":2}}]`。

---

2026年10月03日 00时21分

- Log deduplication is back (`log_cleaner.deduplication`, on by default): consecutive identical logs are written once, followed by their repeat count before the next different log.
- Errors-only mode (`log_cleaner.errors_only`) is now off by default on clients as well as servers, so normal logs are no longer hidden. Existing config files keep their value; set it to `false` to see all logs.

- 恢复日志去重功能（`log_cleaner.deduplication`，默认开启）：连续相同的日志只输出一次，并在下一条不同日志前输出重复次数。
- 仅错误日志模式（`log_cleaner.errors_only`）在客户端和服务端都改为默认关闭，正常日志不再被隐藏。已有配置文件会保留原值，需要看到全部日志时请设为 `false`。

---

2026年10月03日 00时14分

- Fixed first-join equipment on 1.21.1 arriving without enchantments when the config still used the 1.20.1 `{NBT}` syntax. On 1.21.1, items written as `id{NBT}` are now upgraded to components the same way the game upgrades old worlds, so configs copied from 1.20.1 keep working; the `id[components]` syntax works as before. Text the game cannot read is now reported in the log instead of being ignored.

- 修复 1.21.1 中首次加入发放的装备在配置仍使用 1.20.1 `{NBT}` 写法时不带附魔的问题。1.21.1 现在会把 `物品ID{NBT}` 按游戏升级旧存档的方式转换为组件，从 1.20.1 复制来的配置可直接使用；`物品ID[组件]` 写法照常可用。无法识别的物品文本现在会在日志中报错，不再被静默忽略。

---

2026年10月02日 23时58分

- Fixed custom spawn on 1.21.1: new players and respawns now go to the configured spawn (for example a village) instead of the vanilla world spawn. Since 1.20.5 the game re-applies the world spawn on every start, which was mistaken for an admin `/setworldspawn` and saved the vanilla spawn as a fixed spawn. 1.21.1 worlds created with earlier builds keep that fixed spawn; use a new world.
- Fixed a crash when entering a world on 1.21.1 (`IllegalClassLoadError` for `FlightServerMixins`): flight packet handling no longer calls into the mixin package.
- Fixed joining a world failing with "Invalid player data" on 1.20.1: the noclip eye-height check no longer runs before the player is fully created.
- Fixed oversized custom payloads on 1.20.1 failing with a class-loading error instead of the intended "Packet limit exceeded" message.
- The KineticCore data pack is no longer listed as incompatible on 1.21.1.

- 修复 1.21.1 自定义出生点无效：新玩家和重生现在会到配置的出生点（例如村庄），不再是原版世界出生点。1.20.5 起游戏每次启动都会重新设置一次世界出生点，此前被误认为管理员执行了 `/setworldspawn`，从而把原版出生点保存成了固定出生点。用之前版本创建的 1.21.1 世界会保留这个固定出生点，请新建世界。
- 修复 1.21.1 进入世界时崩溃（`FlightServerMixins` 的 `IllegalClassLoadError`）：飞行数据包处理不再调用 mixin 包内的类。
- 修复 1.20.1 进入世界时提示"无效的玩家数据"：穿墙视角高度的判断不再在玩家创建完成之前执行。
- 修复 1.20.1 自定义数据包超出大小时报类加载错误，而不是预期的"Packet limit exceeded"提示。
- 1.21.1 中 KineticCore 数据包不再显示为不兼容。

---

2026年10月02日 23时00分

- KineticCore now supports Forge 1.20.1 and NeoForge 1.21.1. Jars are named `kineticcore-<loader>-<minecraft>-<version>.jar`, for example `kineticcore-forge-1.20.1-26.10.2.jar`.
- On 1.21.1, items written as text use the `/give` syntax: `1x minecraft:diamond_sword[enchantments={levels:{"minecraft:sharpness":5}}]` instead of `{NBT}`. This affects `config/kineticcore/player.toml` (`first_join.items` and `first_join.armor.*`); existing entries with `{...}` data need rewriting on 1.21.1. Copied items are also output in this syntax there.
- Fixed four previously ineffective behaviors:
  - Bees placed by spawn egg or command, or spawned naturally, now get no gravity, the same as bred bees.
  - Noclip in creative now keeps the standing eye height in any pose.
  - Mobs removed without dying (despawned or discarded by another mod) now drop the equipment they picked up. Mobs that are killed, unloaded or change dimension are not affected.
  - Once the server is running, the world spawn position reports the exact custom spawn.
- Fixed vanilla structures such as trial chambers failing to load on 1.21.1 ("Tried to read NBT tag that was too big"): the network NBT limit no longer applies to unlimited reads such as structures, level and player data.
- Removed log deduplication. The log cleaner now only filters logs (errors-only mode and keywords); the old `log_cleaner.deduplication` entry is removed from the config automatically. Note that errors-only mode is on by default on clients and hides all logs below ERROR.

- KineticCore 现在支持 Forge 1.20.1 和 NeoForge 1.21.1。文件名为 `kineticcore-<加载器>-<游戏版本>-<版本>.jar`，例如 `kineticcore-forge-1.20.1-26.10.2.jar`。
- 1.21.1 中以文本表示的物品改用与 `/give` 一致的写法：`1x minecraft:diamond_sword[enchantments={levels:{"minecraft:sharpness":5}}]`，不再使用 `{NBT}`。涉及 `config/kineticcore/player.toml`（`first_join.items` 与 `first_join.armor.*`）；在 1.21.1 上，已有的带 `{...}` 数据的条目需要改写。复制物品功能在该版本也输出这种写法。
- 修复四项此前未生效的行为：
  - 通过刷怪蛋、指令或自然生成的蜜蜂现在会和繁殖出的蜜蜂一样无重力。
  - 创造模式穿墙时，任何姿态下都保持站立视角高度。
  - 生物在非死亡情况下被移除（自然消失或被其他模组清除）时，会掉落其捡起的装备。被击杀、随区块卸载或切换维度的生物不受影响。
  - 服务器启动完成后，世界出生点会返回精确的自定义出生点。
- 修复 1.21.1 中试炼密室等原版结构加载失败（"Tried to read NBT tag that was too big"）的问题：网络 NBT 大小限制不再作用于结构、存档和玩家数据等不限大小的读取。
- 移除日志去重功能。日志清理现在只保留过滤（仅错误日志模式和关键词屏蔽）；旧的 `log_cleaner.deduplication` 配置项会自动删除。注意：客户端默认开启仅错误日志模式，会隐藏 ERROR 以下的全部日志。

---

历史记录（原记录未标注时间）

- Fixed command suggestions in text fields drifting out of line with long commands; suggestions and usage hints now follow the scrolled text.
- Drop-down and context menus now show at most 10 rows; longer menus scroll with the mouse wheel or a scrollbar you can drag or click.

- 修复文本框中命令较长时，命令补全弹窗与文字错位的问题；补全和用法提示现在跟随滚动后的文字对齐。
- 下拉菜单和右键菜单最多显示 10 行，更长的菜单可用滚轮、拖动或点击滚动条浏览。
