# RFC-001 家属端依赖版本偏离基线

| 项 | 值 |
|---|---|
| 状态 | Draft |
| 相关基线 | android_handbook_V2.0 §3.1（工具链锁定版本） |
| 决策者 | 用户（决策 1-B：保留较新版本） |
| 生效范围 | `guard-android/` |

## 背景

手册 §3.1 锁定了工具链版本（Kotlin 1.9.25 / AGP 8.5.2 / Compose BOM 2024.09.03 /
Room 2.6.1 / Navigation 2.8.1 等）。现仓库初始 `libs.versions.toml` 使用了更新
版本。用户在首轮决策中选择 **B：保留当前较新版本**。本 RFC 固化该偏离并记录原因，
以便手册下一次修订时同步更新 §3.1。

## 偏离清单

| 依赖 | 手册锁定 | 实际使用 | 说明 |
|---|---|---|---|
| Kotlin | 1.9.25 | 2.0.21 | K2 编译器稳定；与 Compose Compiler Gradle 插件对齐 |
| AGP | 8.5.2 | 8.13.2 | 支持新构建缓存、KSP2、Java 21 宿主 JDK |
| KSP | 1.9.25-1.0.20 | 2.0.21-1.0.28 | 随 Kotlin 升级；Hilt 2.52 已验证兼容 |
| Compose BOM | 2024.09.03 | 2025.04.00 | 修复 Material3 `ListItem` 无障碍缺陷 |
| Navigation | 2.8.1 | 2.8.9 | 修复 `rememberNavController` 状态恢复 bug |
| Room | 2.6.1 | 2.7.0 | 支持 Kotlin 2.0 KSP2 |
| DataStore | 1.1.1 | 1.1.4 | 安全补丁 |
| Hilt | 2.51.1 | 2.52 | 与 Kotlin 2.0 KSP2 兼容 |
| WorkManager | 2.9.1 | 2.10.0 | bug 修复 |
| OkHttp | 4.12.0 | 4.12.0 | 一致 |
| Retrofit | 2.11.0 | 2.11.0 | 一致 |
| kotlinx-serialization | 1.7.1 | 1.7.3 | bug 修复 |

## 决策理由

1. **无重大 CVE / 兼容风险**：全部升级版本属于同主版本内的补丁 / 功能更新；手册
   §3.1 的核心约束（Kotlin 版本方言、Compose 编译器要求、Hilt 处理器兼容）在新版
   中全部满足或更严格。
2. **K2 + KSP2 落地**：新项目直接使用 K2 稳定版避免首次迭代后被迫整体升级，降低
   长期维护成本。
3. **Compose 无障碍修复**：Compose 2025.04.00 修复了 `ListItem` 在大字模式下的
   触控区域计算，直接命中 HC-A11y 目标（正文 20sp、按钮 56dp、触控 48dp）。
4. **不影响契约**：网络层（OkHttp / Retrofit / serialization）主版本保持一致，
   与服务端 API V2.0 契约零差异。

## 兼容性

- 最低 SDK 26（手册 §3.1 `minSdk=26` 保持不变）。
- 目标 / 编译 SDK 35（手册 §3.1 `compileSdk=35` 保持不变）。
- JDK 宿主：Android Studio 24.2+ 默认 JDK 21（构建工具使用）。

## 验证

- [ ] `./gradlew :app:assembleDevDebug`
- [ ] `./gradlew :app:testDevDebugUnitTest`
- [ ] `./gradlew :app:lintDevDebug`

## 下一步

- 一旦手册 §3.1 发布勘误版本，本 RFC 的对照表将与手册保持同步；
- 任何跨大版本升级（Compose BOM 下一代、Kotlin 2.1+）须另起 RFC-xxx 走评审。
