# 虚空维度与传送器

## 运行时配置

传送器使用 NeoForge 的服务器配置系统，不需要额外的配置库。首次打开世界后会在该世界的
`serverconfig/lazy-teleporter.toml` 生成以下设置：

- `chargeTicks`：蓄力游戏刻，默认 `20`，范围 `1..72000`。
- `cooldownSeconds`：成功后的冷却秒数，默认 `5`，范围 `0..3600`。
- `safeSearchRadius`：安全落点的水平搜索半径，默认 `8`，范围 `0..16`。
- `createVoidSafetyPlatform`：找不到支撑方块时是否允许在虚空端补平台，默认开启。

传送器首版内置维度黑名单 `compactmachines:compact_world`。玩家处于该维度时，手持传送器
和 Curios 槽位中的传送器均不可激活，并会收到本地化提示。

配置由 NeoForge 同步给客户端；当前不提供游戏内配置界面或远程修改入口。世界生成参数不从
该文件读取。

## 数据包配置网格

内置维度位于 `data/lazy/dimension/void.json`。数据包可用同一路径覆盖它，例如：

```json
{
  "type": "lazy:void",
  "generator": {
    "type": "lazy:grid_generator",
    "settings": {
      "biome": "lazy:void",
      "border_block": "minecraft:stone_bricks",
      "grid_chunk_size": 3,
      "inner_block": "minecraft:smooth_stone",
      "layer_height": 128
    }
  }
}
```

`layer_height` 允许 `-64..317`，`grid_chunk_size` 允许 `1..64`，两种方块必须是有效的注册
方块 ID。修改后需要退出并重新进入世界；已生成区块不会重写，只有新区块使用新设置，因此
变更方块或周期可能在新旧区块边界形成接缝。

## 传送语义

每件传送器独立保存外界返回点和虚空目标点。首次进入使用 `(0, layer_height + 1, 0)`，
首次从虚空返回使用主世界出生点。安全搜索失败、目标维度不存在或跨维度切换失败时，物品
数据与冷却均保持不变。传送器不会修改外界方块；只有虚空端可按配置补建 3×3 落脚平台。

旧 Expo 的 `expo:*` 资源标识和物品数据不会迁移。
