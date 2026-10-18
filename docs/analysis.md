# 分析 API 与适配契约

核心通过统一的 Java 类型模型提供元数据分析、派生 CRUD 方法分析、属性路径解析和补全。运行时适配器通过反射提供类型事实，IDE 适配器通过 PSI 提供相同事实；映射规则、方法语法和语义绑定由核心处理。

`model` 包定义类型和注解模型，`diagnostic` 包提供运行时与工具共用的诊断，`analysis` 包提供分析入口、引用和补全结果。元数据分层、无锁构建及幂等注册见[元数据设计](metadata-design.md)。

## 分析入口

每个稳定的类型与配置快照使用一个 `MybatisExtAnalyzer`：

```java
TypePolicy policy = /* 适配当前项目可识别的 TypeHandler 配置 */;
ExtContext context = new ExtContext();
MybatisExtAnalyzer analyzer = new MybatisExtAnalyzer(policy, context);
AnalysisResult<TableInfo> metadata = analyzer.analyzeMetadata(entityType);
if (metadata.isSuccess()) {
    TableInfo table = metadata.getValue();
    AnalysisResult<Semantic> method = analyzer.analyzeMethod(table, methodDeclaration);
    AnalysisResult<List<CompletionItem>> completion =
            analyzer.completeMethod(table, methodDeclaration, caretOffsetInMethodName);
}
```

示例中的 `entityType` 是 `JavaType`，`methodDeclaration` 是已在所属类型上下文中替换泛型的 `JavaMethod`。光标偏移相对于方法名，从零开始。

| 入口 | 返回内容 |
| --- | --- |
| `analyzeMetadata(type)` | 实体或投影的 `AnalysisResult<TableInfo>` |
| `analyzeMethod(table, method)` | 派生 CRUD 方法的 `AnalysisResult<Semantic>` |
| `completeMethod(table, method, offset)` | 方法名补全的 `AnalysisResult<List<CompletionItem>>` |
| `resolvePropertyPath(table, path)` | 属性路径的 `AnalysisResult<PropertyInfo>`，包含已解析路径段的引用 |
| `completePropertyPath(table, path, offset)` | 光标所在路径段的 `List<CompletionItem>`；父路径无效时返回空列表 |

`AnalysisResult` 包含值、诊断和引用，`isSuccess()` 表示值非空且没有诊断。补全结果即使附带诊断，也可能保留候选；调用方应分别处理候选与诊断。

分析不需要启动 Spring、创建 SqlSessionFactory 或连接数据库，也不实例化业务类型和 ID 生成器。反射场景可使用 `ReflectionTypePolicy(new Configuration())`，但它只反映传入配置中的 TypeHandler；要与实际运行结果一致，应传入项目的真实配置。

## 类型与注解模型

适配器实现 `JavaType`、`JavaField`、`JavaMethod`、`JavaParameter` 和 `JavaAnnotatedElement`。原有反射类 `GenericType` 等也实现这些接口。各适配器遵守以下约定：

