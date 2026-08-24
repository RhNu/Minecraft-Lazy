# 架构约定

## 版本与边界

Lazy 固定面向 Minecraft 1.21.1 NeoForge。注册经 `DeferredRegister` 和延迟持有者完成；客户端代码与公共/服务端代码隔离，玩家可见文本全部本地化。当前机器与网络接口是模组内部 SPI，不承诺第三方兼容性。

旧机器 BlockEntity NBT、旧物品 NBT、旧配置键和旧工作状态不迁移。方块与配方 ID 保持不变；读不到新结构时按空状态处理。

## 多项目边界

根项目只聚合任务。`runtime` 拥有 core、feature 和运行时注册；`integration-api` 拥有生命周期 SPI；每个 `integrations/*` project 只编译一个第三方集成；`mod` 拥有 `@Mod`、资源、元数据与发布；`datagen` 独占 Provider、语言贡献、模型 helper 和 `GatherDataEvent`；`codegen/*` 与 `build-logic` 负责关系声明和编译期治理。

`runtime`、`integration-api` 与 integration 只生成内部 library JAR。`:mod:jar` 仅展开这些 project artifact，重复路径直接失败，并排除子模块 manifest 和签名。最终产物不含子模块 JAR、DataGen 类或第三方包。`:mod:sourcesJar` 聚合相同模块的源码；Maven publication 直接发布这两个聚合 artifact，不暴露内部 project 或可选 partner 依赖。

跨模块声明必须显式 `public` 并在具体声明上标记 `@LazyInternalApi`；其余实现保持 `internal`，禁止使用文件级注解批量放大 API。`integration-api`、注解与 processor 等真正的契约模块启用 strict explicit API；实现模块通过编译各消费方验证边界，不使用 friend path 绕过模块可见性。

Kotlin 文件的物理路径不复制无信息量的完整包名前缀：`runtime/src/*/kotlin` 直接从 `core`、`feature` 等领域目录开始，每个 integration 与 codegen 模块的源码直接位于自己的 source root。Kotlin `package` 和最终二进制包名保持 `rhx.lazy.*`，物理目录只表达模块内有意义的分组。

## Integration 编译期治理

每个 integration 的 `lazyIntegration` Gradle DSL 是 ID、owner、side、必需/可选 mod、integration 依赖和 DataGen 参与状态的唯一真相源。约定插件统一注入 runtime、annotations 与 processor，并生成规范化 descriptor artifact、KSP 参数和可选依赖元数据；partner API 由模块声明为 `compileOnly`，开发运行依赖由同模块的 `integrationRuntime` 导出，`:mod` 按 descriptor 选择 project configuration，不维护第三方坐标硬编码表。

Lazy 管理的入口实现 `CommonIntegration`/`ClientIntegration`。context 显式提供 `ModContainer`、mod bus 和 game bus；integration 不直接读取全局总线或 `ModList`。本地 KSP 校验入口数量、接口、side 与 framework 注解，并生成能访问模块内 `internal` 实现的 public bridge。

`:mod` 的聚合 KSP 对 descriptor 做重复 ID、未知/循环依赖、side 和硬依赖闭包校验，再生成 common/client 静态 catalog、`META-INF/lazy/integrations.json`、KubeJS 发现文件和 DataGen contribution catalog。common 路径不引用 client bridge；Jade、JEI、KubeJS 仍由第三方 lifecycle 发现。运行时不使用反射、`ServiceLoader` 或类路径扫描。

入口只在所有 `requiredMod` 存在时按拓扑序安装。安装阶段异常携带 integration ID 和阶段立即终止。`run*Integrations` 使用同一 descriptor 图计算选择闭包；服务端显式选择 client-only 集成会在 Gradle 配置阶段失败。

## DataGen 边界

DataGen 只从 `runtime`、Curios 与 Mystical Agriculture 的窄 `DataGenExports` facade 获取 holder、资源 ID 和 bootstrap 回调。holder 只能在 Provider 执行阶段解析，继续禁止在注册阶段提前 `get()`。静态资源位于 `mod/src/main/resources`，生成结果写入并提交到 `mod/src/generated/resources`。

普通 `build` 不执行 DataGen，也不会打包 `datagen` project。根 `runData` 转发到 `:datagen:runData`；该 profile 只加载声明参与且 Provider 实际需要的 partner 依赖。KSP 会在 DSL 的 DataGen 声明与 contribution 不一致时终止编译。

## 机器资源流水线

资源机器统一分为三层：

