# MyBatis-Ext

MyBatis-Ext 是一个 MyBatis 增强插件，支持基于注解的元数据映射、通用 CRUD 和根据方法名自动生成 SQL，并提供配套 IDEA 插件。

## 特性

- **注解映射** - 支持多表连接、继承映射和嵌套对象

- **方法名CRUD** - 根据符合规范的方法名自动生成CRUD操作，无需手动编写SQL

- **[IDEA 插件](mybatis-ext-idea-plugin/README.md)** - 提供方法名 CRUD 的诊断、补全和 SQL 预览，以及 MyBatis XML 的导航与补全

- **轻量级设计** - 依赖精简，内置字节码增强组件，减少依赖冲突

- **无感增强** - 在原有的Spring Boot+MyBatis项目中引入本插件starter，无需修改配置即可获得增强能力

- **兼容已有映射** - 可与已有 MyBatis XML 和 SQL 注解映射共同使用

- **多数据库方言支持** - 内置支持MySQL、Oracle、DM（达梦）、H2等多种数据库方言，可自定义扩展

## 模块说明

MyBatis-Ext 项目由以下几个主要模块组成：

- **mybatis-ext** - 核心模块，提供基础功能实现
- **mybatis-ext-spring** - Spring集成模块
- **mybatis-ext-spring-boot-starter** - Spring Boot starter，提供自动配置
- **mybatis-ext-idea-plugin** - IDEA 插件，提供 MyBatis XML 的诊断、导航和补全，以及实体映射检查与方法名 CRUD 诊断、补全

## 集成方式

建议通过Spring Boot方式集成，以获得开箱即用的体验。

### 通过Spring Boot方式集成

```xml
<dependency>
    <groupId>io.github.mybatis-ext</groupId>
    <artifactId>mybatis-ext-spring-boot-starter</artifactId>
    <version>最新版本</version>
</dependency>
```

### 通过 Spring 方式集成

```xml
<dependency>
    <groupId>io.github.mybatis-ext</groupId>
    <artifactId>mybatis-ext-spring</artifactId>
    <version>最新版本</version>
</dependency>
```

配置参考 [applicationContext.xml](mybatis-ext-spring/src/test/resources/applicationContext.xml)

### 与其他MyBatis扩展框架集成

MyBatis-Ext 可以与其他流行的MyBatis扩展框架集成使用：

#### 与 MyBatis-Plus 集成

参考 [mybatis-plus-test](mybatis-plus-test) 模块

#### 与 MyBatis-Flex 集成

参考 [mybatis-flex-test](mybatis-flex-test) 模块

## 配置属性

在Spring Boot环境中，可以通过以下属性进行配置：

```properties
# 是否启用MyBatis-Ext (默认: true)
mybatis-ext.enabled=true

# 未标注 @Filterable 的实体属性是否默认参与条件生成 (默认: true)
mybatis-ext.default-filterable=true
```

## 使用指南

### 快速入门

引入依赖并完成 MyBatis 数据源和 Mapper 扫描配置后，即可定义实体和 Mapper。假设数据库中已有 `demo_user` 表，包含 `id` 和 `name` 两列。

实体类 `User`：

```java
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

@Table(name = "demo_user")
public class User {

    @Id
    @Column
    public Long id;

    @Column
    public String name;
}
```

Mapper 继承 `BaseMapper`，同时声明按方法名生成 SQL 的查询：

```java
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import io.github.mybatisext.mapper.BaseMapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    List<User> listByNameOrderById(String name);
}
```

获取 `UserMapper` Bean 后，可调用通用方法或自定义方法，无需编写对应的 XML：

```java
User query = new User();
query.name = "Alice";
List<User> users = userMapper.list(query);
List<User> orderedUsers = userMapper.listByNameOrderById("Alice");
```

默认配置下，`list(query)` 仅使用非空的 `name` 生成等值条件；自定义方法则根据方法名按 `name` 查询并按 `id` 升序排列。

### 实体与映射配置

插件使用注解描述表、列、关联关系和嵌套结构：

- `@Table` - 描述实体类对应的表，如未指定表名，则使用类名的下划线小写形式作为表名

- `@Column` - 描述属性对应的列，若属性是列表则对应多个数据行，若属性是对象则对应多个列；如未指定列名，则使用属性名的下划线小写形式作为列名

- `@Id` - 描述属性对应的主键列，指定主键生成方式，需与`@Column`一起使用

- `@TableRef` - 注解DTO类，并描述该DTO类对应的实体类

- `@ColumnRef` - 注解DTO属性，并描述该DTO属性对应的实体类属性

- `@JoinRelation` - 描述关联关系，表示属性是关联表或关联列，多个`@JoinRelation`一起使用可表示关联路径

- `@JoinParent` - 描述实体类对应的表和父类对应的表之间的关联关系

- `@Cascade` - 级联，用以获取关联属性的关联属性，需与`@JoinRelation`一起使用

- `@Fetch` - 指定关联属性的加载策略，不指定时使用JOIN方式加载，需与`@JoinRelation`一起使用

