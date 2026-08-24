# 塑形机

## 文档状态

本文件是**已实现设计**，对应 `lazy:shaper` 的行为与扩展约定。

以下三条是实现的固定前提：

- 目标形态是**全机一个**，不是每条并行一个。
- 目标形态用**幽灵样品槽**设定，不新增物品也不新增贴图。
- JEI 采用**以基准形态为中心的辐射配方**，不做完整互转矩阵。

其余数值、命名、贴图和交互细节已经随本次实现定稿。

## 定位

`lazy:shaper` 把同一种材料的常见形态互相转换：铁锭出铁板，铁块出铁粒，铁杆回铁板。
它不产生材料，也不消耗材料——只换形状。定位是懒狗工具箱里的「后期低成本大批量整形」，
配方即时完成，没有进度条，没有能量，没有配方书。

机器默认 8 条并行输入线，共享一个输出池。全机只有一个设置：目标形态。

## 单位制

所有形态换算走一张**单位值表**，基准是 **1 锭 = 144 单位**。这是 Mekanism 与 GregTech
系的熔融量惯例，好处是杆（半锭）、齿轮（四锭）这类非整数比在整数域内就能表达，
将来若要接「熔成流体」也不必重算一遍表。

| 形态 | 形态 id | 标签模板 | 单位值 |
| --- | --- | --- | --- |
| 粒 | `nugget` | `c:nuggets/{material}` | 16 |
| 锭 | `ingot` | `c:ingots/{material}` | 144 |
| 宝石 | `gem` | `c:gems/{material}` | 144 |
| 粉 | `dust` | `c:dusts/{material}` | 144 |
| 粗矿 | `raw_material` | `c:raw_materials/{material}` | 144 |
| 板 | `plate` | `c:plates/{material}` | 144 |
| 杆 | `rod` | `c:rods/{material}` | 72 |
| 线 | `wire` | `c:wires/{material}` | 72 |
| 齿轮 | `gear` | `c:gears/{material}` | 576 |
| 块 | `storage_block` | `c:storage_blocks/{material}` | 1296 |
| 粗矿块 | `raw_storage_block` | `c:storage_blocks/raw_{material}` | 1296 |

NeoForge 1.21.1 自身只定义 `nuggets`、`ingots`、`gems`、`dusts`、`raw_materials`、
`rods`、`storage_blocks` 七组（全部是**复数**路径）。`plates`、`wires`、`gears` 来自
模组，装了才有；查不到候选的形态自然缺席，不需要额外开关。

标签模板带 `{material}` 占位符而不是单纯的前缀，是因为粗矿块的路径是
`c:storage_blocks/raw_iron` 而不是 `c:raw_storage_blocks/iron`。反查时按**字面前缀最长者胜**
消歧：`c:storage_blocks/raw_iron` 判为「iron 的粗矿块」，不是「raw_iron 的块」。真名以
`raw_` 开头的材料是这条规则的已知盲区，用黑名单排除。

## 换算算法

设输入形态单位值 `a`，输出形态单位值 `b`，`g = gcd(a, b)`。最小整数交易是：

```
inPerTrade  = b / g      每笔交易消耗的输入件数
outPerTrade = a / g      每笔交易产出的输出件数
trades      = min(输入件数 / inPerTrade, 输出容量 / outPerTrade)
消耗 = trades * inPerTrade
产出 = trades * outPerTrade
```

验算：

| 转换 | a → b | inPerTrade | outPerTrade | 结果 |
| --- | --- | --- | --- | --- |
| 粒 → 锭 | 16 → 144 | 9 | 1 | 9 粒 → 1 锭 |
| 锭 → 粒 | 144 → 16 | 1 | 9 | 1 锭 → 9 粒 |
| 块 → 粒 | 1296 → 16 | 1 | 81 | 1 块 → 81 粒 |
| 板 → 杆 | 144 → 72 | 1 | 2 | 1 板 → 2 杆 |
| 杆 → 齿轮 | 72 → 576 | 8 | 1 | 8 杆 → 1 齿轮 |
| 齿轮 → 块 | 576 → 1296 | 9 | 4 | 9 齿轮 → 4 块 |

