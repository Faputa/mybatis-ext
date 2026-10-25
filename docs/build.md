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
4. 使用 Gradle 工具窗口的 `check` 执行日常检查，`verifyAll` 还包含 Plugin Verifier，已知问题见下文；运行 `mybatis-ext-idea-plugin → intellij platform → runIde` 可启动开发沙箱，调试时使用同一任务的 Debug 操作。

核心源码变化后，Gradle 会按任务依赖重编译核心并更新插件沙箱和安装包。已运行的沙箱或已安装的插件不会自动替换正在使用的代码；重新运行或按 IDEA 热替换支持范围调试。

## 常用命令

在仓库根目录执行：

提供与常用 Maven 生命周期对应的根任务：

| 原 Maven 命令 | Gradle 命令 | 执行范围 |
| --- | --- | --- |
| `mvn clean compile` | `.\gradlew.bat clean compile` | 编译全部模块的主代码并处理资源，不运行测试 |
| `mvn clean test` | `.\gradlew.bat clean test` | 编译主代码和测试代码，运行全部模块测试 |
| `mvn clean package` | `.\gradlew.bat clean package` | 执行测试及格式检查，生成模块 JAR、源码、Javadoc 和 IDEA 插件 ZIP |
| `mvn clean install` | `.\gradlew.bat clean install` | 完成 `package` 后，将四个 Maven 坐标安装到本地 Maven 仓库 |
| `mvn clean deploy` | `.\gradlew.bat clean deploy` | 完成 `install` 和发布验证后，将四个 Maven 坐标上传 Central Portal |

`package` 不写入本地 Maven 仓库；`install` 只安装 BOM、核心、Spring、Boot Starter，不安装测试模块或 IDEA 插件。测试或检查失败时，不执行本地安装或 Central 上传。`deploy` 需要下文的 Central Token、GPG 和 `signPublications=true` 配置；上传后仍需在 Portal 手动确认公开发布。

其他日常命令：

```powershell
.\gradlew.bat check
.\gradlew.bat spotlessApply
.\gradlew.bat spotlessCheck
.\gradlew.bat :mybatis-ext:test
.\gradlew.bat :mybatis-ext-idea-plugin:test :mybatis-ext-idea-plugin:verifyPluginStructure :mybatis-ext-idea-plugin:buildPlugin
.\gradlew.bat :mybatis-ext-idea-plugin:runIde
```

`check` 包括代码格式检查及全部模块测试。完整验证命令 `.\gradlew.bat verifyAll` 还包括插件结构检查、基线 Plugin Verifier、ZIP 打包及 Maven POM 生成；其已知 API 使用问题见下文。插件包位于 `mybatis-ext-idea-plugin/build/distributions/`。

## IDEA 兼容验证

选择 2023.3 作为基线，是为了覆盖仍使用 Java 17 平台的 IDEA，同时用固定 SDK 约束 API；当前功能不需要以 262 的新 API 为前提。构建工具本身使用新版 JVM 不影响这一边界。

默认 `runIde` 和 `test` 使用固定的 233 SDK。要使用本机已安装的新版 IDEA，显式指定路径；`runLocalIde` / `testLocalIde` 只在提供路径后注册，发布编译仍使用 233：

```powershell
# 将路径替换为要验证的 IDEA 安装目录
$env:IDEA_HOME = 'C:/path/to/idea'
.\gradlew.bat :mybatis-ext-idea-plugin:testLocalIde
.\gradlew.bat :mybatis-ext-idea-plugin:runLocalIde
.\gradlew.bat :mybatis-ext-idea-plugin:verifyPlugin "-PverificationIdePaths=$env:IDEA_HOME"
.\gradlew.bat :mybatis-ext-idea-plugin:verifyPlugin '-PverificationIdeVersions=2024.2.3,2025.1' "-PverificationIdePaths=$env:IDEA_HOME"
```

`verificationIdePaths` 可用分号分隔多个完整 IDE 安装目录；`verificationIdeVersions` 用逗号分隔额外的 Community SDK 版本，自动下载并缓存。验证同时检查固定基线和所有额外目标；报告位于插件 `build/reports/pluginVerifier/`。版本范围声明不能替代验证，新版平台 API 变化必须通过编译、PSI 测试和二进制验证处理。跨版本 PSI 测试使用目标 IDE 的测试框架与运行时。

需要 HTTP 代理时，按环境配置 `http.proxyHost`、`http.proxyPort`、`https.proxyHost` 和 `https.proxyPort`，通过命令行 `-D` 参数或用户级 Gradle 配置传入。

SDK 和构建依赖已缓存后，可追加 `--offline`，同时让 Gradle 和 Plugin Verifier 离线运行。Verifier 将使用 SDK 内的插件与本地依赖缓存，不向 Marketplace 查询依赖；离线结果应连同依赖解析情况检查，不能通过忽略缺失的必需依赖来认定兼容。

历史验证记录（2026-10-22，插件开发包 `0.1.0`，不代表当前产物的验证结果）：

