# 元数据设计

本文面向维护者，记录当前实现的元数据模型、构建流程、共享状态和下游依赖。类型身份的名称方案列为待决策项，尚未实施。业务使用方式和分析 API 分别见 [README](../README.md) 与[分析 API 与适配契约](analysis.md)。

## 核心设计目标：无锁编程

**无锁编程是元数据架构的核心设计目标。两层元数据、缓存外构建和共享图的幂等注册共同支撑这一目标。** 并发构建允许重复计算，通过稳定的身份和幂等注册收敛结果。

**类型身份约定：同一解析环境中，表示同一类型的 `JavaType` 必须相等、哈希一致，且在使用期间保持稳定。**

这里的无锁编程指元数据业务代码不使用 `synchronized`、`Lock` 等显式锁组织构建流程。原子注册由 `ConcurrentHashMap` 等并发容器负责，不要求容器内部采用严格的 lock-free 算法，也不自行编写 CAS 自旋或重试协议。

这一目标由以下设计共同保证：

- **两层元数据分离依赖。** `TableDef` 负责声明解析，`TableInfo` 负责关联绑定；关联绑定依赖已完成的声明，定义阶段不反向构建关联结果。分层用于控制递归依赖，是无锁构建的结构基础。
- **完整构建在缓存回调之外完成。** `TableDef` 和 `TableInfo` 先独立构建，`computeIfAbsent` 回调只发布或选取完整结果，避免递归构建进入缓存回调。
- **共享 JOIN 图通过幂等注册积累。** 各次构建直接使用共享图，相同路径复用已登记别名，不同路径通过并发集合原子占用别名。
- **失败语义允许保留有效注册。** 失败的解析结果不进入缓存，已完成的依赖声明及已登记的路径、别名可以保留；重复执行应复用这些注册，不要求事务式回滚。

修改元数据流程时须保留上述约束：构建直接使用共享图，注册后的路径别名保持稳定。遇到并发或递归问题，应检查分层依赖、键的相等性与稳定性、注册操作的幂等性。

## 职责与模型分层

元数据层将适配器提供的 Java 类型与注解事实转换成声明模型和关联解析结果。它不连接数据库，不注册 MappedStatement，也不执行 ID 生成或实体 ID 回写。

| 模型 | 职责与关键关系 |
| --- | --- |
| `TableDef` | 保存输入 `classType`、解析出的底层 `tableType`、物理 `TableName` 和属性声明树 |
| `PropertyDef` | 保存属性类型、列和注解声明、来源、所属表类型及嵌套属性声明 |
| `TableInfo` | 保存一个输入类型的解析结果，包括根 `JoinTableInfo`、属性树、别名索引和共享 `JoinGraph` |
| `PropertyInfo` | 保存属性完整路径、映射类型、所属 JOIN、列、JDBC 类型、ID、过滤和加载信息 |
| `JoinGraph` | 保存同一底层表类型下可复用的 JOIN 路径到 SQL 别名映射，以及已占用的别名集合 |
| `JoinNode` / `JoinPath` | 用表类型、前驱路径和左右物理列名描述关联身份 |
| `JoinTableInfo` / `JoinColumnInfo` | 保存当前 `TableInfo` 中具体的表与连接条件，供 SQL 和结果映射使用 |
| `PropertyPath` / `PropertyKey` | 分别负责点分属性路径查找和递归构建中的循环检测 |

`TableDef` 保留声明语义，`TableInfo` 绑定具体属性路径和关联表。不同 DTO 可以共享 `JoinGraph`，但各自的属性树、`JoinTableInfo` 和别名索引由各次构建创建；共享别名不等于共享整个 DTO 元数据。

## 类型事实、配置与生命周期

元数据工厂依赖 `JavaType`、`JavaAnnotation` 和 `TypePolicy`，不直接解释 PSI。反射适配器提供运行时类型事实，PSI 适配器在当前 IDE 请求中提供源码事实；成员和父类型必须已完成当前泛型上下文的替换。

`TypePolicy` 决定类型是标量、对象还是未知。反射实现根据当前 Configuration 的 TypeHandler 注册判断；工具实现根据当前可识别类型和配置判断。返回 `UNKNOWN` 时，标量判断产生未解析类型诊断；已解析但未配置为标量的普通 PSI 类型按对象处理。相同 Java 类型在不同 TypeHandler 配置下可能产生不同属性结构，因此配置属于缓存作用域的一部分。