三条性质直接来自这个式子，也是选它的理由：

- **无损**：只做整笔交易，永远不出现除不尽的碎料。
- **无隐藏状态**：凑不满一笔就一件不动，余料原样留在输入槽里看得见。机器不需要
  精华转换器那样的余数账本，破坏掉落也就不需要携带账本。
- **O(1)**：两次除法一次 `min`，与件数无关。

`trades` 同时被输出容量夹住，所以「一刻打完」的实际含义是**一刻做满输出池能装下的量**。
1024 铁锭 → 1024 铁板（1:1）确实一刻完成；1024 铁块 → 铁粒（1:81）则按输出容量分刻推进。
另一条路是像模拟室那样挂 `LongItemStack` 待发队列，但那会让一台堵住的机器变成无界内存
写入点——这里刻意不走那条路，让下游 IO 成为限流器。

## 界面与设置

```
┌─ 塑形机 ─────────────────────────┐
│       [铁板]        [IO] [警告]  │
├──────────────────────────────────┤
│  入 [铁锭×1024][铜锭×1024][ ][ ]  │
│     [ ][ ][ ][ ]                  │
│  出 [铁板×1024][铜板×1024][ ][ ]  │
│     [ ][ ][ ][ ]                  │
├──────────────────────────────────┤
│  玩家背包                          │
└──────────────────────────────────┘
```

**样品槽**是不消耗的幽灵槽，全机唯一的设置。鼠标持物点击写入样品，空手点击清空；
样品本身永远不会被拿走或消耗，也可以从 JEI 直接拖入（LDLib2 的
`asXeiRecipeIngredient` 已有这条通路）。

机器只存这一件样品，目标形态按需从索引里 O(1) 反查得到——不额外存形态 id，
样品自己就是图标，因此**新增贴图数为零，新增物品数为零**。样品解析不出形态
（数据包变更、模组移除）时机器停摆并在界面报警，不静默降级。

放不进样品槽的物品说明它当前不属于任何已知形态。

## 每刻逻辑

```
样品为空                        → 返回
8 条输入线全空                  → 返回
逐线:
  输入槽空                      → 跳过
  索引反查形态失败              → 跳过
  输入形态 == 目标形态          → 跳过（不做恒等转换）
  该材料没有目标形态的产物      → 跳过
  按上节算 trades，为 0         → 跳过（输出满，记为 BLOCKED）
  消耗输入，产出写入共享输出池
ioController.tick()
tickDisplayState()
```

输出池是 8 个共享槽，插入顺序为「先并入同物品的槽，再占空槽」，与
`SimulationOutputRouter.insertItemLocal` 同构。两条线出同一种板会自动并槽，
腾出的槽还给其它材料。

## 性能

机器每刻是纯整数运算，没有配方查找、没有战利品表、没有实体创建，因此性能预算全在
「每刻要看几次」上：

- 空机成本是一次样品判空。
- 有料时成本是 8 次 `HashMap<Item, …>` 查表加 8 组整数运算，与槽内件数无关。
- 检索表在 `TagsUpdatedEvent` 与配置变更时重建，绝不在 tick 内扫标签。
- 堵塞时 `trades` 算出 0 就退出，不产生写入、不 `setChanged`、不同步。
- 没有进度、没有批次、没有待发队列，持久化只有槽位与样品，存档体积恒定。

**8 条并行是性能上界，不只是容量设定。** 单槽 1024 意味着一台机器最多同时处理 8 种材料，
所以无论吞吐多大，每刻工作量都被 8 卡死。若改成小容量多槽位，同样的吞吐会让每刻工作量
随槽位数线性上涨——这是选大槽的真实理由。

## 静态检索表

`core.material.MaterialIndex` 在 `TagsUpdatedEvent` 与相关配置变更时重建两张表：

