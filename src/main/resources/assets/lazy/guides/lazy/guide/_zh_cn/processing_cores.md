---
navigation:
  parent: index.md
  title: 处理核心
  icon: lazy:processing_core_t1
  position: 110
item_ids:
  - lazy:processing_core_t1
  - lazy:processing_core_t2
  - lazy:processing_core_t3
  - lazy:processing_core_t4
---

# 处理核心

<ItemImage id="lazy:processing_core_t1" scale="1.4" />

处理核心是机器共用的组件。在模拟室中，处理核心控制速度和产出倍率。模拟室最多接受 64 个核心，运行过程中不会消耗核心。

| 核心 | 速度 | 产出 |
| --- | ---: | ---: |
| <ItemLink id="lazy:processing_core_t1" /> | ×1 | ×1 |
| <ItemLink id="lazy:processing_core_t2" /> | ×2 | ×4 |
| <ItemLink id="lazy:processing_core_t3" /> | ×6 | ×12 |
| <ItemLink id="lazy:processing_core_t4" /> | ×18 | ×36 |

这些数值由服务端配置控制。每个核心贡献自身的速度和产出倍率，因此增加核心会同时提高模拟速度和虚拟产出抽取次数。
