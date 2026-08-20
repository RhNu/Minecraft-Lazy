---
navigation:
  parent: index.md
  title: 修复器
  icon: lazy:repairer
  position: 40
item_ids:
  - lazy:repairer
---

# 修复器

<BlockImage id="lazy:repairer" scale="1.15" />

修复器一次修复一个已损坏且具有耐久度的物品，不需要材料，也不会消耗物品。

将物品放入输入槽并按下铁砧按钮。每次操作会在配置的最小值和最大值之间随机修复物品最大耐久度的百分比。默认范围是 5%–15%；两个值都由服务端配置控制，并限制在 1%–100%。

输入物品属于玩家而不是机器；破坏或拆除修复器时，它会作为单独掉落物返回。兼容联动可以在修复后执行处理；安装 Silent Gear 时会重新计算其装备数据。

<Recipe id="lazy:repairer" />
