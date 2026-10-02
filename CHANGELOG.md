历史记录（原记录未标注时间）

- Added the KineticGui.findPage API: addons can find a page in the current screen's back-navigation chain, so data can still reach a parent page while one of its child pages is open.
- Fixed command suggestions in text fields drifting out of line with long commands; suggestions and usage hints now follow the scrolled text.
- Drop-down and context menus now show at most 10 rows; longer menus scroll with the mouse wheel or a scrollbar you can drag or click.

- 新增 KineticGui.findPage 接口：附属模组可以在当前界面的返回链中查找页面，打开子页面时数据仍能更新到下层的父页面。
- 修复文本框中命令较长时，命令补全弹窗与文字错位的问题；补全和用法提示现在跟随滚动后的文字对齐。
- 下拉菜单和右键菜单最多显示 10 行，更长的菜单可用滚轮、拖动或点击滚动条浏览。

---

2026年10月02日 13时53分

- Fixed a development-client startup crash caused by reading client settings before Forge loaded their configuration.
- Client settings now use their in-memory defaults or edits until loading completes, then read and write the loaded configuration.
- Added a regression test covering startup defaults, preload edits, loaded values, and native writes. All core verification suites and the final-JAR API check pass.

- 修复 Forge 尚未加载客户端配置时提前读取设置，导致开发客户端启动崩溃的问题。
- 配置加载前使用内存默认值或编辑值；加载完成后正常读写已加载的配置。
- 新增回归测试，覆盖启动默认值、加载前编辑、加载后读取和原生写入。核心验证套件及最终 JAR API 检查全部通过。
