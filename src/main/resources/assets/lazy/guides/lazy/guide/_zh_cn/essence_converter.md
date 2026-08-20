---
navigation:
  parent: index.md
  title: 精华转换器
  icon: lazy:essence_converter
  position: 60
item_ids:
  - lazy:essence_converter
---

# 精华转换器

<BlockImage id="lazy:essence_converter" scale="1.15" />

安装 Mystical Agriculture 后会提供精华转换器。它会把精华合并为选定档位，并将完整的目标精华作为产物输出。

## 目标与转换

先选择目标档位，再放入精华。转换器含有完整产物或不足一个产物的余数时，不能更改目标。它会接受当前存在的六档 Mystical Agriculture 精华；安装 Mystical Agradditions 后才有 Insanium。

输入会按 Inferium 等价值转换，因此可以混合输入不同档位。不足一个目标产物的余数会被保留，不会丢失。默认容量为 1,000,000,000,000 个完整目标精华，可在服务端配置中修改。

输入槽接受精华，输出槽取出选定档位；清空操作会销毁全部已存精华。使用通用的 [IO 设置](io.md)将产物输出到相邻库存或兼容网络。

如果保存 Insanium 目标时移除了 Mystical Agradditions，已存数值会降级为 Supremium，不会直接丢失。
