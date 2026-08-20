# 架构约定

## 项目边界

Lazy 当前提供模组入口、注册基础设施、修复器、缓冲器、能量电池、能量源、物品复制器、虚空网格维度、配套传送器，以及少量无业务含义的通用工具。其他物品、方块和维度仍属于后续内容工作，在设计确认前不得进入运行源码。

项目只面向 Minecraft 1.21.1 NeoForge，不承担 Fabric、Forge 或多加载器兼容层。Minecraft 小版本也是兼容边界的一部分，依赖升级不得隐式改变游戏版本。

## 包组织

所有 Kotlin 源码位于 `rhx.lazy` 包下，并让物理目录与包名保持一致。

```text
rhx.lazy
├─ Lazy.kt
├─ core
│  ├─ command
│  ├─ configurator
│  ├─ datagen
│  ├─ io
│  ├─ registry
│  ├─ render
│  │  └─ client
│  ├─ storage
│  └─ teleport
├─ feature
│  ├─ buffer
│  ├─ energy
│  ├─ itemcopier
│  ├─ machine
│  ├─ protection
│  ├─ repairer
│  ├─ rise
│  ├─ simulation
│  │  └─ client
│  ├─ teleporter
│  │  └─ client
│  └─ voidworld
└─ integration
   ├─ beyonddimensions
   ├─ ae2
   ├─ appflux
   ├─ curios
   │  └─ client
   ├─ jade
   │  ├─ client
   │  └─ mysticalagriculture
   │     └─ client
   ├─ jei
   ├─ kubejs
├─ mekanism
   ├─ mysticalagriculture
   └─ silentgear
```

