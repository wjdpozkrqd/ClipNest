# ClipNest

ClipNest 是一款本地优先的 Android 智能剪贴板与主题资料管理器。

## 当前版本

- 本次 0.2.1 是工程稳定性修正版本，不代表功能已达到正式发行标准。

- 版本：0.2.1
- applicationId：`com.clipnest.app`
- minSdk：29（Android 10）
- compileSdk：35
- targetSdk：35
- JDK：建议 JDK 17 或 21
- 构建：Gradle 命令行


## GitHub Actions 云端构建（推荐）

仓库已提供 `.github/workflows/android-ci.yml`。将项目推送到 GitHub 的 `main` 或 `master` 分支后，会自动在 GitHub 托管的 Ubuntu Runner 上配置 JDK 21、Gradle 8.9 和 Android SDK 35，并执行 `assembleDebug`。也可以在 GitHub 仓库的 **Actions** 页面手动运行 `ClipNest Android CI`。

构建成功后，在对应工作流运行记录底部的 **Artifacts** 区域下载 `ClipNest-debug-apk`，解压即可得到 `app-debug.apk`。构建失败时，优先查看该次运行的 `Build Debug APK` 步骤日志；如有诊断报告，也可下载 `ClipNest-build-diagnostics`。

首次使用时，请确保仓库默认分支为 `main` 或 `master`。本工作流只负责自动编译和保存 Debug APK，不会自动发布到应用商店，也不会签署正式发行版。

## Linux 命令行构建

确保 `JAVA_HOME` 指向 JDK 21，Android SDK 已安装 Android 35 与 Build Tools 35.0.0。

当前压缩包未内置 Gradle Wrapper JAR。云端 Linux 需预先安装 Gradle 8.9，并确保 `gradle --version` 可正常运行。然后执行：

```bash
chmod +x build-linux.sh
./build-linux.sh
```

脚本会生成 Gradle Wrapper 并执行 Debug 构建。若首次生成 Wrapper 时网络无法访问 Gradle 分发服务，请先配置可访问的 Gradle 镜像/网络。

APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 当前已实现

- Compose 原生界面与底部导航
- Room 本地数据库
- 剪贴板内容手动添加
- 从系统剪贴板主动读取并导入
- 内容类型初步识别（链接、手机号、邮箱、验证码、代码、文本）
- 历史记录、搜索、复制、删除
- 收藏/永久保存入口
- 分类统计
- 主题创建
- 主题自定义字段新增和删除

## 当前版本的明确限制

1. Android 10+ 对后台读取其他应用剪贴板有系统限制。本版本不承诺后台自动捕获所有复制事件。
2. 输入法服务尚未加入，本版本先完成核心数据模型和主应用。
3. 自定义分类管理、编辑已有主题字段、JSON/ZIP 备份恢复、加密存储尚未实现。
4. 当前版本用于第一轮真机验证，不应作为正式公开发行版本。
5. 复制内容仅在用户主动操作时导入；不要把验证码、密码等敏感内容作为普通资料长期保存。

## 工程结构

```text
ClipNest/
├── app/
│   └── src/main/
│       ├── java/com/clipnest/app/
│       │   ├── MainActivity.kt
│       │   └── data/
│       │       ├── Models.kt
│       │       ├── ClipNestDao.kt
│       │       └── ClipClassifier.kt
│       └── AndroidManifest.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 下一阶段

- 独立输入法模块
- 自定义分类 CRUD
- 主题字段编辑、排序、剪贴板引用
- JSON 导出与恢复
- 加密备份
- 自动清理策略和敏感信息保护
- UI 与无障碍/大字体适配
