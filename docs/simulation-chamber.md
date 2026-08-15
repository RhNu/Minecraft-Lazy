# 模拟室

模拟室使用一个不消耗的模拟目标和一组不消耗的模拟核心工作，不需要土壤或 FE。目标槽接受显式物品模拟配方、可自动识别的树苗、作物、植物、矿物、神秘农业种子，或已绑定的数据模型；核心槽接受同一等级的可堆叠核心。

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

最终配方按以下顺序组合：

1. 匹配的 `lazy:item_simulation` 按 `priority` 降序、配方 ID 升序选择一个；显式配方会完整替换自动基底。
2. 没有显式配方时组合所有适用且未被黑名单禁止的自动适配器；接管输入的特化适配器（目前为神秘农业）不会叠加通用作物结果。
3. 按配方 ID 升序追加所有匹配的 `lazy:item_simulation_injection`。注入不能单独创建可运行配方。
4. 组合结果超过 28 个输出条目时整份有效配方禁用并记录错误，不会静默截断。

注入配方沿用物品与流体输出格式，并且必须至少声明一种输出：

```json
{
  "type": "lazy:item_simulation_injection",
  "input": { "tag": "minecraft:saplings" },
  "item_outputs": [
    { "stack": { "id": "minecraft:stick" }, "chance": 0.25, "min_rolls": 1, "max_rolls": 2 }
  ]
}
```

## 自动树木与作物

- `minecraft:saplings` 中可在同命名空间配对 `<name>_log` 和 `<name>_leaves` 的树苗，每轮产出 1–4 原木、5% 树苗和 1–3 树叶；红树胎生苗按红树特例配对。
- 橡树和深色橡树额外有 5% 苹果，丛林木有 5% 可可豆，红树有 5% 红树根和 1% 泥泞红树根。
- 对应 `CropBlock` 的种子会使用最大年龄状态和空工具实时滚取当前数据包中的方块战利品表，因此原版及模组作物保留自己的产物、返种与附加掉落规则。
- 西瓜种子固定产出一个西瓜块，同时滚取西瓜方块表得到西瓜片，并有 5% 返种；南瓜滚取南瓜方块表并有 5% 返种。
- 无法按命名配对的特殊树、不是 `CropBlock` 的植物不会猜测产物，可使用显式配方或注入补充。

安装神秘农业时，所有启用且已注册的作物（包括扩展注册作物）由专用适配器接管。每轮固定产出一个种子和一个精华，并按作物的 secondary chance 分别追加种子和精华；`fertilized_essence` 遵循神秘农业自身配置概率。未安装该模组时不会加载其 API 类。

## 自动植物

`lazy:automatic_plant` 物品标签中的方块物品会按标签自动生成配方，语义是「把它种下去再收割」——取对应方块的状态实时滚取当前数据包中的方块战利品表，因此原版和模组植物都保留自己的产物与附加掉落规则。

- 默认标签内容为 `#minecraft:small_flowers`、`#minecraft:tall_flowers`、`minecraft:pink_petals` 和 `minecraft:spore_blossom`；模组花卉只要注册进原版花卉标签就自动生效。
- 整合包可以向 `lazy:automatic_plant` 追加任意「方块物品 + 有意义的方块战利品表」条目，例如苔藓、海泡菜或模组灌木，不需要改动代码。
- 双层植物（`half` 属性）取下半部分状态，否则高花的战利品表条件不成立、模拟会空转。
- 滚表时使用剪刀作为工具，因此剪刀限定掉落的模组植物也能正常出货。
- 方块是 `CropBlock` 的物品由自动作物适配器处理，本适配器会跳过，避免同一张战利品表被叠加两次。
- 注意标签里不要塞树叶类方块（例如 `#minecraft:flowers` 就包含 `minecraft:cherry_leaves` 和 `minecraft:flowering_azalea_leaves`），否则会得到与预期不符的产物。

## 自动矿物

开启服务端配置 `automaticMinerals` 时：

- `c:ingots/<material>` 输入会寻找 `c:raw_materials/<material>`；`c:gems/<material>` 输入从同一宝石标签选择规范产物。
- 两类候选统一按服务端 `automaticMineralModPriority` 配置选择。默认顺序是 `kubejs > minecraft > alltheores > create > mekanism > jaopca`；未列出的命名空间随后按命名空间和完整物品 ID 排序，缺失模组会自然跳过。
- 同时匹配多个材料或缺少候选时不生成自动配方。单个矿物的例外用显式模拟配方覆写。
- `lazy:automatic_simulation_blacklist` 禁止目标的全部自动来源；`lazy:automatic_tree_blacklist`、`lazy:automatic_crop_blacklist`、`lazy:automatic_plant_blacklist`、`lazy:automatic_mineral_blacklist` 和 `lazy:automatic_mystical_blacklist` 只禁止对应来源。黑名单不影响显式配方，注入仍需已有基底。
- 可在 `lazy:data_model_blacklist` 实体类型标签中禁止数据模型绑定及模拟。

服务端会在登录、`/reload` 和相关配置变化后同步自动配方快照，客户端目标槽校验与 JEI 运行期显示以该快照为准。JEI 中无法静态计算概率的方块战利品表候选会显示“战利品表产物”提示；实际生产时始终滚取当时已加载的数据包。

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

  event.recipes.lazy.item_simulation_injection('#minecraft:saplings')
    .itemOutput(Lazy.simulation.item('minecraft:stick', 0.25, 1, 2))
    .id('kubejs:sapling_sticks')

  event.remove({ type: 'lazy:item_simulation_injection', id: 'kubejs:sapling_sticks' })
})
```

`apiVersion` 只在不兼容的脚本 API 变更时递增；新增可选字段或函数不递增主版本。
