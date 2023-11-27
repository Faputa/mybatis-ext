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
