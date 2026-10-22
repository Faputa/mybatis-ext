# MyBatis Ext IDEA plugin

IntelliJ 插件模块，与主项目使用同一 Gradle 多模块构建，通过 `project(":mybatis-ext")` 直接依赖核心及根平台的依赖管理。核心目标为 Java 8；插件固定基于 IDEA Community 2023.3.8（233）SDK，以 Java 17 编译。

## 功能

| 范围 | 已实现 |
| --- | --- |
| XML 识别 | mapper/configuration 上下文；离线 MyBatis Mapper/Config DTD，包含官方 HTTP/HTTPS 地址 |
| Mapper 导航 | 红色/青色小鸟作为 XML/Mapper 文件及跳转图标；Java 类名/方法名与 XML 开始标签旁的双向跳转按钮；继承方法、多 XML 文件候选；语句候选以 `[值]` 显示 databaseId，未指定或为空时省略；文件名在右侧辅助区域显示，保留原有候选顺序 |
| XML 引用 | Java 类型、内置及 XML/注解别名；resultMap、extends、include/refid、嵌套 select；结果属性、嵌套属性；配置 mapper resource |
| XML 参数 | `#{}`、`${}` 引用及补全；按当前 Mapper 方法的有效参数解析，支持继承泛型替换；`@Param` 优先于通用 paramN；单个标量、单对象、collection/list/array、foreach item/index、bind |
| XML 诊断 | 已关联 Mapper 的未知方法，无法解析的类型、结果属性、参数、SQL/resultMap 引用；同 namespace/ID/databaseId 的重复声明；允许没有 Java 接口的 XML-only namespace |
| XML 补全 | 标签、属性、引用候选、参数与嵌套属性、JdbcType、statementType 等；有 DTD 时同时使用 IDEA XML 能力 |
| 普通 Mapper 诊断 | 提示缺失 XML/注解映射；排除默认/静态方法与常见 MyBatis-Plus/Flex 注入型 Mapper |
| Ext 元数据 | 核心共享规则的诊断；TableRef/ColumnRef、关联左右属性路径的跳转与补全；leftTableAlias 字符串可跳转到对应的 alias/tableAlias 声明；Table/TableRef 类及 Java 文件使用轮廓小鸟图标；ColumnRef/JoinRelation 在字段名或方法名所在行显示青色小鸟跳转按钮 |
| 文件图标 | `table.svg` 表示 @Table，`tableref.svg` 表示 @TableRef，`xml.svg` 表示 Mapper XML，`mapper.svg` 表示 Mapper 接口；TableRef 使用带引用箭头的轮廓图标 |
| Ext CRUD | 核心共享语法与语义诊断；方法名关键字、属性与变量补全；属性段 Ctrl+B/Ctrl+单击导航；方法名中的返回类型名可跳转到类型声明，集合、Optional 和数组定位到元素类型；按住 Ctrl 悬浮时仅高亮对应单词，保留多目标选择 |
| CRUD 脚本预览 | 成功解析的增强方法显示圆圈 E 图标，点击打开与方法悬浮文档/Ctrl+Q 相同的 script 预览，支持选择复制；包括 list/count 等纯操作名方法，排除 XML/注解 SQL 和默认/静态方法；复用核心脚本生成，保留 MyBatis 动态标签；悬浮文档在失败时展示诊断原因；简要显示方言，默认原因置于方言文字的悬浮提示中 |

## 构建与验证

最低支持基线为 IDEA **2023.3（233）**，SDK 固定为 **2023.3.8**，不再使用开发者本机 IDEA 作为发布 SDK。平台 API 以此版本编译约束，发布字节码目标为 Java 17；新版兼容性通过 Plugin Verifier 和独立 PSI 测试验证。

在 PowerShell 中：

```powershell
cd C:\Users\Admin\Desktop\myrepo\mybatis-ext
$env:JAVA_HOME = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.2.0.1\jbr'
.\gradlew.bat verifyAll
```

默认任务自动下载并缓存 233 SDK。设置 `IDEA_HOME`、传入 `-PideaHome=...`，或在不入库的 `local.properties` 中设置 `ideaHome=C:/...` 后，可使用 `runLocalIde` / `testLocalIde` 在该 IDE 上调试和测试；这些设置不替换发布 SDK。Gradle Wrapper 使用 9.4.0，IntelliJ Platform Gradle Plugin 使用 2.19.0，同时处理旧版和新版平台描述格式。

根 `verifyAll` 执行全部 Java 模块和插件测试、插件结构校验、基线 Plugin Verifier 及打包，无需 Maven 安装。只验证插件可运行 `gradlew.bat :mybatis-ext-idea-plugin:test :mybatis-ext-idea-plugin:verifyPlugin :mybatis-ext-idea-plugin:verifyPluginStructure :mybatis-ext-idea-plugin:buildPlugin`，Gradle 会先构建所需核心产物。核心版本统一来自根 `gradle.properties`，插件始终使用当前工程核心。IDEA 导入、新版测试和验证矩阵见[构建说明](../docs/build.md)。

核心、Spring 模块及插件的版本统一继承根 `gradle.properties` 的 `version`，插件不单独维护版本号。当前保留开发版本 `0.0.17`，产物：`mybatis-ext-idea-plugin/build/distributions/mybatis-ext-idea-plugin-0.0.17.zip`，不代表本次变更已发布。在 IDEA 的 Settings → Plugins → 齿轮 → Install Plugin from Disk 中选择 ZIP，按 IDEA 提示重新启动。根目录运行 `gradlew.bat :mybatis-ext-idea-plugin:runIde` 启动开发沙箱，不会覆盖日常使用的 IDE 配置。

