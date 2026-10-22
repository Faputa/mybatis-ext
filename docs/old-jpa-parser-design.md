# 方法名 CRUD 解析器设计

> 本文更多是围绕方法名 CRUD 解析的思想实验，记录文法、解析器组合和回退机制的设计设想，不代表当前实现。

本文中的 JPA 指方法名 CRUD 的解析设计；文法与方法签名中的占位写法用于表达思路，并非可直接编译的 Java 代码。

## 解析流程

- 词法分析
- 语法分析
- 语义生成

## 词法分类

- 关键词 keyword
- 名词 noun
    - 来源元数据属性
- 数字 num
- 变量 variable
    - 来源参数
    - 可能存在歧义

## 句子成分

- 动词 verb
- 主语 subject
- 谓词 predicate
- 修饰词 qualifier

## CRUD 语义组成

| 操作 | 语义组成 |
| --- | --- |
| 查询 | 查询项、目标表、关联表、条件、分组、排序、范围 |
| 修改 | 修改列、目标表、关联表、条件 |
| 创建 | 插入列、插入值 |
| 删除 | 目标表、关联表、条件 |

## 方法名文法草案

```text
<grammar>=
    (find|select|list|get)[Distinct][All|One|Top(<integer>|<variable>)][(By|Where)<conditionList>][<modifierList>]
    |exists[(By|Where)<conditionList>][<modifierList>]
    |count[(By|Where)<conditionList>][<modifierList>]
    |(update|modify)[Batch][IgnoreNull][(By|Where)<conditionList>]
    |(delete|remove)[Batch][(By|Where)<conditionList>]
    |(save|insert)[Batch]

<conditionList>=
    <condition>[(And|Or)<conditionList>)]

<condition>=
    <property>
        [Ignorecase][Not][(Is[<variable>])
            |(Equals[<variable>])
            |(LessThan[<variable>])
            |(LessThanEqual[<variable>])
            |(GreaterThan[<variable>])
            |(GreaterThanEqual[<variable>])
            |(Like[<variable>])
            |(StartWith[<variable>])
            |(EndWith[<variable>])
            |(Between[<variable>To<variable>])
            |(In[<variable>])]
        |[Not]
            IsNull
            |IsNotNull
            |IsTrue
            |IsFalse

<modifierList>=
    <modifier>[<modifierList>]

<modifier>=
    OrderBy<propertyList>[Asc|Desc]
    |GroupBy<propertyList>
    |Limit(<integer>|<variable>)To(<integer>|<variable>)
    |Limit(<integer>|<variable>)

<propertyList>=
    <property>[And<propertyList>]

<property>=
    <propertyName>(Dot<propertyName>)*
```

## 参数对应与语法可读性

- 方法名单词与参数的对应关系：同时支持按位置和按名字匹配。
- 文法的可读性：通过类似定义文法的方式组织程序，使阅读代码的人可以理解语法结构。

## 布尔表达式与 Lambda 方案比较

- ×考虑布尔表达式
    - 返回bool的函数表示符号
    - 返回true表示匹配成功
    - 返回false表示匹配失败
    - ||表示选择
    - &amp;&amp;表示连接
    - &amp;&amp;true表示可选
- 问题
    - 无法设置状态还原点
- √考虑lambda表达式
    - 返回bool的lambda表达式表示符号
    - 返回lambda表达式的函数表示元符号

## 通用解析框架与 JPA 解析器

- 通用解析器框架
    - 元符号
        - choice表示选择
        - join表示连接
        - optional表示可选
        - star表示任意次匹配
        - plus表示至少一次匹配
        - count表示匹配指定次数
        - assign表示赋值
    - 上下文接口
        - save保存状态
        - restore还原状态
            - 考虑通过传参的方式保存和还原状态
        - next读取单词
            - 考虑歧义
        - &lt;T&gt;set设置变量
        - &lt;T&gt;get获取变量
- JPA解析器
    - 元符号
        - keyword表示关键字
        - &lt;action&gt;执行代码
    - 符号
        - sentence表示语句
        - noun表示名词
        - num表示数字
        - end表示结束
        - variable表示变量
        - verb表示动词
        - subject表示主语
        - predicate表示谓词
        - qualifier表示修饰词
    - 上下文实现

## 歧义处理

- tokenizer负责记录单词序列
- 将歧义项压栈
- 如果存在歧义则再次分析
- 如果存在多个合法的语义则报错

## 文法表达示例：四则运算

```text
    <expr>::=<term>{("+"|"-")<term>}
    <term>::=<factor>{("*"|"/")<factor>}
    <factor>::=<integer>|"("<expr>")"
    <integer>::=<digit><integer>|<digit>
    <digit>::="0"|"1"|"2"|"3"|"4"|"5"|"6"|"7"|"8"|"9"
```

## CPS 风格的 Symbol 结构

- 支持先初始化后设置continuation
    - 基于状态的方式实现，设置对象的continuation
    - 或者基于函数的方式实现，调用match的时候传入continuation
- 考虑join(choice(A,B),C)
    - 符号的continuation只能由符号自己调用
    - continuation是调用后续符号的函数
- 考虑match和continuation的关系
    - continuation，必须支持赋值
    - match，默认调用matchSelf和continuation
    - matchSelf，考虑支持赋值

## 基于状态的 CPS 组合规则

