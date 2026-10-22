# 构建与 IDE 联动

根工程是唯一的 Gradle 构建，包含八个 Java 模块及 `mybatis-ext-idea-plugin`。Maven POM 和双重构建入口已移除，发布结果仍使用原 Maven 坐标。

## 环境

- Gradle Wrapper：9.4.0，启动 JVM 使用 JDK 17 以上，建议使用 IDEA 的 JBR 25。
- Java 模块：JDK 17 toolchain 编译和测试，`--release 8` 保留 Java 8 API 与字节码边界；H2 等测试依赖运行在 JDK 17。
- IDEA 插件：固定使用 IDEA Community 2023.3.8（233）SDK，JDK 17 toolchain，`--release 17`；SDK 首次构建自动下载并缓存，不随开发者本机 IDEA 改变。
- 本地测试/调试的 IDEA 路径可通过 `IDEA_HOME`、`-PideaHome=...` 或插件目录不入库的 `local.properties` 设置。这些设置不替换发布构建的 233 SDK。

## IDEA 导入

1. 对已导入的旧项目，在 Maven 工具窗口取消根 POM 关联；不要手工保留旧 Maven 模块依赖。
2. 将根 `settings.gradle.kts` 作为 Gradle 工程打开或关联，使用仓库根目录 Wrapper，将 Gradle JVM 设为 JBR 25。
3. 同步后，核心、Spring、测试和插件模块均来自同一 Gradle 项目模型。插件依赖的是核心项目，源码跳转、查找引用及重构无需本地仓库源码 JAR。
4. 使用 Gradle 工具窗口的 `verifyAll` 统一验证；运行 `mybatis-ext-idea-plugin → intellij platform → runIde` 可启动开发沙箱，调试时使用同一任务的 Debug 操作。

核心源码变化后，Gradle 会按任务依赖重编译核心并更新插件沙箱和安装包。已运行的沙箱或已安装的插件不会自动替换正在使用的代码；重新运行或按 IDEA 热替换支持范围调试。

## 常用命令

在仓库根目录执行：

```powershell
.\gradlew.bat verifyAll
.\gradlew.bat spotlessApply
.\gradlew.bat spotlessCheck
.\gradlew.bat releaseBundle
.\gradlew.bat :mybatis-ext:test
.\gradlew.bat :mybatis-ext-idea-plugin:test :mybatis-ext-idea-plugin:buildPlugin
.\gradlew.bat :mybatis-ext-idea-plugin:runIde
```

`verifyAll` 包括代码格式检查、全部模块测试、插件结构检查、基线 Plugin Verifier 检查、ZIP 打包及 Maven POM 生成，不上传或签名。插件包位于 `mybatis-ext-idea-plugin/build/distributions/`。

## IDEA 兼容验证

选择 2023.3 作为基线，是为了覆盖仍使用 Java 17 平台的 IDEA，同时用固定 SDK 约束 API；当前功能不需要以 262 的新 API 为前提。构建工具本身使用新版 JVM 不影响这一边界。

默认 `runIde` 和 `test` 使用固定的 233 SDK。要使用本机已安装的新版 IDEA，显式指定路径；`runLocalIde` / `testLocalIde` 只在提供路径后注册，发布编译仍使用 233：

```powershell
.\gradlew.bat :mybatis-ext-idea-plugin:testLocalIde '-PideaHome=C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.0.1'
.\gradlew.bat :mybatis-ext-idea-plugin:runLocalIde '-PideaHome=C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.0.1'
.\gradlew.bat :mybatis-ext-idea-plugin:verifyPlugin '-PverificationIdePaths=C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.0.1'
.\gradlew.bat :mybatis-ext-idea-plugin:verifyPlugin '-PverificationIdeVersions=2024.2.3,2025.1' '-PverificationIdePaths=C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.0.1'
```

`verificationIdePaths` 可用分号分隔多个完整 IDE 安装目录；`verificationIdeVersions` 用逗号分隔额外的 Community SDK 版本，自动下载并缓存。验证同时检查固定基线和所有额外目标；报告位于插件 `build/reports/pluginVerifier/`。版本范围声明不能替代验证，新版平台 API 变化必须通过编译、PSI 测试和二进制验证处理。跨版本 PSI 测试使用目标 IDE 的测试框架与运行时。

