# 开发指南

## 事实源

开发前先定位事实源，不在文档、测试或构建脚本之间复制同一份清单。

| 信息 | 事实源 |
| --- | --- |
| 插件与依赖版本 | `gradle/libs.versions.toml` |
| 游戏与模组元数据属性 | `gradle.properties` |
| 模块集合 | `settings.gradle.kts` |
| Integration 条件与依赖 | 各 `integrations/*/build.gradle.kts` 的 `lazyIntegration` |
| 注册内容与配置范围 | 对应运行时代码 |
| 配方、标签、模型与翻译 | 静态或生成资源及其 Provider |
| 玩家使用说明 | GuideME 资源 |
| 跨模块约束 | [架构](architecture.md) |
| 非显然算法与失败语义 | [规格目录](spec/) |

依赖版本采用版本目录中的固定值。升级依赖时检查解析结果、Minecraft/NeoForge 兼容性、许可证、side 要求与 Maven 来源；只有涉及库 API 用法时才需要查询外部文档或已解析源码。

## 常用任务

| 任务 | 命令 | 结果 |
| --- | --- | --- |
| 模块检查 | `./gradlew :<module>:check` | 只编译、测试并格式检查目标模块及必要依赖 |
| 模块测试 | `./gradlew :<module>:test` | 只运行目标模块测试 |
| 完整检查 | `./gradlew check` | 叶子项目、build logic、生成器测试与 ktlint |
| 构建发布物 | `./gradlew build` | `mod` 聚合 JAR 与 sources JAR |
| 生成数据 | `./gradlew runData` | 更新 `mod/src/generated/resources` |
| 基础客户端/服务端 | `./gradlew runClient` / `./gradlew runServer` | 只加载基础运行依赖 |
| 完整 Integration 环境 | `./gradlew runClientIntegrations` / `./gradlew runServerIntegrations` | 按 descriptor 解析 Integration 闭包 |
| 渲染 SVG 资产 | `./gradlew renderArtTextures` | 更新资源目录中的 PNG |

Integration 运行任务可通过 `-Plazy.integrations=<ids>` 选择子集。有效 ID 与依赖闭包由 descriptor 决定，文档不维护可选值列表。

## 变更与验证

验证从最小受影响模块开始。先运行对应的 `:<module>:test`、`:<module>:ktlintCheck` 或 `:<module>:check`；局部任务失败时先修复，不继续扩大任务图。跨模块 API、build logic、根聚合、DataGen、打包或发布边界发生变化时，在局部验证通过后再运行全仓 `check`。孤立模块改动不以全仓 `check` 作为默认首轮验证。

| 变更类型 | 最低验证 | 额外检查 |
| --- | --- | --- |
| 单模块 Kotlin | 对应 `:<module>:test` 与 `:<module>:ktlintCheck` | 公共契约受影响时检查直接消费者 |
| Kotlin DSL 或 build logic | 对应模块检查与 `./gradlew -p build-logic check` | 根聚合或插件行为受影响时运行 `./gradlew check` |
| 注册、资源或 DataGen | 对应模块检查与 `./gradlew runData` | 检查生成差异；跨模块生成边界运行 `./gradlew check` |
| Integration | `./gradlew :integrations:<id>:check` | 公共 Integration 契约变更时运行全仓检查，并在匹配 side 的环境做烟雾测试 |
| 打包与发布配置 | `./gradlew build` | 检查聚合 JAR 与 publication 元数据 |
| UI、渲染与输入 | 对应模块检查 | 客户端实机检查缩放、交互、本地化与状态更新 |
| 服务端行为或 capability | 对应模块检查 | 专用服务器或最小复现场景验证；跨模块协议变化时运行全仓检查 |

验证目标来自变更影响，不在文档中长期保存逐功能手工步骤。需要重复执行的验收应转化为稳定的行为测试或独立检查工具。

## 测试原则

测试只维护代码契约，不成为内容数据的第二事实源。

### 应测试

| 类型 | 示例 |
| --- | --- |
| 行为 | 选择优先级、权限判定、重试与暂停状态转换 |
| 边界 | 容量、溢出、空值、无效输入、side 隔离 |
| 事务 | 失败不扣账、随机结果不重滚、恢复后继续提交 |
| 往返 | 对象经 codec 或持久化接口后保持语义等价 |
| 生成器规则 | descriptor 图的重复、循环、闭包和 owner 校验 |

### 不应测试

| 断言对象 | 原因 | 替代方式 |
| --- | --- | --- |
| 具体配方内容、配方全集 | 与资源或 Provider 重复 | 验证配方算法、codec 能力或 DataGen 是否成功 |
| JSON 原文、字段排列、整段生成资源 | 固定序列化表示 | 对象往返或结构性校验 |
| 翻译和玩家文案原文 | 与语言资源重复 | 验证本地化 key 存在性的生成规则 |
| 源码文本中的调用片段 | 绑定实现写法 | 抽出可调用边界并验证行为 |
| 手写的注册项或 Integration ID 全集 | 与注册/descriptor 重复 | 从事实源派生输入后验证通用不变量 |

配方解析、组合和选择本身可以测试；测试数据应是用例内构造的最小对象，并只表达待验证规则。Codec 往返可以使用任意 `DynamicOps`，但不得断言编码后的文字表示。

## Integration 开发

新增或修改 Integration 时：

1. 在对应模块声明 `lazyIntegration` descriptor 与 partner 依赖。
2. 使用 `integration-api` context 实现所需生命周期。
3. 将第三方类型限制在该 Integration 的源码边界内。
4. 通过生成 bridge/catalog 接入 `mod`，并为 DataGen 明确声明参与状态。
5. 测试模块自身行为以及依赖图能够被聚合校验。

生命周期模型与第三方发现入口见 [Integration 生命周期规格](spec/integration-lifecycles.md)。

## UI 与网络

GUI、HUD、binding/RPC 和方块实体托管优先使用 LDLib2。公共 UI 结构不能引用客户端专属类；两侧创建的固定界面必须具有一致元素树。

布局状态、事件分侧与二级窗口的项目约束见 [LDLib2 实现要点](ldlib2.md)。

| 操作 | 通道 | 服务端责任 |
| --- | --- | --- |
| 只读展示 | S2C binding 或紧凑快照 | 提供当前事实状态 |
| 修改世界或机器 | UI server event / payload | 重新校验玩家、距离、目标和权限 |
| 客户端渲染 | client-only 注册 | 不进入公共类加载路径 |

## 代码与提交

- Kotlin 遵循官方风格；格式问题使用 `./gradlew ktlintFormat` 修复。
- 可执行一次 `./gradlew addKtlintCheckGitPreCommitHook` 安装本地检查钩子。
- 提交使用 Conventional Commits：`<type>(<scope>): <description>`。
- 本地发布与临时输出保持在已忽略目录中。