- 类型模型不要求提供 `Class`、`Field`、`Method` 或业务注解代理，也不要求加载被分析项目的实体类。
- `getName()` 对已解析的普通类使用二进制名称，嵌套类使用 `$`；基本类型使用关键字，数组使用组件名称加 `[]`，如 `java.lang.String[]`，不使用 `Class.getName()` 的数组描述符。
- 成员、父类和接口类型已在所属类型的泛型上下文中替换。例如 `Parent<Long>` 的 `T id` 返回 `Long`。
- `getTypeParameters()` 表示当前类型的实际泛型实参；数组通过 `getComponentType()` 表达。
- `isAssignableTo(Class<?> target)` 由适配器实现，判断到已知 JDK 或框架类型的原始类型赋值兼容性，遵循 `Class.isAssignableFrom` 语义，包括数组和接口到 `Object` 的兼容性。不进行装箱、基本类型转换或泛型实参兼容性判断，也不解析无关泛型实参；模型层不依赖反射实现。
- 无法解析的符号、未替换的类型变量和暂不支持的通配符以 `isResolved() == false` 表达，不替换为 `Object`。
- 类型相等及哈希包含所属解析环境和泛型实参；不同项目中同名类型不能共享缓存身份。
- `getBeanProperties()` 提供整个类型的 JavaBeans getter 选择结果及可写性。字段与 getter 的映射优先级、继承扫描和注解业务含义由核心处理。
- `JavaAnnotation.value(name)` 提供包含默认值的有效值。类属性返回 `JavaType`，嵌套注解数组返回 `JavaAnnotation[]`，枚举返回核心使用的枚举值。重复注解通过 `annotations(...)` 展开。
- `TypePolicy.classify` 返回 `SCALAR`、`OBJECT` 或 `UNKNOWN`；`isScalar` 遇到 `UNKNOWN` 时产生未解析类型诊断。当前 PSI 实现对已解析且未命中内置标量、配置或枚举规则的类型返回 `OBJECT`，自定义标量需显式配置。

接口中的 `Class<? extends Annotation>` 仅标识框架已知的注解类型。适配器应使用当前快照中的轻量描述或源引用，避免长期持有失效的 PSI。

## 诊断与错误处理

元数据工厂和 CRUD 解析器在规则校验处直接产生诊断。运行时异常通过 `getDiagnostic()` 保留该诊断，分析入口将已知规则错误转为结果；两者共享校验规则，不根据异常消息重新分类。

`JpaParser.parse(...)` 是严格运行时入口，直接返回 `Semantic`；语法错误抛出 `ParserException`，元数据或绑定错误可抛出 `MybatisExtException`。普通异常构造方法的 `getDiagnostic()` 可能为空，此类异常及其他未预期异常继续抛出。数据库连接和 SQL 执行错误由原运行时异常链处理。

`Diagnostic.Code` 区分语法、歧义、语义和未解析类型。元数据构建遇到已知规则错误即停止，不返回无效的部分表模型，也不保证一次收集所有错误。属性路径解析失败时，可保留错误之前已成功解析的引用。

`Diagnostic` 和 `DiagnosticLocation` 均不可变。`getMessage()` 返回原始问题描述，`format()` 返回带上下文的文本；补充来源时不修改既有异常消息或诊断。未解析类型补充位置时，会在保留原异常原因链的基础上创建新异常。

`DiagnosticLocation` 保存类型上下文、声明来源、注解成员、解析路径和实际文本。例如 `@ColumnRef("address.missing")` 的错误归属于声明该注解的字段或 getter；关联条件分别定位 `joinColumn.leftColumn`、`joinColumn.rightColumn` 和 `joinColumn.leftTableAlias`。无法确定精确文本范围时保留声明位置。

诊断可随异常序列化，类型名、成员名、路径、原始文本和范围保留。反射或 PSI 来源不参与序列化，反序列化后的 `getLocation()` 仍可用，其中 `getOwner()`、`getSource()` 为空。

## 引用、范围与补全

所有文本范围从零开始、左闭右开，偏移使用 Java 字符串的 UTF-16 索引。不同结果的参照文本如下：

| 结果 | 范围的参照文本 |
| --- | --- |
| `DiagnosticLocation.start/end` | `DiagnosticLocation.getText()` |
| 方法分析的 `SymbolReference.start/end` | 传入的完整方法名 |
| 属性路径解析的 `SymbolReference.start/end` | 传入的属性路径 |
| 方法补全的 `CompletionItem.replaceStart/replaceEnd` | 传入的完整方法名 |
| 属性路径补全的 `CompletionItem.replaceStart/replaceEnd` | 传入的属性路径 |

