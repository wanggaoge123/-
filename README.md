# 听书（清爽版）

一个**零广告**的安卓听书 App，内置 **34 个可用书源**。

> 界面与播放器源自开源项目 `eprendre/tingshu`（MIT 协议）；本项目剔除了原版全部广告 SDK 与广告代码。

## 📥 直接下载安装（推荐）

**下载地址（点进去，在页面下方的 Assets 里点 `TingShu-ZeroAd-v*.apk`）：**

👉 **https://github.com/wanggaoge123/-/releases/latest**

安装步骤：

1. 手机上点开上面的链接 → 页面底部 **Assets** 区域 → 点 `TingShu-ZeroAd-v2.6.3.apk` 开始下载
2. **等下载彻底完成**（文件约 7.6 MB，可对照同页 `SHA256SUMS.txt` 校验哈希）
3. 点开 APK 安装；若提示「未知来源 / 外部来源应用」，在系统设置里允许浏览器或文件管理器安装应用
4. 已装官方版「我的听书」的**可以直接覆盖安装**，无需先卸载（签名与官方完全相同）

> ⚠️ 装不上时先看两条：
> - **文件没下完整**是安卓报「解析软件包时出现问题」最常见的原因 —— 重新下载，或用下载工具下载后比对 SHA-256
> - 本版 versionCode 是 **2603**，高于官方 v2.6.0（2600），所以能覆盖安装；若你手机上装的是**更新的官方版本**（比如 2.6.4+），得先卸载官方版再装本版

## 这是什么

- **界面 + 播放器**：源自开源项目 `eprendre/tingshu`（MIT 协议，允许自由复制、修改、分发）
- **书源**：34 个仍在维护的书源 —— 酷我畅听、懒人听书、听书宝、哔哩哔哩、芒果听书、幻听网、爱听书、麻辣听书、云图有声……
- **零广告**：原版 App 内置的穿山甲(GroMore)、快手广告、腾讯优量汇、AnyThink 等 **9 家广告联盟在本项目里完全不存在**，没有开屏广告、没有后台偷跑流量

## 版本与升级

| 项目 | 本版 | 官方版 |
|---|---|---|
| versionCode | **2603** | 2600 |
| versionName | **2.6.3** | 2.6.0 |
| 包名 | `com.github.eprendre.tingshu` | 同左 |
| 签名证书 SHA1 | `25:05:8D:B6:E2:C2:7B:08:26:A8:F8:4C:5B:3F:48:70:1F:8E:8B:57` | 同左 |

### 版本记录

- **2.6.3**：再修启动闪退。`MediaSessionCompat(this, tag)` 两参构造内部会用 flags=0 建 PendingIntent.getBroadcast（androidx.media 1.2.1 的坑），改成显式传入接收者组件 + FLAG_IMMUTABLE 的 PendingIntent。
- **2.6.2**：修**启动闪退**。`TingShuService.onCreate` / `NotificationBuilder` 创建 `PendingIntent` 时没带 `FLAG_IMMUTABLE`，
  targetSdk 33 的 App 在 Android 12+ 上会直接抛 `IllegalArgumentException`（这个 Service 由 MainActivity 一启动就拉起，
  所以症状就是点开就闪退）。顺带把 Room 库文件名从 `book-db` 改为 `book-db-zc`，避免与官方版库结构冲突导致崩溃。
- **2.6.1**：修**装不上**。`versionCode` 从 100 提到 2601（官方是 2600，低于它算降级安装，安卓直接拒）。

**改版本号的规矩**：`versionCode` 必须**严格大于**手机上已装版本的 versionCode，否则安卓会拒绝（降级安装）。
每次发新版只需在 `app/build.gradle` 里把 `versionCode` 加 1、`versionName` 改一改，
然后 push —— 云端会自动编译并**自动发布到 Releases**（同一版本只发一次，重复推送会覆盖同名资源）。

## 怎么编译出 APK

