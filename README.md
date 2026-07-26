# Lazy

Lazy 是一个面向 Minecraft 1.21.1 NeoForge 的 Kotlin 模组项目。目前项目处于设计阶段，源码只包含可扩展的注册、数据生成与通用工具骨架，不包含可游玩的具体内容。

## 开发环境

- Java 21
- Minecraft 1.21.1
- NeoForge 21.1.242
- ModDevGradle 2.0.142
- Kotlin 2.4.0
- KotlinForForge 5.12.0
- 可选兼容：Curios、Silent Gear、Beyond Dimensions

## 常用命令

```shell
./gradlew build
./gradlew runData
./gradlew runClient
./gradlew runClientIntegrations
```

Windows PowerShell 下可将 `./gradlew` 换为 `.\gradlew.bat`。

## 文档

- [架构约定](docs/architecture.md)
- [内容设计草案](docs/content-design.md)
- [开发与依赖](docs/development.md)
