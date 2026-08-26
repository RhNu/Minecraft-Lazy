# 植物模拟兼容规格

## 解析边界

通用自动解析只接受 `CropBlock` 的最大年龄，或同时具有明确物品/方块映射、可骨粉生长、整数 `age` 与非空成熟战利品的作物。标准树苗按注册名配对，并把树干、剪取树叶与树叶战利品拆成独立部件。布尔、枚举、多方块和事件采收状态由对应 Integration 或数据配方明确描述。

第三方 Integration 只读取注册 ID、状态属性与标签，不链接 partner 类。解析时缺少方块、属性、属性值或工具方块标签为空，会禁用对应目标并按上下文去重记录错误。

每个 Integration 的候选使用 `lazy:plant_integration/<modid>` 分组；单个目标仍保留唯一 source/配方 ID，便于检查同组变体与定位冲突。

## 首批矩阵

表中“最大”表示整数属性的已注册最大值；每个部件均通过该状态的真实方块战利品表产生内部结果。

| 模组 | 输入目标 | 成熟状态 / 工具条件 | 复合部件与预期产物 |
| --- | --- | --- | --- |
| Farmer’s Delight | `cabbage_seeds` | `cabbages[age=最大]` | 卷心菜与种子 |
| Farmer’s Delight | `onion` | `onions[age=最大]` | 洋葱 |
| Farmer’s Delight | `tomato_seeds` | `tomatoes[age=最大]` | 番茄、种子及战利品表中的腐烂番茄概率 |
| Farmer’s Delight | `rice` | `rice_panicles[age=最大]` | 成熟稻穗产物 |
| Farmer’s Delight | 红/棕蘑菇菌落物品 | 对应 colony `[age=最大]` | 菌落成熟产物 |
| Fruits Delight | 13 种果树树苗 | 普通果叶 `type=fruits`；榴莲叶 `fruit=fruits` | 树干 1–4、剪取结构叶 1–3、成熟果叶 1–3；果实与树苗由战利品表决定 |
| Fruits Delight | `blueberry_bush` / `cranberry` | 对应 bush `[age=最大]` | 蓝莓 / 蔓越莓灌木产物 |
| Fruits Delight | `lemon_seeds` | `lemon_tree[age=最大,half=lower]` | 柠檬树成熟产物 |
| Fruits Delight | `pineapple_sapling` | `pineapple[age=最大]` | 菠萝 |
| Fruits Delight | `hamimelon_seeds` | `hamimelon` | 哈密瓜方块战利品 |
| Kaleidoscope Cookery | 辣椒、生菜、水稻、番茄种植物 | 对应 crop `[age=最大]` | 各成熟作物产物 |
| Kaleidoscope End | `ender_mint` | `ender_mint[age=最大]` | 末影薄荷 |
| Kaleidoscope End | `dream_berry` | `dream_berry_head[berries=true]` | 成熟梦境浆果 |
| Kaleidoscope Nether | `soul_pepper` / `poisonous_fruit` | 对应方块 `[age=最大]` | 灵魂椒 / 毒果 |
| Kaleidoscope Nether | `crimson_fruit` / `warped_fruit` | 对应 cave vines `[berries=true]` | 绯红 / 诡异果实 |
| Kaleidoscope Tavern | `grapevine` | 无工具 | `grape_crop[age=最大]`，普通葡萄与青葡萄概率 |
| Kaleidoscope Tavern | `grapevine` | 方块物品属于 `can_grow_ice_grape` | `ice_grape_crop[age=最大]`，优先于普通方案 |
| Kaleidoscope Tavern | `grapevine` | 方块物品属于 `can_grow_gold_grape` | `gold_grape_crop[age=最大]`，优先于普通方案；冰与金条件同时满足时冲突关闭 |
| Kaleidoscope Grilling | 油菜、鱼腥草、洋葱、甘薯种植物 | 对应 crop `[age=最大]` | 各成熟作物产物 |
| Kaleidoscope Grilling | `pepper_sapling` | 成熟叶 `has_pepper=true` | 树干 1–4、剪取结构叶 1–3、成熟花椒叶 1–3，含树苗与花椒战利品 |
| Avaritia’s Delight | 烈焰番茄、水晶卷心菜、中子素小麦种子；钻石晶格马铃薯 | 对应作物 `[age=最大]` | 各成熟作物的真实战利品 |
| Corn Delight | `corn_seeds` | `corn_crop[age=最大,upper=false]` | 成熟玉米与种子；只抽取承担收获战利品的下半部 |
| Crabber’s Delight | `palm_sapling` | 标准棕榈结构 | 树干 1–4、剪取树叶 1–3、普通树叶 1–3 |
| Delight o’ Flight | 云莓、雷霆果种子 | 对应 bush / vine `[age=最大]` | 云莓与雷霆果成熟战利品 |
| Delight o’ Flight | `cloudshroom_colony` | `age=最大,weather=0` | 无环境上下文时使用晴朗菌落产物；天气特化可由数据配方覆盖 |
| Delight o’ Flight | `lotus_seeds` | `lotus_flower[flower_age=最大,high=1]` | 成熟花战利品，并显式加入事件独有的莲藕与莲叶；不调用右键事件 |
| Eternal Starlight’s Delight | 四种 mushroom / marimold colony | 对应 colony `[age=最大]` | 成熟菌落战利品 |
| Mooncake Delight | `white_sesame` | `sesame_crop[age=最大]` | 黑、白芝麻 |
| Pineapple Delight | `pineapple_crop` | `pineapple_crop[age=最大]` | 菠萝与种植物 |
| Rustic Delight | 三类甜椒种子、咖啡豆、棉花种子 | 对应作物 `[age=最大]` | 九色甜椒分组、咖啡与棉花成熟战利品 |
| Twilight Flavors & Delight | `mushgloom_colony` | colony `[age=最大]` | 暮色森林蘑菇菌落产物 |
| Twilight Flavors & Delight | `ironwood_sapling` | 标准铁木结构 | 树干 1–4、剪取树叶 1–3、普通树叶 1–3，保留战利品表中的金属概率 |
| Ube’s Delight | 蒜、姜、紫薯 | 对应 crop `[age=最大]` | 各成熟作物产物 |
| Ube’s Delight | `lemongrass_seeds` | 成熟支撑茎与成熟叶 | 香茅种子、茎部和叶部战利品分别抽取 |
| Veggies Delight | 甜椒、西兰花、花椰菜、蒜、甘薯、芜菁、西葫芦种植物 | 对应 crop `[age=最大]` | 七类成熟蔬菜战利品 |
| Vintage Delight | 黄瓜、齿轮莓、幽灵椒、燕麦、花生种植物 | 对应 crop / bush `[age=最大]` | 成熟战利品，包含花生战利品表中的魔法花生概率 |
| Vintage Delight | `magic_peanut` | 魔法藤复合结构 | 藤干 1–4、魔法花生部件 1–3 |