- 根包只保存模组入口和 `MOD_ID`。
- `core` 保存经过跨领域复用证明的 Minecraft 扩展、公共基类，以及命令、数据生成和注册的全局装配入口。
- `feature` 使用垂直切片组织业务；方块、物品、方块实体、界面、事件、配置和注册跟随各自领域，不再按 Minecraft 类型横向分包。
- `integration` 保存第三方模组适配。集成可以依赖 `core` 和 `feature`，业务领域不得反向依赖集成。
- 可选集成通过 `IntegrationModule` 统一公共侧与客户端初始化，并由唯一的显式模块列表确定顺序。bootstrap 的字段、签名和初始化器不得引用第三方类型；只有确认对应模组已加载后才能解析 adapter。模组已安装但初始化失败属于启动错误，不静默降级。
- Jade 集成是上述生命周期的明确例外：`LazyJadePlugin` 通过 Jade 的 `@WailaPlugin` 发现，公共数据提供器与客户端组件提供器分别由 Jade 注册，不进入 `IntegrationManager`。Lazy 自身 bootstrap 不引用 Jade API；精华转换器的 Jade bridge 只有在对应内容模组已加载后才解析。
- IO 由 `IoConfiguration`、`IoController` 与机器 `IoAdapter` 统一管理。三种互斥模式为被动、六面配置和网络输出：被动模式允许各面按机器自身输入/输出能力交互；六面配置按机器朝向保存上、左、前、右、下、后各面的禁用/输入/输出/双向状态，并可选择是否主动弹出；网络模式允许各面输入并把产物送往所选网络。
- 模式分发、重试退避、暂停与结果映射只存在于 `IoController`；`IoAdapter` 只实现 `maintain`、`pushToFaces` 与 `pushToNetwork`，并用 `acceptsInput` 声明是否可接收输入。机器在自身逻辑之后调用 `IoController.tick()`，本刻产出的产物在同一刻送出，不会多占一刻显示为阻塞；`maintain` 在三种模式下都执行，网络未绑定、已暂停或处于退避期间缓冲仍继续消化。相邻方块能力缓存统一走 `NeighborCapabilities`，侧面能力注册统一走 `IoCapabilityRegistration`，放置时的配置卡应用统一走 `Level.applyConfigurationCardOnPlacement`。
- `IoConfiguration` 视为不可变值：`NetworkTargetRef` 的不透明数据只在存取边界深拷贝，读取路径（侧面能力查询、每 tick 推送、界面绑定）不再复制 NBT。
- 机器由 `core.MachineBlock` 与 `core.MachineBlockEntity` 两个基类收敛。`MachineBlock` 统一朝向状态、放置时的配置卡应用、界面有效性、交互顺序（配置卡优先于机器界面），以及唯一的掉落管线；`MachineBlockEntity` 只描述自己有什么：`hasStoredContents()` 表示内部存储需要随掉落物方块走，`takeHeldItems()` 表示只是替玩家保管、应当单独掉落的物品，`settingKeys()` 列出属于设置而非存储的持久化键，`computeDisplayState()` 表示要在世界里显示什么。
- 掉落顺序固定为「机器本体在前，代管物品在后」，全部由 `MachineBlock.onRemove` 生成，机器方块的战利品表一律为空表。内部存储写入掉落物方块，机器设置一律不写入：重新放置的机器总是回到默认配置，只能由放置时携带的配置卡重新播种。没有内容的机器不携带方块实体数据，掉落物仍可与全新机器堆叠。
- `lazy:configuration_card` 保存完整 `IoConfiguration`。卡可直接打开同一套 IO 面板；右击机器应用配置，潜行右击复制机器配置；玩家携带唯一明确配置的卡放置默认状态机器时，机器复制该配置。AE2 只为这张通用卡增加无线接入点链接行为，Curios 只增加可装备的配置卡槽。
- `lazy:modular_configurator` 由 `core.configurator` 注册并保存 18 个最高 1024 件的材料槽以及按模块 ID 隔离的不透明数据。Core 只提供模块注册、材料过滤、交互分发、持久化和服务端权威 GUI，不引用任何第三方类型。可选集成按确定的注册顺序宣告可用材料和方块交互；重复模块 ID 必须使启动失败。
- `integration.mekanism` 只在 Mekanism 存在时注册模块。它经配置卡能力与升级接口复制配置及已安装升级，粘贴时严格比对配置数据类型，并仅从工具槽位中按差额补齐升级。Mekanism API 类型不得越过该包的类加载边界。
- `c:tools/wrench` 标签的物品对机器生效：潜行右键拆除并优先放入玩家背包、放不下才落在玩家脚边；普通右键把机器顺时针转过一格。两个动作都在 `core.MachineWrench` 里判定，挂在 `PlayerInteractEvent.RightClickBlock` 上——潜行且手持物品时原版根本不会调用方块自身的 `useItemOn`，方块侧无法承载拆除。拆除先移除方块实体再销毁方块，`onRemove` 便自然不再重复掉落；旋转后显式 `invalidateCapabilities()`，因为同方块的状态变化不会自动失效邻居的能力缓存。
- 网络输出由 `core.io.NetworkInsertCapability`、`NetworkPayload`、`NetworkOutputProvider` 与统一路由器组成。稳定能力 ID 为 `lazy:item`、`lazy:fluid` 与 `neoforge:energy`；机器和提供者只通过能力交集配对。提供者或能力暂时缺失时保留绑定并重试，目标数据损坏或 Beyond Dimensions 已确认网络永久不存在时才退回被动模式；无法确认提交结果时持久暂停，避免重复写入。
- Beyond Dimensions 直接实现物品、流体和 FE 三项插入能力，并在集成包内保留网络 ID、`Long` 数量、模拟语义和异常保护，不再经过全局单提供者存储服务。
- AE2 目标保存无线接入点的维度与方块坐标。每次输出只在目标区块已加载、接入点活动且 Grid 可用时，将物品或流体写入该 Grid 的 `MEStorage`；不创建区块票据，也不保存运行时 Grid 标识。Applied Flux 存在时，由后初始化的 `integration.appflux` 适配器增加 FE 能力并以 `FluxKey(FE)` 写入同一库存，禁止调用 AE2 网络供电缓存。
- `integration.mysticalagriculture` 自成可选垂直切片：只有 Mystical Agriculture 存在时才挂载其 `DeferredRegister`、配置、能力、数据生成和创造标签入口。代码只通过资源 ID 延迟解析精华，不链接两项上游模组的 Java 类型；Agradditions 仅决定 Insanium 是否可用。
- Repairer 的修复后处理接口由领域自身持有，第三方适配器只注册回调。回调可并存且不得重复注册；单个回调失败不回滚基础修复。
- `feature.teleporter` 可以依赖独立的 `feature.voidworld`；其他跨领域依赖应保持显式且数量有限。
- 当前没有对外公开的扩展契约，因此不创建空的 `api` 包。出现真实外部调用方后再定义稳定 API。

测试包镜像主源码领域结构；跨领域测试工具放入 `rhx.lazy.core.testing`。

## 注册生命周期

每个领域由自己的注册对象持有一个或多个 `DeferredRegister`，并实现 `RegistryModule` 统一挂载。当前分别使用 `RepairerRegistries`、`BufferRegistries`、`EnergyRegistries`、`TeleporterRegistries`、`ProtectionRegistries` 和 `VoidWorldRegistries`；`LazyRegistries` 是唯一的集中挂载入口，由 Lazy 主对象在 KotlinForForge 提供的 `MOD_BUS` 上调用。

注册项必须保留为 `DeferredBlock`、`DeferredItem` 或 `DeferredHolder` 等延迟持有者。不得在静态初始化或注册装配阶段提前取出游戏对象。供应器在真正的注册阶段解析其他延迟持有者是允许的，例如创建方块对应的 `BlockItem`。

