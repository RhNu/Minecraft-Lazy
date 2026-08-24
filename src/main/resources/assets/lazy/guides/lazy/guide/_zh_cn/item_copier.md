---
navigation:
  parent: index.md
  title: 物品复制器
  icon: lazy:item_copier
  position: 30
item_ids:
  - lazy:item_copier
---

# 物品复制器

<BlockImage id="lazy:item_copier" scale="8" />

物品复制器会反复生成选定物品，并将其推入相邻库存或兼容网络，不会消耗作为模板的物品。

## 模板与间隔

打开界面，手持物品点击幽灵模板槽即可标记，也可以从 JEI 直接拖入目标产物；空鼠标点击可以清除模板。模板只保存一个物品，但每次推送都会尝试输出该物品正常最大堆叠数。

间隔按钮会循环切换以下服务端调度：

| 设置 | 间隔 |
| --- | ---: |
| 快速 | 10 刻 |
| 普通 | 20 刻 |
| 慢速 | 100 刻 |
| 极慢 | 200 刻 |

## 自动化

使用通用的 [IO 设置](io.md)选择相邻面输出或网络目标。破坏或拆除机器时，模板和间隔会随机器保留。

<Recipe id="lazy:item_copier" />