一个 `TableInfoFactory` 固定使用构造时的 `TypePolicy` 和 `ExtContext`，并持有自己的 `TableDefFactory`、`JoinGraphFactory`。运行时由 `ExtEnhancer` 持有该工厂；分析入口由 `MybatisExtAnalyzer` 持有。IDE 的 `ExtSession` 每次请求创建独立的 `PsiModel` 和分析器。

实例使用期间，类型模型、泛型上下文、`ExtContext` 和 TypeHandler 配置应保持稳定。源码或配置改变后重新创建实例及其结果。元数据工厂不做增量失效和依赖追踪，不能通过复用旧缓存掩盖快照变化。

## 构建与缓存

构建入口依次完成以下工作：

1. `TableInfoFactory.buildTableInfo(JavaType)` 查询该输入类型的解析结果缓存。
2. 未命中时，`TableDefFactory` 解析底层表类型，扫描字段、getter、继承及引用，形成 `TableDef` 和 `PropertyDef` 树。
3. `JoinGraphFactory` 按底层表类型取得共享图。
4. `TableInfoFactory` 创建根 JOIN，按声明所属类型解析属性、继承 JOIN、关联链和嵌套结构，生成 `TableInfo`。
5. 完整构建成功后，通过并发容器选取该输入类型的缓存结果并返回。

| 缓存 | 当前键 | 值与共享范围 |
| --- | --- | --- |
| `classTypeToTableDef` | 输入 `JavaType` | 当前定义工厂中的完整声明树 |
| `classTypeToTableInfo` | 输入 `JavaType` | 当前解析工厂中的完整属性与 JOIN 绑定结果 |
| `tableTypeToJoinGraph` | `TableDef.tableType` | 当前图工厂中，同一底层表类型共用的路径与别名 |

`classType` 是正在解析的实体、子类或 DTO，`tableType` 是沿引用和继承最终找到的表声明类型。它们不能混作同一种缓存身份。`TableName` 根据表名和 schema 比较；两个不同 Java 表类型即使映射到相同物理表名，当前实现也不会因此合并图或元数据缓存。

并发调用可能分别完成构建，再由 `computeIfAbsent` 选取同一缓存结果。`TableInfoFactory.buildTableInfo(TableDef)` 直接构建，不经过输入类型的 `TableInfo` 缓存，不保证每次返回同一实例。失败时的缓存与注册行为见“失败语义与并发边界”。

## 表解析、属性扫描与覆盖

`resolveTableType` 遇到 `TableRef` 时转向引用类型，遇到 `Table` 时确定表声明，否则继续查找父类，直到 `Object`。没有表声明时，独立解析返回空，构建 `TableDef` 则报告缺少表注解。引用遍历会检查未解析类型和循环引用。

物理表名优先使用 `Table.name`，为空时将表类型简单名转为下划线形式；schema 独立保留，由 `TableName` 组合为限定表名。

属性扫描从当前类型向父类进行，每层先处理带映射注解的字段，再处理当前层声明的 JavaBeans getter。同名属性已有结果就跳过，因此已选中的子类声明优先于父类声明，同层已选中的字段优先于 getter。未形成映射的普通字段不占用属性名。getter 的泛型类型及只读性由类型适配器提供，元数据层不再重复推导 JavaBeans 规则。

扫描时根据当前 `Table` 或 `TableRef` 上下文分别处理 `Column` 和 `ColumnRef`；在允许展开关联时处理 `JoinRelation`。未标注相关映射的成员不会自动变成列。

| 声明 | 定义阶段的处理 |
| --- | --- |
| `Column` | 标量映射列名，默认由属性名转下划线；非标量展开嵌套属性。记录 ID、过滤和 JDBC 类型 |
| `ColumnRef` | 按显式引用路径或当前属性名查找目标声明，继承列、JDBC 类型、ID 和关联声明；本地 Fetch、Filterable、Cascade 覆盖被引用属性的对应设置 |
| `JoinRelation` | 记录有序关联链及 Fetch、Cascade；属性标记为非本表列，非标量继续展开内部属性 |
| `JoinParent` | 后续父类属性不再作为当前表的本表列；解析阶段为父表创建 JOIN |

