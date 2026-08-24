# 架构

## 目标

Lazy 将运行时、可选集成、发布入口和数据生成拆成独立编译边界。架构的核心目标是让可选依赖保持可选、让生成关系在编译期可验证，并让机器共享一致的资源与输出语义。

本文只描述跨模块约束和系统级数据流。功能清单见 [内容设计](content-design.md)，具体难点见 [规格目录](spec/)。

## 模块职责

| 模块 | 拥有的内容 | 可依赖方向 |
| --- | --- | --- |
| `integration-api` | Integration context、生命周期接口、内部 API 标记 | 最小公共依赖 |
| `runtime` | 核心设施、注册、内置内容、公共 SPI | `integration-api` |
| `integrations/*` | 单一第三方模组适配与其专属 DataGen contribution | `runtime`、`integration-api`、声明的 Integration 依赖、partner API |
| `mod` | 模组入口、资源、聚合 JAR、运行配置与发布 | 聚合运行时及选定 Integration 产物 |
| `datagen` | DataGen 入口、Provider 和生成资源编排 | 窄 DataGen export 与声明参与的 Integration |
| `codegen/*` | Integration 注解、descriptor 校验与 catalog 生成 | 独立编译期契约 |
| `build-logic` | 约定插件、Integration DSL、打包与开发运行图 | Gradle 构建模型 |

根项目只提供聚合任务。最终发布物由 `mod` 聚合内部模块产物；内部模块不是独立发布 API。

## API 边界

| 范围 | Kotlin 可见性 | 标记 |
| --- | --- | --- |
| 模块内部实现 | `internal` | 无 |
| Lazy 模块之间共享 | 显式 `public` | `@LazyInternalApi` |
| 第三方兼容类型 | 留在对应 `integrations/*` | 按跨模块需要最小暴露 |

`@LazyInternalApi` 表示 Lazy 自身的跨模块契约，不代表对外稳定 API。文件级批量标记不会表达真实边界，因此标记必须落在实际跨模块声明上。

## Integration 装配

Integration 的声明、生成和安装形成一条编译期可追踪链路：

| 阶段 | 输入 | 结果 |
| --- | --- | --- |
| 模块声明 | `lazyIntegration` DSL | ID、owner、side、模组条件、依赖关系、DataGen 参与状态 |
| 模块编译 | 入口实现与 KSP 注解 | 可访问模块内部实现的 bridge |
| 聚合编译 | 所有 descriptor | 已校验依赖图与 common/client catalog |
| 运行时 | catalog 与已安装模组 | 按依赖顺序安装满足条件的入口 |
| DataGen | contribution catalog | 只加载参与当前生成任务的贡献 |

依赖图必须在配置或编译阶段完成闭包、side 与循环校验。第三方拥有入口生命周期的集成仍保持模块隔离，其发现方式见 [Integration 生命周期规格](spec/integration-lifecycles.md)。

## DataGen 所有权

| 内容 | 所有者 |
| --- | --- |
| Provider、语言贡献、模型 helper | `datagen` 或 Integration 的 DataGen contribution |
| 静态资源 | `mod/src/main/resources` |
| 生成结果 | `mod/src/generated/resources` |
| 可供 DataGen 使用的运行时标识 | 窄 `DataGenExports` facade |

Provider 执行时才解析注册 holder。普通构建不隐式执行 DataGen；生成资源是否漂移由显式 `runData` 后的工作树差异判断。

## 机器运行时分层

| 层 | 责任 | 不承担的责任 |
| --- | --- | --- |
| `core.resource` | 资源身份、长数量、固定种类仓、事务、能力视图 | 工作调度与目标发现 |
| `core.process` | 工作状态、待提交结果、提交顺序 | 对外传输 |
| `core.io` | 被动访问、面输出、网络输出、预算与失败恢复 | 生成机器产物 |
| `feature/*` | 输入解析、机器特有算法、UI 与显示状态 | 重复实现公共资源或传输循环 |

处理机器遵循同一数据方向：

```text
输入或设置 → 工作快照 → 资源事务 → 输出仓 → 被动抽取 / 面输出 / 网络输出
```

工作预算与运输预算相互独立。资源只在事务成功后改变，所有输出路径从同一输出源扣账。涉及随机结果、背压和整数换算的细节见 [机器处理规格](spec/machine-processing.md)。

## IO 边界

机器通过 `IoController` 选择被动、面输出或网络输出。`OutputDispatcher` 负责固定预算内的公平调度；网络适配器通过统一结果类型报告已接受数量、可重试状态、目标状态和结果是否可确认。

机器不持有第三方网络对象。可持久化目标只保存由 provider 定义的稳定引用，并在输出时重新解析。完整状态语义见 [IO 与网络输出规格](spec/io-and-network-output.md)。

## 持久化与同步

| 数据 | 原则 |
| --- | --- |
| 实际输入、输出与账本 | 由方块实体作为服务端事实源持久化 |
| 活动作业与待提交结果 | 与机器内容一起保存，恢复后不重新随机生成 |
| IO 设置 | 独立于真实内容，由配置卡或界面重新应用 |
| 世界正面显示 | 只同步渲染所需的紧凑状态 |
| 菜单数据 | 初始快照加必要增量，服务端重新校验写操作 |

持久化结构属于代码中的 codec/NBT 实现，本文不维护字段名清单。
