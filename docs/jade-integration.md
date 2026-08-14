# Jade 可选支持设计

## 状态

本设计已确认并实现。

目标是在玩家指向 Lazy 方块时，通过 Jade 显示不打开界面也值得知道的状态。Jade 仍是完全可选依赖：未安装 Jade 时，Lazy 的客户端和专用服务器都必须正常启动。

基线固定为 Minecraft 1.21.1、NeoForge 21.1.x 和 Jade `15.10.5+neoforge`。Jade API 以 `compileOnly` 引入，开发运行环境通过 `localRuntime` 安装 Jade，发布产物不包含 Jade。

## 建议结论

值得增加支持。首版只补充各机器的业务状态，不重复 Jade 已经能够从 NeoForge capability 读取的通用物品、流体和能量信息。

建议的信息范围如下：

| 方块 | 首版信息 | 不显示的内容 |
| --- | --- | --- |
| 缓冲器 | 物品总量、流体总量、维度网络直送状态 | 完整槽位列表、网络中的内容 |
| 能量源 | 被动、主动推送或网络推送模式 | `500,000,000 / 500,000,000 FE` 容量条；该数值只是单次传输上限的实现表示，不是有限储能 |
| 物品复制器 | 模板物品、推送间隔 | 六个相邻容器内容、每次推送结果 |
| 修复器 | 当前物品及剩余耐久；空机器不增加额外行 | 修复按钮、服务端修复百分比配置 |
| 机器外壳 | 无额外信息 | — |

## Jade 生命周期

Jade 通过 `@WailaPlugin` 发现 `IWailaPlugin` 实现，并分别调用公共侧 `register` 与客户端 `registerClient`。这套生命周期由 Jade 所有，不接入 `IntegrationManager`：

- 新增公开、可实例化的 `rhx.lazy.integration.jade.LazyJadePlugin`，由 Jade 注解发现。
- 服务端数据提供器放在 `integration.jade`，只引用公共 Minecraft、Lazy 和 Jade API。
- 组件提供器放在 `integration.jade.client`，只能从 `registerClient` 触达。
- `LazyIntegrations`、`Lazy` 和 `LazyClient` 均不得引用 Jade 插件或 Jade API。
- 注册时以方块或方块实体的类作为目标，不提前对 Lazy 的延迟持有者调用 `get()`。

这构成架构中一个明确的生命周期例外：集成代码仍位于 `integration`，但发现与初始化由 Jade 负责。

## 数据与显示

机器状态以服务端为准。使用 `StreamServerDataProvider<BlockAccessor, D>` 产生紧凑快照，在客户端组件提供器中通过同一提供器解码。不能依赖 LDLib2 的界面同步数据恰好已经出现在客户端。

建议每种方块使用独立 UID，例如：

- `lazy:buffer`
- `lazy:energy_source`
- `lazy:item_copier`
- `lazy:repairer`

组件提供器沿用同一 UID，使 Jade 自动为每项支持生成可关闭的插件配置。还需生成对应的 `config.jade.plugin_lazy.*` 中英文配置名称；其他新增文本同样必须本地化。

快照只包含当前指向所需信息：

- 数量和间隔使用变长整数。
- 开关和模式使用布尔值或小枚举。
- 物品复制器模板与修复器物品最多各发送一个 `ItemStack`。
- 不发送完整缓冲器槽位。
- 不主动发送维度网络内容，也不因 Jade 查询触发网络存储访问。

Jade 默认约每 250 ms 请求一次服务端数据。以上快照均应是只读、常数时间操作，不进行配方全表扫描、邻接能力遍历或物品路由。

## 与 Jade 内置信息的关系

Jade 15.10.5 包含通用的物品、流体和能量存储提供器，并会尝试读取默认 NeoForge capability。缓冲器已经公开物品和流体 capability。

因此自定义支持遵循以下规则：

- 不再创建缓冲器物品/流体槽位视图，只显示聚合数量和直送状态。
- 能量源不注册自定义能量存储视图，只显示输出模式。
- 不覆盖 Jade 的全局通用 provider，也不修改其他模组的 tooltip。
- 如果实机验证发现 Jade 对同一方块产生重复或误导信息，只对对应 Lazy 方块做最小范围的修正；不得全局关闭通用信息。

能量源的 `IEnergyStorage` 为兼容外部抽取而把单次传输上限同时报告为当前量和容量，这不表示机器存有有限的 500 MFE。实现通过只匹配能量源方块的空 energy storage extension provider 覆盖 Jade 通用 capability provider，避免显示误导性的有限容量条，不影响其他方块。

## 构建与元数据

当前实现：

1. 保留 `localRuntime("maven.modrinth:jade:...")`，用于普通客户端和集成运行。
2. 使用相同版本的 `compileOnly` 编译 Jade API，不把 Jade 打包进 Lazy。
3. `neoforge.mods.toml` 将 `jade` 声明为 `type="optional"`、`side="BOTH"`、`ordering="AFTER"`。
4. 可选依赖范围为 `[15.10.5,16.0.0)`，不未经验证声明兼容 Jade 16。

虽然 Jade 可以只安装在客户端，但上述自定义机器状态需要服务端数据提供器，因此完整信息只保证在客户端与服务端都安装 Jade 时出现。仅客户端安装 Jade 时，基础方块名等 Jade 自带信息应继续工作，自定义行在没有服务端数据时静默省略。

## 验证

验证范围：

- 不安装 Jade：普通客户端、专用服务器和现有测试均可启动。
- 只在客户端安装 Jade：连接未安装 Jade 的 Lazy 服务器不会报错或显示伪造状态。
- 客户端与服务端都安装 Jade：各目标方块显示正确状态，状态变化在下一次 Jade 刷新后更新。
- Jade 插件类不会在 Lazy 自身 bootstrap 中提前加载。
- 每个 provider 可以在 Jade 配置中独立关闭，新增配置名和内容均有 `zh_cn`、`en_us` 翻译。
- 缓冲器的大量内容不会使 tooltip 或网络包随库存条目数线性增长。
- 能量源不会向玩家展示具有误导性的有限储能语义。

自动验证运行 `./gradlew runData`、`./gradlew check` 和 `./gradlew build`。发布前仍应使用 `clientIntegrations`、`serverIntegrations` 完成 HUD 显示和专用服务器实机验证。

## 参考

- [Jade 项目页](https://modrinth.com/mod/jade)
- [Jade 1.20–1.21.5 插件入门](https://jademc.readthedocs.io/en/latest/plugins22/getting-started/)
- 本地 Gradle 缓存中的 Jade `15.10.5+neoforge` API 与通用 provider 字节码