诊断的 `getPath()` 可能包含内部解析前缀，范围仍相对于实际文本。例如实际注解文本为 `missing`、内部路径为 `value.missing` 时，错误范围是 `[0,7)`。`hasRange() == false` 时 `start/end` 为 `-1`；由默认值推导的属性名不虚构文本范围。插件负责将解码文本中的范围映射到 Java 字面量或 XML 文档，处理转义、CDATA 等差异。

`SymbolReference` 的目标为 `PropertyInfo`、`Variable` 或实际绑定的 Java 声明，适配器据此定位属性声明或方法参数。条件绑定确定后，其属性末段的导航引用替换为实际绑定声明；对象参数定位到其对应属性，`Between` 可对应两个参数。嵌套路径的前段、无参数条件、排序、分组和查询字段保留实体属性引用。此替换不改变 `Condition.propertyInfo` 表达的查询字段，插件直接使用核心返回的最终导航目标。`Variable.source` 随参数绑定和属性展开保留实际声明，不参与变量相等性与 SQL 渲染。`PropertyInfo` 的属性来源有两种含义：

- `source` 表示当前属性声明，用于定位被引用的字段或 getter，以及归属该声明的诊断。
- `mappingSource` 表示最终映射来源，用于从映射声明跳转到被映射的属性或关联类型。直接声明的列为 `null`；`ColumnRef` 链指向最终引用声明，显式 `JoinRelation` 指向最后一层映射属性，推断关联类型时指向关联类型。

`PropertyDef.sourceType` 表示源码声明所属类型，`declaringType` 表示映射类型。适配器应按导航目的选择来源，不通过重复解释注解推导最终映射。

路径按 `address.city` 的形式解析，空路径段（如 `address.`、`address..city`）视为错误。`@ColumnRef` 在目标实体元数据上解析，关联左右属性在各自表的上下文中解析；目标上下文由适配器确定。

方法补全提供语法允许的关键字、属性和变量候选，允许参数或返回类型尚未补全。补全仅分析光标之前的语法上下文，并给出当前 token 的替换范围，尽量保留后续文本；不保证光标后方的整个方法合法，插入后应再次完整分析。

## 生命周期与运行时边界

一个 `MybatisExtAnalyzer` 服务一个稳定的源码、泛型和配置快照。实例使用期间保持传入的 `ExtContext` 和 TypeHandler 配置不变；源码或配置变化后重新创建实例及其结果。插件将持久化设置转换为当前请求的 `ExtContext`，PSI 访问遵循 IDE 的读访问规则。

分析入口不提供增量依赖追踪或同一实例的并发使用保证。返回的元数据、`Semantic` 及其集合仍沿用现有可变模型，调用方应按只读结果使用，避免影响后续分析结果。

运行时解析不创建成功的 `AnalysisResult`，引用和补全信息只在对应请求中收集。方法参数绑定、条件构造和 SQL 渲染沿用运行时规则；工具适配器不另行实现这些规则。SQL 生成、ResultMap/MappedStatement 注册、Javassist 增强及数据源访问属于运行时层。

`MapperResolver` 共享增强 Mapper 适用性、`@MapTable`/`ExtMapper<T>` 实体解析和已有 statement 的优先级判断。适配器为 `isDerivedMethod` 提供按完整 statement ID 查询 XML/注解映射的函数，核心检查继承链。方法重载的运行时返回类型归一化与分派由运行时实现负责。

分析能力位于原核心模块，部分注解和元数据依赖 MyBatis 的 `JdbcType`、`FetchType` 等类型。插件通过同一 Gradle 工程依赖并打包核心与 MyBatis；核心语义变化后需要重新构建和升级插件，新增 PSI 类型事实或 UI 入口仍可能需要适配。插件类加载器中的依赖与目标项目的运行时对象分别管理。

## 验证

`MybatisExtAnalyzerTest` 对照内存模型与反射模型的元数据、CRUD 语义、引用、诊断和补全，核心 SQL 集成测试验证运行时行为。

```powershell
.\gradlew.bat :mybatis-ext:test :mybatis-ext-test:test
```
