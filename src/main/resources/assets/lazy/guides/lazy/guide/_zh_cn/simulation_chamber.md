---
navigation:
  parent: index.md
  title: 模拟室
  icon: lazy:simulation_chamber
  position: 50
item_ids:
  - lazy:simulation_chamber
---

# 模拟室

<BlockImage id="lazy:simulation_chamber" scale="1.15" />

模拟室运行数据驱动的物品和实体模拟，不需要维持真实农场或生物。产物会先存放在内部物品、流体库存中，等待 IO 输出。

## 输入

- **目标**：可放入有明确或自动模拟的物品、刷怪蛋、已绑定的 <ItemLink id="lazy:data_model" />，以及其它受支持的实体载体。
- **核心**：最多放入 64 个核心，每个核心贡献对应档位的速度和产出倍率。
- **工具**：提供可选行为。三个工具槽会在批次推进时读取；工具不会被消耗或损坏。

## 数据模型

使用空白 <ItemLink id="lazy:data_model" /> 点击受支持的生物，即可将该生物绑定到模型。潜行使用已绑定的模型可以清除绑定。玩家以及被模拟规则拒绝的目标无法绑定。

核心数值和叠加规则见[模拟核心](simulation_cores.md)页面。

## 工具槽

- 武器会作为模拟实体的击杀工具，按工具槽顺序第一个武器生效。
- 岩浆桶会过滤带有 incinerated 标签的产物。
- 抽取工作始终遵守服务端 `rollBudgetPerTick`；IO 模式只决定仓内产物运往哪里。

配方、自动材料规则、实体战利品、产出概率和工具标签都由数据驱动。输出库存已满或无法确认输出时，模拟室会暂停并显示积压警告。使用通用的 [IO 设置](io.md)配置物品/流体路由。

<Recipe id="lazy:simulation_chamber" />