### 方法一：GitHub 自动编译（推荐，不用装任何环境）

1. 把整个项目上传到你的 GitHub 仓库
2. 打开仓库页面 → **Actions** 标签 → 左侧选 **Build APK** → 右侧点 **Run workflow**
3. 等 3~8 分钟。编译完会自动做三件事：
   - 校验 APK（ZIP 完整性 / 4 字节对齐 / 签名链）
   - 把 `TingShu-ZeroAd-v<版本>.apk` 传成 Artifacts
   - 在 **Releases** 页发布一个带 APK 附件的版本（别人点开就能直接下载）

> 每次你修改代码并 push，它也会自动重新编译；只有 `versionName` 对应的 Release 不存在时才会新发一个。

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
  —— 所以本项目编译出的 APK **可以直接覆盖安装官方版**，不必先卸载。
- ⚠️ **覆盖安装后收藏 / 播放历史会重置一次**：本项目的数据层是重写的，Room 数据库结构与官方 2.6.0 不同，
  首次打开会按新结构重建数据库（代码里开了 `fallbackToDestructiveMigration`，不会闪退，但旧记录会清掉）。
- 想换成自己的签名：替换 `app/keystore.jks` 并同步修改 `app/build.gradle` 里
  `signingConfigs` 的密码。换签名后与原版不再一致，才需要先卸载原版再安装。

## 装不上时怎么排查

| 现象 | 原因 | 处理 |
|---|---|---|
| 解析软件包时出现问题 / packageInfo is null | 文件下载不完整（最常见），或下载到的其实是 Actions 的 **zip 压缩包** | 重新下载；确认文件名是 `.apk`、大小约 7.6 MB；比对 SHA-256 |
| 无法安装，版本号低于已安装版本 | 手机上已装更高 versionCode 的版本 | 把 `versionCode` 调高后重新编译，或先卸载原版 |
| 应用未安装 / 签名不一致 | 手机上装的同名 App 是别的签名 | 先卸载旧版再安装 |
| 安装成功但打开闪退 | 数据库结构冲突 | 卸载后重装（会清数据） |

## 把它改成你自己的 App

| 想改什么 | 改哪里 |
|---|---|
| 应用名字 | `app/src/main/res/values/strings.xml` 里的 `app_name` |
| 图标 | 替换 `app/src/main/res/mipmap-anydpi/ic_launcher.xml`、`ic_launcher_round.xml`（矢量），以及 `mipmap-anydpi-v26/` 下的自适应图标 |
| 包名 | `app/build.gradle` 的 `applicationId`、`app/src/main/AndroidManifest.xml` 的 `package`，以及源码目录 `com/github/eprendre/tingshu/` 重命名 |
| 签名 | 放入 `app/keystore.jks` 并改 `app/build.gradle` 里 `signingConfigs` 的密码 |
| 版本号 | `app/build.gradle` 的 `versionCode` / `versionName`（发新版必须调大 `versionCode`） |

## 增删书源

书源全部在 `app/src/main/java/com/github/eprendre/sources_by_eprendre/`：

- **新增**：把书源文件拷进来，然后在 `SourceEntry.kt` 的 `getSources()` 里加一行
- **删除**：从 `SourceEntry.kt` 的 `getSources()` 里去掉对应那行即可
- **设置页里的书源下拉列表**：`app/src/main/res/values/arrays.xml`（新增书源后同步加一条）

## 技术栈

- Kotlin 1.7.21 / AGP 7.4.2 / Gradle 7.6.4，compileSdk 33、minSdk 21
- ExoPlayer 2.9.6（后台播放 + 通知栏控制）
- Room（收藏）、Glide（封面）、RxJava2、Jsoup、Fuel、OkHttp

## 许可

界面与播放器内核源自 `eprendre/tingshu`，MIT 协议。本项目在其基础上：
1. 重写了数据层以接入新一代书源框架
2. 移除了全部广告相关代码与依赖
3. 升级到现代构建工具链
