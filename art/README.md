# Art 目录约定

`art/` 下保存可编辑的 SVG 源文件，`src/main/resources/assets/lazy/textures/` 下保存导出的 PNG 结果。源文件是唯一权威来源，PNG 只作为游戏资源产物。

## 当前组织

- `art/block/machine/`：机器共用的底面、侧面、顶面材质。
- `art/block/overlay/`：各机器正面 overlay（透明背景叠加层），每个机器一个。
- `art/item/icon/`：物品图标的 SVG 源文件。
- `art/slot/empty/`：空槽位图标的 SVG 源文件。

## 命名规则

- 机器共用材质按视觉职责命名为 `bottom.svg`、`side.svg`、`top.svg`。
- overlay 以机器名命名（如 `buffer.svg`、`energy_source.svg`）。
- SVG 文件名即为 PNG 材质名，不再需要后缀映射。

## 导出方式

- 使用 `./gradlew renderArtTextures` 将 `art/` 下的 SVG 渲染到对应的 PNG 路径。
- 导出结果保留 `art/` 的子目录结构，例如 `art/block/machine/bottom.svg` 对应 `src/main/resources/assets/lazy/textures/block/machine/bottom.png`，`art/block/overlay/buffer.svg` 对应 `src/main/resources/assets/lazy/textures/block/overlay/buffer.png`。

## 渲染原理

机器方块使用双 element 模型实现 overlay 叠加：
1. 第一个 element 是完整的立方体，底/侧/顶/正面各使用对应材质。
2. 第二个 element 仅渲染正面，使用透明背景的 overlay 材质。
3. overlay 的透明像素透出底层材质，不透明像素显示 overlay 图案。
4. 方块朝向通过 blockstate 的 `facing` 属性 + 模型 Y 轴旋转实现，overlay 随之旋转。

## 物品图标风格

物品图标共用一套语言，新增图标请沿用：

- 光源固定在左上：外描边 `#12171f`，顶边最亮、左边次亮、右边压暗、底边最暗。
- 通用外壳金属：`#8a99a3` / `#5f6e78` / `#414d56` / `#2b353d` / `#212a31`。
- 显示区一律是内凹面板：青色系用于能量与配置（`#31707a` / `#173f48` / `#9deaf0` / `#eafcff`），紫色系用于数据与实体（`#5e3f95` / `#2b1848` / `#c69cf0` / `#e5cdff`）。
- 状态灯沿用机器 overlay 的三色签名：`#e05b4d`、`#f0bd4f`、`#59d09d`。
- 轮廓互不重复：配置卡为竖版卡片，数据模型为横版卡带，传送器为切角宝石，处理核心为圆角方形芯片。
- 处理核心四个等级共用同一芯片轮廓与处理标记，只靠边框金属（铜/金/钢/下界合金）、透镜色相与底部 1~4 颗等级灯区分。
