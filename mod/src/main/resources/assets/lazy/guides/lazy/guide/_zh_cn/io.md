---
navigation:
  parent: automation.md
  title: IO 与自动化
  icon: lazy:configuration_card
  position: 10
---

# IO 与自动化

<Column alignItems="center">
  <ItemImage id="lazy:configuration_card" scale="1.4" />
</Column>

IO 面板决定机器如何接收和送出物品、流体与 FE。

## 输出模式

- **被动：** 机器不会主动送出内容，管道和相邻机器仍可取用支持的内容。
- **面配置：** 每一面都能设为禁用、输入、输出或双向。开启自动输出后，机器会从标为输出的面送出产物。
- **网络：** 将支持的产物送入选定网络。

侧面设置以机器正面为准。用扳手旋转机器后，设置会跟随新的朝向。

## 选择网络

### 应用能源 2

1. 将<ItemLink id="lazy:configuration_card" />放入 AE2 无线接入点的链接槽。
2. 把已经链接的卡片放在手中、背包或 Curios 槽位中。
3. 打开机器的 IO 面板，选择 **AE2 ME 网络**。

手中的卡片优先。如果背包里有连接到不同网络的卡片，请先拿住所需卡片，再选择网络。

AE2 可以接收物品和流体。安装 Applied Flux 后，也能接收 FE。

### Beyond Dimensions

1. 在 Beyond Dimensions 中将目标网络设为主网络。
2. 打开机器的 IO 面板，选择 **Beyond Dimensions**。

## 传输中断

- 目标网络消失后，机器会断开连接并回到被动模式。
- 如果无法确认一次传输是否完成，输出会暂停，避免重复送出内容。
- 检查目标网络后再选择**继续**；也可以选择**断开连接**并回到被动模式。

## 配置卡

<ItemLink id="lazy:configuration_card" />可以复制、编辑和应用整套 IO 设置。具体操作和放置规则见[配置卡](configuration_card.md)。

## 拆除机器

- 已存储的产物会保留在掉落的机器物品中。
- 玩家放入的物品会单独掉落，例如修复器中的待修物品。
- 再次放置机器时，IO 模式、侧面设置、自动输出和网络选择都会重置。

用扳手拆除机器时，规则相同。
