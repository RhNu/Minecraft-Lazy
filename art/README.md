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
