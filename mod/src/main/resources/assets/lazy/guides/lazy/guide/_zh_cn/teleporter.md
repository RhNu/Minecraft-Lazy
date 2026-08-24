---
navigation:
  parent: tools.md
  title: 传送器
  icon: lazy:teleporter
  position: 30
item_ids:
  - lazy:teleporter
---

# 传送器

<Column alignItems="center" fullWidth={true}>
  <ItemImage id="lazy:teleporter" scale="1.4" />
</Column>

传送器用于在普通世界与 Lazy 的专用虚空维度之间往返。每件传送器分别保存自己的外界返回点和虚空目标点。

## 用法

- 手持蓄力一秒即可传送。成功使用后默认冷却五秒。
- 进入虚空时会记录当前位置作为返回点；在虚空中使用会返回该位置。
- 物品提示会显示已保存的返回点与目标点坐标；传送失败不会覆盖它们。
- 安装 Curios 后，可以把传送器装备到专用槽位。在控制设置中绑定“激活传送器”即可免蓄力使用；冷却和维度限制仍然生效。

服务器可以调整蓄力时间、冷却、安全落点搜索范围和安全平台。部分维度可能禁止传送。

<Recipe id="lazy:teleporter" />
