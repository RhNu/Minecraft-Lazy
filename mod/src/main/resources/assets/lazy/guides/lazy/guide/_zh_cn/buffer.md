---
navigation:
  parent: machines.md
  title: 缓冲器
  icon: lazy:buffer
  position: 20
item_ids:
  - lazy:buffer
---

# 缓冲器

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:buffer" scale="8" />
</Column>

缓冲器可以在一个方块中大量存放物品和流体。

## 存储

- 8 个物品槽，每槽存放一种物品，最多 256 个。
- 4 个流体罐，每罐存放一种流体，最多 64,000 mB。
- 物品槽支持普通点击、拆分、拖放和 Shift 点击。

- 右键打开界面。
- 潜行右键在动作栏显示物品和流体总量。
- 清空按钮会在确认后销毁全部内容。

## 自动化

相邻机器可以输入或取出内容。[IO 设置](io.md)可以让缓冲器主动输出。

破坏或用扳手拆除后，物品和流体会保存在掉落的缓冲器中；重新放置时，IO 设置会重置。

<Recipe id="lazy:buffer" />
