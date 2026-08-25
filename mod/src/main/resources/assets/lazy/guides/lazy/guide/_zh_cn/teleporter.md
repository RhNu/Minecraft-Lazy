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

传送器会打开由服务端控制的菜单，用来创建和访问 Lazy 虚空维度中的封装空间。外界返回点和最近选择的空间属于玩家，不再绑定到某一件物品。

## 用法

- 手持传送器右键即可打开菜单。
- 可以创建 15×15×15 的封装空间，在按新到旧排列的列表中选择、命名或前往空间。
- 短 ID 会一直显示；悬停可查看完整 UUID。删除空间需要二次确认，并会摧毁外壳以及其中的全部方块和实体。
- “前往原点”会到达虚空中心平台；“返回外界”优先使用最近一次成功进入虚空前的位置，再回退到重生点。
- 成功传送后默认冷却五秒；失败传送不会覆盖外界返回点。
- 安装 Curios 后，装备传送器并绑定“激活传送器”即可打开同一菜单。

普通玩家只能列出和管理自己创建的空间，管理员可以管理全部空间。服务器可以调整冷却、安全落点搜索范围、每名玩家的空间上限和维度限制。

<Recipe id="lazy:teleporter" />
