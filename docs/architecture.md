# 架构约定

## 版本与边界

Lazy 固定面向 Minecraft 1.21.1 NeoForge。注册经 `DeferredRegister` 和延迟持有者完成；客户端代码与公共/服务端代码隔离，玩家可见文本全部本地化。当前机器与网络接口是模组内部 SPI，不承诺第三方兼容性。

旧机器 BlockEntity NBT、旧物品 NBT、旧配置键和旧工作状态不迁移。方块与配方 ID 保持不变；读不到新结构时按空状态处理。

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