`ColumnRef` 可以重命名，也可以引用点分嵌套路径。引用后的属性仍使用当前声明的 Java 类型、来源和只读性。`declaringType` 依据引用路径的外层根属性确定，防止把内嵌 Java 对象误当成独立所属表。

`Cascade` 在此处控制嵌套对象中的关联声明是否继续展开，不表示执行级联 INSERT、UPDATE 或 DELETE。普通嵌套列的展开与关联继续展开需分别判断。

## 属性解析与上下文

`processNameToPropertyDef` 按声明所属类型绑定属性，处理 `TableRef` 引用链和父类链，并检查当前声明的物理表与绑定表是否一致。`JoinParent` 切换父类属性的所属 JOIN，普通继承继续沿用当前表。

解析阶段区分两个路径前缀：`propertyPrefix` 形成返回对象中的完整属性路径，如 `address.city`；`ownPropertyPrefix` 用于当前表定义中的连接列查找。进入关联表或父表 JOIN 后，连接列查找上下文相应切换，不能直接拿完整对象路径代替每一侧表内的列路径。

属性类型按以下规则形成 `PropertyInfo`：

- Collection 保留集合类型，`ofType` 保存通过继承链解析出的元素类型，初始类型为 `COLLECTION`。
- Optional 使用解包后的类型；其他没有类型实参的属性保留自身类型。除 Collection、Optional 外，仍带类型实参的属性报 `Unsupported property type`，不自动擦除泛型。
- 其余类型根据 `TypePolicy` 区分标量 `RESULT` 和对象 `ASSOCIATION`。
- `Id` 设置 ID 策略，并将属性类型设为 `ID`。通过 Configuration 构造的运行时工厂还会检查 CUSTOM 生成器类公开、非抽象，并具有公开无参构造器，但不执行构造器；分析入口只保留生成器类型。
- 显式 Fetch 保留加载类型；原本属于 `RESULT` 的属性会转为 `ASSOCIATION`，供后续嵌套查询映射处理。
- 显式 Filterable 转为 `FilterableInfo`；缺省时按 `ExtContext.defaultFilterable` 决定是否产生过滤信息，默认条件为非空测试、等值比较和 AND。

`JavaTypes.unwrap` 在定义扫描或关联目标推断时处理数组、Collection 和 Optional。它只提供类型解包，不意味着数组一律按集合关联映射；最终属性分类还依赖当前 TypeHandler 和解析规则。`DISCRIMINATOR` 当前仅为枚举保留值，元数据工厂没有构造该类型的分支。

## JOIN 身份、别名与局部作用域

共享图按底层表类型组织。根节点没有左侧路径；关联节点由目标表类型及左侧 `JoinPath` 集合标识，支持多列连接。`JoinPath` 包含左侧节点及左右物理列名，因此多级路径会递归包含前驱身份。

别名不参与 `JoinNode.equals/hashCode`。路径集合的相等性不依赖注解中连接列的排列顺序；临时创建的不同节点只要路径与表类型相等，就能复用别名。节点进入图以后，参与相等性和哈希的表类型、路径及列名必须保持稳定。

`resolveJoinNodeAlias` 使用并发容器完成幂等注册：

```java
String alias = joinGraph.getJoinNodeToAlias()
        .computeIfAbsent(joinNode, k -> buildAlias(joinGraph, defaultName));
joinNode.setAlias(alias);
```

同一路径只在首次注册时分配别名，后续调用直接复用。分配时通过并发集合 `aliasRegistry.add` 原子占用候选名称，显式名称不可用时依次尝试 `t0`、`t1` 等回退名称。别名是在当前整张共享图内唯一，而非只在一次 DTO 构建中唯一。

注解中的局部别名与最终 SQL 别名承担不同职责：

- 根表、TableRef 和 JoinParent 的显式别名进入当前声明层的 `localAliasToJoinTableInfo`。
- 每个属性在该局部映射的副本上，按注解顺序解析自身的 JoinRelation 链。`leftTableAlias` 只能引用该作用域中已经登记的表，不能从共享图查询任意其他属性的别名。
- 未指定 `leftTableAlias` 时，连接左侧为当前关联链所在的表。显式别名在同一局部作用域重复会报错；共享图为不同路径分配回退名称不能使局部重复声明变为合法。
- `TableInfo.aliasToJoinTableInfo` 使用最终 SQL 别名索引当前构建的表绑定，同一路径对应的表绑定在该结果中复用。