- `@Filterable` - 注解实体类属性、DTO属性或CRUD方法参数，可指定被注解元素在过滤时的行为

其他辅助功能注解：

- `@MapTable` - 如不想继承CRUD接口，可用`@MapTable`注解自定义的CRUD接口，类似于继承`ExtMapper`

- `@OnlyById` - 标记参数只使用ID属性作为过滤条件

### 查询条件与空值处理

`list(query)`、`count(query)` 等方法根据实体属性自动生成条件时，遵循以下规则：

- `mybatis-ext.default-filterable=true`：未标注 `@Filterable` 的普通属性默认在值非空时参与等值比较，多个条件以 `AND` 连接。
- `mybatis-ext.default-filterable=false`：未标注 `@Filterable` 的属性不参与自动条件生成，需通过注解显式指定。
- 属性上的 `@Filterable` 优先于默认设置，可指定比较方式和条件生效规则；默认 `test=TestMode.NotNull`，`test=TestMode.False` 可排除该属性。

方法名中显式声明的条件按命名规则生成，不受上述开关影响，也不会因参数为 `null` 自动省略。例如 `listByNameOrderById(null)` 仍保留 `name = ?` 条件；查询空值应使用 `IsNull`。`@OnlyById` 参数按主键匹配，同样不会因主键为 `null` 而省略主键条件。

动态过滤条件全部被省略时，可能不生成 `WHERE` 子句；使用此类条件更新或删除数据时，应确认实际生效的过滤范围。

### 通用CRUD方法

插件提供通用CRUD接口 BaseMapper，继承后可直接获得通用CRUD方法。

BaseMapper 默认提供以下方法：

- `int save(T entity)` - 保存实体

- `int saveIgnoreNull(T entity)` - 保存实体，忽略null值字段

- `int saveBatch(List<T> list)` - 批量保存实体

- `int saveBatchIgnoreNull(List<T> list)` - 批量保存实体，忽略null值字段

- `int update(@OnlyById T entity)` - 按ID更新实体

- `int updateIgnoreNull(@OnlyById T entity)` - 按ID更新实体，忽略null值字段

- `int updateBatch(@OnlyById List<T> list)` - 批量按ID更新实体

- `int updateBatchIgnoreNull(@OnlyById List<T> list)` - 批量按ID更新实体，忽略null值字段

- `int delete(@OnlyById T query)` - 按ID删除实体

- `int deleteBatch(@OnlyById List<T> query)` - 批量按ID删除实体

- `T get(@OnlyById T query)` - 按ID获取实体

- `List<T> list(T query)` - 按实体条件查询，可用@Filterable指定过滤行为