测试使用 IDEA 真实 Java/XML PSI fixture，验证引用、索引、核心分析、字符串偏移与编辑器补全插入行为。同一份 Java fixture 分别经过反射和 PSI，比较元数据、最终映射来源、错误诊断、方法引用及 MySQL/PostgreSQL 动态脚本；另验证目标项目版本不同、缺少 Maven 元信息或直接提供框架源码时，合法分析仍然可用。

## 设计与使用边界

- `model/PsiModel` 只读取 PSI，不加载、执行用户项目的实体类、生成器或 TypeHandler。成员类型在所属泛型上下文中替换；框架注解默认值与重复注解转换为核心 Java 模型。
- 元数据按钮直接读取核心 `PropertyInfo.mappingSource`，不在插件中重复解释最终映射。`JoinRelation` 重复注解或 `JoinRelations` 容器的最终目标由核心确定，按钮显示在字段名或方法名所在行；字段或 Getter 的关联类型与集合元素类型遵循核心支持范围。实体元数据构建失败时不显示该实体的映射按钮；中间关联仍可通过字面值引用跳转。数组关联当前不受核心支持。
- `ExtSession` 在单次请求中创建模型与 MybatisExtAnalyzer；不会跨文档修改保存可变核心缓存或 PSI 结果。索引仅记录 XML 的 namespace，后续声明解析使用当前 PSI。
- PSI 按 JavaBeans 规则匹配 getter 与兼容的 void setter，泛型继承和重载参与可写性判断；元数据关联的容器类型解包复用核心规则，支持固定元素类型的集合子类。
- XML 脚本参数以 Mapper 方法签名为准，与是否继承某种框架基类无关；继承方法在当前 Mapper 上下文中替换泛型。仅在找不到对应方法时，使用 XML 的 `parameterType` 补充类型信息。普通文本和 CDATA 中的占位符通过 XML 文本 token 提供引用，供 IDEA 的跳转与重命名使用。
- 普通 MyBatis 属性引用优先跳转到 getter 所属类及其父类中的同名非静态字段，没有对应字段时跳转 getter；属性类型仍遵循 JavaBeans 规则及泛型替换。Ext 元数据引用保持定位到实际映射声明。
- 诊断仍为核心不可变值。`ExtLocation`/`SourceText` 将解码后的文本位置映射为实际 Java/XML 范围；重复注解实例无法唯一确定时回退到声明，不猜测具体位置。
- `Settings → MyBatis Ext` 中的 `defaultFilterable` 应与运行时一致。通过外部配置注册 TypeHandler 的业务类型，需要在同一设置页列出其 Java 全名；插件无法执行任意 Spring/Java 配置来重建运行时 Configuration。
- CRUD 预览优先扫描 Mapper 所在模块的 properties、YAML 和 XML 数据源 URL 配置；该模块没有 URL 时再扫描项目。支持 MySQL、Oracle、DM、PostgreSQL、H2 和 SQL Server；找不到 URL、包含未解析占位符、不支持的协议或存在方言冲突时均使用 MySQL。不同 Spring profile 不推测激活状态，存在冲突时同样回退；预览不展示连接地址或凭据。
- 方言选择按模块共享缓存，配置内容、文件增删改名或项目根目录变化时重新检测；普通 Java 内容编辑不触发扫描，未变化配置文件的 URL 解析结果继续复用。无配置时显示 `MySQL（默认）`，配置存在但方言无法确定时显示 `MySQL（默认，方言未确定）`。
- 悬浮预览需要 IDEA 启用鼠标悬浮快速文档，也可使用 Ctrl+Q。仅预览当前签名的派生 CRUD 动态脚本，不执行 OGNL、连接数据库或展开重载方法的运行时分派；已有 XML/SQL 注解映射的方法不展示派生脚本。配置扫描不执行 Java 配置或解析外部环境变量、YAML 锚点等动态配置。
- SQL 参数中的 Map 动态键、动态拼接的引用、复杂 OGNL、无法解析的类型均不能完全静态验证；动态键不作错误诊断。OGNL 仅对可识别的属性路径提供软引用，不执行表达式。
- 关联路径按左右表及先前关联的显式别名确定补全上下文；核心负责实际关联合法性检查。继承映射和内嵌投影的复杂路径需要与实际项目保持版本一致。
- 此版本实现 Java/XML 支持；未包含数据库逆向生成、Kotlin/Spring 配置全量推断、SQL 数据库列语义检查或 SQL 执行能力。
- 核心语法通过同步构建和更新插件跟随演进，不会在已安装插件中自动更新。插件通过自己的类加载器携带 MyBatis，不使用项目 classpath 上的运行时对象。
- 本地安装不需要签名。Kleopatra/GPG 密钥不能直接代替 JetBrains Marketplace 所需的证书与私钥；本工程不读取 GPG 私钥，也不发布到 Marketplace。

普通 MyBatis 项目可仅使用基础能力；引用 mybatis-ext 的项目额外启用元数据与 CRUD 能力。

## 核心版本兼容

插件与所携带核心通过 Gradle 项目依赖、一致性测试和统一打包保持同步；不读取目标项目版本来阻断功能。无法解析的源码或不支持的表达通过已有诊断说明原因。发布核心语义变化时仍需升级版本、同步构建插件并验证行为；新增注解形态、PSI 类型事实或 UI 入口可能需要插件适配，新方言也需要补充数据源协议识别。最低基线为 233；支持范围由发布描述和验证结果共同约束，不能通过放宽 `sinceBuild` / `untilBuild` 推导未测试版本的兼容性。

图标统一位于 `icons`，两个原始图标使用 `mybatisx_` 前缀；插件主图标为 `META-INF/pluginIcon.svg`。第三方图标的来源与许可随插件保存在 `META-INF/NOTICE-MybatisX.txt`、`META-INF/LICENSE-MybatisX.txt`。
