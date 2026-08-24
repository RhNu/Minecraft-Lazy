# Lazy

Lazy 是面向 Minecraft 1.21.1 NeoForge 的 Kotlin 懒狗工具箱。仓库采用 Gradle 多项目构建：运行时、第三方集成、DataGen 与编译期代码生成彼此独立编译，最终仍只分发一个平铺的 `lazy` JAR。

## 开发环境

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.242
- ModDevGradle 2.0.142
- Kotlin 2.4.0
- KSP2 2.3.11
- KotlinForForge 5.12.0

## 项目布局

- `runtime`：core、feature 与运行时注册，不包含 `@Mod`、DataGen 或第三方 API。
- `integrations/*`：每个第三方集成独立编译，通过 `lazyIntegration` DSL 声明依赖关系。
- `integration-api`：生命周期 SPI、上下文与内部 API 标记。
- `codegen/*`：Integration 注解和 KSP 校验/静态 catalog 生成器。
- `mod`：`@Mod` 入口、静态/生成资源、元数据与唯一分发 JAR。
- `datagen`：只用于开发的数据 Provider 和独立 `runData` profile，不进入发布物。
- `build-logic`：NeoForge、Kotlin、测试、Integration DSL 与打包约定插件；版本坐标统一位于 version catalog。

各模块的 Kotlin source root 已去掉重复的 `rhx/lazy/...` 物理目录前缀；源码仍使用 `rhx.lazy.*` package，JVM 包名不受影响。

## 常用命令

```shell
./gradlew build
./gradlew runData
./gradlew runClient
./gradlew runServer
./gradlew runClientIntegrations
./gradlew runServerIntegrations
```

`runClient`/`runServer` 只带必需依赖。完整 profile 加载对应 side 的集成；也可用 `-Plazy.integrations=ae2,jade` 选择子集，集成依赖会自动闭包。Windows PowerShell 下可将 `./gradlew` 换为 `.\gradlew.bat`，并将属性参数写成 `"-Plazy.integrations=ae2,jade"`。

`./gradlew build` 的分发物位于 `mod/build/libs/lazy-<version>.jar`。子项目 JAR 只供构建内部消费，不单独发布。

## 文档

- [架构约定](docs/architecture.md)
- [内容设计草案](docs/content-design.md)
- [开发与依赖](docs/development.md)
