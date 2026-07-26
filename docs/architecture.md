# 架构约定

## 项目边界

Lazy 当前提供模组入口、注册基础设施、修复器、缓冲器、能量电池、能量源、虚空网格维度、配套传送器，以及少量无业务含义的通用工具。其他物品、方块和维度仍属于后续内容工作，在设计确认前不得进入运行源码。

项目只面向 Minecraft 1.21.1 NeoForge，不承担 Fabric、Forge 或多加载器兼容层。Minecraft 小版本也是兼容边界的一部分，依赖升级不得隐式改变游戏版本。

## 包组织

所有 Kotlin 源码位于 `rhx.lazy` 包下，并让物理目录与包名保持一致。

```text
rhx.lazy
├─ Lazy.kt
├─ core
│  ├─ command
│  ├─ datagen
│  └─ registry
├─ feature
│  ├─ buffer
│  ├─ energy
│  ├─ protection
│  ├─ repairer
│  ├─ rise
│  ├─ teleporter
│  └─ voidworld
└─ integration
   ├─ beyonddimensions
   └─ curios
      └─ client
```

- 根包只保存模组入口和 `MOD_ID`。
- `core` 保存经过跨领域复用证明的 Minecraft 扩展、公共基类，以及命令、数据生成和注册的全局装配入口。
- `feature` 使用垂直切片组织业务；方块、物品、方块实体、界面、事件、配置和注册跟随各自领域，不再按 Minecraft 类型横向分包。
- `integration` 保存第三方模组适配。集成可以依赖 `core` 和 `feature`，业务领域不得反向依赖集成。
- 可选集成使用只包含 Minecraft/NeoForge 类型的内部门面隔离第三方符号；具体适配器只能在确认对应模组已加载后解析。当前 Beyond Dimensions 门面以网络 ID 为边界，包装物品、流体和 FE 的查询、模拟、插入与提取，不承担网络成员和权限管理。
- `feature.teleporter` 可以依赖独立的 `feature.voidworld`；其他跨领域依赖应保持显式且数量有限。
- 当前没有对外公开的扩展契约，因此不创建空的 `api` 包。出现真实外部调用方后再定义稳定 API。

测试包镜像主源码领域结构；跨领域测试工具放入 `rhx.lazy.core.testing`。

## 注册生命周期

每个领域由自己的注册对象持有一个或多个 `DeferredRegister`，并实现 `RegistryModule` 统一挂载。当前分别使用 `RepairerRegistries`、`BufferRegistries`、`EnergyRegistries`、`TeleporterRegistries`、`ProtectionRegistries` 和 `VoidWorldRegistries`；`LazyRegistries` 是唯一的集中挂载入口，由 Lazy 主对象在 KotlinForForge 提供的 `MOD_BUS` 上调用。

注册项必须保留为 `DeferredBlock`、`DeferredItem` 或 `DeferredHolder` 等延迟持有者。不得在静态初始化或注册装配阶段提前取出游戏对象。供应器在真正的注册阶段解析其他延迟持有者是允许的，例如创建方块对应的 `BlockItem`。

方块与对应物品由同一领域注册对象使用相同路径注册；自定义 `BlockItem` 通过 `core.registry.registerBlockItem` 在物品注册供应器内部延迟解析方块持有者。方块实体构建通过 `core.registry.buildType` 收敛 Minecraft 1.21.1 Java API 的空安全边界。能力注册由领域内的 `BufferCapabilities` 和 `EnergyCapabilities` 处理；全局创造模式标签页由 `LazyCreativeTabRegistry` 组合各领域物品。模组自有界面优先复用 LDLib2 的菜单类型，不为单个界面重复注册菜单和同步 payload。

## 事件总线与侧隔离

注册表和数据生成等生命周期事件使用模组事件总线。游戏运行事件只有在出现相应功能时才挂载到 NeoForge 游戏事件总线。

客户端类只能放入显式的 `client` 子包并从客户端事件订阅器触达。当前快捷键代码位于 `integration.curios.client`，公共注册模块和业务领域均不得引用该包，避免专用服务器在类加载阶段解析客户端类。

LDLib2 UI 树必须能在逻辑服务端与客户端以相同结构构造。与方块实体有关的值通过 binding 延迟读取；不得根据仅一侧存在的方块实体增删元素，以免两侧同步序号错位。UI server event 只负责传递意图，改变世界前仍须在服务端重新校验方块、方块实体、玩家和距离。

## 数据与能力边界

方块实体持久化优先通过 LDLib2 `FieldManagedStorage`、`@Persisted` 与 `@LazyManaged` 实现。只需要存盘的方块实体不引入完整的描述同步接口；客户端展示的数据由打开的 UI 定向同步。托管集合加载后仍须执行长度、空值和容量归一化，业务变更后显式 `markDirty`。

Mojang Codec 继续用于数据驱动对象和跨版本结构化数据，NeoForge capability 继续作为物品、流体和能量互操作契约，Fzzy Config 继续管理服务器权威配置。这些原生契约不由 LDLib2 的 UI 或托管字段替代。

## 数据生成

Gradle 的 `data` 运行配置输出到 `src/generated/resources`，并将主资源目录作为已有资源输入。`GatherDataEvent` 按客户端与服务端边界装配 provider，正常执行会刷新已经确认内容对应的生成资源。

客户端 provider 与服务端 provider 分开装配，当前生成缓冲器的模型、方块状态、配方、标签、空掉落表和双语本地化。生成资源应保持可重复；缓存文件、Blockbench 工程文件和运行目录不得进入发布 JAR。

## 本地化与资源标识

所有玩家可见文本使用翻译组件，不在代码中硬编码最终显示文案。翻译键使用 `lazy` 命名空间并按 `item`、`block`、`gui`、`message`、`tooltip` 等用途分组。

模组资源位置通过 `lazyId` 创建，避免散落命名空间字符串。注册名使用小写蛇形命名，并在注册表、资源路径和数据生成之间保持一致。
