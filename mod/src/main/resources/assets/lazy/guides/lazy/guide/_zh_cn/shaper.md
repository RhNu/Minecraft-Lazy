---
navigation:
  parent: machines.md
  title: 塑形机
  icon: lazy:shaper
  position: 60
item_ids:
  - lazy:shaper
---

# 塑形机

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:shaper" scale="8" />
</Column>

塑形机可以在不损失材料、不消耗能量的情况下转换材料形态。

## 用法

- 手持想要的产物点击幽灵样品槽。
- 放入同种材料的粒、锭、宝石、粉、粗矿、板、杆、线、齿轮或储存块。
- 空鼠标点击样品槽可以清除选择。

塑形机只进行完整的无损换算。例如，九粒变成一锭，一锭变成九粒，一板变成两杆。凑不出完整产物的余料会留在输入仓。

## 存储与输出

- 输入仓和输出仓各自最多容纳八种物品。
- [IO 设置](io.md)控制侧面输入、自动输出和网络输出。
- JEI 会列出每种材料可用的换算。

输入与输出会保存在掉落的机器中；重新放置时，样品和 IO 设置会重置。

<Recipe id="lazy:shaper" />
