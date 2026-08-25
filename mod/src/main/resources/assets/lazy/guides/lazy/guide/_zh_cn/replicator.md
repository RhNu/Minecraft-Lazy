---
navigation:
    parent: machines.md
    title: 复制器
    icon: lazy:replicator
    position: 40
item_ids:
    - lazy:replicator
---

# 复制器

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:replicator" scale="8" />
</Column>

复制器会反复生成标记的资源，不消耗真实来源。

## 资源与间隔

- 界面只有一个资源槽：手持物品点击该槽，或从 JEI 把物品、流体拖到同一个位置。安装 Mekanism 后，也可以直接从 JEI 拖入化学品。
- 点击数量按钮，或右键/中键资源槽，可以输入精确数值，也可以用 `−` / `+` 按资源单位调整。
- 再次标记同一种资源会保留数量；标记另一种资源会采用它的默认数量。用空鼠标左键资源槽可以清除标记。

间隔按钮可以切换四种速度：

| 设置 |   间隔 |
| ---- | -----: |
| 快速 |  10 刻 |
| 普通 |  20 刻 |
| 慢速 | 100 刻 |
| 极慢 | 200 刻 |

## 自动化

通过 [IO 设置](io.md)把复制品送入与所选资源对应的相邻能力，或支持的网络。Mekanism 化学品会通过相邻化学品能力输出。未被接受的输出不会缓存；复制器会在下一次工作时重新提供一份副本。

破坏或拆除后，标记资源和间隔会保存在掉落的机器中；重新放置时，IO 设置会重置。

<Recipe id="lazy:replicator" />