需要 HTTP 代理时可在命令中传入 `'-Dhttp.proxyHost=localhost' '-Dhttp.proxyPort=10809' '-Dhttps.proxyHost=localhost' '-Dhttps.proxyPort=10809'`，或放入用户级 Gradle 配置，不把个人代理写入仓库。

SDK 和构建依赖已缓存后，可追加 `--offline`，同时让 Gradle 和 Plugin Verifier 离线运行。Verifier 将使用 SDK 内的插件与本地依赖缓存，不向 Marketplace 查询依赖；离线结果应连同依赖解析情况检查，不能通过忽略缺失的必需依赖来认定兼容。

本轮验证记录（2026-10-22，调整版本继承前的插件开发包 `0.1.0`）：

| IDEA 目标 | Plugin Verifier 1.411 | Java/XML PSI 测试 |
| --- | --- | --- |
| Community 2023.3.8 / 233.15619.7 | Compatible | 51 项通过；同时作为编译 SDK |
| Community 2024.2.3 / 242.23339.11 | Compatible | 未运行 |
| Community 2025.1 / 251.23774.435 | Compatible；1 处弃用 API | 未运行 |
| 本机 Ultimate 2026.2.3 / 262.10968.63 | Compatible；2 处弃用 API | 51 项通过 |

最终矩阵使用完整 SDK 和缓存进行离线验证，插件必需的 Java/平台依赖均来自对应 SDK，无二进制兼容错误及计划移除 API 告警。保留的弃用调用是 DTD 的 `ResourceRegistrar.addStdResource(..., Class)`，以及新版平台中的 `DaemonCodeAnalyzer.restart()`；为保留 233 基线继续使用公共兼容入口，后续平台移除前需重新验证并适配。声明范围为 `233` 至 `262.*`，上述结果覆盖所列代表版本，并非每个中间版本或补丁的人工编辑器验收。

Java 格式化使用 Spotless 8.2.1 / Eclipse 4.37，复用 `.vscode/java-formatter.xml` 的四空格和不主动折行规则；Gradle Kotlin 脚本使用 ktfmt 0.61。范围仅为各模块 `src` 内的 Java 文件及 Gradle 脚本，不处理生成目录或其他资料。首次运行会下载工具；根 `gradle.properties` 默认设置 `eclipseMirror=https://mirrors.ustc.edu.cn/eclipse`，可通过 `-PeclipseMirror=其他镜像地址` 覆盖，镜像不改变格式版本或规则。下载成功后可使用 Gradle 缓存。

Java 配置显式要求方法、成员类型及不同成员分组之间留空行，展开方法和非空代码块，不将控制语句体压在同一行，并保留有意义的手工换行。`if`、`else` 和循环体始终写大括号（`else if` 除外），不同逻辑阶段之间留空行；后两项属于编码约定，Eclipse 格式化器不会自动补大括号或划分逻辑段落，详见根 `AGENTS.md`。

## 依赖与打包

根 `gradle.properties` 中的 `version` 是核心、Spring 模块和 IDEA 插件的唯一版本来源，根平台维护公共依赖及 BOM。插件的描述文件、JAR 和 ZIP 均继承同一版本，不在插件构建中重复声明。仅发布插件修复时也递增根版本；未变化的 Java 库无需重复对外发布。

所有内部依赖均为 Gradle `project` 依赖，不经 Maven 本地仓库。核心的开发编译仍使用原始 Javassist；对外运行时及插件安装包使用经过 Shadow 重定位的核心 JAR，Javassist 路径保持为 `io.github.mybatisext.javassist`。普通 `*-plain.jar` 仅供构建辅助，不作为发布核心。

MyBatis 及 Spring 的原 provided 依赖用 `compileOnly` 表达，并写入发布 POM 的 `provided` 范围；测试显式获得这些依赖。测试/示例模块及 IDEA 插件不作为公共 Java 库发布。

## Maven 格式发布