方块与对应物品由同一领域注册对象使用相同路径注册；自定义 `BlockItem` 通过 `core.registry.registerBlockItem` 在物品注册供应器内部延迟解析方块持有者。方块实体构建通过 `core.registry.buildType` 收敛 Minecraft 1.21.1 Java API 的空安全边界。能力注册由领域内的 `BufferCapabilities` 和 `EnergyCapabilities` 处理；物品复制器只查询邻接物品能力，不对外注册自身能力。全局创造模式标签页由 `LazyCreativeTabRegistry` 组合各领域物品。模组自有界面优先复用 LDLib2 的菜单类型，不为单个界面重复注册菜单和同步 payload。

## 事件总线与侧隔离

注册表和数据生成等生命周期事件使用模组事件总线。游戏运行事件只有在出现相应功能时才挂载到 NeoForge 游戏事件总线。

客户端类只能放入显式的 `client` 子包并从 `LazyClient` 组合入口触达。当前快捷键代码位于 `integration.curios.client`，只有本地加载 Curios 时才挂载事件；公共注册模块和业务领域均不得引用该包，避免专用服务器在类加载阶段解析客户端类。跨端可选 payload 必须声明为 optional，并在发送前确认连接已经协商对应 channel。

方块实体渲染器同样遵守这条边界：共用的 `core.render.client.MachineDisplayRenderer` 不认识任何具体机器，各领域在自己的 `client` 子包里注册（当前为 `feature.simulation.client.SimulationClientRenderers`），由 `LazyClient` 显式挂载。新增机器只需覆写 `computeDisplayState()` 并注册同一个渲染器。

LDLib2 UI 树必须能在逻辑服务端与客户端以相同结构构造。与方块实体有关的值通过 binding 延迟读取；不得根据仅一侧存在的方块实体增删元素，以免两侧同步序号错位。UI server event 只负责传递意图，改变世界前仍须在服务端重新校验方块、方块实体、玩家和距离。

## 数据与能力边界

方块实体持久化优先通过 LDLib2 `FieldManagedStorage`、`@Persisted` 与 `@LazyManaged` 实现。只需要存盘的方块实体不引入完整的描述同步接口；客户端展示的数据由打开的 UI 定向同步。托管集合加载后仍须执行长度、空值和容量归一化，业务变更后显式 `markDirty`。

唯一的例外是世界渲染：需要在方块上显示内容的机器覆写 `MachineBlockEntity.computeDisplayState()`，由 `MachineBlockEntity` 统一走原版 `getUpdateTag` / `getUpdatePacket` 通道同步一份 `core.render.MachineDisplayState`（一个已解析好的图标物品加一个 IDLE/RUNNING/BLOCKED 活性）。这条通道只承载渲染数据，不承载机器状态，因此 `handleUpdateTag` 只读取显示状态、不执行完整加载。不覆写的机器 `getUpdatePacket()` 返回 `null`、更新标签为空，完全不产生开销。图标由服务端解析后再发送——不能自我识别的目标（绑定实体的数据模型）在离开服务端之前就换成可辨认的物品——渲染器因此不含任何机器策略。变更检测同样收敛在基类：图标变化立即广播，活性翻转按固定间隔限流（输出积压排空时活性可能每刻抖动），轮询按方块坐标错相分摊到整个周期。

精华转换器不保存逐件物品列表，而保存目标档、完整目标数量与不足一个目标的精华量余数。所有换算均以固定档位精华量做 O(1) 整数运算；读取存档和每个服务端 tick 都执行容量归一化，并在 Insanium 不可用时降级为 Supremium。网络写入抛出异常且无法确认是否已提交时，转换器会持久暂停网络输出，直到玩家重新选择输出方式，避免自动重试造成重复写入。

Mojang Codec 继续用于数据驱动对象和跨版本结构化数据，NeoForge capability 继续作为物品、流体和能量互操作契约，Fzzy Config 继续管理服务器权威配置。这些原生契约不由 LDLib2 的 UI 或托管字段替代。

## 数据生成

Gradle 的 `data` 运行配置使用包含全部可选兼容模组的 integrations 类路径，输出到 `src/generated/resources`，并将主资源目录作为已有资源输入。基础 `GatherDataEvent` 与各集成分别装配自己的 provider，正常执行会刷新已经确认内容对应的生成资源。

客户端 provider 与服务端 provider 分开装配，当前生成缓冲器的模型、方块状态、配方、标签、空掉落表和双语本地化。生成资源应保持可重复；缓存文件、Blockbench 工程文件和运行目录不得进入发布 JAR。

## 本地化与资源标识

所有玩家可见文本使用翻译组件，不在代码中硬编码最终显示文案。翻译键使用 `lazy` 命名空间并按 `item`、`block`、`gui`、`message`、`tooltip` 等用途分组。

模组资源位置通过 `lazyId` 创建，避免散落命名空间字符串。注册名使用小写蛇形命名，并在注册表、资源路径和数据生成之间保持一致。