- `core.resource`：不可变资源身份、大数数量、固定种类仓、事务和 NeoForge capability 视图。
- `core.process`：`WorkProvider`、`WorkController`、`WorkStatus` 和唯一的 `PreparedCommit`。
- `core.io`：`OutputSource`、每刻传输预算、面/网络调度、重试和结果未知暂停。

机器共用 `processing_core_t1` 至 `processing_core_t4` 四级处理核心。核心物品与等级定义在通用机器模块中，各机器自行解释对应等级的速度、产出或其它效果。

统一路径为：输入或设置生成工作，工作生成资源事务，事务写入唯一输出仓，随后 capability 被动抽取、主动面输出和网络输出都从同一仓扣账。工作预算与运输预算互不影响。

每个工作机器的一刻顺序固定为：旧产物预输出 → 提交旧 `PreparedCommit` → 生成新工作 → 用同一运输预算输出新产物 → 更新显示。前后输出合计最多发起 64 次目标调用。

## 资源模型

`ResourceKind<V>` 定义资源身份的规范化、精确匹配和 NBT 编解码。目前内置 `ItemVariant`、`FluidVariant` 和无模板的 `EnergyVariant`。物品和流体都按数据组件精确匹配。

`ResourceAmount<V>` 保存不可变身份和正 `Long` 数量；`ResourceBundle` 表示一次工作的多种结果；`ResourceDelta` 表示一个事务中的扣除和增加。

`ResourceStore<V>` 使用固定条目数组。插入先合并精确相同身份，再占用空条目；每条目有独立的 `Long` 容量。模拟插入、提取、容量查询和 `ResourceTransaction` 都只扫描固定条目数。所有模板防御性复制，产量合并和交易乘法使用溢出检查。

NeoForge capability 只是仓的有界视图：物品一次只暴露或提取合法原版堆叠，流体按 `Int` 边界分块。大数 UI 单独同步模板和 `Long` 数量，不制造超大原版堆叠。

## 工作生命周期

`WorkController` 维护 `IDLE/RUNNING/BLOCKED/FAULTED`。随机工作一旦生成便封装为至多一份 `PreparedCommit`；提交失败时不允许生成下一份结果。待提交内容和工作单位数一起持久化，因此重载、输出堵塞或部分提交都不会重滚。

当一次随机结果的种类超过当前空余条目时，`PreparedCommit` 可逐身份推进；工作游标只在整份结果提交完后前进。确定性机器使用 `ResourceTransaction` 同时扣输入并加输出。

## IO 与网络

`IoController` 保留被动、面和网络三种互斥模式。机器只提供 `StoredOutputSource` 或 `InfiniteOutputSource`，不再各自实现推送循环。

`OutputDispatcher` 在资源身份、方向和资源类型之间轮转。面输出把物品限制为合法堆叠、把流体和 FE 限制在 `Int`；网络输出直接用 `ResourceAmount` 报价，由 provider 返回实际接受的 `Long` 数量。

`TransferResult` 区分部分接受、暂时不可用、目标丢失、目标无效和结果未知。暂时不可用按 20 tick 退避；已确认目标丢失回到被动模式；结果未知会持久暂停，直到玩家明确恢复或重选目标。

AE2、Applied Flux 和 Beyond Dimensions 都实现同一 `NetworkOutputProvider` SPI。AE2 直接向 `MEStorage` 发送 `Long` 数量；Applied Flux 增加 FE key；Beyond Dimensions 保留其网络 ID、长数量和异常保护。

## 机器归属

- 模拟室：28 个物品种类条目和 28 个流体种类条目，单种 `Long.MAX_VALUE`；活动作业快照配方、时长、处理核心与工具。
- 塑形机：最多 8 种输入和 8 种输出的共享大数仓；每个条目用一次整数换算提交全部可完成交易。
- 缓冲器：通用 store，容量保持物品 8×256、流体 4×64,000 mB。
- 精华转换器：领域余数账本加单条目大数输出仓。
- 物品复制器与能量源：定时或持续的 `InfiniteOutputSource`，不创建虚假库存。
- 修复器：原地组件修改，不强行进入资源工作模型。

## 持久化和验证

新 store、活动作业和 `PreparedCommit` 使用各自统一的新 NBT 结构。机器设置不随掉落物保存；真实输入、输出和账本按 `MachineBlockEntity` 的内容管线写入方块物品。IO 配置只能用配置卡重新应用。

世界正面显示仍走 `MachineDisplayState` 的原版更新包，只同步图标和活性，不复制整个机器状态。交付前运行 `./gradlew runData` 与 `./gradlew check`。