果树清单为苹果、杨梅、榴莲、无花果、山楂、猕猴桃、荔枝、芒果、山竹、橙、桃、梨与柿子。

当前整合包中的 Barbeque’s Delight、Brewin’ And Chewin’、Cuisine Delight、Cutting Delight、End’s Delight、KubeJSDelight、Ocean’s Delight、Seed Delight 与 Syal’s Delight 已审计，当前版本没有由自身提供的活体植物模拟目标。Kaleidoscope Compat 与 Kaleidoscope World Liquor 也没有活体植物目标。Applied Cooking、Cooking for Blockheads、Lab 11 Mods Unified 与 Supplementaries 仅通过可选兼容声明关联 Farmer’s Delight，不归入其植物 Integration；版本更新时仍需复查注册表与战利品表。

## 验证重点

| 范围 | 必须覆盖 |
| --- | --- |
| 上下文 | 三槽上限、无序一对一、重叠 Ingredient、额外行为工具、方块标签 |
| 选择 | 显式/自动回退、实体默认 profile、全部 injection、同级冲突 |
| 冻结 | 重载和中途换槽不改变活动作业 |
| 方块战利品 | JSON、网络、NBT 默认值，外层概率/次数，成熟状态与工具 |
| 代表目标 | 水稻、榴莲、柠檬、梦境浆果、两种下界藤、三种葡萄、花椒树、玉米、莲花、香茅、魔法花生 |
