# 本次升级迁移指南

## 适用范围

正常使用实体注解、Mapper 和 Spring Boot starter 的业务代码不需要为了分析能力改写。直接访问核心元数据、语义模型、解析器或反射实现的自定义扩展，需要检查下列 API 变化并重新编译；不能仅替换 JAR。升级时保持核心、Spring 集成和 starter 版本一致。

## 类型模型

元数据、变量和语义对象中原本暴露 `GenericType` 的类型位置改为 `JavaType`。读取名称、泛型参数和成员时使用模型接口；仅在运行时确实需要 `Class<?>` 时使用反射桥接：

```java
JavaType type = propertyInfo.getJavaType();
String name = type.getName();
// 仅接受反射来源的 JavaType；PSI 或其他静态模型不能转换为 Class。
Class<?> runtimeClass = ReflectionTypePolicy.requireClass(type);
```

`GenericTypeFactory.build(...)` 仍提供反射类型，原 `GenericType` 实现了类型模型接口。不要对任意 `JavaType` 强转 `GenericType`，也不要为了插件分析加载项目实体类。实现独立类型适配器时，遵守[分析 API 与适配契约](analysis.md)，尤其是泛型替换、未解析类型、快照身份及注解默认值。

## 注解和生成器

`PropertyDef` 中的注解值使用 `JavaAnnotation`。例如原 `propertyDef.getId().idType()` 改为：

```java
JavaAnnotation id = propertyDef.getId();
if (id != null) {
    IdType idType = id.value("idType");
}
```

类属性返回 `JavaType`，嵌套注解数组返回 `JavaAnnotation[]`；不要转换为业务注解代理。`PropertyInfo.getCustomIdGenerator()` 返回生成器类型描述，运行时执行由现有 ID 生成流程处理。前置校验仅检查公开无参构造条件，构造器异常在实际生成 ID 时暴露。

## 分析和诊断

工具使用 `MybatisExtAnalyzer`，运行时继续使用严格的元数据工厂和 `JpaParser.parse(...)`。不要用分析失败后返回的空值继续构建 MappedStatement；先判断 `AnalysisResult.isSuccess()`。补全结果即使带诊断仍可能有候选，按其替换范围处理。

`MybatisExtException` / `ParserException` 的 `getDiagnostic()` 可能为空，通用异常不会自动归类。已知规则的诊断包含错误类别和声明位置；没有字符范围时定位声明。范围相对于解码后的 `DiagnosticLocation.getText()`，不是 Java/XML 文档偏移。诊断是不可变值，不能用 setter 补写来源或改写异常消息。

`PropertyInfo.source` 为当前声明，`mappingSource` 为最终映射来源；ColumnRef 和多层关联导航应使用后者。一个 `MybatisExtAnalyzer` 只服务稳定的类型、配置快照；配置或源码变化时重新创建。结果包含现有可变模型，调用方按只读使用，不跨请求共享或并发修改。

## 条件模型

解析器通过 `And` 优先于 `Or` 的文法直接生成 `Condition`，移除 `ConditionList` 和 `ConditionHelper.buildForConditionList(...)`。`Condition.getSubConditions()` 的返回类型由 `Set<Condition>` 改为 `List<Condition>`，保留条件的声明顺序及重复项；`equals` 和 `hashCode` 仍按集合语义比较子条件，忽略顺序及重复项。直接使用这些 API 的自定义扩展需要调整并重新编译。

## 构建和依赖管理

从 IDEA 取消旧 Maven 关联，导入根 `settings.gradle.kts`。Java 库使用 JDK 17、`--release 8` 编译；插件固定使用 IDEA Community 2023.3.8 SDK、`--release 17`，Gradle 本身可运行于新版 JBR。本机 `ideaHome` 只选择独立的测试/调试 IDE，不再决定编译 SDK。原 `mvn test/package/install` 分别改用根 `gradlew.bat verifyAll`、`releaseBundle`、`publishToMavenLocal`，详见[构建说明](build.md)。

外部 Maven 项目仍依赖原 group/artifact 坐标。如果以前把 `mybatis-ext-parent` 作为 `<parent>` 继承，应切换到自身的构建父工程，并在 `dependencyManagement` 中按 `type=pom`、`scope=import` 导入此 BOM；BOM 不提供 Maven 插件、构建配置和 parent 属性继承。

插件已携带构建时的核心，无需在目标项目安装额外分析包；核心源码更新后应同步验证、构建并发布插件。已安装插件不会随项目依赖升级而自动更新，目标版本不同也不会被硬性拦截；新语法与 PSI 形态仍需测试和必要适配。

插件工程模块由 `mybatis-ext-idea` 更名为 `mybatis-ext-idea-plugin`，Gradle 任务路径与 ZIP 文件名同步调整；已有 Gradle 运行配置需更新任务路径，并重新同步工程。插件 ID `io.github.mybatisext.idea`、展示名称 `MyBatis Ext` 和 Java 包名保持不变。设置服务改用项目级 `@Service` 注册，状态名 `MybatisExtAnalysis` 及存储文件 `mybatis-ext.xml` 保持不变，无需迁移设置。
