# 变更记录

## 待发布：共享核心分析与 IDEA 插件

核心、Spring 模块和 IDEA 插件统一继承根版本，当前保留开发配置 `0.0.17`，不代表本次变更已发布；正式发布前需分配未发布过的新版本。插件最低基线为 IDEA 2023.3（233）、Java 17。

### 新增功能

- 核心增加 `model` Java 类型模型、`diagnostic` 诊断及 `analysis` 分析入口。反射运行时和 PSI 共用元数据、方法 CRUD 规则，提供诊断、属性引用和补全；分析无需启动 Spring 或连接数据库。
- IDEA 插件支持 MyBatis XML/配置识别、离线 DTD、类型和 SQL 引用、参数补全与诊断，以及 Mapper/XML 文件图标和双向行标导航。
- IDEA 插件支持 Table/TableRef、ColumnRef、JoinRelation 和方法 CRUD 的诊断、跳转、补全；元数据图标定位到最终属性，表文件使用轮廓小鸟图标。
- `JoinColumn.leftTableAlias` 字符串支持 Ctrl+B / Ctrl+单击跳转到表、父表或同一属性此前关联的别名声明；空别名、未知或有歧义的别名不猜测目标。
- CRUD 悬浮文档复用核心生成动态 MyBatis 脚本，保留动态标签，显示分析失败原因；从项目数据源配置选择方言，无法确定时使用 MySQL。

### 功能优化

- 核心和插件共用根版本号，插件不再单独维护版本；版本变更统一在根 `gradle.properties` 中完成。
- 插件模块明确命名为 `mybatis-ext-idea-plugin`；分析设置改为项目级轻量服务，插件身份与已有设置存储保持不变。
- 全项目使用根 Gradle Wrapper 多模块构建，插件直接依赖当前核心项目；IDE 可感知模块源码与依赖，不再使用插件内的目标版本拦截。
- 插件固定基于 IDEA Community 2023.3.8 SDK 编译，独立提供新版 IDEA 测试与调试任务、基线及跨版本 Plugin Verifier 检查，避免本机安装版本影响发布基线。
- 运行时异常保留结构化诊断及原因链；元数据构建失败不向共享缓存泄漏部分关联。直接属性声明与最终映射来源分别记录，供运行时解释和 IDE 导航共用。
- 统一 Java/Gradle 格式化与检查，增加本地交付归档任务、Wrapper 分发校验和 JAR 许可；保留 Maven 坐标、Java 8 核心字节码、provided 范围和 Javassist 重定位。

### 问题修复

- 补齐 PSI 对 MyBatis 默认二进制数组、输入流和 Object 标量类型的识别，并遵循 TypeHandler 的父类继承规则，避免将合法列误分析为关联；通过反射/PSI 元数据对照验证。

### 使用说明

- 仓库使用 Gradle 构建，业务项目可使用 Maven 管理依赖。根 `mybatis-ext-parent` 发布为依赖管理 BOM。
- 插件以静态源码事实分析，不能自动还原任意 Spring 配置、动态 TypeHandler、复杂 OGNL 或所有 SQL 运行时行为；详情见[插件说明](../mybatis-ext-idea-plugin/README.md)。
