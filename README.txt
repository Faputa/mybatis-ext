背景
    mybatis作为Java最流行的两大ORM框架之一，具有轻量级的特点，但是同时使用起来也相对繁琐，哪怕是及其简单的功能也需要在xml或注解中定义sql。因此有一种思路是基于mybatis再封装成一层ORM框架，补强mybatis不具备的功能，典型实现有mybatis-plus和mybatis-flex。
    mybatis-plus和mybatis-flex都通过重写mybatis部分代码的方式实现了对mybatis的增强，某种意义上可以理解为对mybatis的fork，但同时这种增强方式显然违反开闭原则，一旦上游代码修改，下游代码必须跟随更改。
    其次这类增强框架通常做的是提供通用CRUD接口和DSL的能力，和一些业务上开箱即用的能力，对于用法上的增强并无太多建树。比如没有实现spring data jpa方便的根据方法名生成查询；无法表达复杂的关联关系，没有利用resultMap强大的表达能力。
需求
    基于mybatis，且遵守开闭原则，基于mybatis官方提供的增强方式实现。
    基于mapper方法名自动生成符合含义的CRUD语句。
    可以将任意结构的Java对象映射到数据库中的表结构，可以表达任意层次结构、关联关系、加载方式。
    不侵入项目，只需补全xml映射文件之后即可移除本框架，且本框架只作用于dao层，不侵入service层。
    自动支持其他mybatis增强框架，本框架不支持的功能可以结合其他框架实现。
思路
    通过重写configuration的方式对mybatis进行增强，该方法为mybatis官方提供的增强方式。
    重写后的configuration调用原来的configuration，可以兼容其他mybaits增强框架，如mybatis-plus和mybatis-flex。。
    基于resultMap的强大能力，允许构建非常复杂的结果映射，同时实现连接查询、级联查询、懒加载等功能。
功能
    Java对象定义元数据
        支持类型对应到数据库中某个表
        支持简单类型属性对应到表中的某个列
        支持对象属性对应到表中的多个列
        支持map属性对应到表中的多个列
        考虑支持列表属性对应到表中多个行
            需要将对应多个行的单个对象转换成列表
        支持简单类型属性对应到关联表中的某个列
        支持对象属性对应到关联表中的某个列
        支持map属性对应到关联表中的某个列
        支持列表属性对应到关联表中的某个列
        支持父类对应到当前表
        支持父类对应到关联表
        支持关联表和当前表不直接关联
        支持定义所在表的属性
        支持定义所在表的列的属性
        支持指定查询关联列方式
    方法名定义CRUD
        支持单条查询
        支持批量查询
        支持级联查询
            连接查询
            分开查询
            懒加载查询
        支持新增
        支持批量新增
        考虑支持级联新增
        支持删除
        支持批量删除
        考虑支持级联删除
        支持更新
        支持批量更新
        考虑支持级联更新
    通用CRUD方法
        基于方法名CRUD实现
        支持条件对象查询
        支持条件对象嵌套层级
    代码生成器
    逆向生成器
    queryDSL
实现
    略

基于mybatis实现
    类似jpa根据函数名自动生成对应的语句
静态生成statement
    类似Lombok，编译时生成xml文件
动态生成statement
    1.参考mybatis plus
    2.@SelectProvider，参考mybatis-mapper，侵入性太强且不灵活，考虑废弃
    3.mybatis插件机制，考虑无法与其他插件并存，比如pagehelper，废弃

extends Configuration
考虑在获取statement的时候生成，可以在一个特定的时间对所有已注册的mapper进行检查
    @Override hasStatement(String statementName, boolean validateIncompleteStatements)
    @Override getMappedStatement(String id, boolean validateIncompleteStatements)
考虑在addMapper之后的时候生成，并对已注册的mappers进行检查。因为反例，考虑废弃
    @Override addMappers(String packageName, Class<?> superType)
    @Override addMappers(String packageName)
    @Override addMapper(Class<T> type)
    反例：考虑下面的情况（实验：CameraExtMapper）
        子mapper继承父mapper
        父子mapper各自有自己的statement实现
        子mapper先注册，父mapper后注册
    当注册子mapper的时候，子mapper中继承自父mapper的方法将找不到statement！！
        容易造成子mapper中生成的方法覆盖父mapper中方法的statement的情况，此问题可以解决
        addMapper之后不能马上对mapper进行检查，因为mapper中方法的statement可能定义在父mapper中，此问题不可解决
        综上，在addMapper之后生成相较于getMappedStatement时生成并无优势！！
call MapperRegistry.getMappers()

https://www.cnblogs.com/roytian/p/12762218.html
https://mybatis.org/mybatis-3/zh_CN/configuration.html#plugins
https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/zh/index.html

吐槽mybatisplus，侵入性强+乱用分层模型+弱鸡JPA+代码实现脏
https://www.zhihu.com/question/279766037

考虑用注解还是基接口标记需要被增强的接口
    用注解，无法提供默认的增强方法
    用基接口，存在标记传染的问题

