English

- Replaced the external pinyin library with KineticCore's own Unicode code-point transliteration data and implementation.
- Preserved full-pinyin, initials, mixed-query, and match-ranking behavior while extending search coverage to traditional, Extension A, and supplementary Han characters.
- Removed pinyin library and fat-JAR packaging requirements.
- Updated KineticCore's build dependencies to use public repositories and produce the regular ForgeGradle mod JAR.

简体中文

- 使用 KineticCore 自有的 Unicode 码点拼音转换实现与读音数据，替代第三方拼音库。
- 保留全拼、首字母、混合查询和匹配排序，并扩展对繁体、CJK 扩展 A 区及补充平面汉字的搜索支持。
- 移除拼音库及 fat-JAR 打包依赖。
- 更新 KineticCore 的构建依赖来源，并改为生成常规 ForgeGradle 模组 JAR。
