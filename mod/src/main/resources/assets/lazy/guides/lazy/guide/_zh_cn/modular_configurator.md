---
navigation:
  parent: tools.md
  title: 模块化配置器
  icon: lazy:modular_configurator
  position: 40
item_ids:
  - lazy:modular_configurator
---

# 模块化配置器

<Column alignItems="center" fullWidth={true}>
  <ItemImage id="lazy:modular_configurator" scale="1.4" />
</Column>

模块化配置器用于保存配置其它模组机器时需要的材料。它有 18 个槽位，每格最多存放 1024 个相同物品。

## 操作

- 右键空气打开存储界面。
- 右键不支持的方块也会打开存储界面。
- 潜行右键空气清除保存的机器配置，存储材料不会消失。

箱子等方块仍优先执行自身的正常右键交互，因此可能先打开方块界面。

## Mekanism

安装 Mekanism 后：

- 潜行右键机器，复制设置和各种升级数量。
- 右键另一台完全相同的机器，应用保存内容。
- 相关机器和不同工厂等级不算同一种机器。

设置应用成功后，工具会消耗内部存放的 Mekanism 升级物品，补齐缺少的升级。目标机器已有的额外升级不会被移除。设置或权限检查失败时，不会消耗材料。

动作栏会显示配置结果，以及升级材料是否足够。

<Recipe id="lazy:modular_configurator" />
