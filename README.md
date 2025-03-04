# MyBatis-Ext

MyBatis-Ext 是一个基于 MyBatis 的扩展插件，旨在简化和增强 MyBatis 的使用体验，提供更多便捷的特性和功能。

## 特征

- **强大的元数据表达能力** - 可以表达任意复杂的关联关系和层次结构，支持多表连接、继承关系和嵌套对象

- **方法名CRUD** - 根据符合规范的方法名自动生成CRUD方法，无需手动编写SQL

- **轻量级设计** - 除了依赖MyBatis核心库，没有其他第三方库依赖

- **无侵入性** - 添加或移除插件不会影响原有的代码逻辑，完全兼容现有MyBatis项目

## 集成

建议直接通过spring boot的方式集成，以便达到开箱即用的效果。

通过spring boot方式集成

```xml
<dependency>
    <groupId>io.github.mybatis-ext</groupId>
    <artifactId>mybatis-ext-spring-boot-starter</artifactId>
    <version>最新版本</version>
</dependency>
```

通过spring mvc方式集成，配置参考[applicationContext.xml](mybatis-ext-spring\src\test\resources\applicationContext.xml)

```xml
<dependency>
    <groupId>io.github.mybatis-ext</groupId>
    <artifactId>mybatis-ext-spring</artifactId>
    <version>最新版本</version>
</dependency>
```

不依赖spring方式集成，参考[MybatisExtTest.java](mybatis-ext-test\src\test\java\io\github\mybatisext\test\MybatisExtTest.java)

```xml
<dependency>
    <groupId>io.github.mybatis-ext</groupId>
    <artifactId>mybatis-ext</artifactId>
    <version>最新版本</version>
</dependency>
```

## 用法

### 定义元数据

插件使用注解的方式描述元数据，支持表达任意复杂的关联关系和嵌套结构。

- `@Table` - 描述实体类对应的表，如果没有指定表名，则使用类名的下划线小写形式作为表名

- `@Column` - 描述属性对应的列，如果属性是列表则对应多个数据行，如果属性是对象则对应多个列；如果没有指定列名，则使用属性名的下划线小写形式作为列名

- `@Id` - 描述属性对应的列为主键，指定主键生成的方式，需和`@Column`一起使用

- `@JoinRelation` - 描述关联关系，表示属性是关联表或关联列，多个`@JoinRelation`一起使用可以表示关联路径

- `@JoinParent` - 描述实体类对应的表和父类对应的表之间是关联关系

- `@EmbedParent` - 将父类中的属性嵌入到实体类中作为实体类中的属性

其他辅助功能注解：

- `@TableRef` - 注解DTO类，并描述DTO类对应的实体类

- `@ColumnRef` - 注解DTO属性，并描述DTO属性对应的实体类属性

- `@Filterable` - 注解实体类属性、DTO属性、CRUD方法参数，可以指定被注解元素在过滤时的行为

- `@MapTable` - 如果不想继承CRUD接口，可以用`@MapTable`注解自定义的CRUD接口，类似于继承`ExtMapper`

### 通用CRUD方法

插件提供通用CRUD接口 BaseMapper，继承可直接获得通用CRUD方法。

BaseMapper 默认提供以下方法：

- `int save(T entity)` - 保存

- `int saveIgnoreNull(T entity)` - 保存，并且忽略null

- `int saveBatch(List<T> list)` - 批量保存

- `int saveBatchIgnoreNull(List<T> list)` - 批量保存，并且忽略null

- `int update(@OnlyById T entity)` - 按照ID更新

- `int updateIgnoreNull(@OnlyById T entity)` - 按照ID更新，并且忽略null

- `int updateBatch(@OnlyById List<T> list)` - 批量按照ID更新

- `int updateBatchIgnoreNull(@OnlyById List<T> list)` - 批量按照ID更新，并且忽略null

- `int delete(@OnlyById T query)` - 按照ID删除

- `int deleteBatch(@OnlyById List<T> query)` - 批量按照ID删除

- `T get(@OnlyById T query)` - 按照ID获取

- `List<T> list(T query)` - 按照实体查询，可用@Filterable指定过滤的行为

- `List<T> list(T query, RowBounds rowBounds)` - 按照实体分页查询，可用@Filterable指定过滤的行为，兼容pagehelper

- `long count(T query)` - 按照实体查询总数，可用@Filterable指定过滤的行为

- `boolean exists(T query)` - 按照实体查询是否存在，可用@Filterable指定过滤的行为

### 方法名CRUD

除了 BaseMapper 默认提供的方法，用户还可以按照一定的命名规范自定义CRUD方法，且无需手动编写SQL，事实上 BaseMapper 中默认提供的方法都是因为满足命名规范自动生成的。

除了继承 BaseMapper，用户还可以直接继承 ExtMapper 或者在 Mapper 上注解`@MapTable`，这样用户可以获得一个没有默认CRUD方法的CRUD接口。

以下是方法名CRUD的规则：

#### 查询方法

命名规则：`(find|select|list|get)[Distinct][All|One|(Top(<integer>|<variable>))][<propertyList>][(By|Where)<conditionList>][<groupBy>[<having>]][<orderBy>][<limit>]`

其中：

- `integer` - 整数

- `variable` - 变量，来自于方法的参数，支持`Dot`语法获取参数内部的属性

- `propertyList` - 属性列表，命名规则`<property>And<propertyList>`

- `property` - 属性，来自于实体类或DTO类中对应列的属性，支持`Dot`语法获取属性内部的属性

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
  - `<property>[Ignorecase][Not]Between[<variable>To<variable>]` - 范围
  - `<property>[Ignorecase][Not]In[<variable>]` - 枚举
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

#### 更新

命名规则：`(update|modify)[Batch][IgnoreNull][(By|Where)<conditionList>]`

#### 删除

命名规则：`(delete|remove)[Batch][(By|Where)<conditionList>]`

#### 保存

命名规则：`(save|insert)[Batch][IgnoreNull]`

## 贡献与支持

欢迎提交 Issue 或 Pull Request。