关联目标优先使用 `JoinRelation.table`；缺省时根据属性类型或集合元素类型推断。每一对连接属性都经 `getOwnSingleColumn` 检查，必须是该侧表的本表叶子列，不能直接连接一个对象、非本表列或没有列名的属性。关联到显式目标表的最终列，从目标声明取得列名、JDBC 类型和映射来源。

节点创建时即确定最终别名。首次注册顺序可能影响别名文本；稳定性指已注册路径的映射不再改变，不保证不同构建顺序产生相同的 SQL 别名。

## 属性路径、循环检测与来源

`PropertyPath.resolve` 统一遍历 `PropertyDef` 和 `PropertyInfo` 的嵌套属性映射。按点分段查找，空段或不存在的属性报错；声明查找、关联列查找和分析入口共享这套路径逻辑。分析时可通过分段回调收集引用，失败位置按当前段计算。

循环检查分为两层：表类型解析记录访问过的类型，检查 TableRef 引用循环；属性展开使用 `PropertyKey(ownerType, name)` 记录当前递归链。每次递归复制并扩展当前链，避免将兄弟分支重复引用同一属性误判为循环。该机制报告循环，不通过提前发布半成品对象拼接循环元数据。

来源信息用于诊断和导航，需保持以下区分：

| 字段 | 含义 |
| --- | --- |
| `source` | 当前字段或 getter 声明，用于属性引用和本地注解诊断 |
| `mappingSource` | 引用链最终的映射声明；显式关联列指向目标属性，推断目标时可指向关联类型；直接声明列为空 |
| `sourceType` | 声明所在的源码类型，供错误归属使用 |
| `declaringType` | 映射属性应绑定到的类型上下文，可与源码类型不同 |

元数据规则错误产生带 `DiagnosticLocation` 的异常。运行时入口直接抛出；分析入口将已知诊断包装为失败结果，未预期异常继续传播。诊断定位与 IDE 展示的契约见分析 API 文档，元数据层不负责 UI 范围映射。

## 失败语义与并发边界

元数据校验失败时不缓存失败的 `TableInfo`；失败前已经登记的路径和别名允许保留，已完成的依赖声明也可以留在自己的缓存中。重复失败仍应报错，但相同路径不会继续消耗新别名，已有成功结果的属性绑定及既有路径别名保持稳定。

失败后的保留注册可能使后来不同路径采用回退别名，因此测试应验证路径复用和既有结果稳定，不要求别名文本与全新工厂完全相同。

局部 `TableInfo` 及属性映射在构建期间由当前调用维护，完整结果再由缓存发布；共享图会继续积累后续路径。返回模型及集合仍可变，调用方应按只读使用。以上设计不构成整个 MybatisExtAnalyzer、PSI 模型或运行时 Configuration 的并发使用保证。

共享图的路径映射和别名集合分别更新，并发读取两者不保证得到同一时刻的快照。

## 下游消费与职责边界

| 消费方 | 使用的元数据 | 边界 |
| --- | --- | --- |
| `JpaParser` / `ConditionHelper` | 属性路径、过滤信息、实体与投影的底层表身份 | 负责方法语义、条件与参数绑定，消费已解析的实体映射 |
| SQL 脚本生成及 `NestedSelectHelper` | 列、JOIN 依赖、别名和连接条件 | 按前驱依赖收集 JOIN；嵌套查询参数还保留逻辑属性路径和 JDBC 类型 |
| `ResultMapHelper` | ID、RESULT、ASSOCIATION、COLLECTION、Fetch、属性路径及 JDBC 类型 | 构造结果映射；运行时需要 Class 时通过反射桥接获取 |
| ID 与写入处理 | ID 策略、自定义生成器类型、本表列、只读性 | 生成值、执行写入和回写对象属于运行时层，元数据构建不执行这些副作用 |
| `MybatisExtAnalyzer` / IDEA 适配器 | 属性树、来源及带位置的诊断 | 提供导航与分析结果；IDE 不另行实现映射规则 |

`PropertyInfo.equals/hashCode` 当前只比较 `fullName` 和 `columnName`，不能把它当成跨 TableInfo、跨配置的完整类型或映射身份。名称映射的 HashMap 遍历顺序也不作为业务契约；语义一致性需结合最终属性绑定、JOIN 与参数映射验证。

