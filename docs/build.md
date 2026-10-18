# 构建

根工程使用 Gradle Wrapper 管理八个 Java 模块，发布结果使用 Maven 坐标。

## 环境与导入

- Gradle Wrapper 9.4.0，启动 JVM 使用 JDK 17 以上。
- Java 模块使用 JDK 17 toolchain 编译和测试，通过 `--release 8` 保留 Java 8 API 与字节码边界。
- 在 IDEA 中导入根目录的 `settings.gradle.kts`，使用仓库根目录 Wrapper 同步工程。

## 验证与打包

在仓库根目录执行：

提供与常用 Maven 生命周期对应的根任务：

| 原 Maven 命令 | Gradle 命令 | 执行范围 |
| --- | --- | --- |
| `mvn clean compile` | `.\gradlew.bat clean compile` | 编译全部模块的主代码并处理资源，不运行测试 |
| `mvn clean test` | `.\gradlew.bat clean test` | 编译主代码和测试代码，运行全部模块测试 |
| `mvn clean package` | `.\gradlew.bat clean package` | 执行测试及格式检查，生成模块 JAR、源码、Javadoc  |
| `mvn clean install` | `.\gradlew.bat clean install` | 完成 `package` 后，将四个 Maven 坐标安装到本地 Maven 仓库 |
| `mvn clean deploy` | `.\gradlew.bat clean deploy` | 完成 `install` 和发布验证后，将四个 Maven 坐标上传 Central Portal |

`package` 不写入本地 Maven 仓库；`install` 只安装 BOM、核心、Spring、Boot Starter，不安装测试模块。测试或检查失败时，不执行本地安装或 Central 上传。`deploy` 需要下文的 Central Token、GPG 和 `signPublications=true` 配置；上传后仍需在 Portal 手动确认公开发布。


```powershell
.\gradlew.bat spotlessApply spotlessCheck
.\gradlew.bat check
.\gradlew.bat assemble
.\gradlew.bat verifyAll
```

`check` 执行格式检查与全部 Java 模块测试。`verifyAll` 额外生成 Maven POM。

核心 JAR 重定位 Javassist 并携带许可文件；归档固定文件顺序并去除文件时间戳，Wrapper 校验 Gradle 分发包的 SHA-256。

## Maven 格式发布

版本统一配置在根 `gradle.properties`。核心、Spring 集成和 starter 保留 Maven 坐标，根 `mybatis-ext-parent` 发布为依赖管理 BOM。

```powershell
.\gradlew.bat publish
.\gradlew.bat publishToMavenLocal
```

`publish` 只写入本地文件仓库：根 `build/repository` 及各发布模块的 `build/nmcp/m2`，不会对外上传。签名使用 Gradle Signing 的 `signing.gnupg.*` 属性，凭据放用户级 `gradle.properties` 或环境配置，不提交仓库。

### 上传 Maven Central

使用 Nmcp 1.6.2 将现有的四个 Maven 坐标汇总上传到 Central Portal，不上传测试模块。沿用原 Maven Central 插件未启用 `autoPublish` 时的行为：上传并等待平台校验，通过后在 Portal 手动确认公开发布。

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

正式发布前，确定未发布过的新版本，仅修改根 `gradle.properties` 中的 `version`。当前开发配置为 `0.0.17`，不得覆盖已发布的同版本产物。

完成版本和凭据配置后，执行一次：

```powershell
.\gradlew.bat clean deploy
```

`deploy` 调用 `nmcpPublishAggregationToCentralPortal`，先通过 `install` 和 `verifyAll` 完成测试、格式检查并生成 Maven POM，再签名上传四个 Maven 坐标。上传后在 Central Portal 确认公开发布。未开启签名时拒绝上传；若未在用户级配置中启用签名，可在命令末尾追加 `-PsignPublications=true`。

#### 可选：发布前预检查

需要在上传前检查产物时，先执行：

```powershell
.\gradlew.bat clean verifyAll
.\gradlew.bat nmcpZipAggregation -PsignPublications=true
```

检查 `build/nmcp/zip/aggregation.zip` 中四个坐标的 POM、主产物、源码/Javadoc（Java 库）及对应 `.asc` 签名。该 ZIP 可用于手工上传；`nmcpZipAggregation` 本身不执行完整验证。检查通过后，执行 `gradlew.bat deploy` 继续上传，无需再次 `clean`。

数据库集成测试主要使用 H2 兼容模式；部分特定数据库语法用例保留禁用状态，测试通过不代表真实数据库验收。
