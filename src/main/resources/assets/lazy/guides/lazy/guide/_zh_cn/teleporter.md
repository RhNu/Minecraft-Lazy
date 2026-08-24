---
navigation:
  parent: index.md
  title: 传送器
  icon: lazy:teleporter
  position: 105
item_ids:
  - lazy:teleporter
---

# 传送器

<ItemImage id="lazy:teleporter" scale="1.4" />

传送器用于在普通世界与 Lazy 的专用虚空维度之间往返。每件传送器分别保存自己的外界返回点和虚空目标点。

## 用法

- 手持蓄力一秒即可传送。成功使用后默认冷却五秒。
- 进入虚空时会记录当前位置作为返回点；在虚空中使用会返回该位置。
- 物品提示会显示已保存的返回点与目标点坐标；传送失败不会覆盖它们。
- 安装 Curios 后，可以把传送器装备到专用槽位，并用默认未绑定的“使用传送器”按键激活。按键激活无需蓄力，但仍受冷却和维度限制。

服务端可以调整蓄力时间、冷却、安全位置搜索半径，以及是否在虚空端创建安全平台。部分维度可能禁止激活传送器。

<Recipe id="lazy:teleporter" />
