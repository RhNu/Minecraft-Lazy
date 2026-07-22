# 虚空维度与传送器

## 运行时配置

传送器使用 Fzzy Config，服务端与客户端都必须安装 `fzzy_config`。首次启动后会生成
`config/lazy/teleporter.toml`，包含以下设置：

- `chargeTicks`：蓄力游戏刻，默认 `20`，范围 `1..72000`。
- `cooldownSeconds`：成功后的冷却秒数，默认 `5`，范围 `0..3600`。
- `safeSearchRadius`：安全落点的水平搜索半径，默认 `8`，范围 `0..16`。
- `createVoidSafetyPlatform`：找不到支撑方块时是否允许在虚空端补平台，默认开启。

配置由服务器同步，默认要求权限等级 2 才能在 Fzzy Config 界面中修改。世界生成参数不从
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
