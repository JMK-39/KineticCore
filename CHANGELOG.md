2026年10月08日 — Checkerboard item backgrounds / 棋盘格物品背景

- Item backgrounds repeat one complete 8×8 texture with equal horizontal and vertical scaling. Standard slots show 8×8 checker squares. Rectangular areas no longer stretch the pattern or cut off partial tiles; borders and item positions are preserved.

- 物品背景重复使用一张完整的 8×8 贴图，横纵方向使用相同缩放倍率，标准格子中的棋盘色块为 8×8。长方形区域不再拉伸图案或截取半张贴图，保留边框及物品位置。

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
