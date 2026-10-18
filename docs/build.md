# 构建

根工程使用 Gradle Wrapper 管理八个 Java 模块，发布结果使用 Maven 坐标。

## 环境与导入

- Gradle Wrapper 9.4.0，启动 JVM 使用 JDK 17 以上。
- Java 模块使用 JDK 17 toolchain 编译和测试，通过 `--release 8` 保留 Java 8 API 与字节码边界。
- 在 IDEA 中导入根目录的 `settings.gradle.kts`，使用仓库根目录 Wrapper 同步工程。

## 验证与打包

在仓库根目录执行：

```powershell
.\gradlew.bat spotlessApply spotlessCheck
.\gradlew.bat check
.\gradlew.bat assemble
.\gradlew.bat verifyAll
.\gradlew.bat releaseBundle
```

`check` 执行格式检查与全部 Java 模块测试。`verifyAll` 额外生成 Maven POM；`releaseBundle` 验证后生成本地文件仓库及交付归档，不对外上传。

核心 JAR 重定位 Javassist 并携带许可文件；归档固定文件顺序并去除文件时间戳，Wrapper 校验 Gradle 分发包的 SHA-256。

## Maven 格式发布

版本统一配置在根 `gradle.properties`。核心、Spring 集成和 starter 保留 Maven 坐标，根 `mybatis-ext-parent` 发布为依赖管理 BOM。

```powershell
.\gradlew.bat publish
.\gradlew.bat publishToMavenLocal
```

`publish` 只写入根 `build/repository`。需要签名时通过 `-PsignPublications=true` 使用本机 GPG，凭据保存在用户级配置中。

数据库集成测试主要使用 H2 兼容模式；部分特定数据库语法用例保留禁用状态，测试通过不代表真实数据库验收。