```text
choice
    创建一个新符号
        遍历参数，任意匹配成功返回true，否则返回false
    将每个参数的continuation设置为对新符号continuation的调用
    返回新符号
join
    创建一个新符号
        匹配第一个参数，返回结果
    将每个参数的continuation设置为对后面参数的调用
    将最后参数的continuation设置为对新符号continuation的调用
    返回新符号
optional
    创建一个新符号
        匹配参数，成功返回true，否则返回调用continuation的结果
    将参数的continuation设置为对新符号continuation的调用
    返回新符号
star
    创建一个新符号
        匹配参数，成功返回true，否则返回调用continuation的结果
    将参数的continuation设置为对参数的调用和对新符号continuation的调用
        先调用参数，如果返回false则调用新符号continuation
    返回新符号
plus
    创建一个新符号
        匹配参数，返回结果
    将参数的continuation设置为对参数的调用和对新符号continuation的调用
        先调用参数，如果返回false则调用新符号continuation
count
    创建一个新符号
        匹配符号参数，检查计数器和参数结果，如果计数器+参数结果等于给定值则返回true，否则返回false
    将参数的continuation设置为对参数的调用和对新符号continuation的调用
        如果计数器小于给定值则调用参数，否则调用新符号continuation
    返回新符号
assign
    创建一个新符号
        匹配参数，返回结果
    将参数的continuation设置为赋值和对新符号continuation的调用
        先赋值，然后调用新符号continuation
    返回新符号
```

## 基于函数的 CPS 组合规则

```text
    match形式为boolean match(state, continuation)
choice
    choice(a,b)%=a%|b%
join
    join(a)%=a%
    join(a,b)%=a(b%)
    join(a,b,c)%=a(join(b,c)%)
optional
    optional(a)%=a%|%
star
    star(a)%=a(star(a)%)|%
plus
    plus(a)%=a(star(a)%)
    或者
    plus(a)%=a(plus(a)%|%)
count
    count(a,1)%=a%
    count(a,n)%=a(count(a,n-1)%)
assign
    assign(a)%=a%
```

## 状态与值的传递问题

- 获取值
    - 获取匹配文本
    - 获取设置的值
    - 如何定位？（重点）
        - 位置
        - 名字
        - 作用域
        - 上下文
- 设置值
    - 如何设置值
- 返回值
    - 如何默认传递返回值？（问题）

## 可回退的 State

- 考虑记录父节点state和上一个节点state
- 后面节点不允许修改前面节点的state
    - continuation只能修改传入的state
- state本质上是可回退的ast吗？
    - 是表达式树

## 符号匹配的值语义

- 有没有必要呢？
    - 需要重新设计 JPA 语法

## 匹配结果与作用域

- 匹配到的字符串
- 匹配到的值，符号每次匹配都会产生一个值
- 每次对非终结符匹配都会产生一个作用域

## 只有非终结符产生值的备选方案

- assign无法定义符号的值
    - assign的意义，可以准确地取到设置的值

## 值的定位与访问

- 作用域+符号+索引
- 考虑所有符号匹配后都可以产生值
    - get(作用域,符号,索引)
    - getLocal(符号,索引)=get(this.作用域,符号,索引)
    - getLocal(符号)=getLocal(符号,0)
    - set(作用域,符号,值)
    - set(作用域,名字,值)
    - setLocal(符号,值)=set(this.作用域,符号,值)
    - setLocal(名字,值)=set(this.作用域,名字,值)
    - return(值)=set(this.作用域.父作用域,this.作用域.符号,值)

## 手动返回值

- 调用return

## 默认返回值

- 如果非终结符的值没有设置，则将传入符号的值设置为非终结符的值

## 元符号求值规则

- choice
    - 值为匹配到的参数的值
- join
    - 值为最后一个符号的值
- optional
    - 值为参数匹配到的值或默认值
- star
    - 值为参数最后一次匹配的值或默认值
- plus
    - 值为参数最后一次匹配的值
- count
    - 值为参数最后一次匹配的值
- assign
    - 值为参数的值
    - 在当前作用域中为值注册名字

## 匹配与 Continuation 签名

```text
match
    (state, result, continuation) -> boolean

continuation
    (state, result) -> boolean
```

## 属性与参数的匹配方式

- 变量名匹配
- 属性名匹配
- 索引匹配
- 类型匹配？

## 特殊参数的跳过规则

```java
private static boolean isSpecialParameter(Class<?> clazz) {
    return RowBounds.class.isAssignableFrom(clazz) || ResultHandler.class.isAssignableFrom(clazz);
}
```

## 指定更新字段的签名设想

```text
update(Model)
update(Model, @Param id)
updateById(Model)
updateById(Model, @Param id)
updateName(Model)
updateNameById(Model, @Param id)
updateNameById(@Param name, @Param id)

update(List<Model>)
update(List<Model>, @Param id)
updateById(List<Model>)
updateById(List<Model>, @Param id)
updateName(List<Model>)
updateNameById(List<Model>, @Param id)
updateNameById(@Param List<name>, @Param id)

PASS
updateNameAndEmailById(@Param name, @Param email, @Param id)
ERROR
updateNameAndEmailById(@Param List<name>, @Param email, @Param id)
ERROR
updateNameAndEmailById(@Param List<name>, @Param List<email>, @Param id)
```
