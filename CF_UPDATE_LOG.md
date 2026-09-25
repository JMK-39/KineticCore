English

- Fixed client options recovery timing: missing or damaged options files are restored from defaultoptions.txt before Minecraft loads settings, so configured language and key bindings apply on the next launch.
- Fixed Mixin application failures by correcting mapped accessors and invokers for bee weather, flight, effects screens, resource reload notices, despawn rules, GPU render targets, and custom spawn handling.

- Replaced the external pinyin library with KineticCore's own Unicode code-point transliteration data and implementation.
- Preserved full-pinyin, initials, mixed-query, and match-ranking behavior while extending search coverage to traditional, Extension A, and supplementary Han characters.
- Removed pinyin library and fat-JAR packaging requirements.
- Updated KineticCore's build dependencies to use public repositories and produce the regular ForgeGradle mod JAR.

简体中文

- 修正客户端选项恢复时机：Minecraft 读取设置前，会从 defaultoptions.txt 恢复缺失或损坏的选项文件，使配置中的语言和按键在下次启动时生效。
- 修复多处 Mixin 应用失败，调整蜜蜂天气、飞行、效果界面、资源重载提示、实体消失规则、GPU 渲染目标和自定义重生点相关的映射访问器。

- 使用 KineticCore 自有的 Unicode 码点拼音转换实现与读音数据，替代第三方拼音库。
- 保留全拼、首字母、混合查询和匹配排序，并扩展对繁体、CJK 扩展 A 区及补充平面汉字的搜索支持。
- 移除拼音库及 fat-JAR 打包依赖。
- 更新 KineticCore 的构建依赖来源，并改为生成常规 ForgeGradle 模组 JAR。