## 类型身份：当前实现与待决策方案

当前元数据缓存、`JoinNode` 和 `PropertyKey` 均直接使用 `JavaType` 的相等性。`computeIfAbsent` 要求键的相等性与哈希稳定，不要求每种类型只能创建一个包装对象。

| 适配实现 | 当前相等性依据 |
| --- | --- |
| `GenericType` | 底层 Class、泛型实参和数组组件类型；Class 身份保留 ClassLoader 差异 |
| `PsiModel.Type` | 所属 PsiModel 实例身份与 PsiType 的相等性 |

当前 `getName()` 对已解析的普通类返回二进制名称，嵌套类使用 `$`，不携带泛型实参；基本类型使用关键字，数组使用组件名称加 `[]`，如 `java.lang.String[]`。`getTypeName()` 也不能直接当作统一完整键：PSI 返回规范类型文本，当前反射实现仍返回 `getName()`。

### 待决策：在受控作用域内采用名称身份

已讨论在同一缓存作用域内，以 `JavaType.getName()` 标识类型声明，减少对包装对象和适配实现身份的依赖，尚未决定实施。前提是该作用域内同一全限定名只对应一个有效类型声明，并继续按工厂、配置和源码快照隔离缓存。当前相等性及缓存键保持不变。

实施前应分别确定以下范围，不能一次替换所有类型比较：

- JOIN 表身份和图缓存：可评估按全限定类型名组织表身份，同时保留完整 JOIN 路径；不改为按物理表名合并。
- 完整属性元数据和通用 JavaType 相等性：属性已完成泛型替换，需要确认是否使用名称加泛型实参及数组结构，或对仅名称键明确限定支持范围。
- 普通具体子类如 `User extends BaseEntity<Long>` 有独立类名；同一泛型类型直接以不同实参参与解析，如 `Entity<Long>` 与 `Entity<String>`，仅名称无法区分，两种场景需要分别验证。
- 同一作用域混用多 ClassLoader 同名定义、多个源码解析环境或不同版本声明时，名称唯一的前提不成立。应明确这些场景是否属于支持范围，不能把当前适配器的隔离能力默认为名称方案仍然具备。

无论是否调整，类型契约测试都应覆盖独立创建的等价包装对象、不同泛型和数组、缓存作用域隔离，以及等价 JOIN 路径的别名复用。相等对象必须哈希一致；不相等对象允许哈希冲突。反射与 PSI 的契约对照比较解析语义，不要求两种适配器的包装对象跨环境相等。

## 代码入口与验证范围

主要实现为 [TableDefFactory](../mybatis-ext/src/main/java/io/github/mybatisext/metadata/TableDefFactory.java)、[TableInfoFactory](../mybatis-ext/src/main/java/io/github/mybatisext/metadata/TableInfoFactory.java)、[JoinGraphFactory](../mybatis-ext/src/main/java/io/github/mybatisext/metadata/JoinGraphFactory.java) 和 [PropertyPath](../mybatis-ext/src/main/java/io/github/mybatisext/metadata/PropertyPath.java)。修改前应同时检查定义扫描、上下文绑定和下游消费，不能只依据单个缓存方法推导整体行为。

| 验证层次 | 现有测试入口与关注点 |
| --- | --- |
| 定义扫描 | `TableDefFactoryTest`：列、内嵌结构、引用覆盖、继承、Cascade 和循环检查 |
| 绑定解析 | `TableInfoFactoryTest`：投影、嵌套来源、父表关联、多级路径、非法别名和生成器验证 |
| 图身份 | `JoinGraphFactoryTest`：底层表共享、相同路径复用、不同路径及别名回退 |
| 并发与生命周期 | `MetadataCacheIsolationTest`：配置隔离、重复失败、并发别名注册、缓存复用和多级 JOIN SQL |
| 类型与适配 | 反射类型测试、`MybatisExtAnalyzerTest`、插件 `CoreContractTest`：泛型与数组、模型解析、反射/PSI 的映射和语义对照 |
| 最终行为 | 核心和集成模块中的 SQL、BoundSql、结果映射及数据库测试，按改动范围验证参数绑定和实际结果 |

此表描述现有验证入口，不表示所有类型身份边界已有直接覆盖。名称身份方案实施前，需补齐它所依赖的契约用例，再决定是否调整实现。
