# Integration 生命周期规格

## 声明模型

每个 `integrations/*` 模块通过 `lazyIntegration` 描述自身。descriptor 是构建、代码生成、开发运行和 DataGen 共同使用的关系源。

| 属性 | 用途 |
| --- | --- |
| ID | 生成物命名、选择与诊断 |
| owner | 决定入口由 Lazy 还是第三方框架安装 |
| side | 控制 catalog、运行任务和类加载边界 |
| required mods | 决定 Integration 是否可安装 |
| optional mods | 描述可用但不构成安装前提的能力 |
| Integration 依赖 | 形成拓扑安装与运行选择闭包 |
| DataGen 状态 | 约束 contribution 与生成运行环境 |

版本范围引用由构建配置解析；本文不复制模组列表和版本。

## Lazy 拥有的入口

| 阶段 | common | client |
| --- | --- | --- |
| 实现契约 | `CommonIntegration` | `ClientIntegration` |
| 可用 context | 模组容器、公共事件总线 | 客户端生命周期所需 context |
| 生成结果 | common bridge/catalog | client bridge/catalog |
| 安装条件 | side 与必需模组满足 | 客户端 side 且必需模组满足 |

KSP 在模块内验证入口与 descriptor 一致，并生成能访问 `internal` 实现的 bridge。聚合处理器验证整个依赖图，再生成静态 catalog。运行时只消费生成结果，不重新建立关系清单。

## 第三方拥有的入口

部分框架规定自己的发现与回调接口。此类 Integration 仍由 descriptor 参与构建治理，但入口由 owner 对应的生成规则交给第三方加载。

| owner | 生命周期责任 | Lazy 保留的责任 |
| --- | --- | --- |
| Jade | HUD plugin 的公共与客户端注册 | 模块隔离、side 安全、数据快照与 DataGen 声明 |
| JEI | recipe/category plugin 回调 | 模块隔离、客户端约束与生成发现文件 |
| KubeJS | plugin 与脚本绑定回调 | 模块隔离、依赖条件与生成发现文件 |

owner 是入口所有权，不改变第三方类型只能留在对应 Integration 模块的边界。

## Jade 数据边界

Jade 的服务端 provider 读取机器事实状态并生成紧凑快照，客户端 component 只负责展示。

| 快照应包含 | 快照不应触发 |
| --- | --- |
| 当前指向方块所需的聚合状态 | 完整库存或网络内容传输 |
| 小枚举、布尔值与必要模板 | 配方全集扫描 |
| 能区分机器业务状态的信息 | 邻接网络或 capability 路由 |

Jade 已能表达的通用 capability 信息不由 Lazy 重复实现。自定义 provider 只补充机器业务语义，并保持每次查询为有界只读操作。

## DataGen contribution

| 规则 | 校验位置 |
| --- | --- |
| descriptor 声明参与时必须存在对应 contribution | 聚合 KSP |
| contribution 存在时 descriptor 必须声明参与 | 聚合 KSP |
| Provider 只在 DataGen 模块执行 | DataGen catalog 与模块边界 |
| partner holder 只在 Provider 阶段解析 | contribution 实现 |

DataGen 运行环境由参与闭包派生，不在 `datagen` 中维护另一份 Integration 清单。

## 失败与诊断

| 阶段 | 要求 |
| --- | --- |
| Gradle 配置 | 未知 Integration、side 不兼容和无法解析的选择立即失败 |
| KSP | 重复 ID、入口不匹配、循环和依赖闭包错误携带模块上下文 |
| 运行时安装 | 异常携带 Integration ID 与生命周期阶段并终止当前启动 |
| 第三方回调 | 由 owner 的框架触发，错误仍应能定位到对应 Integration |

## 测试边界

测试 processor 和 descriptor 图的通用规则，不把当前 Integration ID 全集、生成 JSON 原文或生成类字节内容复制进断言。类加载测试可以从生成 descriptor/catalog 派生输入，验证 common 路径不解析客户端或 partner API。