- `formOf: Map<Item, MaterialFormMatch>` — 物品 →（材料, 形态）。
- `itemFor: Map<MaterialKey, Item>` —（材料, 形态）→ 规范产物。

构建方式是遍历一次 `BuiltInRegistries.ITEM.getTags()`，对每个标签路径尝试匹配形态模板。
材料数量在数百量级、形态十余种，整表在重载时一次算完。

反查歧义按模拟室既有先例处理：**一个物品命中多个形态时不猜，直接不参与转换**
（对应 `TaggedMaterialAdapter` 里 `matches.size > 1 -> null` 那条）。
正查歧义（一个 `c:` 标签下多个候选产物）走下一节的优先级体系。

## 优先级体系通用化

`TaggedMaterialPriority` 与 `taggedMaterialIdComparator` 目前住在
`feature/simulation/TaggedMaterialSimulation.kt`，只服务模拟室。塑形机需要同一套
「从标签里按序挑一个产物」的语义，因此把它上提到 `core.material.MaterialTagPreference`，
成为全模组唯一入口。

配置项也跟着上提：

| 现状 | 目标 |
| --- | --- |
| `lazy-simulation.toml` → `taggedMaterialModPriority` | `lazy-material.toml` → `modPriority` |

默认值不变（`kubejs > minecraft > alltheores > create > mekanism > jaopca`，
未命中者按命名空间与完整物品 ID 升序）。这是**破坏性配置变更**，提交按项目惯例带 `!`，
沿用 `20ed0df` 那次配置迁移的处理方式。

`feature/simulation` 侧保留 `TaggedMaterialRules`（它是模拟室特有的「输入标签长出输出标签」
规则），只把优先级解析改为调用 core。

## 数值倒挂与守门

单位值表是一张**全局约定**，而整合包里各模组的真实配方成本可能与它不符。两个方向：

- 表值**低于**真实成本 → 往返净亏，玩家自己会不用，无害。
- 表值**高于**真实成本 → 往返净赚，出现复制。

已知的典型案例是石英：原版下界石英块是 4 石英，若某模组补上 `c:storage_blocks/quartz`，
按表里 1296 会被当成 9 石英，形成 2.25× 复制。AGENTS.md 明确「不考虑平衡性」，
所以这里不做保守化设计，但要把闸门备齐：

- `lazy:material_form` 是同步的数据包注册表，可逐形态移除或覆写单位值，也可新增形态。
- `lazy:shaper/blacklist/input` 物品标签禁止对应物品作为输入。
- `lazy:shaper/blacklist/output` 物品标签禁止对应物品作为输出。
- 默认黑名单预置已知倒挂项（石英块等），整合包可增删。

反过来，原版煤炭是已知的**缺口**而非倒挂：`c:storage_blocks/coal` 存在，但煤炭本身没有
任何 `c:` 形态标签，所以煤炭 ↔ 煤炭块不会生效。整合包补一条 `c:gems/coal` 即可。

## 机器通用交互

塑形机是标准 Lazy 机器，直接继承既有约定，不引入新规则：

- 基类为 `core.MachineBlock` 与 `core.io.IoManagedBlockEntity`。
- IO 三模式（被动 / 六面配置 / 网络输出）由 `IoController` 提供；`IoAdapter` 只声明
  `capabilities = setOf(NetworkInsertCapabilities.ITEM)` 并实现输出池的推送。
- 扳手右键旋转、潜行右键拆除；配置卡读写 IO 配置。
- 掉落顺序为「机器本体在前」。输入输出槽属于存储，随掉落物方块走；样品属于设置，
  一律不保存，重新放置的机器回到空样品。
- 正面显示目标形态的图标；活性为转换成功 `RUNNING`、输出满 `BLOCKED`、其余 `IDLE`，
  走 `MachineBlockEntity.computeDisplayState()` 既有通道。

