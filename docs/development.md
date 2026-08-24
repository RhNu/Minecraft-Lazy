# 开发与依赖

## 固定工具链

项目使用 Java 21、Gradle 9.2.1、ModDevGradle 2.0.142、NeoForge 21.1.242、Parchment 2024.11.17、Kotlin 2.4.0、KSP2 2.3.11、KotlinForForge 5.12.0 和 LDLib2 2.2.29。插件与依赖坐标集中在 `gradle/libs.versions.toml`，游戏/元数据属性集中在 `gradle.properties`；构建脚本不得使用动态选择器。

Kotlin 插件版本与 KotlinForForge 5.12.0 捆绑的 Kotlin 运行库保持一致。模组元数据使用 `kotlinforforge` 语言加载器，并要求至少 5.12 版本。

LDLib2 是范围 `[2.2.29,2.3.0)`、双端必需的外部模组依赖，不打包进 Lazy JAR。其 `all` 构件从 FirstDark Maven 解析，Yoga、Taffy 与 Kotlin 传递依赖从 Maven Central 补齐。升级 KotlinForForge 或 LDLib2 时必须用 `dependencyInsight` 复查 Kotlin、Yoga 和 Taffy 的最终解析版本。

Curios API 9.5.1+1.21.1 是范围 `[9.5.1+1.21.1,10.0.0)`、双端可选的兼容依赖，用于传送器装备槽位、自定义槽位校验与槽位物品查询。编译使用官方 API classifier，基础开发运行与发布环境不自动携带 Curios；未安装时只关闭装备槽、快捷键和对应 payload。payload 使用可选协商，客户端发送前检查服务端 channel，允许两侧安装状态不一致。

Silent Gear 4.2.1.1 与 Beyond Dimensions 0.7.26 都是双端可选兼容依赖，版本范围分别为 `[4.2.1.1,4.3.0)` 与 `[0.7.26,0.8.0)`。三项集成统一使用 `compileOnly` 编译，只有 integrations 开发运行类路径加载完整模组及 SilentLib；Lazy 不打包或传递这些模组。兼容 bootstrap 只在检测到对应 mod id 后解析 adapter，第三方类型不得离开各自 integration 包。

AE2 19.2.17 与 Applied Flux 1.21-2.1.5-neoforge 是双端可选网络输出依赖，integrations 运行环境同时固定 Glodium 1.21-2.2-neoforge。AE2 提供物品、流体存储 API 和无线接入点链接槽；Applied Flux 只在独立 `integration.appflux` 类加载边界内提供 `FluxKey(FE)`。模块列表必须保持 AE2 在 Applied Flux 之前，基础运行和发布 JAR 不携带这些模组。

GuideME 21.1.1 是双端必需依赖，通过 `localRuntime` 进入所有开发运行配置，并从 `assets/lazy/guideme_guides` 和 `assets/lazy/guides` 读取 Lazy 指南。Mekanism 1.21.1-10.7.19.85 是双端可选依赖，只在 integrations 运行环境载入完整模组；配置卡能力、安全检查与升级 API 的直接引用必须留在 `integration.mekanism`。

Mystical Agriculture 8.0.27 与 Mystical Agradditions 8.0.14 是双端可选内容依赖，integrations 运行环境同时固定 Cucumber 8.0.16。Lazy 不直接链接它们的 Java API；精华、灌注水晶和配方材料均按资源 ID 解析。仅安装 Agriculture 时提供五档转换，Agradditions 存在时启用 Insanium。

## 构建约定

`build-logic` 提供 `lazy.kotlin-library`、`lazy.neoforge-library`、`lazy.integration`、`lazy.mod` 和 `lazy.datagen`。新 integration 必须使用 `lazy.integration`，在本模块声明 `lazyIntegration` descriptor、partner `compileOnly` 与 `integrationRuntime`，不得修改 `Lazy` 入口或增加运行时扫描。runtime、annotations 和 processor 基础依赖由约定插件注入，不在每个模块重复声明。