保留以下坐标，版本来自根配置：

- `io.github.mybatis-ext:mybatis-ext-parent`：依赖管理 BOM，供 Maven 的 `dependencyManagement` / `import` 使用。
- `io.github.mybatis-ext:mybatis-ext`
- `io.github.mybatis-ext:mybatis-ext-spring`
- `io.github.mybatis-ext:mybatis-ext-spring-boot-starter`

```powershell
# 生成包含 JAR、源码、Javadoc、POM 及 Gradle 元数据的文件仓库，不对外上传
.\gradlew.bat publish

# 按开发环境的 Maven 仓库设置安装，供其他本地项目使用
.\gradlew.bat publishToMavenLocal

# 需要签名时才调用本机 GPG；日常构建不访问签名密钥
.\gradlew.bat publish -PsignPublications=true
```

`publish` 默认只写入根 `build/repository`。签名使用 Gradle Signing 的 `signing.gnupg.*` 属性，凭据放用户级 `gradle.properties` 或环境配置，不提交仓库。文件仓库产物可供后续仓库上传流程使用；原 Maven Central Portal 的 Maven 插件自动上传流程已移除，本构建不会自动上传或发布到 Central / Marketplace。

## 交付包与发布步骤

1. 当前保留根开发配置 `0.0.17`，暂不发布。正式发布前，根据[变更记录](changelog.md)和[迁移指南](migration.md)确定未发布过的新版本，仅修改根 `gradle.properties` 中的 `version`，核心、Spring 模块和插件自动同步；不得覆盖仓库中已有的同版本产物，插件版本也不得低于其已发布版本。
2. 运行 `gradlew.bat clean releaseBundle`。该任务先执行完整验证及本地文件仓库发布，再生成 `build/distributions/mybatis-ext-<核心版本>-release.zip`。包内 `maven/` 仅包含当前核心版本的四个 Maven 坐标，`idea/` 为当前插件 ZIP，另附文档、README 和 MIT 许可；不收集训练资料、IDE 配置、测试数据或历史版本。
3. 需要 Maven Central 签名时，使用已配置的 GPG 执行 `gradlew.bat clean releaseBundle -PsignPublications=true`。检查四个坐标下的 POM、主产物、源码/Javadoc（Java 库）及对应 `.asc`、校验和。交付 ZIP 是归档包，不是可直接上传 Central Portal 的 bundle；Portal 上传包需以其中 `maven/` 的坐标目录为根，按平台当前要求制作并验证。
4. 对待支持的 IDEA SDK 运行插件兼容性验证，再通过 Install Plugin from Disk 做编辑器验收；`verifyPluginStructure` 和 PSI 测试不能代替跨 IDE 版本的二进制验证。最低声明为 233，兼容上限以插件构建配置及验证记录为准。插件 ZIP 可独立本地安装；Marketplace 发布与证书签名需按 JetBrains 流程配置。
5. 确认版本、迁移说明、发布目标和凭据后再执行外部发布。`publish`、`releaseBundle` 均不会执行这一外部步骤。

全部归档固定文件顺序并去除文件时间戳；核心、源码和 Javadoc JAR 包含 MIT 许可。Wrapper 使用 SHA-256 校验 Gradle 分发包。生成的文件仓库同时提供 Maven 发布产物校验和；对外分发总交付包时可用 `Get-FileHash build/distributions/mybatis-ext-<核心版本>-release.zip -Algorithm SHA256` 记录哈希。

测试中的数据库集成主要使用 H2 兼容模式。现有 Oracle/DM 匿名块及 SQL Server 特定更新/删除语法用例保留 `@Disabled`，完整验证通过不代表已在这些真实数据库上完成验收。默认只验证固定基线；跨版本矩阵需通过 `verificationIdeVersions` 或 `verificationIdePaths` 显式提供目标 IDE。默认任务不执行 Marketplace 上传或证书签名。

插件不再根据目标项目版本号阻断分析。构建保证插件与所携带核心同源，一致性测试约束两端语义；目标项目无法解析的类型或表达由已有诊断处理。新核心语义或新 PSI 形态仍需同步验证、适配并发布插件。