TODO
通用动态SQL方法
    save
    saveBatch
    update
    updateFull
    delete
    deleteBatch
    get
    list
        定义where对象（核心）
    count
    exists
    ——————
    实现方案
        SqlProvider 问题：无法重写
        buildMappedStatement
方法名定义查询（核心）
    find|select|list|get...By...
    update...By...
    delete|remove...By...
    count...By...
    exists...By...
关联关系（核心）
    一对一
    多对一
    一对多
    多对多
    ——————
    支持场景
        懒加载
        即时加载
queryDSL
    构建出来的SQL和不涉及关联关系查询
    暂时不重要，通用方法和方法名查询不基于queryDSL
    设计
        参考mybatis-flex的QueryWrapper
    如何执行？
        使用@SelectProvider
            如何获取datasource以选择方言？
                getMappedStatement时将Configuration放到线程变量中，供SqlProvider从线程变量中获取
                或者通过拦截器设置线程变量
            返回类型怎么转换？
                字段名按照mybatis规则对应，下划线命名对应驼峰
代码生成器
逆向生成表
    更新
    重新创建
    无动作


>>>元数据结构设计

考虑构建sql需要的信息
    保存、修改
        表的列
    查询、条件
        实体字段
    关联查询
        关联表

考虑同时支持领域建模和数据库建模

考虑支持多层维度属性
例子
a
    id
    name
b
    id
    name
    a_id
c
    id
    name
    b_id

A
    ...
B
    ...
C
    id → t0_id
    name → t0_name
    bId → t0_b_id
    bName → t1_name
        b
        b_id=b.id
    aName → t2_name
        b,a
        b_id=b.id
        b.a_id=a.id

select
t0.id as t0_id,
t0.name as t0_name,
t0.b_id as t0_b_id,
t1.name as t1_name,
t2.name as t2_name
from c t0
left join b t1 on b.id = c.b_id
left join a t2 on a.id = b.a_id

//第一步，构造子图
    初始化上一个节点lastJoinTableInfo为空
    初始化别名到节点的映射aliasToJoinTableInfo
        如果根节点存在别名
            将根节点加入aliasToJoinTableInfo
    遍历关联关系注解
        声明节点JoinTableInfo
        新建表信息TableInfo
        如果存在别名
            如果别名已存在节点
                如果节点已存在表信息
                    报错别名重复
                节点设置表信息
            否则
                新建节点
                节点设置表信息
                节点加入映射
        否则
            新建节点
            节点设置表信息
        遍历连接列
            新建连接列信息
            如果指定了左表别名
                否则如果别名存在节点
                    节点添加连接列信息到右连接信息
                否则
                    新建节点
                    节点添加连接列信息到右连接信息
                    节点加入映射
            否则
                如果上一个节点不为空
                    上一个节点添加连接列信息到右连接信息
                否则
                    根节点添加连接列信息到右连接信息
        设置上一个节点为当前节点
//第二步，检查子图完整性
    遍历aliasToJoinTableInfo节点
        如果节点没有表信息
            报错未定义的别名
//第三步，合并子图到全图并优化
    广度优先遍历未合并的子节点
        如果子节点的所有父节点都已合并
            如果子节点的特征已存在
                替换子节点为已存在的节点
            否则
                合并子节点

resultmap子节点
    id
    result
    association
    collection
    discriminator

resultmap命名自动生成规则
    类名+别名+递归层数+生成标志
    类名+别名+字段名+递归层数+生成标志

resultmap嵌套select命名自动生成规则
    包名+select+类名简写+递归层数+生成标志
    包名+selec+类名简写+字段名+递归层数+生成标志

考虑取消resultType
    遍历PropertyInfo
        如果columnInfo不为空
            如果idType不为空
                resultType为ID
            否则
                resultType为RESULT
        否则
            如果ofType不为空
                resultType为COLLECTION
                递归
            否则
                resultType为ASSOCIATION
                递归

考虑支持父类
    如果存在父类有三种情况
        父类字段和子类表没有关系
            忽略
        父类字段是子类表的字段
            嵌合
        父类的表是子类表的关联表（右表）

考虑方法重载
    确定参数类型
    确定参数数量

如何根据实际数据库类型生成sql？
    生成MappedStatement根据datasource选择方言
如何兼容动态数据源？
    只有实际执行的时候才注册MappedStatement，校验mapper方法的时候不能注册MappedStatement
        动态数据源顾名思义，datasource获取的实际数据源是动态的，实际执行时候和校验时候获取的实际数据源可能不一致，实际执行时候获取到数据源才是真实的数据源
        校验的时候不需要使用真正的datasource，默认即可，以提升性能
    或者考虑databaseId？
        不可，databaseId的原理是在启动时根据datasource选择匹配的databaseId的语句作为statement，原理和根据datasource选择方言的做法一样，所以不能解决问题。注：mybatis-plus的dynamic-datasource所以不支持databaseId
