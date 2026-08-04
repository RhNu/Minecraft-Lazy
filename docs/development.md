# 开发与依赖

## 固定工具链

项目使用 Java 21、Gradle 9.2.1、ModDevGradle 2.0.142、NeoForge 21.1.242、Parchment 2024.11.17、Kotlin 2.4.0、KotlinForForge 5.12.0 和 LDLib2 2.2.29。版本集中保存在 `gradle.properties`，构建脚本不得使用 `latest.release`、版本区间解析或其他动态选择器。

Kotlin 插件版本与 KotlinForForge 5.12.0 捆绑的 Kotlin 运行库保持一致。模组元数据使用 `kotlinforforge` 语言加载器，并要求至少 5.12 版本。

LDLib2 是范围 `[2.2.29,2.3.0)`、双端必需的外部模组依赖，不打包进 Lazy JAR。其 `all` 构件从 FirstDark Maven 解析，Yoga、Taffy 与 Kotlin 传递依赖从 Maven Central 补齐。升级 KotlinForForge 或 LDLib2 时必须用 `dependencyInsight` 复查 Kotlin、Yoga 和 Taffy 的最终解析版本。

Curios API 9.5.1+1.21.1 是范围 `[9.5.1+1.21.1,10.0.0)`、双端可选的兼容依赖，用于传送器装备槽位、自定义槽位校验与槽位物品查询。编译使用官方 API classifier，基础开发运行与发布环境不自动携带 Curios；未安装时只关闭装备槽、快捷键和对应 payload。payload 使用可选协商，客户端发送前检查服务端 channel，允许两侧安装状态不一致。

Silent Gear 4.2.1.1 与 Beyond Dimensions 0.7.26 都是双端可选兼容依赖，版本范围分别为 `[4.2.1.1,4.3.0)` 与 `[0.7.26,0.8.0)`。三项集成统一使用 `compileOnly` 编译，只有 integrations 开发运行类路径加载完整模组及 SilentLib；Lazy 不打包或传递这些模组。兼容 bootstrap 只在检测到对应 mod id 后解析 adapter，第三方类型不得离开各自 integration 包。

Mystical Agriculture 8.0.27 与 Mystical Agradditions 8.0.14 是双端可选内容依赖，integrations 运行环境同时固定 Cucumber 8.0.16。Lazy 不直接链接它们的 Java API；精华、灌注水晶和配方材料均按资源 ID 解析。仅安装 Agriculture 时提供五档转换，Agradditions 存在时启用 Insanium。

## 开发运行模组

JEI 19.39.0.369 与 Jade 15.10.5 通过 `localRuntime` 加入开发环境。它们不会出现在 Lazy 的必需依赖声明中，也不会通过 Maven publication 传递给使用者。

- JEI 用于检查物品、配方、标签和未来的自定义配方分类。
- Jade 用于检查方块、方块实体、能力和未来的自定义观察信息。
- 在真正调用两者 API 前，不添加 `compileOnly` API 依赖或兼容插件代码。

## 按需候选

- spark 适合分析服务器 tick、分配与卡顿，作为调查性能问题时的临时运行模组，不固定进基础环境。
- EMI 可作为 JEI 的替代界面或兼容性测试对象，默认不与 JEI 同时固定加载。
- Patchouli 只在确认需要游戏内手册后引入。
- GeckoLib 只在确认存在复杂骨骼动画后引入。
- YACL、Cloth Config 或相似配置 UI 库只在原生配置文件不足以满足交互需求后选择其一。
- Lazy 仍是单 NeoForge 项目，不引入跨平台抽象层。

## 界面实现

GUI、HUD、UI binding/RPC 和方块实体托管优先使用 LDLib2。界面结构使用公共侧安全的 Kotlin DSL，外观通过资源包中的 LSS 定义，并优先继承 LDLib2 `mc.lss` 的原版 Minecraft 主题。公共 UI 代码不得引用 Minecraft 客户端类，确保专用服务器侧隔离。

只读展示使用 S2C binding；会改变世界的操作使用 UI server event，并在处理时重新执行权限、距离和目标有效性校验。固定结构界面必须在两侧创建完全相同的元素树。

渲染器、Shader、编辑器和节点图仅在功能已经确认需要时引入，不预建空框架。

引入任何候选前，应重新核对 Minecraft 1.21.1 与 NeoForge 的稳定版本、许可证、服务端兼容性和 Maven 来源。

## 常用流程

`./gradlew clean build` 验证 Kotlin 编译、元数据生成、资源处理和 JAR 打包。`./gradlew runData` 使用 integrations 类路径刷新基础、Curios 与可选机器的数据生成输出。`./gradlew runClient` 启动只带 JEI 与 Jade、但不带可选集成模组的开发客户端。发布前还要检查 Lazy JAR 不含 `com/lowdragmc/lowdraglib2`，并用 `dependencyInsight` 核对 Kotlin、Yoga 与 Taffy。

完整兼容组合使用 `./gradlew runClientIntegrations` 和 `./gradlew runServerIntegrations` 验证 Curios、Silent Gear、Beyond Dimensions、Mystical Agriculture 与 Mystical Agradditions；普通 `runClient`、`runServer` 与单元测试不包含任何可选集成模组，用于持续检查类加载安全性。

## Kotlin 代码风格

项目通过 ktlint 自动校验 Kotlin 与 Kotlin DSL：`./gradlew check` 会包含 `ktlintCheck`，本地可用 `./gradlew ktlintFormat` 自动格式化。需要在提交前自动检查时，执行一次 `./gradlew addKtlintCheckGitPreCommitHook` 安装本地 Git hook；GitHub Actions 也会单独运行 `ktlintCheck`。

GameTestServer 配置保留用于未来自动化游戏测试。在没有注册 GameTest 时，它可能按 NeoForge 的默认行为以失败退出，因此当前不作为验收命令。

本地 Maven 发布输出到被忽略的 `repo` 目录。正式发布仓库、签名、更新检查和发布平台任务均不在当前脚手架范围内。