| IDEA 目标 | Plugin Verifier 1.411 | Java/XML PSI 测试 |
| --- | --- | --- |
| Community 2023.3.8 / 233.15619.7 | Compatible | 51 项通过；同时作为编译 SDK |
| Community 2024.2.3 / 242.23339.11 | Compatible | 未运行 |
| Community 2025.1 / 251.23774.435 | Compatible；1 处弃用 API | 未运行 |
| Ultimate 2026.2.3 / 262.10968.63 | Compatible；2 处弃用 API | 51 项通过 |

历史矩阵使用完整 SDK 和缓存进行离线验证；其中的弃用调用包括 DTD 的 `ResourceRegistrar.addStdResource(..., Class)` 和新版平台的 `DaemonCodeAnalyzer.restart()`。当前声明范围为 `233` 至 `262.*`，每次发布仍需对实际产物重新验证。

`0.0.17` 的 233 基线报告记录了 `ExtImplicitReferences` 对 `PsiSymbolService` 的 3 处内部 API 使用，以及实验性 API 使用。内部 API 使用仍输出到控制台及报告，但不阻断构建；兼容性问题及错误调用 OverrideOnly API 仍会使验证失败。报告位于 `mybatis-ext-idea-plugin/build/reports/pluginVerifier/`。日常测试、结构校验和打包成功也不代表跨版本兼容性验证通过。

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

`publish` 只写入本地文件仓库：根 `build/repository` 及各发布模块的 `build/nmcp/m2`，不会对外上传。签名使用 Gradle Signing 的 `signing.gnupg.*` 属性，凭据放用户级 `gradle.properties` 或环境配置，不提交仓库。

### 上传 Maven Central

使用 Nmcp 1.6.2 将现有的四个 Maven 坐标汇总上传到 Central Portal，不上传测试模块或 IDEA 插件。沿用原 Maven Central 插件未启用 `autoPublish` 时的行为：上传并等待平台校验，通过后在 Portal 手动确认公开发布。

在用户级 `C:/Users/Admin/.gradle/gradle.properties` 中配置 Central Portal 生成的 User Token 和 GPG；这里的 Central 凭据对应原 Maven `settings.xml` 中 `central` server 的 token 用户名和口令，不是网页登录密码：

```properties
centralUsername=你的Central用户令牌用户名
centralPassword=你的Central用户令牌口令
signPublications=true
signing.gnupg.executable=gpg
signing.gnupg.useLegacyGpg=false
signing.gnupg.keyName=你的完整密钥指纹
signing.gnupg.passphrase=你的GPG密钥口令
```

Central 凭据也可通过环境变量 `CENTRAL_USERNAME`、`CENTRAL_PASSWORD` 提供。口令以明文保存在用户配置中，不要放入仓库。

正式发布前，根据[变更记录](changelog.md)确定未发布过的新版本，仅修改根 `gradle.properties` 中的 `version`。当前开发配置为 `0.0.17`，不得覆盖已发布的同版本产物。核心、Spring 模块和插件自动同步版本，插件版本也不得低于其已发布版本。

完成版本和凭据配置后，执行一次：

```powershell
.\gradlew.bat clean deploy
```

`deploy` 调用 `nmcpPublishAggregationToCentralPortal`，先通过 `install` 和 `verifyAll` 完成测试、格式检查、插件结构及基线兼容性验证，并生成 Maven POM，再签名上传四个 Maven 坐标。上传后在 Central Portal 确认公开发布。未开启签名时拒绝上传；若未在用户级配置中启用签名，可在命令末尾追加 `-PsignPublications=true`。

#### 可选：发布前预检查

需要在上传前检查产物时，先执行：

```powershell
.\gradlew.bat clean verifyAll
.\gradlew.bat nmcpZipAggregation -PsignPublications=true
```

检查 `build/nmcp/zip/aggregation.zip` 中四个坐标的 POM、主产物、源码/Javadoc（Java 库）及对应 `.asc` 签名。该 ZIP 可用于手工上传；`nmcpZipAggregation` 本身不执行完整验证。检查通过后，执行 `gradlew.bat deploy` 继续上传，无需再次 `clean`。

插件发布前还需按上文配置 `verificationIdeVersions` 或 `verificationIdePaths` 验证目标 IDEA，并通过 Install Plugin from Disk 做编辑器验收。默认基线验证和 PSI 测试不能代替跨版本兼容性验证。插件 ZIP 独立发布，Marketplace 上传与证书签名需按 JetBrains 流程配置。

全部归档固定文件顺序并去除文件时间戳；核心、源码和 Javadoc JAR 包含 MIT 许可。Wrapper 使用 SHA-256 校验 Gradle 分发包。生成的文件仓库同时提供 Maven 发布产物校验和。

测试中的数据库集成主要使用 H2 兼容模式。现有 Oracle/DM 匿名块及 SQL Server 特定更新/删除语法用例保留 `@Disabled`，完整验证通过不代表已在这些真实数据库上完成验收。默认只验证固定基线；跨版本矩阵需通过 `verificationIdeVersions` 或 `verificationIdePaths` 显式提供目标 IDE。默认任务不执行 Marketplace 上传或证书签名。

插件不再根据目标项目版本号阻断分析。构建保证插件与所携带核心同源，一致性测试约束两端语义；目标项目无法解析的类型或表达由已有诊断处理。新核心语义或新 PSI 形态仍需同步验证、适配并发布插件。