- `List<T> list(T query, RowBounds rowBounds)` - 按实体条件查询并传递分页参数，可用@Filterable指定过滤行为，可配合 PageHelper 使用，详见[分页](#分页)

- `long count(T query)` - 按实体条件统计总数，可用@Filterable指定过滤行为

- `boolean exists(T query)` - 按实体条件检查记录是否存在，可用@Filterable指定过滤行为

### 分页

MyBatis-Ext 支持通过方法名中的 `Top`、`Limit` 限制查询范围，但不内置分页插件或自动总数查询。需要分页及总记录数时，可以配合 [PageHelper](https://github.com/pagehelper/Mybatis-PageHelper) 使用。在项目中引入并配置 PageHelper 后，可通过 `BaseMapper.list(T query, RowBounds rowBounds)` 重载传入 PageHelper 的 `PageRowBounds`：

```java
import com.github.pagehelper.PageRowBounds;

SysUser query = new SysUser();
PageRowBounds rowBounds = new PageRowBounds(0, 20);
List<SysUser> users = sysUserMapper.list(query, rowBounds);
Long total = rowBounds.getTotal();
```

以上示例按 PageHelper 默认配置查询偏移量为 `0`、最多 `20` 条的数据，并通过 `getTotal()` 获取总记录数。分页 SQL 改写及总数查询由 PageHelper 负责，具体行为以 PageHelper 配置为准。

### 方法名CRUD

除了 BaseMapper 默认提供的方法外，用户还可以按照特定命名规范自定义CRUD方法，无需手动编写SQL。事实上，BaseMapper 中默认提供的方法都是通过满足命名规范自动生成的。

除了继承 BaseMapper，用户也可以直接继承 ExtMapper 或在 Mapper 上使用`@MapTable`注解，获得一个不含默认CRUD方法的自定义接口。

以下是方法名CRUD的规则：

#### 查询方法

命名规则：`(find|select|list|get)[Distinct][All|One|(Top(<integer>|<variable>))][<propertyList>|<returnTypeName>][(By|Where)<conditionList>][<groupBy>[<having>]][<orderBy>][<limit>]`

其中：

- `integer` - 整数

- `variable` - 变量，来自方法参数，支持`Dot`语法获取参数内部属性

- `propertyList` - 属性列表，命名规则`<property>(And<property>)*`

- `returnTypeName` - 返回值实体类型名称

- `property` - 属性，来自实体类或DTO类中对应列的属性，支持`Dot`语法获取内部属性

- `conditionList` - 条件表达式，命名规则 `<condition>((And|Or)<condition>)*`；`And` 优先于 `Or`

- `condition` - 条件，命名规则如下
  - `<property>[Ignorecase][Not]` - 等值比较
  - `<property>[Ignorecase][Not]Is[<variable>]` - 等值比较
  - `<property>[Ignorecase][Not]Equals[<variable>]` - 等值比较
  - `<property>[Ignorecase][Not]LessThan[<variable>]` - 小于
  - `<property>[Ignorecase][Not]LessThanEqual[<variable>]` - 小于等于
  - `<property>[Ignorecase][Not]GreaterThan[<variable>]` - 大于
  - `<property>[Ignorecase][Not]GreaterThanEqual[<variable>]` - 大于等于
  - `<property>[Ignorecase][Not]Like[<variable>]` - 模糊匹配
  - `<property>[Ignorecase][Not]StartWith[<variable>]` - 以模式开头匹配
  - `<property>[Ignorecase][Not]EndWith[<variable>]` - 以模式结尾匹配
  - `<property>[Ignorecase][Not]Between[<variable>To<variable>]` - 范围查询
  - `<property>[Ignorecase][Not]In[<variable>]` - 枚举查询
  - `<property>[Not]IsNull` - 是否为空
  - `<property>[Not]IsNotNull` - 是否非空
  - `<property>[Not]IsTrue` - 是否为真
  - `<property>[Not]IsFalse` - 是否为假

- `groupBy` - 分组子句，命名规则`GroupBy<propertyList>`

- `having` - 分组条件子句，命名规则`Having<conditionList>`

- `orderBy` - 排序子句，命名规则`OrderBy<orderByList>`

- `orderByList` - 排序列表，命名规则`<property>[Asc|Desc][And<orderByList>]`

- `limit` - 查询范围限制子句，命名规则`Limit((<integer>|<variable>)To(<integer>|<variable>)|(<integer>|<variable>))`

#### 查询是否存在

命名规则：`exists[(By|Where)<conditionList>]`

#### 查询数量

命名规则：`count[(By|Where)<conditionList>]`

#### 更新操作

命名规则：`(update|modify)[Batch][<propertyList>][IgnoreNull][(By|Where)<conditionList>]`

#### 删除操作

命名规则：`(delete|remove)[Batch][(By|Where)<conditionList>]`

#### 保存操作

命名规则：`(save|insert)[Batch][IgnoreNull]`

#### 方法重载与空参数

重载方法对应 SQL 的选择基于生成脚本的 OGNL 表达式实现，根据参数名称和值的类型选择对应的 SQL。若参数名相同、仅类型不同，传入 `null` 时可能无法区分，例如：

```java
long count(@Param("id") Long id);
long count(@Param("id") String id);
```

即使传入 `(Long) null`，XML 仍只能获得空值，无法得知调用了哪个重载，可能匹配多个不同的 SQL 分支并导致 SQL 错误。

建议为不同查询意图使用不同的方法名，如 `countById`、`countByName`，避免仅靠可空参数的类型区分重载。

### 数据库方言支持

MyBatis-Ext 提供了多种数据库方言支持，以适应不同的数据库环境：

- MySQL
- Oracle
- PostgreSQL
- SQL Server 2012及以上（SQL Server 2008可使用 `SqlServer2008Dialect`）
- DM（达梦）
- H2
- 其他（可通过实现 `Dialect` 接口扩展）

插件会根据数据源的 JDBC URL 自动选择合适的方言。也可以通过自定义 `DialectSelector` 实现来覆盖默认行为。
`jdbc:sqlserver:` 默认选择 `SqlServerDialect`；连接 SQL Server 2008 时需要通过自定义 `DialectSelector` 返回 `SqlServer2008Dialect`。

## 实现原理

MyBatis-Ext 根据实体元数据和方法名解析结果，在运行时生成 MyBatis `MappedStatement`。运行时与 IDEA 插件共用元数据及方法名 CRUD 分析逻辑，使编辑期诊断、补全与运行时行为保持一致。详见[分析 API 与适配契约](docs/analysis.md)。

## 开发与构建

在 IDEA 中导入根目录的 `settings.gradle.kts`，使用 Gradle Wrapper 和 JDK 17 以上的构建环境。核心及 Spring 模块目标为 Java 8；IDEA 插件基于 2023.3.8 SDK，以 Java 17 编译和测试。

```powershell
.\gradlew.bat spotlessApply spotlessCheck
.\gradlew.bat check :mybatis-ext-idea-plugin:verifyPluginStructure :mybatis-ext-idea-plugin:buildPlugin
```

环境配置、IDE 导入、兼容验证及交付包生成见[构建说明](docs/build.md)。功能变化见[变更记录](docs/changelog.md)。

## 贡献与支持

欢迎提交 Issue 或 Pull Request 来完善本项目。

## 许可证

MyBatis-Ext 使用 [MIT 许可证](LICENSE)。
