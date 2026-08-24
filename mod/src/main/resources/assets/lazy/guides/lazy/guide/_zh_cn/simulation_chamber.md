---
navigation:
  parent: machines.md
  title: 模拟室
  icon: lazy:simulation_chamber
  position: 70
item_ids:
  - lazy:simulation_chamber
---

# 模拟室

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:simulation_chamber" scale="8" />
</Column>

模拟室无需建造农场或长期饲养生物，也能生产对应掉落物。

## 输入

- **目标：**受支持的物品、刷怪蛋或已绑定的 <ItemLink id="lazy:data_model" />。
- **处理核心：**最多 64 个。等级越高，速度和产出越高。
- **工具：**最多三个可选工具，不会被消耗或损坏。

## 数据模型

使用空白 <ItemLink id="lazy:data_model" /> 点击受支持的生物，再把绑定后的模型放进目标槽。核心数值见[处理核心](processing_cores.md)。

## 工具槽

- 第一把武器会作为模拟击杀工具。
- 岩浆桶会移除正常情况下会被岩浆烧毁的掉落物。

JEI 会显示可用的模拟和可能产物。输出仓已满时，模拟室会暂停，直到腾出空间。通过 [IO 设置](io.md)输出物品和流体。

<Recipe id="lazy:simulation_chamber" />
