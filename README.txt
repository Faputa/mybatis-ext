基于mybatis实现
    类似jpa根据函数名自动生成对应的语句
静态生成statement
    类似Lombok，编译时生成xml文件
动态生成statement
    1.参考mybatis plus
    2.@SelectProvider，参考mybatis-mapper，侵入性太强且不灵活，考虑废弃
    3.mybatis插件机制，考虑无法与其他插件并存，比如pagehelper，废弃

extends Configuration
考虑在获取statement的时候生成
    @Override hasStatement(String statementName, boolean validateIncompleteStatements)
    @Override getMappedStatement(String id, boolean validateIncompleteStatements)
考虑在addMapper之后的时候生成，并对已注册的mappers进行检查。因为反例，考虑废弃
    @Override addMappers(String packageName, Class<?> superType)
    @Override addMappers(String packageName)
    @Override addMapper(Class<T> type)
    反例：考虑下面的情况（实验：DemoExtMapper）
        子mapper继承父mapper
        父子mapper各自有自己的xml实现
        子mapper先注册，父mapper后注册
    当注册子mapper的时候，子mapper中继承自父mapper的方法将找不到实现！！！
        既容易造成子mapper中生成的方法覆盖父mapper中方法的定义的情况，此问题可以解决
        addMapper之后不能马上对mapper进行检查，mapper中方法的定义可能在父mapper中，此问题不可解决
        综上，在addMapper之后生成相较于getMappedStatement时生成并无优势！！！
call MapperRegistry.getMappers()

https://www.cnblogs.com/roytian/p/12762218.html
https://mybatis.org/mybatis-3/zh/configuration.html#plugins
