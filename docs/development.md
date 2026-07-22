# 开发与依赖

## 固定工具链

项目使用 Java 21、Gradle 9.2.1、ModDevGradle 2.0.142、NeoForge 21.1.242、Parchment 2024.11.17、Kotlin 2.4.0 和 KotlinForForge 5.12.0。界面依赖固定为 UILib 9.0.0，并固定其前置 Architectury API 13.0.6。版本集中保存在 `gradle.properties`，构建脚本不得使用 `latest.release`、版本区间解析或其他动态选择器。

Kotlin 插件版本与 KotlinForForge 5.12.0 捆绑的 Kotlin 运行库保持一致。模组元数据使用 `kotlinforforge` 语言加载器，并要求至少 5.12 版本。

## 开发运行模组

JEI 19.39.0.369 与 Jade 15.10.5 通过 `localRuntime` 加入开发环境。它们不会出现在 Lazy 的必需依赖声明中，也不会通过 Maven publication 传递给使用者。

- JEI 用于检查物品、配方、标签和未来的自定义配方分类。
- Jade 用于检查方块、方块实体、能力和未来的自定义观察信息。
- 在真正调用两者 API 前，不添加 `compileOnly` API 依赖或兼容插件代码。

## 按需候选

- spark 适合分析服务器 tick、分配与卡顿，作为调查性能问题时的临时运行模组，不固定进基础环境。
- EMI 可作为 JEI 的替代界面或兼容性测试对象，默认不与 JEI 同时固定加载。
- Patchouli 只在确认需要游戏内手册后引入。
- Curios 只在内容需要额外装备槽位后引入。
- GeckoLib 只在确认存在复杂骨骼动画后引入。
- YACL、Cloth Config 或相似配置 UI 库只在原生配置文件不足以满足交互需求后选择其一。
- Lazy 仍是单 NeoForge 项目，不使用 Architectury 的跨平台抽象；Architectury API 仅作为 UILib 的固定运行前置存在。

## 界面依赖

UILib 从 DAQEM 官方 Maven 获取，并仅在 `client` 包内引用。1.21.1 发布物使用 `com.daqem.uilib.client.gui` API；不要照搬面向更新 Minecraft 版本的在线示例。公共菜单与网络快照不引用 UILib 或 Minecraft 客户端类，确保专用服务器侧隔离。

引入任何候选前，应重新核对 Minecraft 1.21.1 与 NeoForge 的稳定版本、许可证、服务端兼容性和 Maven 来源。

## 常用流程

`./gradlew clean build` 验证 Kotlin 编译、元数据生成、资源处理和 JAR 打包。`./gradlew runData` 刷新数据生成输出。`./gradlew runClient` 启动带 JEI 与 Jade 的开发客户端。

GameTestServer 配置保留用于未来自动化游戏测试。在没有注册 GameTest 时，它可能按 NeoForge 的默认行为以失败退出，因此当前不作为验收命令。

本地 Maven 发布输出到被忽略的 `repo` 目录。正式发布仓库、签名、更新检查和发布平台任务均不在当前脚手架范围内。
