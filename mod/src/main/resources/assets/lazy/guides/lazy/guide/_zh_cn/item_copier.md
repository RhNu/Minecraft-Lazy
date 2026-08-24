---
navigation:
  parent: machines.md
  title: 物品复制器
  icon: lazy:item_copier
  position: 40
item_ids:
  - lazy:item_copier
---

# 物品复制器

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:item_copier" scale="8" />
</Column>

物品复制器会反复生成选定物品，不消耗模板。

## 模板与间隔

- 手持物品点击幽灵模板槽，选择要复制的物品。
- 也可以从 JEI 把物品拖到模板槽。
- 空鼠标点击模板槽可以清除选择。
- 每次工作会尝试输出一组正常最大堆叠。

间隔按钮可以切换四种速度：

| 设置 | 间隔 |
| --- | ---: |
| 快速 | 10 刻 |
| 普通 | 20 刻 |
| 慢速 | 100 刻 |
| 极慢 | 200 刻 |

## 自动化

通过 [IO 设置](io.md)把复制品送入相邻库存或支持的网络。目标无法完整接收时，复制器会等待并稍后重试。

破坏或拆除后，模板和间隔会保存在掉落的机器中；重新放置时，IO 设置会重置。

<Recipe id="lazy:item_copier" />
