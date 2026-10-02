# KineticCore — 26.10.2

## English

- Fixed a development-client startup crash caused by reading client settings before Forge loaded their configuration.
- Client settings now use their in-memory defaults or edits until loading completes, then read and write the loaded configuration.
- Added a regression test covering startup defaults, preload edits, loaded values, and native writes. All core verification suites and the final-JAR API check pass.

## 中文

- 修复 Forge 尚未加载客户端配置时提前读取设置，导致开发客户端启动崩溃的问题。
- 配置加载前使用内存默认值或编辑值；加载完成后正常读写已加载的配置。
- 新增回归测试，覆盖启动默认值、加载前编辑、加载后读取和原生写入。核心验证套件及最终 JAR API 检查全部通过。