Kotlin source root 已压平：runtime 保留 `core`/`feature` 等领域目录，integration、codegen 与 build-logic 文件不再套 `rhx/lazy/...` 的单目录链。移动文件无需同步 package 路径；package 声明仍决定 JVM 名称。

模块测试以合成的 `lazy_test` NeoForge mod 运行，并把 project 依赖的 main source set 合并到同一 mod classloader，模拟最终平铺 JAR。测试应验证新架构的公开契约、生成 catalog 与资源所有权，不保留旧 manager 的实现细节。

## 按需候选

- spark 适合分析服务器 tick、分配与卡顿，作为调查性能问题时的临时运行模组，不固定进基础环境。
- EMI 可作为 JEI 的替代界面或兼容性测试对象，默认不与 JEI 同时固定加载。
- Patchouli 只在确认需要游戏内手册后引入。
- GeckoLib 只在确认存在复杂骨骼动画后引入。
- YACL、Cloth Config 或相似配置 UI 库只在原生配置文件不足以满足交互需求后选择其一。
- Lazy 不引入跨平台抽象层；多项目只用于 NeoForge 构建边界与编译期治理。

## 界面实现

GUI、HUD、UI binding/RPC 和方块实体托管优先使用 LDLib2。界面结构使用公共侧安全的 Kotlin DSL，外观通过资源包中的 LSS 定义，并优先继承 LDLib2 `mc.lss` 的原版 Minecraft 主题。公共 UI 代码不得引用 Minecraft 客户端类，确保专用服务器侧隔离。

只读展示使用 S2C binding；会改变世界的操作使用 UI server event，并在处理时重新执行权限、距离和目标有效性校验。固定结构界面必须在两侧创建完全相同的元素树。

渲染器、Shader、编辑器和节点图仅在功能已经确认需要时引入，不预建空框架。

引入任何候选前，应重新核对 Minecraft 1.21.1 与 NeoForge 的稳定版本、许可证、服务端兼容性和 Maven 来源。

## 常用流程

`./gradlew check` 覆盖所有叶子子项目、build-logic、KSP validator、NeoForge 单元测试与 ktlint。`./gradlew build` 生成 `:mod` 的唯一分发 JAR和聚合 sources JAR；普通构建不执行 DataGen。`./gradlew runData` 只使用独立 DataGen profile，输出到 `mod/src/generated/resources`，CI 应在执行后用 Git diff 检查漂移。

普通 `runClient`/`runServer` 只携带必需依赖。完整组合使用 `runClientIntegrations`/`runServerIntegrations`；`-Plazy.integrations=appflux,jade` 可选择子集，Gradle 会根据 descriptor 自动补齐 integration 依赖。未知 ID 立即失败；服务端显式选择 JEI 等 client-only 项会立即失败。AE 烟雾测试至少覆盖链接卡覆盖绑定、背包同目标去重/多目标歧义、接入点区块卸载后重载、Grid 拆分与合并，以及 FE Cell 只收到 `FluxKey(FE)`。Mekanism 烟雾测试覆盖复制/粘贴、安全拒绝、升级补齐和边界 capability 代理。

发布前检查聚合 JAR 只有一个 `META-INF/neoforge.mods.toml`，包含 `META-INF/lazy/integrations.json` 与 `kubejs.plugins.txt`，且不含 `rhx/lazy/datagen`、第三方包、嵌套 JAR 或子模块 manifest。生成的 publication POM 不应包含 dependencies。

## Kotlin 代码风格

项目通过 ktlint 自动校验 Kotlin 与 Kotlin DSL：`./gradlew check` 会包含 `ktlintCheck`，本地可用 `./gradlew ktlintFormat` 自动格式化。需要在提交前自动检查时，执行一次 `./gradlew addKtlintCheckGitPreCommitHook` 安装本地 Git hook；GitHub Actions 也会单独运行 `ktlintCheck`。

GameTestServer 配置保留用于未来自动化游戏测试。在没有注册 GameTest 时，它可能按 NeoForge 的默认行为以失败退出，因此当前不作为验收命令。

本地 Maven 发布输出到被忽略的 `repo` 目录。正式发布仓库、签名、更新检查和发布平台任务均不在当前脚手架范围内。