样品无法随机器搬迁这一点，正确的补法是给 `lazy:modular_configurator` 增加一个塑形机模块
（它本来就按模块 ID 隔离不透明数据，用来批量复制机器设置），而不是把样品塞进配置卡。
这属于后续工作，不在本次范围内。

## JEI

按确认的方案做**以基准形态为中心的辐射配方**：每种材料选一个基准形态，只生成
「基准 ↔ 其它每种可用形态」，不做 N² 矩阵。基准优先级为
锭 > 宝石 > 粉 > 粗矿 > 其余中单位值最低者。

每材料约 6–10 条，两百种材料约 1200–2000 条虚拟配方。

客户端索引**不需要自定义同步包**：`c:` 标签本身随存档同步，`TagsUpdatedEvent` 两侧都会触发，
而 `modPriority` 属于 `ModConfig.Type.SERVER`——`ConfigSync.syncConfigs()` 会把整个 SERVER
配置集打包成 `ConfigFilePayload` 发给客户端，因此两侧能各自构建出同一张表。这一点与模拟室
不同：模拟室的快照包是因为它依赖战利品表与适配器，塑形机没有这层依赖。

但**两个输入的到达顺序不能假定**。`SyncConfig` 是经 `RegisterConfigurationTasksEvent` 注册的
普通配置阶段任务，而标签由原版 `SynchronizeRegistriesTask` 送达；NeoForge 在
`configureEarlyTasks` 的注释里明说只有早期任务才排在 `SynchronizeRegistriesTask` 之前，
也就是说客户端很可能**先拿到标签、后拿到配置**。因此索引重建挂**两个**信号：

- `TagsUpdatedEvent` — 标签变了。
- 配置加载 / 重载事件 — `modPriority` 变了；形态表随数据包重载一起更新。

两者任一触发都重建全表。这样无论谁先到，最终态都一致，也顺带覆盖了运行期改配置的情况。
构建本身是遍历一次标签表，重复触发的代价可以接受。

## 包结构

```text
rhx.lazy.core.material              新增
├─ MaterialForm.kt                  形态定义（id、标签模板、单位值）
├─ MaterialForms.kt                同步数据包注册表与默认形态
├─ MaterialIndex.kt                 重载期构建的两张检索表
├─ MaterialIndexReloads.kt          标签与配置变更监听
├─ MaterialTagPreference.kt         由 feature/simulation 上提
└─ MaterialConfig.kt                lazy-material.toml

rhx.lazy.feature.shaping            新增垂直切片
├─ ShaperBlock.kt
├─ ShaperBlockEntity.kt
├─ ShaperBlockItem.kt
├─ ShaperCapabilities.kt
├─ ShaperLanes.kt                   8 路大容量输入与共享输出池
├─ ShaperTrade.kt                   纯换算数学，可单元测试
├─ ShaperRegistries.kt
├─ ShaperTags.kt
└─ ShaperUI.kt
```

`ShaperRegistries` 追加进 `LazyRegistries.modules`；`ShaperTrade` 是纯换算模型，
测试放 `rhx.lazy.feature.shaping` 镜像包。

## 配套资源

- `art/block/overlay/shaper.svg` 复用既有底部、侧面与顶部机壳纹理。
- `assets/lazy/lss/shaper.lss`。
- 方块状态、模型、配方与空掉落表由数据生成产出；与其它机器一致，不添加专用挖掘标签。
- `en_us` 与 `zh_cn` 双语，含每个形态的显示名。
- `assets/lazy/guides/lazy/guide/shaper.md` 与 `_zh_cn/shaper.md`。

## 已定实现细节

- 合成配方为切石机、两个铜锭与机器外壳。
- 单位值表使用同步的数据包注册表 `lazy:material_form`；数据包可覆盖默认条目或新增形态。
- 样品槽提示直接列出当前材料可执行的换算，JEI 提供以基准形态为中心的完整辐射预览。
- 默认形态清单维持表中十一种；`sheets`、`foils`、`coins` 等由整合包按需注册。
