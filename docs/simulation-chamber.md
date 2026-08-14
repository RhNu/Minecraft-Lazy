# 模拟室

模拟室使用一个不消耗的模拟目标和一组不消耗的模拟核心工作，不需要土壤或 FE。目标槽接受显式物品模拟配方、可自动识别的矿物，或已绑定的数据模型；核心槽接受同一等级的可堆叠核心。

空数据模型对生物使用后会绑定该生物类型，绑定成功时播放提示音并显示附魔光效；手持已绑定模型潜行使用可清除绑定。模拟室内置 16 个不在界面显示的物品缓存槽和 28 个大容量流体输出槽。维度网络输出会优先直接接收待分发产物，其他输出方式再使用内部缓存；当前批次会在受配置限制的多个游戏刻内完成，批次完成后若产物尚未全部送出，模拟暂停并专注输出，同时在界面中显示警告图标。

## 数据包配方

物品模拟配方位于普通 `recipe` 数据目录，类型为 `lazy:item_simulation`：

```json
{
  "type": "lazy:item_simulation",
  "input": { "item": "minecraft:wheat_seeds" },
  "duration": 1200,
  "priority": 0,
  "item_outputs": [
    { "stack": { "id": "minecraft:wheat", "count": 1 }, "chance": 1.0, "min_rolls": 1, "max_rolls": 3 }
  ],
  "fluid_outputs": []
}
```

实体 profile 使用 `lazy:entity_simulation`。`roll_loot_table` 默认开启；`loot_table` 可覆盖实体实例选择的默认战利品表。模拟会收集战利品表、自定义死亡掉落及装备掉落，但不会触发生物死亡/掉落事件，也不会生成经验。`display_item_outputs` 和 `display_fluid_outputs` 只用于 JEI 预览。

```json
{
  "type": "lazy:entity_simulation",
  "entity": "minecraft:cow",
  "duration": 1200,
  "priority": 0,
  "roll_loot_table": true,
  "fluid_outputs": [
    { "stack": { "id": "minecraft:milk", "amount": 1000 }, "chance": 0.3 }
  ],
  "display_item_outputs": [{ "id": "minecraft:leather", "count": 1 }],
  "display_fluid_outputs": [{ "id": "minecraft:milk", "amount": 1000 }]
}
```

概率必须在 `0..1`，显式周期必须大于零，数量范围必须满足 `0 <= min_rolls <= max_rolls`，物品配方至少有一种输出，单个配方最多声明 28 种产物。省略 `duration` 时使用服务端 `defaultDuration` 配置。可以使用 NeoForge 数据包条件包裹配方，以便只在目标模组存在时加载。

显式匹配优先于自动矿物；多个显式匹配先按 `priority` 降序，再按配方 ID 升序选取。

## 自动矿物

开启服务端配置 `automaticMinerals` 时：

- `c:ingots/<material>` 输入会寻找 `c:raw_materials/<material>`；候选依次偏好输入命名空间、`minecraft`，最后按注册名排序。
- `c:gems/<material>` 输入直接产出同一宝石。
- 同时匹配多个材料、缺少粗矿候选或位于 `lazy:automatic_mineral_blacklist` 标签内时不生成自动配方。
- 可在 `lazy:data_model_blacklist` 实体类型标签中禁止数据模型绑定及模拟。

## KubeJS

脚本 API 版本为 `Lazy.apiVersion === 1`。通过 KubeJS 标准 recipe schema 创建、覆盖和删除配方；`.id(...)` 与 `event.remove(...)` 保持 KubeJS 原有语义。

```js
ServerEvents.recipes(event => {
  event.recipes.lazy.item_simulation('examplemod:rice_seeds')
    .duration(600)
    .priority(10)
    .itemOutput(Lazy.simulation.item('examplemod:rice', 1.0, 2, 5))
    .id('kubejs:rice_simulation')

  event.recipes.lazy.entity_simulation('examplemod:yak')
    .fluidOutput(Lazy.simulation.fluid('minecraft:milk', 1000, 0.3, 1, 1))
    .rollLootTable(true)
    .id('kubejs:yak_simulation')

  event.remove({ type: 'lazy:item_simulation', id: 'lazy:simulation/wheat' })
})
```

`apiVersion` 只在不兼容的脚本 API 变更时递增；新增可选字段或函数不递增主版本。
