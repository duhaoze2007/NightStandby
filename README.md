# 守夜 Standby

为 **Xiaomi 17 Pro（25098PN5AC）· Xiaomi HyperOS 4（Android 17）** 调校的竖屏 / 横屏充电床头屏。

它把 iPhone 横屏充电 Standby 的体验带到 Android，并且**竖着充电也能用**：一块大时钟、离线日历、系统媒体控制、可上下翻动的小组件堆叠，以及会自动转红、自动全黑的 OLED 友好界面。

## 已实现

- **大时钟**：24 / 12 小时制，可显示秒；按分钟刷新以省电。
- **天气**：Open-Meteo 实时天气 + 缓存；可选系统最后已知位置，也可手动指定城市。天气与定位是唯一允许联网的模块。
- **日历**：通过 `CalendarContract` 只读系统日历，显示今天与未来 7 天日程；支持权限引导。
- **音乐控制**：通过 Android `MediaSession` 显示正在播放的曲名、艺术家、专辑、封面、进度，并支持播放/暂停、上一首、下一首、点击进度条跳转。
- **小组件堆放**：日历 / 音乐上下滑动切换，保留当前页；可在设置中分别开关。
- **自动红光**：光线传感器低于阈值（默认 8 lux）时切换为纯黑 + 红色单色界面；也可手动强制红光。
- **OLED 模拟熄屏**：无操作 60 秒后画面变为**纯黑**（实测 0 个非黑像素），屏幕本身不熄灭；轻触、震动或重新插电时唤醒。
- **防烧屏**：每 3 分钟轻移 2dp，可关闭。
- **省电**：默认 30Hz；无操作时停止时钟/媒体刷新；加速度计只在黑屏时启用；天气 20 分钟、日历 5 分钟低频刷新；无后台服务、无广告、无统计。
- **隐私**：首次启动隐私同意页、完整隐私政策、权限说明；关闭云备份；可一键清除本地数据。

## 安装（已连接 ADB 的小米 17 Pro）

```bash
adb install -r dist/NightStandby-1.0.0.apk
```

首次打开：阅读并同意隐私政策。然后点右上角齿轮：

1. **日历** → 授权只读日历；
2. **通知使用权 · 音乐** → 在系统列表中开启「守夜 · 音乐控制」；
3. **定位权限**（可选）→ 允许大致位置；拒绝时会按时区回退到城市天气。

建议在系统设置中把屏幕超时设长，或直接保持充电并打开守夜。应用自身使用 `FLAG_KEEP_SCREEN_ON`，不会随系统超时熄屏；60 秒后只是把像素变黑。

## 构建

环境：macOS、JDK 17、Android SDK Platform 36 / Build Tools 36.0.0。

正式发布包使用独立 release 证书签名，证书和口令不进入 Git：

```properties
# signing/keystore.properties
storeFile=signing/nightstandby-release.jks
storePassword=...
keyAlias=nightstandby
keyPassword=...
```

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home
./gradlew :app:assembleRelease
```

产物：`app/build/outputs/apk/release/app-release.apk`。

如果没有 `signing/keystore.properties`，Gradle 会构建未签名的 release APK，方便第三方使用自己的证书签名。官方发布的 APK 使用 `CN=NightStandby` 证书。

## 下载

GitHub Releases：<https://github.com/duhaoze2007/NightStandby/releases/latest>

## 联网与权限边界

Release APK 只声明：

| 权限 | 用途 |
| --- | --- |
| `INTERNET` | 天气 / 城市地理编码 |
| `ACCESS_COARSE_LOCATION` | 为天气取一次系统最后已知粗略位置（可选） |
| `READ_CALENDAR` | 只读日程 |

通知使用权通过 `BIND_NOTIFICATION_LISTENER_SERVICE` 服务获得，不额外声明通知读取权限。代码中**只有 `WeatherRepository` 发起网络请求**（`api.open-meteo.com` 与 `geocoding-api.open-meteo.com`）；日历、媒体、传感器、时钟全部本地处理。没有账号、广告、统计或崩溃上报 SDK。

## 低功耗设计

- 窗口 `preferredRefreshRate = 30f`（实测显示模式切到 30Hz）。
- 时钟只在分钟边界刷新；媒体进度只在音乐小组件可见且播放时每秒刷新。
- 黑屏后主界面从 Compose 组合中移除，仅注册加速度计；唤醒后立即注销。
- 光线传感器 1Hz 采样；仅在自动红光开启时注册。
- 使用纯黑背景与少量低亮度像素，减少 OLED 发光面积。
- 无前台服务、无轮询网络、无后台常驻任务；媒体会话变更由 `MediaSession` 回调和 5 秒一次的前台轮询兜底。

## HyperOS 说明

HyperOS 的「自启动」策略可能拒绝系统重新绑定第三方通知监听服务。因此守夜在请求音乐会话时，除了监听服务，还会在应用前台直接通过 `MediaSessionManager.getActiveSessions()` 读取当前会话。音乐控制不依赖应用被加入自启动白名单；但**通知使用权必须开启**。

部分播放器不暴露标准 `MediaSession`（尤其是没有通知控制的播放器/网页音频），这类音频不会出现在音乐卡片中。

## 已知限制

- 「自动变黑」是应用内全黑覆盖，不是系统级 AOD / 真正熄屏；屏幕保持点亮，以便瞬时唤醒。
- 应用不会在插电时自动启动，需要手动打开（避免滥用后台启动限制）。
- 天气依赖 Open-Meteo 网络；断网时显示上次缓存并标记「缓存」。
- 定位只使用系统最后已知位置，不持续定位；位置缺失时使用设置城市或时区回退城市。
- 不同的 HyperOS 版本可能调整通知使用权页面的入口；设置内已提供入口，必要时请到系统设置中手动开启。

## 目录

- `app/src/main/java/...` — Compose 界面、`StandbyHome`、日历/天气/媒体数据层。
- `app/src/debug/...` — 仅调试用的本地媒体会话与调试入口，不进入 release APK。
- `screenshots/` — 实机截图。
