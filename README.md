# MyBatis-Ext

MyBatis-Ext 是一个基于 MyBatis 的扩展插件，旨在简化和增强 MyBatis 的使用体验，提供更多便捷的特性和功能。

## 特性

- **强大的元数据表达能力** - 可以表达任意复杂的关联关系和层次结构，支持多表连接、继承关系和嵌套对象

- **方法名CRUD** - 根据符合规范的方法名自动生成CRUD操作，无需手动编写SQL

- **轻量级设计** - 除了依赖MyBatis核心库外，没有其他第三方库依赖

- **无侵入性** - 添加或移除插件不会影响原有的代码逻辑，完全兼容现有MyBatis项目

- **多数据库方言支持** - 内置支持MySQL、Oracle、DM（达梦）、H2等多种数据库方言，可自定义扩展

## 项目架构

MyBatis-Ext 项目由以下几个主要模块组成：

- **mybatis-ext** - 核心模块，提供基础功能实现
- **mybatis-ext-spring** - Spring集成模块
- **mybatis-ext-spring-boot-starter** - Spring Boot starter，提供自动配置

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

### 通过Spring MVC方式集成

配置参考 [applicationContext.xml](mybatis-ext-spring/src/test/resources/applicationContext.xml)

```xml
<dependency>
    <groupId>io.github.mybatis-ext</groupId>
    <artifactId>mybatis-ext-spring</artifactId>
    <version>最新版本</version>
</dependency>
```

在Spring XML配置中，需要使用 `ExtSqlSessionFactoryBean` 替代原生的 `SqlSessionFactoryBean`：

```xml
<bean id="sqlSessionFactory" class="io.github.mybatisext.spring.ExtSqlSessionFactoryBean">
    <property name="dataSource" ref="dataSource" />
    <property name="mapperLocations" value="${mybatis.mapper-locations}" />
</bean>

<bean id="mybatisExtBeanPostProcessor" class="io.github.mybatisext.spring.MybatisExtBeanPostProcessor">
</bean>

<bean id="mapperMethodValidator" class="io.github.mybatisext.spring.MapperMethodValidator">
    <constructor-arg>
        <list>
            <ref bean="sqlSessionFactory" />
        </list>
    </constructor-arg>
</bean>
```

### 不依赖Spring的集成方式

参考 [MybatisExtTest.java](mybatis-ext-test/src/test/java/io/github/mybatisext/test/MybatisExtTest.java)

```xml
<dependency>
    <groupId>io.github.mybatis-ext</groupId>
    <artifactId>mybatis-ext</artifactId>
    <version>最新版本</version>
</dependency>
```

Java代码集成示例：

```java
// 创建数据源
DataSource dataSource = ...;
TransactionFactory transactionFactory = new JdbcTransactionFactory();
Environment environment = new Environment("development", transactionFactory, dataSource);

// 使用ExtConfiguration替代原生Configuration
Configuration configuration = ConfigurationFactory.create(environment, new ExtContext());

// 注册Mapper
configuration.addMapper(YourMapper.class);
((ConfigurationInterface) configuration).validateAllMapperMethod(); // 验证所有映射方法

// 创建会话工厂
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
```

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

# 默认是否启用过滤 (默认: true)
mybatis-ext.default-filterable=true
```

## 使用指南

### 定义元数据

插件使用注解方式描述元数据，支持表达任意复杂的关联关系和嵌套结构：

- `@Table` - 描述实体类对应的表，如未指定表名，则使用类名的下划线小写形式作为表名

- `@Column` - 描述属性对应的列，若属性是列表则对应多个数据行，若属性是对象则对应多个列；如未指定列名，则使用属性名的下划线小写形式作为列名

- `@Id` - 描述属性对应的主键列，指定主键生成方式，需与`@Column`一起使用

- `@JoinRelation` - 描述关联关系，表示属性是关联表或关联列，多个`@JoinRelation`一起使用可表示关联路径

- `@JoinParent` - 描述实体类对应的表和父类对应的表之间的关联关系

- `@EmbedParent` - 将父类中的属性嵌入到实体类中作为实体类的属性

其他辅助功能注解：

- `@TableRef` - 注解DTO类，并描述该DTO类对应的实体类

- `@ColumnRef` - 注解DTO属性，并描述该DTO属性对应的实体类属性

- `@Filterable` - 注解实体类属性、DTO属性或CRUD方法参数，可指定被注解元素在过滤时的行为

- `@MapTable` - 如不想继承CRUD接口，可用`@MapTable`注解自定义的CRUD接口，类似于继承`ExtMapper`

- `@IfTest` - 条件测试注解，可用于动态指定SQL条件

- `@OnlyById` - 标记参数只使用ID属性作为过滤条件

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

- `List<T> list(T query, RowBounds rowBounds)` - 按实体条件分页查询，可用@Filterable指定过滤行为，兼容PageHelper

- `long count(T query)` - 按实体条件统计总数，可用@Filterable指定过滤行为

- `boolean exists(T query)` - 按实体条件检查记录是否存在，可用@Filterable指定过滤行为

### 方法名CRUD

除了 BaseMapper 默认提供的方法外，用户还可以按照特定命名规范自定义CRUD方法，无需手动编写SQL。事实上，BaseMapper 中默认提供的方法都是通过满足命名规范自动生成的。

除了继承 BaseMapper，用户也可以直接继承 ExtMapper 或在 Mapper 上使用`@MapTable`注解，获得一个不含默认CRUD方法的自定义接口。

以下是方法名CRUD的规则：

#### 查询方法

命名规则：`(find|select|list|get)[Distinct][All|One|(Top(<integer>|<variable>))][<propertyList>][(By|Where)<conditionList>][<groupBy>[<having>]][<orderBy>][<limit>]`

其中：

- `integer` - 整数

- `variable` - 变量，来自方法参数，支持`Dot`语法获取参数内部属性

- `propertyList` - 属性列表，命名规则`<property>And<propertyList>`

- `property` - 属性，来自实体类或DTO类中对应列的属性，支持`Dot`语法获取内部属性

- `conditionList` - 条件列表，命名规则`<condition>(And|Or)<conditionList>`

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

- `limit` - 分页子句，命名规则`Limit((<integer>|<variable>)To(<integer>|<variable>)|(<integer>|<variable>))`

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

### 数据库方言支持

MyBatis-Ext 提供了多种数据库方言支持，以适应不同的数据库环境：

- MySQL
- Oracle
- PostgreSQL
- DM（达梦）
- H2
- 其他（可通过实现 `Dialect` 接口扩展）

插件会根据数据源的 JDBC URL 自动选择合适的方言。也可以通过自定义 `DialectSelector` 实现来覆盖默认行为。

## 实现原理

MyBatis-Ext 通过以下机制实现其核心功能：

1. **配置替换** - 使用 `ExtConfiguration` 替换原本的 MyBatis `Configuration` 类
2. **方法解析** - 通过 `JpaParser` 解析符合命名规则的方法
3. **元数据分析** - 使用 `TableInfoFactory` 分析实体类的元数据信息
4. **动态生成语句** - 在运行时动态生成 `MappedStatement`，无需提前定义XML
5. **Bean后处理** - 通过 Spring 的 `BeanPostProcessor` 机制自动增强 `SqlSessionFactory`

## 贡献与支持

欢迎提交 Issue 或 Pull Request 来完善本项目。

## 许可证

MyBatis-Ext 使用开源许可证，详情请查看项目许可文件。
