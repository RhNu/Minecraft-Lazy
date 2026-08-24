---
navigation:
  parent: index.md
  title: 命令
  icon: lazy:teleporter
  position: 40
---

# 命令

## 返回地表

<CommandLink command="/lazy rise">运行 /lazy rise</CommandLink>

从玩家所在位置向上寻找，将玩家移动到同一竖直方向内第一个安全且能看见天空的位置。

## 伤害保护

保护命令需要 2 级管理员权限。

- <CommandLink command="/lazy protection damage_cap">查看当前设置</CommandLink>
- <CommandLink command="/lazy protection damage_cap on">开启保护</CommandLink>
- <CommandLink command="/lazy protection damage_cap off">关闭保护</CommandLink>
- `/lazy protection damage_cap set <数值>` — 设置单次伤害上限
- <CommandLink command="/lazy protection damage_cap reset">重置已保存设置</CommandLink>

### 伤害上限

- 正数会限制每次受击造成的伤害。
- 设为 `0` 并开启保护后，普通伤害不会生效。
- 能够无视无敌状态的伤害不受影响。
- 重置后会清除已保存设置；保护关闭，数值恢复为 `0`。
