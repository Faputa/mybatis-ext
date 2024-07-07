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
https://mybatis.org/mybatis-3/zh/configuration.html#plugins
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
    count
    exists
    ——————
    实现方案
        SqlProvider 问题：无法重写
        buildMappedStatement
方法名定义查询
    find|select|list|get...By...
    update...By...
    delete|remove...By...
    count...By...
    exists...By...
维表字段
    一对一
    多对一
    一对多
    多对多
    ——————
    支持场景
        懒加载
        即时加载
queryDSL
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

queryDSL
    select ? from ? where ?
    select ? from ? left join ? on ? where ?
    select ? from ? order by ?
    select ? from ? group by ?
    select ? from ? group by ? having ?
    insert into ? values (?)
    insert into ? (?) values (?)
    update ? set ? where ?
    update ? left join ? on ? set ? where ?
    delete from ? where ?
    delete from ? left join ? on ? where ?

dsl设计思路
    dsl=ast+astBuilder

select(...)
selectAll(A.class)
selectProperty(P.class)

ast设计，参考jsqlparser
    Select
        List<SelectItem> selectItems
        FromItem fromItem
        List<Join> joins
        Expression where
        List<Expression> groupBy
        Expression having
        Limit limit
    Update
    Insert
    Delete

考虑优化冗余join表
    因为selectAll的时候可能已经存在join表，如果在此基础上增加join表，可能存在冗余
    是，对selectAll优化
    否，对selectProperty优化

考虑方法重载
    确定参数类型
    确定参数数量
