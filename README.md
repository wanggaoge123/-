# 听书（清爽版）

一个**零广告**的安卓听书 App，内置 **34 个可用书源**。

## 这是什么

- **界面 + 播放器**：源自开源项目 `eprendre/tingshu`（MIT 协议，允许自由复制、修改、分发）
- **书源**：34 个仍在维护的书源 —— 酷我畅听、懒人听书、听书宝、哔哩哔哩、芒果听书、幻听网、爱听书、麻辣听书、云图有声……
- **零广告**：原版 App 内置的穿山甲(GroMore)、快手广告、腾讯优量汇、AnyThink 等 **9 家广告联盟在本项目里完全不存在**，没有开屏广告、没有后台偷跑流量

## 怎么编译出 APK

### 方法一：GitHub 自动编译（推荐，不用装任何环境）

1. 把整个项目上传到你的 GitHub 仓库
2. 打开仓库页面 → **Actions** 标签 → 左侧选 **Build APK** → 右侧点 **Run workflow**
3. 等 3~8 分钟，编译完成后进入该次运行记录，页面底部 **Artifacts** 里下载 `tingshu-release-apk`
4. 解压得到 `app-release.apk`，传到手机安装

> 每次你修改代码并 push，它也会自动重新编译。

### 方法二：本地编译

需要 JDK 11 + Android SDK（或直接用 Android Studio 打开本目录，它会自动补齐 Gradle Wrapper）：

```
gradle assembleRelease
```

> 说明：仓库里**不包含** `gradle-wrapper.jar`（二进制文件不便通过部分推送通道上传），
> 但**包含**签名密钥 `app/keystore.jks`（用于保证签名一致，见下节）。
> 已装 Gradle 7.6.4 可直接用上面的命令；用 Android Studio 打开则会自动生成 wrapper，
> 之后就能用 `./gradlew assembleRelease` 了。

## 输出文件位置

```
app/build/outputs/apk/release/app-release.apk
```

## 签名说明

- 仓库**已包含**签名密钥 `app/keystore.jks`（别名 `tingshu`，密码 `123456`），
  CI 编译前会检查并直接使用它，因此**每次编译出的 APK 签名完全一致**，可以互相覆盖安装。
- 这把密钥的证书指纹与官方「我的听书」正式版**完全相同**
  （SHA1 `25:05:8D:B6:E2:C2:7B:08:26:A8:F8:4C:5B:3F:48:70:1F:8E:8B:57`），包名也一致
  —— 所以本项目编译出的 APK **可以直接覆盖安装官方版**，不必先卸载，收藏与播放历史都会保留。
- 想换成自己的签名：替换 `app/keystore.jks` 并同步修改 `app/build.gradle` 里
  `signingConfigs` 的密码。换签名后与原版不再一致，才需要先卸载原版再安装。

## 把它改成你自己的 App

| 想改什么 | 改哪里 |
|---|---|
| 应用名字 | `app/src/main/res/values/strings.xml` 里的 `app_name` |
| 图标 | 替换 `app/src/main/res/mipmap-anydpi/ic_launcher.xml`、`ic_launcher_round.xml`（矢量），以及 `mipmap-anydpi-v26/` 下的自适应图标 |
| 包名 | `app/build.gradle` 的 `applicationId`、`app/src/main/AndroidManifest.xml` 的 `package`，以及源码目录 `com/github/eprendre/tingshu/` 重命名 |
| 签名 | 放入 `app/keystore.jks` 并改 `app/build.gradle` 里 `signingConfigs` 的密码 |
| 版本号 | `app/build.gradle` 的 `versionCode` / `versionName` |

## 增删书源

书源全部在 `app/src/main/java/com/github/eprendre/sources_by_eprendre/`：

- **新增**：把书源文件拷进来，然后在 `SourceEntry.kt` 的 `getSources()` 里加一行
- **删除**：从 `SourceEntry.kt` 的 `getSources()` 里去掉对应那行即可
- **设置页里的书源下拉列表**：`app/src/main/res/values/arrays.xml`（新增书源后同步加一条）

## 技术栈

- Kotlin 1.8.22 / AGP 7.4.2 / Gradle 7.6.4，compileSdk 33
- ExoPlayer 2.9.6（后台播放 + 通知栏控制）
- Room（收藏）、Glide（封面）、RxJava2、Jsoup、Fuel、OkHttp

## 许可

界面与播放器内核源自 `eprendre/tingshu`，MIT 协议。本项目在其基础上：
1. 重写了数据层以接入新一代书源框架
2. 移除了全部广告相关代码与依赖
3. 升级到现代构建工具链
