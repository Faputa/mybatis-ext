package io.github.mybatisext.idea;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.swing.JEditorPane;

import org.apache.ibatis.session.Configuration;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.navigation.CtrlMouseData;
import com.intellij.codeInsight.navigation.actions.GotoDeclarationOrUsageHandler2;
import com.intellij.icons.AllIcons;
import com.intellij.lang.LanguageDocumentation;
import com.intellij.lang.documentation.CompositeDocumentationProvider;
import com.intellij.model.Symbol;
import com.intellij.model.psi.ImplicitReferenceProvider;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.model.psi.PsiSymbolService;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.ui.UiInterceptors;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.UIUtil;

import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.dialect.DefaultDialectSelector;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.dialect.MySqlDialect;
import io.github.mybatisext.dialect.OracleDialect;
import io.github.mybatisext.dialect.PostgreSqlDialect;
import io.github.mybatisext.dialect.SqlServerDialect;
import io.github.mybatisext.idea.ext.AnalysisSettings;
import io.github.mybatisext.idea.ext.CrudDocumentation;
import io.github.mybatisext.idea.ext.ExtImplicitReferences;
import io.github.mybatisext.idea.ext.ExtInspection;
import io.github.mybatisext.idea.ext.ExtLocation;
import io.github.mybatisext.idea.ext.ExtNavigation;
import io.github.mybatisext.idea.ext.ExtReferences;
import io.github.mybatisext.idea.ext.ExtSession;
import io.github.mybatisext.idea.ext.PreviewDialect;
import io.github.mybatisext.idea.model.PsiModel;
import io.github.mybatisext.idea.xml.MapperIconProvider;
import io.github.mybatisext.idea.xml.MapperInspection;
import io.github.mybatisext.idea.xml.MapperNavigation;
import io.github.mybatisext.idea.xml.MappingReference;
import io.github.mybatisext.idea.xml.MybatisIcons;
import io.github.mybatisext.idea.xml.MybatisInspection;
import io.github.mybatisext.idea.xml.MybatisProject;
import io.github.mybatisext.idea.xml.MybatisReferences;
import io.github.mybatisext.idea.xml.XmlBindings;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.statement.SemanticScriptHelper;

/** 使用真实的 IDEA PSI 测试环境，不模拟核心或编辑器 API。 */
public final class PluginFixtureTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_17;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Path core = Path.of(System.getProperty("mybatisext.core.jar"));
        Path mybatis = Path.of(System.getProperty("mybatisext.mybatis.jar"));
        ModuleRootModificationUtil.addModuleLibrary(getModule(), "mybatis-ext-fixture", List.of(jar(core), jar(mybatis)), List.of());
        myFixture.addClass("""
                package sample;
                import io.github.mybatisext.annotation.*;
                @Table public class User {
                    @Id @Column public Long id;
                    @Column public String name;
                    @Column public Address address;
                    public static class Address { @Column public String city; }
                }
                """);
    }

    private static String jar(Path path) {
        return "jar://" + path.toAbsolutePath().toString().replace('\\', '/') + "!/";
    }

    private PsiClass mapper(String methods) {
        return myFixture.addClass("package sample; import org.apache.ibatis.annotations.*; public interface UserMapper { " + methods + " }");
    }

    private PsiClass extMapper(String methods) {
        return myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; import org.apache.ibatis.annotations.*; @MapTable(User.class) public interface UserMapper { " + methods + " }");
    }

    private XmlFile xml(String body) {
        return (XmlFile) myFixture.configureByText("UserMapper.xml", "<mapper namespace=\"sample.UserMapper\">" + body + "</mapper>");
    }

    private List<PsiElement> targets(XmlAttributeValue value) {
        List<PsiElement> targets = new ArrayList<>();
        for (PsiReference ref : value.getReferences()) {
            if (ref instanceof MappingReference mapping) {
                for (ResolveResult result : mapping.multiResolve(false)) {
                    targets.add(result.getElement());
                }
            }
        }
        return targets;
    }

    private XmlAttributeValue attr(XmlTag tag, String name) {
        return Objects.requireNonNull(tag.getAttribute(name)).getValueElement();
    }

    public void testXmlNamespaceAndStatementNavigationBothDirections() {
        PsiClass mapper = mapper("User load(Long id);");
        XmlFile file = xml("<select id=\"load\" resultType=\"sample.User\">select 1</select>");
        assertContainsElements(targets(attr(file.getRootTag(), "namespace")), mapper);
        XmlTag select = file.getRootTag().findFirstSubTag("select");
        assertContainsElements(targets(attr(select, "id")), mapper.findMethodsByName("load", false)[0]);
        assertEquals(List.of(select), new MybatisProject(mapper).statements(mapper, "load", true));
    }

    private List<RelatedItemLineMarkerInfo<?>> markers(PsiElement root) {
        return markers(root, new MapperNavigation());
    }

    private List<RelatedItemLineMarkerInfo<?>> markers(PsiElement root, RelatedItemLineMarkerProvider provider) {
        List<LineMarkerInfo<?>> result = new ArrayList<>();
        List<PsiElement> elements = new ArrayList<>();
        root.accept(new PsiRecursiveElementWalkingVisitor() {

            @Override
            public void visitElement(PsiElement element) {
                if (element.getFirstChild() == null) {
                    elements.add(element);
                }
                super.visitElement(element);
            }
        });
        provider.collectSlowLineMarkers(elements, result);
        return result.stream().<RelatedItemLineMarkerInfo<?>>map(info -> (RelatedItemLineMarkerInfo<?>) info).toList();
    }

    public void testMapperFileAndClassIcons() {
        assertTrue(MybatisIcons.MAPPER.getIconWidth() > 0);
        assertTrue(MybatisIcons.XML.getIconWidth() > 0);
        PsiClass mapper = mapper("User load();");
        MapperIconProvider icons = new MapperIconProvider();
        assertNull(icons.getIcon(mapper, 0));
        XmlFile file = xml("<select id=\"load\">select 1</select>");
        assertSame(MybatisIcons.MAPPER, icons.getIcon(mapper, 0));
        assertSame(MybatisIcons.MAPPER, icons.getIcon(mapper.getContainingFile(), 0));
        assertSame(MybatisIcons.XML, icons.getIcon(file, 0));
        assertNull(icons.getIcon(myFixture.configureByText("other.xml", "<configuration/>"), 0));
        PsiClass plain = myFixture.addClass("package sample; public interface NotAMapper {}");
        assertNull(icons.getIcon(plain, 0));
        assertNull(icons.getIcon(plain.getContainingFile(), 0));
    }

    public void testAnnotatedAndEnhancedMapperIconsWithoutXml() {
        MapperIconProvider icons = new MapperIconProvider();
        PsiClass annotated = myFixture.addClass("package sample; @org.apache.ibatis.annotations.Mapper public interface AnnotatedMapper {}");
        assertSame(MybatisIcons.MAPPER, icons.getIcon(annotated, 0));
        PsiClass sql = myFixture.addClass("package sample; public interface SqlMapper { @org.apache.ibatis.annotations.Select(\"select 1\") int load(); }");
        assertSame(MybatisIcons.MAPPER, icons.getIcon(sql, 0));
        assertSame(MybatisIcons.MAPPER, icons.getIcon(extMapper("User find();"), 0));
        PsiClass inherited = myFixture.addClass("package sample; public interface GenericMapper extends io.github.mybatisext.mapper.ExtMapper<User> {}");
        assertSame(MybatisIcons.MAPPER, icons.getIcon(inherited, 0));
    }

    public void testTableAndTableRefClassAndFileIcons() {
        assertTrue(MybatisIcons.TABLE.getIconWidth() > 0);
        assertNotSame(MybatisIcons.MAPPER, MybatisIcons.TABLE);
        MapperIconProvider icons = new MapperIconProvider();
        PsiClass table = JavaPsiFacade.getInstance(getProject()).findClass("sample.User", GlobalSearchScope.allScope(getProject()));
        PsiClass view = myFixture.addClass("package sample; @io.github.mybatisext.annotation.TableRef(User.class) public class UserView {}");
        assertTrue(MybatisIcons.TABLE_REF.getIconWidth() > 0);
        assertNotSame(MybatisIcons.TABLE, MybatisIcons.TABLE_REF);
        assertSame(MybatisIcons.TABLE, icons.getIcon(table, 0));
        assertSame(MybatisIcons.TABLE, icons.getIcon(table.getContainingFile(), 0));
        assertSame(MybatisIcons.TABLE_REF, icons.getIcon(view, 0));
        assertSame(MybatisIcons.TABLE_REF, icons.getIcon(view.getContainingFile(), 0));
        myFixture.addClass("package other; public @interface Table {}");
        PsiClass unrelated = myFixture.addClass("package sample; @other.Table public class Plain {}");
        assertNull(icons.getIcon(unrelated, 0));
        assertNull(icons.getIcon(unrelated.getContainingFile(), 0));
        PsiClass outer = myFixture.addClass("package sample; public class Outer { @io.github.mybatisext.annotation.Table static class Nested {} }");
        assertNull(icons.getIcon(outer.getContainingFile(), 0));
        assertSame(MybatisIcons.TABLE, icons.getIcon(outer.getInnerClasses()[0], 0));
    }

    public void testEnhancedGutterIncludesMethodsWithoutNameReferences() {
        PsiClass mapper = extMapper("java.util.List<User> list(User query); long count(@Param(\"name\") String name); User getById(@Param(\"id\") Long id);");
        var markers = markers(mapper, new ExtNavigation());
        assertEquals(3, markers.size());
        assertSame(AllIcons.Nodes.Enum, MybatisIcons.ENHANCED);
        for (var marker : markers) {
            assertSame(MybatisIcons.ENHANCED, marker.getIcon());
            assertEmpty(marker.createGotoRelatedItems());
            assertNotNull(marker.getNavigationHandler());
            assertEquals("查看 mybatis-ext 生成的 SQL 脚本", marker.getLineMarkerTooltip());
            PsiMethod method = assertInstanceOf(marker.getElement().getParent(), PsiMethod.class);
            String preview = registeredDocumentation(method);
            assertNotNull(preview);
            assertTrue(preview, preview.contains("&lt;script&gt;"));
            assertTrue(preview, preview.contains(new CrudDocumentation().generateDoc(method, marker.getElement())));
        }
    }

    public void testEnhancedGutterPreviewUsesMousePositionAndKeepsCaret() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User find(); }");
        PsiMethod method = file.getClasses()[0].getMethods()[0];
        var marker = markers(method, new ExtNavigation()).get(0);
        var editor = myFixture.getEditor();
        editor.getCaretModel().moveToOffset(0);
        Component gutter = assertInstanceOf(editor.getGutter(), Component.class);
        Point clickPoint = new Point(12, 80);
        List<JBPopup> shownPopups = new ArrayList<>();
        UiInterceptors.registerPersistent(getTestRootDisposable(), new UiInterceptors.PersistentUiInterceptor<JBPopup>(JBPopup.class) {

            @Override
            public boolean shouldIntercept(JBPopup popup) {
                return true;
            }

            @Override
            protected void doIntercept(JBPopup popup, RelativePoint location) {
                Disposer.register(getTestRootDisposable(), popup);
                shownPopups.add(popup);
                assertNotNull(location);
                assertSame(gutter, location.getOriginalComponent());
                assertEquals(clickPoint, location.getOriginalPoint());
            }
        });

        MouseEvent click = new MouseEvent(gutter, MouseEvent.MOUSE_CLICKED, 0, 0, clickPoint.x, clickPoint.y, 1, false, MouseEvent.BUTTON1);
        @SuppressWarnings("unchecked")
        GutterIconNavigationHandler<PsiElement> handler = (GutterIconNavigationHandler<PsiElement>) marker.getNavigationHandler();
        handler.navigate(click, marker.getElement());
        assertEquals(1, shownPopups.size());
        JEditorPane component = UIUtil.findComponentOfType(shownPopups.get(0).getContent(), JEditorPane.class);
        assertNotNull(component);
        assertFalse(component.isEditable());
        assertTrue(component.getText(), component.getText().contains("&lt;script&gt;"));
        assertTrue(component.getText(), component.getText().contains("mybatis-ext CRUD script"));
        component.selectAll();
        assertTrue(component.getSelectedText(), component.getSelectedText().contains("<script>"));
        assertEquals(0, editor.getCaretModel().getOffset());
    }

    public void testEnhancedGutterHonorsExplicitMappingsAndInvalidMethods() {
        PsiClass mapper = extMapper("User get(); @Select(\"select 1\") long count(); User getByMissing(); default User find() { return null; } static User list() { return null; }");
        xml("<select id=\"get\">select 1</select>");
        assertEmpty(markers(mapper, new ExtNavigation()));
        PsiClass ordinary = myFixture.addClass("package sample; public interface PlainMapper { User get(); }");
        assertEmpty(markers(ordinary, new ExtNavigation()));
    }

    public void testGutterNavigationOnDeclarationNamesAndOpeningTags() {
        PsiClass mapper = mapper("User load();");
        PsiMethod method = mapper.getMethods()[0];
        XmlFile file = xml("\n<select id=\"load\">select 1</select>\n<sql id=\"columns\">id</sql>\n<resultMap id=\"user\" type=\"sample.User\"/>\n");
        XmlTag root = file.getRootTag();
        XmlTag select = root.findFirstSubTag("select");
        var javaMarkers = markers(mapper);
        assertEquals(2, javaMarkers.size());
        for (var marker : javaMarkers) {
            assertSame(MybatisIcons.XML, marker.getIcon());
            if (marker.getElement() == mapper.getNameIdentifier()) {
                assertEquals(List.of(root), marker.createGotoRelatedItems().stream().map(item -> item.getElement()).toList());
            } else {
                assertSame(method.getNameIdentifier(), marker.getElement());
                assertEquals(List.of(select), marker.createGotoRelatedItems().stream().map(item -> item.getElement()).toList());
            }
        }
        var xmlMarkers = markers(file);
        assertEquals(2, xmlMarkers.size());
        for (var marker : xmlMarkers) {
            assertSame(MybatisIcons.MAPPER, marker.getIcon());
            assertInstanceOf(marker.getElement(), XmlToken.class);
            assertEquals(XmlTokenType.XML_NAME, ((XmlToken) marker.getElement()).getTokenType());
            assertEquals(XmlTokenType.XML_START_TAG_START, ((XmlToken) marker.getElement().getPrevSibling()).getTokenType());
            assertEquals(List.of(marker.getElement().getParent() == root ? mapper : method), marker.createGotoRelatedItems().stream().map(item -> item.getElement()).toList());
        }
        assertEmpty(markers(myFixture.configureByText("other.xml", "<configuration><select id=\"load\"/></configuration>")));
    }

    public void testGutterPreservesMultipleXmlTargetsAndInheritedStatements() {
        PsiClass parent = myFixture.addClass("package sample; public interface ParentMapper { User load(); }");
        PsiClass child = myFixture.addClass("package sample; public interface ChildMapper extends ParentMapper { User load(); }");
        myFixture.addFileToProject("parent.xml", "<mapper namespace=\"sample.ParentMapper\"><select id=\"load\">1</select></mapper>");
        myFixture.addFileToProject("parent-other.xml", "<mapper namespace=\"sample.ParentMapper\"><select id=\"load\" databaseId=\"mysql\">2</select></mapper>");
        assertEquals(2, markers(parent).size());
        for (var marker : markers(parent)) {
            assertEquals(2, marker.createGotoRelatedItems().size());
        }
        var inherited = markers(child);
        assertEquals(1, inherited.size());
        assertSame(child.getMethods()[0].getNameIdentifier(), inherited.get(0).getElement());
        assertEquals(2, inherited.get(0).createGotoRelatedItems().size());
    }

    public void testCrossNamespaceSqlAndResultMapReferences() {
        mapper("User load();");
        myFixture.addFileToProject("shared.xml", "<mapper namespace=\"sample.Shared\"><sql id=\"columns\">id,name</sql><resultMap id=\"user\" type=\"sample.User\"/></mapper>");
        XmlFile file = xml("<select id=\"load\" resultMap=\"sample.Shared.user\"><include refid=\"sample.Shared.columns\"/></select>");
        XmlTag select = file.getRootTag().findFirstSubTag("select");
        assertEquals("resultMap", ((XmlTag) targets(attr(select, "resultMap")).get(0)).getName());
        assertEquals("sql", ((XmlTag) targets(attr(select.findFirstSubTag("include"), "refid")).get(0)).getName());
    }

    public void testXmlNestedResultPropertyAndAlias() {
        mapper("User load();");
        myFixture.addFileToProject("mybatis-config.xml", "<configuration><typeAliases><typeAlias alias=\"person\" type=\"sample.User\"/></typeAliases></configuration>");
        XmlFile file = xml("<resultMap id=\"user\" type=\"person\"><association property=\"address\"><result property=\"city\" column=\"city\"/></association></resultMap>");
        XmlTag resultMap = file.getRootTag().findFirstSubTag("resultMap");
        assertEquals("User", ((PsiClass) targets(attr(resultMap, "type")).get(0)).getName());
        XmlTag city = resultMap.findFirstSubTag("association").findFirstSubTag("result");
        assertEquals("city", ((PsiField) targets(attr(city, "property")).get(0)).getName());
    }

    public void testXmlBuiltinAliasesAreCaseInsensitive() {
        MybatisProject project = new MybatisProject(xml(""));

        assertEquals("java.lang.String", project.resolveType("STRING").getQualifiedName());
        assertEquals("java.lang.Integer", project.resolveType("int").getQualifiedName());
        assertEquals("java.math.BigDecimal", project.resolveType("decimal").getQualifiedName());
        assertEquals("java.util.Map", project.resolveType("Map").getQualifiedName());
    }

    public void testXmlConfiguredAliasPreservesOverrideOrder() {
        myFixture.addFileToProject("mybatis-config.xml", "<configuration><typeAliases><typeAlias alias=\"string\" type=\"sample.User\"/></typeAliases></configuration>");
        MybatisProject project = new MybatisProject(xml(""));

        assertEquals("sample.User", project.resolveType("STRING").getQualifiedName());
        assertEquals("java.lang.Integer", project.resolveType("int").getQualifiedName());
    }

    public void testXmlPrimitiveAndArrayAliasesKeepSoftReferences() {
        XmlTag statement = xml("<select id=\"load\" resultType=\"_int\" parameterType=\"string[]\"/>").getRootTag().findFirstSubTag("select");

        for (String name : List.of("resultType", "parameterType")) {
            List<PsiReference> references = MybatisReferences.references(attr(statement, name));
            assertEquals(1, references.size());
            assertTrue(references.get(0).isSoft());
        }
    }

    public void testXmlParamAndNestedProperty() {
        mapper("User load(@Param(\"user\") User user);");
        XmlFile file = xml("<select id=\"load\">select #{user.address.city}</select>");
        XmlText text = file.getRootTag().findFirstSubTag("select").getValue().getTextElements()[0];
        List<PsiReference> refs = MybatisReferences.references(text);
        assertEquals(3, refs.size());
        assertInstanceOf(refs.get(0).resolve(), PsiParameter.class);
        assertEquals("city", ((PsiField) refs.get(2).resolve()).getName());
    }

    public void testForeachItemScopeAndMapUnknownKeys() {
        mapper("User load(@Param(\"users\") java.util.List<User> users);");
        XmlFile file = xml("<select id=\"load\"><foreach collection=\"users\" item=\"u\">#{u.name}</foreach></select>");
        XmlTag foreach = file.getRootTag().findFirstSubTag("select").findFirstSubTag("foreach");
        XmlBindings bindings = new XmlBindings(foreach.getValue().getTextElements()[0]);
        assertNotNull(bindings.resolve("u.name"));
        assertEquals("name", ((PsiField) bindings.resolve("u.name").source()).getName());
        assertNull(new XmlBindings(attr(foreach, "collection")).resolve("u.name"));
    }

    public void testXmlInheritedGenericParameterProperties() {
        myFixture.addClass("package sample; public interface BaseMapper<T, K> { int update(T entity); int delete(java.util.List<K> ids); }");
        myFixture.addClass("package sample; public interface UserMapper extends BaseMapper<User, Long> {}");
        XmlFile file = xml("<update id=\"update\" parameterType=\"sample.User\">update users set name=#{name}</update>");
        XmlText text = file.getRootTag().findFirstSubTag("update").getValue().getTextElements()[0];
        XmlBindings bindings = new XmlBindings(text);
        assertNotNull(bindings.resolve("name"));
        assertEquals("name", ((PsiField) bindings.resolve("name").source()).getName());
        assertTrue(bindings.candidates("").containsKey("name"));

        file = xml("<delete id=\"delete\"><foreach collection=\"list\" item=\"id\">#{id}</foreach></delete>");
        XmlTag foreach = file.getRootTag().findFirstSubTag("delete").findFirstSubTag("foreach");
        XmlBindings items = new XmlBindings(foreach.getValue().getTextElements()[0]);
        assertEquals("java.lang.Long", items.resolve("id").type().getCanonicalText());
    }

    public void testXmlInheritedNamedParameterAndMapperSignaturePrecedence() {
        myFixture.addClass("package sample; public interface BaseMapper<T> { int update(@org.apache.ibatis.annotations.Param(\"entity\") T entity); }");
        myFixture.addClass("package sample; public interface UserMapper extends BaseMapper<User> {}");
        xml("<update id=\"update\" parameterType=\"java.lang.String\">update users set name=#{entity.na<caret>me}</update>");
        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        assertNotNull(reference);
        assertInstanceOf(reference.resolve(), PsiField.class);
        assertEquals("name", ((PsiField) reference.resolve()).getName());
    }

    public void testXmlPlaceholderRegisteredReferenceAtCaret() {
        myFixture.addClass("package sample; public class Folder { private String id; public String getId() { return id; } }");
        mapper("int insert(Folder folder);");
        xml("<insert id=\"insert\" parameterType=\"sample.Folder\">\n    insert into folders values (#{i<caret>d,jdbcType=VARCHAR})\n</insert>");
        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        assertNotNull("XML 文本中的占位符必须进入 IDEA 的引用查找链路", reference);
        assertInstanceOf(reference.resolve(), PsiField.class);
        assertEquals("id", ((PsiField) reference.resolve()).getName());
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            reference.handleElementRename("folderId");
        });
        assertTrue(myFixture.getFile().getText().contains("#{folderId,jdbcType=VARCHAR}"));
    }

    public void testXmlFieldNavigationPreservesInheritedGetterType() {
        myFixture.addClass("package sample; public class Base<T> { private Object address; public T getAddress() { return null; } }");
        myFixture.addClass("package sample; public class Folder extends Base<User.Address> {}");
        mapper("int insert(@Param(\"folder\") Folder folder);");
        XmlFile file = xml("<insert id=\"insert\">#{folder.address.city}</insert>");
        XmlText text = file.getRootTag().findFirstSubTag("insert").getValue().getTextElements()[0];
        XmlBindings bindings = new XmlBindings(text);
        PsiField address = assertInstanceOf(bindings.resolve("folder.address").source(), PsiField.class);
        assertEquals("sample.Base", address.getContainingClass().getQualifiedName());
        assertEquals("sample.User.Address", bindings.resolve("folder.address").type().getCanonicalText());
        assertEquals("city", ((PsiField) bindings.resolve("folder.address.city").source()).getName());
        assertSame(address, bindings.candidates("folder").get("address").source());
        List<PsiReference> refs = MybatisReferences.references(text);
        assertSame(address, refs.get(1).resolve());
        assertEquals("city", ((PsiField) refs.get(2).resolve()).getName());

        file = xml("<resultMap id=\"folder\" type=\"sample.Folder\"><association property=\"address\"><result property=\"city\" column=\"city\"/></association></resultMap>");
        XmlTag association = file.getRootTag().findFirstSubTag("resultMap").findFirstSubTag("association");
        assertSame(address, targets(attr(association, "property")).get(0));
        assertEquals("city", ((PsiField) targets(attr(association.findFirstSubTag("result"), "property")).get(0)).getName());
    }

    public void testXmlGetterNavigationWithoutInstanceField() {
        myFixture.addClass("package sample; public class Folder { public static String id; public String getId() { return null; } public String getLabel() { return null; } }");
        mapper("int insert(Folder folder);");
        XmlFile file = xml("<insert id=\"insert\">#{id}, #{label}</insert>");
        XmlText text = file.getRootTag().findFirstSubTag("insert").getValue().getTextElements()[0];
        XmlBindings bindings = new XmlBindings(text);
        assertEquals("getId", ((PsiMethod) bindings.resolve("id").source()).getName());
        assertEquals("getLabel", ((PsiMethod) bindings.resolve("label").source()).getName());
        assertSame(bindings.resolve("label").source(), bindings.candidates("").get("label").source());

        file = xml("<resultMap id=\"folder\" type=\"sample.Folder\"><result property=\"label\" column=\"label\"/></resultMap>");
        XmlTag result = file.getRootTag().findFirstSubTag("resultMap").findFirstSubTag("result");
        assertEquals("getLabel", ((PsiMethod) targets(attr(result, "property")).get(0)).getName());
    }

    public void testGetterRenameUpdatesMetadataAndDefaultParameterProperties() {
        PsiClass row = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; @Table public class GetterRow { @Column public String getLabel() { return null; } }");
        PsiClass view = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; @TableRef(GetterRow.class) public class GetterView { @ColumnRef(\"label\") public String title; }");
        PsiClass mapper = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; import org.apache.ibatis.annotations.*; @MapTable(GetterRow.class) public interface GetterMapper { GetterRow get(@Param(\"label\") String value); }");
        PsiMethod getter = row.findMethodsByName("getLabel", false)[0];

        myFixture.renameElement(getter, "getTitle");

        assertEquals("title", PsiTreeUtil.findChildOfType(view, PsiLiteralExpression.class).getValue());
        assertEquals("title", PsiTreeUtil.findChildOfType(mapper, PsiLiteralExpression.class).getValue());
        assertTrue(new ExtSession(view).analyzer.analyzeMetadata(new PsiModel(getProject()).type(view)).isSuccess());
    }

    public void testBooleanGetterRenameUpdatesNestedMetadataPath() {
        PsiClass nested = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; public class Flags { @Column public boolean isActive() { return false; } }");
        myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; @Table public class FlagRow { @Column public Flags flags; }");
        PsiClass view = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; @TableRef(FlagRow.class) public class FlagView { @ColumnRef(\"flags.active\") public boolean enabled; }");
        PsiClass mapper = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; import org.apache.ibatis.annotations.*; @MapTable(FlagRow.class) public interface FlagMapper { FlagRow get(@Param(\"flagsDotActive\") boolean value); }");

        myFixture.renameElement(nested.findMethodsByName("isActive", false)[0], "isEnabled");

        assertEquals("flags.enabled", PsiTreeUtil.findChildOfType(view, PsiLiteralExpression.class).getValue());
        assertEquals("flagsDotEnabled", PsiTreeUtil.findChildOfType(mapper, PsiLiteralExpression.class).getValue());
    }

    public void testRootGetterRenameUpdatesEncodedParameterProperty() {
        myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; public class Flags { @Column public boolean isActive() { return false; } }");
        PsiClass row = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; @Table public class FlagRow { @Column public Flags getFlags() { return null; } }");
        PsiClass mapper = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; import org.apache.ibatis.annotations.*; @MapTable(FlagRow.class) public interface FlagMapper { FlagRow get(@Param(\"flagsDotActive\") boolean value); }");
        myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; @Table public class OtherRow { @Column public Flags getFlags() { return null; } }");
        PsiClass otherMapper = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; import org.apache.ibatis.annotations.*; @MapTable(OtherRow.class) public interface OtherMapper { OtherRow get(@Param(\"flagsDotActive\") boolean value); }");

        myFixture.renameElement(row.findMethodsByName("getFlags", false)[0], "getOptions");

        assertEquals("optionsDotActive", PsiTreeUtil.findChildOfType(mapper, PsiLiteralExpression.class).getValue());
        assertEquals("flagsDotActive", PsiTreeUtil.findChildOfType(otherMapper, PsiLiteralExpression.class).getValue());
    }

    public void testPlainMapperInspectionReportsMissingStatement() {
        myFixture.enableInspections(new MapperInspection());
        myFixture.configureByText("UserMapper.java", "package sample; @org.apache.ibatis.annotations.Mapper interface UserMapper { User load(); }");

        assertTrue(myFixture.doHighlighting().stream().anyMatch(h -> h.getDescription() != null && h.getDescription().contains("No MyBatis XML or annotation statement")));
    }

    public void testMapperInspectionSkipsEnhancedMapper() {
        myFixture.enableInspections(new MapperInspection());
        myFixture.configureByText("UserMapper.java", "package sample; @org.apache.ibatis.annotations.Mapper @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User get(); }");

        assertFalse(myFixture.doHighlighting().stream().anyMatch(h -> h.getDescription() != null && h.getDescription().contains("No MyBatis XML or annotation statement")));
    }

    public void testXmlBindingsUsesSharedScalarConfiguration() {
        myFixture.addClass("package sample; public class CustomValue {}");
        mapper("User load(CustomValue value);");
        XmlFile file = xml("<select id=\"load\">select #{arbitraryName}</select>");
        XmlText text = file.getRootTag().findFirstSubTag("select").getValue().getTextElements()[0];
        AnalysisSettings.State settings = getProject().getService(AnalysisSettings.class).getState();
        List<String> scalarTypes = settings.scalarTypes;
        try {
            settings.scalarTypes = List.of();
            assertNull(new XmlBindings(text).resolve("arbitraryName"));

            settings.scalarTypes = List.of("sample.CustomValue");
            assertEquals("sample.CustomValue", new XmlBindings(text).resolve("arbitraryName").type().getCanonicalText());
        } finally {
            settings.scalarTypes = scalarTypes;
        }
    }

    public void testGetterRenameUpdatesXmlResultAndParameterProperties() {
        PsiClass row = myFixture.addClass("package sample; public class GetterRow { public String getLabel() { return null; } }");
        mapper("GetterRow load(GetterRow row);");
        XmlFile file = xml("<resultMap id=\"row\" type=\"sample.GetterRow\"><result property=\"label\" column=\"label\"/></resultMap><select id=\"load\">select #{label}</select>");

        myFixture.renameElement(row.findMethodsByName("getLabel", false)[0], "getTitle");

        assertEquals("title", file.getRootTag().findFirstSubTag("resultMap").findFirstSubTag("result").getAttributeValue("property"));
        assertTrue(file.getText(), file.getText().contains("#{title}"));
        assertTrue(file.getText(), file.getText().contains("column=\"label\""));
    }

    public void testMapperGetterShapedMethodRenamePreservesMethodName() {
        PsiClass mapper = mapper("User getLabel();");
        XmlFile file = xml("<select id=\"getLabel\">select 1</select>");

        myFixture.renameElement(mapper.findMethodsByName("getLabel", false)[0], "getTitle");

        assertEquals("getTitle", file.getRootTag().findFirstSubTag("select").getAttributeValue("id"));
    }

    public void testXmlCdataPlaceholderRegisteredReferenceAtCaret() {
        mapper("User load(@Param(\"user\") User user);");
        xml("<select id=\"load\"><![CDATA[select 1 where name=#{user.na<caret>me} and 1 < 2]]></select>");
        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        assertNotNull(reference);
        assertInstanceOf(reference.resolve(), PsiField.class);
        assertEquals("name", ((PsiField) reference.resolve()).getName());
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            reference.handleElementRename("label");
        });
        XmlFile file = (XmlFile) myFixture.getFile();
        String script = file.getRootTag().findFirstSubTag("select").getValue().getTextElements()[0].getValue();
        assertEquals("select 1 where name=#{user.label} and 1 < 2", script);
    }

    public void testXmlParameterTypeFallbackRegisteredReferenceAtCaret() {
        xml("<update id=\"update\" parameterType=\"sample.User\"><if test=\"name != null\">name=#{na<caret>me}</if></update>");
        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        assertNotNull(reference);
        assertInstanceOf(reference.resolve(), PsiField.class);
        assertEquals("name", ((PsiField) reference.resolve()).getName());
    }

    public void testXmlPlaceholderCompletion() {
        mapper("User load(@Param(\"user\") User user);");
        xml("<select id=\"load\">select #{user.na<caret>}</select>");
        LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        if (items == null) {
            assertTrue(myFixture.getFile().getText().contains("#{user.name}"));
        } else {
            assertTrue(Arrays.stream(items).anyMatch(i -> i.getLookupString().equals("name")));
        }
    }

    public void testXmlDiagnosticsUnresolvedAndDuplicate() {
        mapper("User load(@Param(\"id\") Long id);");
        myFixture.enableInspections(new MybatisInspection());
        xml("<select id=\"load\">#{missing}</select><select id=\"load\">1</select><include refid=\"absent\"/>");
        var highlights = myFixture.doHighlighting();
        assertTrue(highlights.stream().anyMatch(h -> h.getDescription() != null && h.getDescription().contains("Duplicate MyBatis statement ID")));
        assertTrue(highlights.stream().anyMatch(h -> h.getDescription() != null && h.getDescription().contains("parameter or property not found")));
        assertTrue(highlights.stream().anyMatch(h -> h.getDescription() != null && h.getDescription().contains("sql reference not found")));
    }

    public void testPsiMetadataCrudAndNestedReferences() {
        PsiClass mapper = extMapper("User findByAddressDotCity(@Param(\"city\") String city);");
        ExtSession session = new ExtSession(mapper);
        var table = session.table(mapper);
        assertTrue(table.getDiagnostics().toString(), table.isSuccess());
        PsiMethod method = mapper.getMethods()[0];
        var result = session.analyzer.analyzeMethod(table.getValue(), session.model.method(method, session.model.type(mapper)));
        assertTrue(result.getDiagnostics().toString(), result.isSuccess());
        assertEquals(2, result.getReferences().size());
        PsiElement parent = ExtReferences.target(result.getReferences().get(0).getTarget());
        assertInstanceOf(parent, PsiField.class);
        assertEquals("address", ((PsiField) parent).getName());
        assertSame(method.getParameterList().getParameters()[0], ExtReferences.target(result.getReferences().get(1).getTarget()));
    }

    public void testExplicitXmlAndAnnotationsOverrideDerivedMethods() {
        PsiClass mapper = extMapper("User arbitrary(); @Select(\"select 1\") User explicit(); User find();");
        myFixture.addFileToProject("UserMapper.xml", "<mapper namespace=\"sample.UserMapper\"><select id=\"arbitrary\">select 1</select></mapper>");
        ExtSession session = new ExtSession(mapper);
        assertFalse(session.derived(mapper, mapper.findMethodsByName("arbitrary", false)[0]));
        assertFalse(session.derived(mapper, mapper.findMethodsByName("explicit", false)[0]));
        assertTrue(session.derived(mapper, mapper.findMethodsByName("find", false)[0]));
    }

    public void testGenericSuperclassAndAnnotationDefaults() {
        myFixture.addClass("package sample; public class Parent<T> { @io.github.mybatisext.annotation.Column public T inherited; }");
        PsiClass cls = myFixture.addClass("package sample; @io.github.mybatisext.annotation.Table public class Child extends Parent<String> { }");
        ExtSession session = new ExtSession(cls);
        var table = session.analyzer.analyzeMetadata(session.model.type(cls));
        assertTrue(table.isSuccess());
        assertEquals("java.lang.String", table.getValue().getNameToPropertyInfo().get("inherited").getJavaType().getName());
        assertEquals("", session.model.type(cls).annotation(Table.class).value("name"));
    }

    public void testMetadataPathReferencesAndEscapedDiagnosticRange() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("UserView.java", """
                package sample; import io.github.mybatisext.annotation.*;
                @TableRef(User.class) class UserView {
                    @ColumnRef("address.city") String city;
                    @ColumnRef("na\\155eX") String broken;
                }
                """);
        PsiClass cls = file.getClasses()[0];
        PsiLiteralExpression good = PsiTreeUtil.findChildOfType(cls.getFields()[0], PsiLiteralExpression.class);
        PsiReference[] refs = good.getReferences();
        assertTrue(Arrays.stream(refs).anyMatch(r -> r.resolve() instanceof PsiField f && f.getName().equals("city")));
        ExtSession session = new ExtSession(cls);
        Diagnostic diagnostic = session.analyzer.analyzeMetadata(session.model.type(cls)).getDiagnostics().get(0);
        ExtLocation.Highlight highlight = ExtLocation.locate(diagnostic, cls);
        assertInstanceOf(highlight.element(), PsiLiteralExpression.class);
        assertEquals("na\\155eX", highlight.range().substring(highlight.element().getText()));
    }

    public void testRepeatedAnnotationDiagnosticRequiresUniqueSource() {
        for (boolean ambiguous : List.of(false, true)) {
            String firstColumn = ambiguous ? "missing" : "name";
            PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("Joined.java", "package sample; import io.github.mybatisext.annotation.*; @Table class Joined { @Column Long id; "
                    + "@JoinRelation(table=User.class,column=\"" + firstColumn + "\",joinColumn=@JoinColumn(leftColumn=\"id\",rightColumn=\"id\")) "
                    + "@JoinRelation(table=User.class,column=\"missing\",joinColumn=@JoinColumn(leftColumn=\"id\",rightColumn=\"id\")) String userName; }");
            PsiClass cls = file.getClasses()[0];
            ExtSession session = new ExtSession(cls);
            Diagnostic diagnostic = session.analyzer.analyzeMetadata(session.model.type(cls)).getDiagnostics().get(0);

            ExtLocation.Highlight highlight = ExtLocation.locate(diagnostic, cls);

            if (ambiguous) {
                assertSame(cls.getFields()[1].getNameIdentifier(), highlight.element());
                assertNull(highlight.range());
            } else {
                assertEquals("missing", assertInstanceOf(highlight.element(), PsiLiteralExpression.class).getValue());
                assertEquals("missing", highlight.range().substring(highlight.element().getText()));
            }
        }
    }

    public void testColumnRefDedicatedGutterTargets() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("UserView.java", """
                package sample; import io.github.mybatisext.annotation.*;
                @TableRef(User.class) class UserView {
                    @ColumnRef("address.city") String city;
                    @ColumnRef Long id;
                    @ColumnRef("") String name;
                    @ColumnRef("na\\155e") public String getDisplayName() { return null; }
                    }
                @TableRef(User.class) class GetterView {
                    @ColumnRef public Long getId() { return null; }
                }
                """);
        var markers = markers(file, new ExtNavigation());
        assertEquals(markers.stream().map(m -> m.getElement().getText()).toList().toString(), 5, markers.size());
        List<String> names = new ArrayList<>();
        for (var marker : markers) {
            assertSame(MybatisIcons.MAPPER, marker.getIcon());
            PsiNameIdentifierOwner member = assertInstanceOf(marker.getElement().getParent(), PsiNameIdentifierOwner.class);
            assertSame(member.getNameIdentifier(), marker.getElement());
            var items = marker.createGotoRelatedItems();
            assertEquals(1, items.size());
            PsiField target = assertInstanceOf(items.iterator().next().getElement(), PsiField.class);
            names.add(target.getName());
            assertEquals(target.getName().equals("city") ? "sample.User.Address" : "sample.User", target.getContainingClass().getQualifiedName());
        }
        assertEquals(List.of("city", "id", "name", "name", "id"), names);
    }

    public void testColumnRefGutterRequiresReferencedTable() {
        PsiFile file = myFixture.configureByText("Plain.java", "package sample; import io.github.mybatisext.annotation.*; class Plain { @ColumnRef(\"name\") String name; }");
        assertEmpty(markers(file, new ExtNavigation()));
    }

    public void testJoinRelationGutterExplicitAndInferredTargets() {
        PsiFile file = myFixture.configureByText("Joined.java", """
                package sample; import io.github.mybatisext.annotation.*;
                @Table class Joined {
                    @Column Long userId;
                    @JoinRelation(table=User.class,column="name",joinColumn=@JoinColumn(leftColumn="userId",rightColumn="id")) String userName;
                    @JoinRelation(table=User.class,joinColumn=@JoinColumn(leftColumn="userId",rightColumn="id")) Long id;
                    @JoinRelation(joinColumn=@JoinColumn(leftColumn="userId",rightColumn="id")) User user;
                    @JoinRelation(joinColumn=@JoinColumn(leftColumn="userId",rightColumn="id")) java.util.List<User> users;
                    @JoinRelation(joinColumn=@JoinColumn(leftColumn="userId",rightColumn="id")) public User getOwner() { return null; }
                }
                """);
        PsiClass joined = ((PsiJavaFile) file).getClasses()[0];
        ExtSession session = new ExtSession(joined);
        var metadata = session.analyzer.analyzeMetadata(session.model.type(joined));
        assertTrue(metadata.getDiagnostics().stream().map(Diagnostic::getMessage).toList().toString(), metadata.isSuccess());
        var markers = markers(file, new ExtNavigation());
        assertEquals(5, markers.size());
        for (int i = 0; i < markers.size(); i++) {
            var marker = markers.get(i);
            assertSame(MybatisIcons.MAPPER, marker.getIcon());
            PsiNameIdentifierOwner member = assertInstanceOf(marker.getElement().getParent(), PsiNameIdentifierOwner.class);
            assertSame(member.getNameIdentifier(), marker.getElement());
            var items = marker.createGotoRelatedItems();
            assertEquals(1, items.size());
            PsiElement target = items.iterator().next().getElement();
            if (i <= 1) {
                PsiField field = assertInstanceOf(target, PsiField.class);
                assertEquals(i == 0 ? "name" : "id", field.getName());
                assertEquals("sample.User", field.getContainingClass().getQualifiedName());
            } else {
                assertEquals("sample.User", assertInstanceOf(target, PsiClass.class).getQualifiedName());
            }
        }
    }

    public void testJoinRelationGutterRepeatedAndContainerAnnotations() {
        PsiFile file = myFixture.configureByText("Joined.java", """
                package sample; import io.github.mybatisext.annotation.*;
                @Table class Joined {
                    @Column Long userId;
                    @JoinRelation(table=User.class,column="name",joinColumn=@JoinColumn(leftColumn="userId",rightColumn="id"))
                    @JoinRelation(table=User.class,column="id",joinColumn=@JoinColumn(leftColumn="id",rightColumn="id")) String name;
                    @JoinRelations({
                        @JoinRelation(table=User.class,column="id",joinColumn=@JoinColumn(leftColumn="userId",rightColumn="id")),
                        @JoinRelation(table=User.class,column="name",joinColumn=@JoinColumn(leftColumn="id",rightColumn="id"))
                    }) String other;
                }
                """);
        var markers = markers(file, new ExtNavigation());
        assertEquals(2, markers.size());
        for (var marker : markers) {
            PsiField member = assertInstanceOf(marker.getElement().getParent(), PsiField.class);
            assertSame(member.getNameIdentifier(), marker.getElement());
            var items = marker.createGotoRelatedItems();
            assertEquals(1, items.size());
            PsiField target = assertInstanceOf(items.iterator().next().getElement(), PsiField.class);
            assertEquals(member.getName().equals("name") ? "id" : "name", target.getName());
        }
    }

    public void testMetadataGuttersStayOnDeclarationLinesAndDeduplicateTargets() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("UserView.java", """
                package sample; import io.github.mybatisext.annotation.*;
                @TableRef(User.class) class UserView {
                    @ColumnRef("name")
                    private String name;
                    @JoinRelation(table=User.class,column="id",joinColumn={})
                    @JoinRelation(table=User.class,column="id",joinColumn={})
                    public Long getId() { return null; }
                }
                """);
        var markers = markers(file, new ExtNavigation());
        assertEquals(2, markers.size());
        var document = PsiDocumentManager.getInstance(getProject()).getDocument(file);
        for (var marker : markers) {
            PsiModifierListOwner member = assertInstanceOf(marker.getElement().getParent(), PsiModifierListOwner.class);
            assertSame(((PsiNameIdentifierOwner) member).getNameIdentifier(), marker.getElement());
            assertEquals(document.getLineNumber(marker.getElement().getTextOffset()), document.getLineNumber(marker.startOffset));
            assertTrue(document.getLineNumber(member.getAnnotations()[0].getTextOffset()) < document.getLineNumber(marker.startOffset));
            assertEquals(1, marker.createGotoRelatedItems().size());
        }
    }

    public void testJoinRelationGutterOmitsUnresolvedAndUnrelatedAnnotations() {
        myFixture.addClass("package other; public @interface JoinRelation { }");
        PsiFile file = myFixture.configureByText("Broken.java", """
                package sample; import io.github.mybatisext.annotation.*;
                @Table class Broken {
                    @JoinRelation(table=Missing.class,joinColumn={}) String missing;
                    @JoinRelation(table=User.class,column="missing",joinColumn={}) String missingProperty;
                    @JoinRelation(table=String.class,joinColumn={}) String notATable;
                    @JoinRelation(joinColumn={}) java.util.List raw;
                    @other.JoinRelation String unrelated;
                }
                """);
        assertEmpty(markers(file, new ExtNavigation()));
    }

    public void testJoinRelationGutterNeverFallsBackToIntermediateTarget() {
        PsiFile file = myFixture.configureByText("Joined.java", """
                package sample; import io.github.mybatisext.annotation.*;
                @Table class Joined {
                    @JoinRelation(table=User.class,column="name",joinColumn={})
                    @JoinRelation(table=User.class,column="missing",joinColumn={}) String broken;
                    @JoinRelations({
                        @JoinRelation(table=User.class,column="id",joinColumn={}),
                        @JoinRelation(table=Missing.class,joinColumn={})
                    }) String unresolved;
                    @JoinRelation(table=Missing.class,joinColumn={})
                    @JoinRelation(table=User.class,column="id",joinColumn={}) Long resolved;
                }
                """);
        var markers = markers(file, new ExtNavigation());
        assertEmpty(markers);
    }

    public void testDefaultParamPropertyNavigation() {
        PsiClass mapper = extMapper("User get(@Param(\"id\") Long value);");
        PsiLiteralExpression literal = PsiTreeUtil.findChildOfType(mapper.getMethods()[0], PsiLiteralExpression.class);
        PsiReference reference = Arrays.stream(literal.getReferences()).filter(r -> r.resolve() instanceof PsiField).findFirst().orElseThrow();
        assertEquals("id", ((PsiField) reference.resolve()).getName());
        assertEquals("id", reference.getRangeInElement().substring(literal.getText()));
    }

    public void testDefaultParamNestedPropertyNavigation() {
        PsiClass mapper = extMapper("User get(@Param(\"addressDotCity\") String value);");
        PsiLiteralExpression literal = PsiTreeUtil.findChildOfType(mapper.getMethods()[0], PsiLiteralExpression.class);
        List<PsiReference> references = Arrays.stream(literal.getReferences()).filter(r -> r.resolve() instanceof PsiField).toList();
        assertEquals(2, references.size());
        assertEquals("address", ((PsiField) references.get(0).resolve()).getName());
        assertEquals("address", references.get(0).getRangeInElement().substring(literal.getText()));
        assertEquals("city", ((PsiField) references.get(1).resolve()).getName());
        assertEquals("City", references.get(1).getRangeInElement().substring(literal.getText()));
    }

    public void testDefaultParamNavigationSurvivesEarlierInvalidProperty() {
        PsiClass mapper = extMapper("""
                User get(@Param("missing") String first, @Param("id") Long second);
                """);
        PsiLiteralExpression literal = PsiTreeUtil.findChildOfType(mapper.getMethods()[0].getParameterList().getParameters()[1], PsiLiteralExpression.class);
        assertTrue(Arrays.stream(literal.getReferences()).anyMatch(r -> r.resolve() instanceof PsiField field && field.getName().equals("id")));
        ExtSession session = new ExtSession(mapper);
        var analyzed = session.analyzer.analyzeMethod(session.table(mapper).getValue(), session.model.method(mapper.getMethods()[0], session.model.type(mapper)));
        assertFalse(analyzed.isSuccess());
        assertTrue(analyzed.getDiagnostics().get(0).getMessage().contains("Parameter: Missing"));
    }

    public void testDefaultParamCompletionSurvivesEarlierInvalidProperty() {
        myFixture.configureByText("UserMapper.java", """
                package sample;
                @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper {
                    User get(@org.apache.ibatis.annotations.Param("missing") Long first,
                             @org.apache.ibatis.annotations.Param("na<caret>") String second);
                }
                """);
        myFixture.completeBasic();
        if (myFixture.getLookup() != null) {
            myFixture.finishLookup('\n');
        }
        assertTrue(myFixture.getFile().getText(), myFixture.getFile().getText().contains("Param(\"name\")"));
    }

    public void testDefaultParamNestedRenamePreservesGrammar() {
        PsiClass mapper = extMapper("""
                User get(@Param("addressDotCity") String value);
                """);
        PsiLiteralExpression literal = PsiTreeUtil.findChildOfType(mapper.getMethods()[0], PsiLiteralExpression.class);
        PsiReference reference = Arrays.stream(literal.getReferences()).filter(r -> r.resolve() instanceof PsiField field && field.getName().equals("city")).findFirst().orElseThrow();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            PsiLiteralExpression renamed = (PsiLiteralExpression) reference.handleElementRename("town");
            assertEquals("addressDotTown", renamed.getValue());
        });
    }

    public void testDefaultParamRootRenamePreservesPropertyName() {
        PsiClass mapper = extMapper("""
                User get(@Param("id") Long value);
                """);
        PsiLiteralExpression literal = PsiTreeUtil.findChildOfType(mapper.getMethods()[0], PsiLiteralExpression.class);
        PsiReference reference = Arrays.stream(literal.getReferences()).filter(r -> r.resolve() instanceof PsiField).findFirst().orElseThrow();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            PsiLiteralExpression renamed = (PsiLiteralExpression) reference.handleElementRename("userId");
            assertEquals("userId", renamed.getValue());
        });
    }

    public void testDefaultParamCollectionPropertyNavigation() {
        PsiClass mapper = extMapper("java.util.List<User> list(@Param(\"id\") java.util.List<Long> values);");
        PsiLiteralExpression literal = PsiTreeUtil.findChildOfType(mapper.getMethods()[0], PsiLiteralExpression.class);
        assertTrue(Arrays.stream(literal.getReferences()).anyMatch(r -> r.resolve() instanceof PsiField field && field.getName().equals("id")));
    }

    public void testDefaultParamPropertyErrorHighlightsEscapedLiteral() {
        myFixture.enableInspections(new ExtInspection());
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User get(@org.apache.ibatis.annotations.Param(\"na\\155eX\") String value); }");
        var errors = myFixture.doHighlighting().stream().filter(h -> h.getDescription() != null && h.getDescription().contains("Parameter:")).toList();
        assertEquals(1, errors.size());
        var error = errors.get(0);
        assertEquals("na\\155eX", myFixture.getFile().getText().substring(error.getStartOffset(), error.getEndOffset()));
    }

    public void testDefaultParamPropertyCompletion() {
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User get(@org.apache.ibatis.annotations.Param(\"na<caret>\") String value); }");
        myFixture.completeBasic();
        if (myFixture.getLookup() != null) {
            myFixture.finishLookup('\n');
        }
        assertTrue(myFixture.getFile().getText(), myFixture.getFile().getText().contains("@org.apache.ibatis.annotations.Param(\"name\")"));
    }

    public void testDefaultParamEmptyPropertyCompletion() {
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User get(@org.apache.ibatis.annotations.Param(\"<caret>\") Long value); }");
        LookupElement[] items = myFixture.completeBasic();
        assertNotNull(items);
        LookupElement candidate = Arrays.stream(items).filter(item -> item.getLookupString().equals("id")).findFirst().orElseThrow();
        myFixture.getLookup().setCurrentItem(candidate);
        myFixture.finishLookup('\n');
        assertTrue(myFixture.getFile().getText(), myFixture.getFile().getText().contains("Param(\"id\")"));
    }

    public void testDefaultParamEscapedPropertyCompletion() {
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User get(@org.apache.ibatis.annotations.Param(\"na\\155<caret>\") String value); }");
        myFixture.completeBasic();
        if (myFixture.getLookup() != null) {
            myFixture.finishLookup('\n');
        }
        assertTrue(myFixture.getFile().getText(), myFixture.getFile().getText().contains("Param(\"name\")"));
    }

    public void testParamAliasDoesNotOfferPropertyCompletion() {
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User getById(@org.apache.ibatis.annotations.Param(\"na<caret>\") Long value); }");
        LookupElement[] items = myFixture.completeBasic();
        assertTrue(items == null || Arrays.stream(items).noneMatch(item -> item.getLookupString().equals("name")));
        assertFalse(myFixture.getFile().getText().contains("Param(\"name\")"));
    }

    public void testDefaultParamNestedCompletionPreservesSuffix() {
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User get(@org.apache.ibatis.annotations.Param(\"addressDotCi<caret>ty\") String value); }");
        myFixture.completeBasic();
        if (myFixture.getLookup() != null) {
            myFixture.finishLookup('\n');
        }
        assertTrue(myFixture.getFile().getText(), myFixture.getFile().getText().contains("@org.apache.ibatis.annotations.Param(\"addressDotCity\")"));
    }

    public void testParamAliasesAndEntityParametersAreNotProperties() {
        PsiClass mapper = extMapper("User getById(@Param(\"alias\") Long value); User find(@Param(\"entity\") User value); @Select(\"select 1\") User get(@Param(\"custom\") Long value);");
        for (PsiMethod method : mapper.getMethods()) {
            PsiLiteralExpression literal = (PsiLiteralExpression) method.getParameterList().getParameters()[0].getAnnotation(MybatisProject.PARAM).findAttributeValue("value");
            assertEmpty(ExtReferences.references(literal));
            ExtSession session = new ExtSession(method);
            if (session.derived(mapper, method)) {
                assertTrue(session.analyzer.analyzeMethod(session.table(mapper).getValue(), session.model.method(method, session.model.type(mapper))).isSuccess());
            }
        }
    }

    public void testDefaultParamPropertyAfterLimitBinding() {
        PsiClass mapper = extMapper("User getLimitAlias(@Param(\"alias\") Integer value, @Param(\"id\") Long id);");
        PsiMethod method = mapper.getMethods()[0];
        PsiLiteralExpression alias = PsiTreeUtil.findChildOfType(method.getParameterList().getParameters()[0], PsiLiteralExpression.class);
        PsiLiteralExpression property = PsiTreeUtil.findChildOfType(method.getParameterList().getParameters()[1], PsiLiteralExpression.class);
        assertEmpty(ExtReferences.references(alias));
        assertTrue(Arrays.stream(property.getReferences()).anyMatch(r -> r.resolve() instanceof PsiField field && field.getName().equals("id")));
    }

    public void testFilterableSecondParamRemainsAnAlias() {
        PsiClass mapper = extMapper("User get(@Param(\"id\") @Filterable(operator=io.github.mybatisext.jpa.CompareOperator.Between, secondVariable=\"upper\") Long low, @Param(\"upper\") Long high);");
        PsiMethod method = mapper.getMethods()[0];
        PsiLiteralExpression property = (PsiLiteralExpression) method.getParameterList().getParameters()[0].getAnnotation(MybatisProject.PARAM).findAttributeValue("value");
        PsiLiteralExpression alias = PsiTreeUtil.findChildOfType(method.getParameterList().getParameters()[1], PsiLiteralExpression.class);
        assertTrue(Arrays.stream(property.getReferences()).anyMatch(r -> r.resolve() instanceof PsiField field && field.getName().equals("id")));
        assertEmpty(ExtReferences.references(alias));
    }

    public void testCrudCompletionRanksBeforeJavaMethodNameSuggestions() {
        for (String name : List.of("listBy<caret>", "listByIdNotBetweenO<caret>", "listNameBy<caret>IdGreaterThanGroupByNameHavingNameIsTargetOrderByNameLimitCount")) {
            myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { java.util.List<String> "
                    + name + "(long lower, long upper); }");
            LookupElement[] items = myFixture.completeBasic();
            assertNotNull(items);
            boolean foundJavaSuggestion = false;
            boolean foundCrudSuggestion = false;
            for (LookupElement item : myFixture.getLookup().getItems()) {
                LookupElementPresentation presentation = new LookupElementPresentation();
                item.renderElement(presentation);
                if (presentation.getTypeText() != null && presentation.getTypeText().startsWith("mybatis-ext ")) {
                    assertFalse("CRUD candidate follows Java suggestion: " + presentation.getItemText(), foundJavaSuggestion);
                    foundCrudSuggestion = true;
                } else {
                    foundJavaSuggestion = true;
                }
            }
            assertTrue(name, foundCrudSuggestion);
            assertTrue("Java method name suggestions should remain available: " + name, foundJavaSuggestion);
        }
    }

    public void testCrudCompletionPreservesSuffix() {
        myFixture.configureByText("UserMapper.java", "package sample; import io.github.mybatisext.annotation.*; @MapTable(User.class) interface UserMapper { User findByNa<caret>OrderById(String name); }");
        LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        if (items != null) {
            LookupElement candidate = Arrays.stream(items).filter(i -> i.getLookupString().equals("findByNameOrderById")).findFirst().orElseThrow();
            myFixture.getLookup().setCurrentItem(candidate);
            myFixture.finishLookup('\n');
        }
        assertTrue(myFixture.getFile().getText(), myFixture.getFile().getText().contains("findByNameOrderById("));
        assertFalse(myFixture.getFile().getText().contains("OrderByIdOrderById"));
    }

    public void testCrudCompletionKeepsKeywordsWithTheSameReplacementDistinct() {
        String prefix = "listByIdNotBetween";
        for (String suffix : List.of("rderById", "")) {
            for (String keyword : List.of("Or", "OrderBy")) {
                myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { java.util.List<User> "
                        + prefix + "O<caret>" + suffix + "(long lower, long upper); }");
                LookupElement[] items = myFixture.completeBasic();
                assertNotNull(items);
                LookupElement candidate = null;
                for (LookupElement item : items) {
                    LookupElementPresentation presentation = new LookupElementPresentation();
                    item.renderElement(presentation);
                    if (keyword.equals(presentation.getItemText())) {
                        candidate = item;
                        break;
                    }
                }
                assertNotNull("Missing " + keyword + " with suffix: " + suffix, candidate);
                myFixture.getLookup().setCurrentItem(candidate);
                myFixture.finishLookup('\n');

                String text = myFixture.getFile().getText();
                String expectedName = prefix + (suffix.isEmpty() ? keyword : "OrderById");
                assertTrue(text, text.contains(expectedName + "(long lower, long upper)"));
                assertFalse(text, text.contains("OrderByIdOrderById"));
                assertEquals(text.indexOf(prefix) + prefix.length() + keyword.length(), myFixture.getCaretOffset());
            }
        }
    }

    public void testCrudCompletionKeepsPropertiesWithTheSameReplacementDistinct() {
        myFixture.addClass("package sample; @io.github.mybatisext.annotation.Table public class Account { @io.github.mybatisext.annotation.Column public String user; @io.github.mybatisext.annotation.Column public String userName; }");
        String prefix = "listBy";
        for (String suffix : List.of("serName", "serNameOrderByUser", "")) {
            for (String property : List.of("User", "UserName")) {
                myFixture.configureByText("AccountMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(Account.class) interface AccountMapper { java.util.List<Account> "
                        + prefix + "U<caret>" + suffix + "(String value); }");
                LookupElement[] items = myFixture.completeBasic();
                assertNotNull(items);
                LookupElement candidate = null;
                for (LookupElement item : items) {
                    LookupElementPresentation presentation = new LookupElementPresentation();
                    item.renderElement(presentation);
                    if (property.equals(presentation.getItemText())) {
                        candidate = item;
                        break;
                    }
                }
                assertNotNull("Missing " + property + " with suffix: " + suffix, candidate);
                myFixture.getLookup().setCurrentItem(candidate);
                myFixture.finishLookup('\n');

                String text = myFixture.getFile().getText();
                String expectedName = prefix + (suffix.isEmpty() ? property : "U" + suffix);
                assertTrue(text, text.contains(expectedName + "(String value)"));
                assertEquals(text.indexOf(prefix) + prefix.length() + property.length(), myFixture.getCaretOffset());
            }
        }
    }

    public void testCrudCompletionUsesOriginalMethodInIncompleteDeclarations() {
        for (String declaration : List.of(
                "User findByNa<caret>(String value);",
                "User findByN<caret>ame(String value);",
                "User <caret>findByName(String value);",
                "User findByNa<caret>(String value)",
                "User findByNa<caret>(",
                "findByNa<caret>(String value);",
                "MissingType findByNa<caret>(String value);",
                "User findByNa<caret>(MissingType value);",
                "User\n findByNa<caret>(String value);")) {
            myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { " + declaration + " }");
            LookupElement[] items = myFixture.completeBasic();
            if (myFixture.getLookup() != null) {
                assertNotNull(declaration, items);
                LookupElement candidate = Arrays.stream(items).filter(i -> i.getLookupString().equals("findByName")).findFirst().orElseThrow();
                myFixture.getLookup().setCurrentItem(candidate);
                myFixture.finishLookup('\n');
            }
            String text = myFixture.getFile().getText();
            assertTrue(declaration + " -> " + text, text.contains("findByName("));
        }
    }

    public void testCrudInspectionDiagnosticAndNavigation() {
        myFixture.enableInspections(new ExtInspection());
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User findByMissing(); }");
        assertTrue(myFixture.doHighlighting().stream().anyMatch(h -> h.getDescription() != null && (h.getDescription().contains("Unexpected") || h.getDescription().contains("Expected") || h.getDescription().contains("expected"))));
    }

    public void testBindScopeAndDynamicMapKeys() {
        mapper("User load(@Param(\"options\") java.util.Map<String,Object> options);");
        XmlFile file = xml("<select id=\"load\"><bind name=\"pattern\" value=\"'%'\"/>#{pattern},#{options.arbitrary}</select>");
        XmlText text = file.getRootTag().findFirstSubTag("select").getValue().getTextElements()[0];
        XmlBindings bindings = new XmlBindings(text);
        assertNotNull(bindings.resolve("pattern"));
        assertTrue(bindings.resolve("options.arbitrary").dynamic());
        assertTrue(MybatisReferences.references(text).stream().allMatch(r -> r.resolve() != null || r.isSoft()));
    }

    public void testMetadataPathCompletion() {
        myFixture.configureByText("UserView.java", "package sample; import io.github.mybatisext.annotation.*; @TableRef(User.class) class UserView { @ColumnRef(\"address.ci<caret>\") String city; }");
        LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        if (items == null) {
            assertTrue(myFixture.getFile().getText().contains("address.city"));
        } else {
            assertTrue(Arrays.stream(items).anyMatch(i -> i.getLookupString().equals("city")));
        }
    }

    public void testRepeatedJoinRelationLeftAndRightOwnership() {
        PsiJavaFile file = (PsiJavaFile) myFixture.configureByText("Joined.java", """
                package sample; import io.github.mybatisext.annotation.*;
                @Table class Joined {
                    @Column Long userId;
                    @JoinRelation(table=User.class,tableAlias="u",joinColumn=@JoinColumn(leftColumn="userId",rightColumn="id"))
                    @JoinRelation(table=User.class,column="name",joinColumn=@JoinColumn(leftTableAlias="u",leftColumn="id",rightColumn="id"))
                    String userName;
                }
                """);
        List<PsiLiteralExpression> literals = new ArrayList<>(PsiTreeUtil.findChildrenOfType(file.getClasses()[0].getFields()[1], PsiLiteralExpression.class));
        PsiLiteralExpression left = literals.stream().filter(l -> "userId".equals(l.getValue())).findFirst().orElseThrow();
        assertTrue(Arrays.stream(left.getReferences()).anyMatch(r -> r.resolve() instanceof PsiField f && f.getName().equals("userId")));
        for (PsiLiteralExpression id : literals.stream().filter(l -> "id".equals(l.getValue())).toList()) {
            assertTrue(id.getText(), Arrays.stream(id.getReferences()).anyMatch(r -> r.resolve() instanceof PsiField f && f.getName().equals("id") && f.getContainingClass().getName().equals("User")));
        }
    }

    private PsiReference leftAliasReference(String source) {
        myFixture.configureByText("Joined.java", "package sample; import io.github.mybatisext.annotation.*; " + source);
        return myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
    }

    private void assertLeftAliasTarget(String source, String annotation, String attribute) {
        PsiReference reference = leftAliasReference(source);
        assertNotNull(reference);
        PsiElement target = reference.resolve();
        assertNotNull(target);
        PsiNameValuePair pair = PsiTreeUtil.getParentOfType(target, PsiNameValuePair.class);
        assertNotNull(pair);
        assertEquals(attribute, pair.getName());
        PsiAnnotation declaration = PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class);
        assertEquals("io.github.mybatisext.annotation." + annotation, declaration.getQualifiedName());
        assertEquals(declaration.findDeclaredAttributeValue(attribute), target);
        assertEquals("\"u\"", target.getText());
        assertEquals("u", reference.getRangeInElement().substring(reference.getElement().getText()));
    }

    public void testLeftTableAliasNavigatesToPreviousRelationDeclaration() {
        String first = "@JoinRelation(table=User.class,tableAlias=\"u\",joinColumn={})";
        String second = "@JoinRelation(table=User.class,column=\"name\",joinColumn=@JoinColumn(leftTableAlias=\"<caret>u\",leftColumn=\"id\",rightColumn=\"id\"))";
        assertLeftAliasTarget("@Table class Joined { " + first + second + " String name; }", "JoinRelation", "tableAlias");
        assertLeftAliasTarget("@Table class Joined { @JoinRelations({" + first + "," + second + "}) public String getName() { return null; } }", "JoinRelation", "tableAlias");
    }

    public void testLeftTableAliasNavigatesToTableAndParentDeclarations() {
        String member = "@JoinRelation(table=User.class,column=\"name\",joinColumn=@JoinColumn(leftTableAlias=\"<caret>u\",leftColumn=\"id\",rightColumn=\"id\")) String name;";
        assertLeftAliasTarget("@Table(alias=\"u\") class Joined { @Column Long id; " + member + " }", "Table", "alias");
        assertLeftAliasTarget("@TableRef(value=User.class,alias=\"u\") class Joined { " + member + " }", "TableRef", "alias");
        assertLeftAliasTarget("@Table @JoinParent(alias=\"u\",joinColumn={}) class Joined extends User { " + member + " }", "JoinParent", "alias");
    }

    public void testLeftTableAliasDoesNotResolveOutsideItsScope() {
        String first = "@JoinRelation(table=User.class,tableAlias=\"u\",joinColumn={})";
        String second = "@JoinRelation(table=User.class,column=\"name\",joinColumn=@JoinColumn(leftTableAlias=\"<caret>u\",leftColumn=\"id\",rightColumn=\"id\"))";
        for (String source : List.of(
                "@Table class Joined { " + second + first + " String name; }",
                "@Table class Joined { " + first + " User other; " + second + " String name; }",
                "@Table(alias=\"u\") class Joined { " + first + second + " String name; }",
                "@Table class Joined { " + second.replace("column=\"name\"", "column=\"name\",tableAlias=\"u\"") + " String name; }",
                "@Table class Joined { " + second.replace("<caret>u", "<caret>") + " String name; }",
                "@Table class Joined { " + second + " String name; }")) {
            PsiReference reference = leftAliasReference(source);
            assertTrue(source, reference == null || reference.resolve() == null);
        }
    }

    public void testNamespaceAndQualifiedTypeCompletion() {
        mapper("User load();");
        myFixture.configureByText("UserMapper.xml", "<mapper namespace=\"sample.UserM<caret>\"/>");
        LookupElement[] items = myFixture.complete(CompletionType.BASIC);
        if (items == null) {
            assertTrue(myFixture.getFile().getText().contains("sample.UserMapper"));
        } else {
            assertTrue(Arrays.stream(items).anyMatch(i -> i.getLookupString().equals("sample.UserMapper")));
        }
        myFixture.configureByText("UserMapper.xml", "<mapper namespace=\"sample.UserMapper\"><select id=\"load\" resultType=\"sample.Us<caret>\">1</select></mapper>");
        items = myFixture.complete(CompletionType.BASIC);
        if (items == null) {
            assertTrue(myFixture.getFile().getText().contains("resultType=\"sample.User\""));
        } else {
            assertTrue(Arrays.stream(items).anyMatch(i -> i.getLookupString().equals("sample.User")));
        }
    }

    public void testCrudConditionGotoDeclarationPrefersUnnamedParameter() {
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User findByNa<caret>me(String name); }");
        PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
        PsiElement[] targets = crudTargets(method.getNameIdentifier(), myFixture.getCaretOffset());
        assertNotNull(targets);
        assertEquals(List.of(method.getParameterList().getParameters()[0]), Arrays.asList(targets));
    }

    public void testCrudConditionGotoDeclarationUsesActualNamedBinding() {
        myFixture.configureByText("UserMapper.java", """
                package sample;
                import org.apache.ibatis.annotations.Param;
                @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper {
                    User findByNa<caret>meAndId(@Param("needle") String search, @Param("key") Long value);
                }
                """);
        PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
        int offset = method.getNameIdentifier().getTextOffset();
        assertEquals(List.of(method.getParameterList().getParameters()[0]), Arrays.asList(crudTargets(method.getNameIdentifier(), offset + "findByNa".length())));
        assertEquals(List.of(method.getParameterList().getParameters()[1]), Arrays.asList(crudTargets(method.getNameIdentifier(), offset + "findByNameAndI".length())));
    }

    public void testCrudConditionGotoDeclarationPreservesMixedAndRepeatedBindingOrder() {
        myFixture.configureByText("UserMapper.java", """
                package sample;
                import org.apache.ibatis.annotations.Param;
                @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper {
                    User findByI<caret>dOrIdAndName(@Param("first") Long first, @Param("second") Long second, @Param("third") String third);
                }
                """);
        PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
        int offset = method.getNameIdentifier().getTextOffset();
        String[] prefixes = {"findByI", "findByIdOrI", "findByIdOrIdAndNa"};
        for (int i = 0; i < prefixes.length; i++) {
            assertEquals(List.of(method.getParameterList().getParameters()[i]), Arrays.asList(crudTargets(method.getNameIdentifier(), offset + prefixes[i].length())));
        }
    }

    public void testCrudConditionGotoDeclarationHandlesNamedBetweenAndExplicitVariables() {
        PsiClass mapper = extMapper("""
                User findById(@Param("id") Long value);
                User findByIdBetween(@Param("low") Long min, @Param("high") Long max);
                User findByIdEqualsKey(@Param("key") Long value);
                User findByNameIsNull(@Param("name") String value);
                """);
        for (String name : new String[]{"findById", "findByIdBetween", "findByIdEqualsKey"}) {
            PsiMethod method = mapper.findMethodsByName(name, false)[0];
            int offset = method.getNameIdentifier().getTextOffset() + "findByI".length();
            PsiElement[] targets = crudTargets(method.getNameIdentifier(), offset);
            assertEquals(Arrays.asList(method.getParameterList().getParameters()), Arrays.asList(targets));
            if (name.equals("findByIdEqualsKey")) {
                offset = method.getNameIdentifier().getTextOffset() + "findByIdEqualsK".length();
                targets = crudTargets(method.getNameIdentifier(), offset);
                assertEquals(Arrays.asList(method.getParameterList().getParameters()), Arrays.asList(targets));
            }
        }
        PsiMethod nullCheck = mapper.findMethodsByName("findByNameIsNull", false)[0];
        PsiElement[] targets = crudTargets(nullCheck.getNameIdentifier(), nullCheck.getNameIdentifier().getTextOffset() + "findByNa".length());
        assertEquals(1, targets.length);
        assertInstanceOf(targets[0], PsiField.class);
    }

    public void testCrudConditionGotoDeclarationSkipsSpecialParameters() {
        myFixture.configureByText("UserMapper.java", """
                package sample;
                import org.apache.ibatis.annotations.Param;
                @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper {
                    User findByI<caret>d(org.apache.ibatis.session.RowBounds bounds, @Param("id") Long value);
                }
                """);
        PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
        PsiElement[] targets = crudTargets(method.getNameIdentifier(), myFixture.getCaretOffset());
        assertEquals(List.of(method.getParameterList().getParameters()[1]), Arrays.asList(targets));
    }

    public void testCrudConditionGotoDeclarationResolvesParameterObjectProperty() {
        myFixture.addClass("package sample; public class Query { public String name; }");
        myFixture.configureByText("UserMapper.java", """
                package sample;
                import org.apache.ibatis.annotations.Param;
                @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper {
                    User findByNa<caret>me(@Param("query") Query query);
                }
                """);
        PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
        PsiElement[] targets = crudTargets(method.getNameIdentifier(), myFixture.getCaretOffset());
        assertEquals(1, targets.length);
        assertInstanceOf(targets[0], PsiField.class);
        assertEquals("Query", ((PsiField) targets[0]).getContainingClass().getName());
    }

    public void testCrudConditionGotoDeclarationResolvesUnnamedObjectAndGetter() {
        myFixture.addClass("package sample; public class Query { public String name; }");
        myFixture.addClass("package sample; public class BeanQuery { public String getName() { return null; } }");
        for (String type : new String[]{"Query", "BeanQuery"}) {
            myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User findByNa<caret>me(" + type + " query); }");
            PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
            PsiElement[] targets = crudTargets(method.getNameIdentifier(), myFixture.getCaretOffset());
            assertEquals(1, targets.length);
            if (type.equals("Query")) {
                assertInstanceOf(targets[0], PsiField.class);
                assertEquals("Query", ((PsiField) targets[0]).getContainingClass().getName());
            } else {
                assertInstanceOf(targets[0], PsiMethod.class);
                assertEquals("BeanQuery", ((PsiMethod) targets[0]).getContainingClass().getName());
            }
        }
    }

    public void testCrudPropertyGotoDeclarationPreservesProjectionAndOrdering() {
        myFixture.configureByText("UserMapper.java", """
                package sample;
                import org.apache.ibatis.annotations.Param;
                @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper {
                    String findNa<caret>meByIdOrderByName(@Param("id") Long id);
                }
                """);
        PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
        int offset = method.getNameIdentifier().getTextOffset();
        for (int relative : new int[]{"findNa".length(), "findNameByIdOrderByNa".length()}) {
            PsiElement[] targets = crudTargets(method.getNameIdentifier(), offset + relative);
            assertNotNull(targets);
            assertEquals(1, targets.length);
            assertInstanceOf(targets[0], PsiField.class);
            assertEquals("name", ((PsiField) targets[0]).getName());
        }
    }

    public void testCrudReturnTypeNameNavigatesToUnwrappedType() {
        for (String returnType : new String[]{"User", "java.util.List<User>", "java.util.Optional<User>", "User[]"}) {
            myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { " + returnType + " findUs<caret>erByName(String name); }");
            PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
            PsiClass user = JavaPsiFacade.getInstance(getProject()).findClass("sample.User", GlobalSearchScope.allScope(getProject()));
            assertNotNull(user);
            assertEquals(List.of(user), Arrays.asList(crudTargets(method.getNameIdentifier(), myFixture.getCaretOffset())));
            assertCrudHover(method, "User", "find".length());
            assertEquals(List.of(method.getParameterList().getParameters()[0]), Arrays.asList(crudTargets(method.getNameIdentifier(), method.getNameIdentifier().getTextOffset() + "findUserByNa".length())));
        }
    }

    private PsiElement[] crudTargets(PsiElement element, int offset) {
        PsiSymbolReference reference = new ExtImplicitReferences().getImplicitReference(element, offset - element.getTextOffset());
        assertNotNull(reference);
        List<PsiElement> targets = new ArrayList<>();
        for (Symbol symbol : reference.resolveReference()) {
            PsiElement target = PsiSymbolService.getInstance().extractElementFromSymbol(symbol);
            assertNotNull(target);
            targets.add(target);
        }
        return targets.toArray(PsiElement[]::new);
    }

    public void testCrudCtrlHoverHighlightsOnlyReferencedWord() {
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User findByNa<caret>meOrderById(String name); }");
        PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
        assertCrudHover(method, "Name", method.getName().indexOf("Name"));
        assertCrudHover(method, "Id", method.getName().indexOf("Id"));

        ExtImplicitReferences provider = new ExtImplicitReferences();
        for (int offset : new int[]{-1, 0, 5, 10, 16, method.getName().length()}) {
            assertNull(provider.getImplicitReference(method.getNameIdentifier(), offset));
        }
        assertNull(provider.getImplicitReference(method, 0));
    }

    public void testCrudCtrlHoverPreservesBetweenTargets() {
        myFixture.configureByText("UserMapper.java", "package sample; @io.github.mybatisext.annotation.MapTable(User.class) interface UserMapper { User findByI<caret>dBetween(@org.apache.ibatis.annotations.Param(\"low\") Long min, @org.apache.ibatis.annotations.Param(\"high\") Long max); }");
        PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
        assertCrudHover(method, "Id", method.getName().indexOf("Id"));
        assertEquals(Arrays.asList(method.getParameterList().getParameters()), Arrays.asList(crudTargets(method.getNameIdentifier(), method.getNameIdentifier().getTextOffset() + 6)));
    }

    public void testCrudImplicitReferencesIgnoreOrdinaryMethods() {
        myFixture.configureByText("Plain.java", "interface Plain { String findByNa<caret>me(String name); }");
        PsiMethod method = PsiTreeUtil.getParentOfType(myFixture.getFile().findElementAt(myFixture.getCaretOffset()), PsiMethod.class);
        assertNull(new ExtImplicitReferences().getImplicitReference(method.getNameIdentifier(), 7));
    }

    private void assertCrudHover(PsiMethod method, String word, int start) {
        assertTrue(ImplicitReferenceProvider.EP_NAME.getExtensionList().stream().anyMatch(provider -> provider instanceof ExtImplicitReferences));
        int absolute = method.getNameIdentifier().getTextOffset() + start;
        TextRange expected = TextRange.from(absolute, word.length());
        for (int i = 0; i < word.length(); i++) {
            CtrlMouseData data = GotoDeclarationOrUsageHandler2.getCtrlMouseData(myFixture.getEditor(), myFixture.getFile(), absolute + i);
            assertNotNull(data);
            assertTrue(data.isNavigatable());
            assertEquals(List.of(expected), data.getRanges());
            assertEquals(GotoDeclarationOrUsageHandler2.GTDUOutcome.GTD,
                    GotoDeclarationOrUsageHandler2.testGTDUOutcome(myFixture.getEditor(), myFixture.getFile(), absolute + i));
        }
    }

    private String documentation(PsiMethod method) {
        return new CrudDocumentation().generateDoc(method, method.getNameIdentifier());
    }

    public void testCrudDocumentationUsesCoreScriptAndPreservesMybatisTags() {
        PsiClass mapper = extMapper("java.util.List<User> findByNameIn(@Param(\"name\") java.util.List<String> name);");
        PsiMethod method = mapper.getMethods()[0];
        String html = documentation(method);
        assertNotNull(html);
        assertTrue(html, html.contains("MySQL（默认）"));
        assertFalse(html, html.contains("Current method signature"));
        assertTrue(html, html.contains("title=\"未找到数据源配置\""));
        assertTrue(html, html.contains("&lt;script&gt;"));
        assertTrue(html, html.contains("&lt;foreach"));
        assertTrue(html, html.contains("#{"));
        ExtSession session = new ExtSession(method);
        var table = session.table(mapper);
        var semantic = session.analyzer.analyzeMethod(table.getValue(), session.model.method(method, session.model.type(mapper)));
        assertTrue(semantic.getDiagnostics().toString(), semantic.isSuccess());
        String expected = SemanticScriptHelper.buildScriptContent(semantic.getValue(), DefaultDialectSelector.MYSQL_DIALECT);
        assertTrue(html, html.contains(StringUtil.escapeXmlEntities(expected)));
        assertEquals(html, new CrudDocumentation().generateDoc(method.getNameIdentifier(), method.getNameIdentifier()));
    }

    public void testCrudDocumentationPreservesIfAndSetTags() {
        PsiClass mapper = extMapper("int updateIgnoreNull(User user);");
        String html = documentation(mapper.getMethods()[0]);
        assertTrue(html, html.contains("&lt;set&gt;"));
        assertTrue(html, html.contains("&lt;if"));
        assertFalse(html, html.contains("<script>"));
    }

    public void testCrudDocumentationReportsAnalysisFailure() {
        PsiClass mapper = extMapper("User findByMissing();");
        String html = documentation(mapper.getMethods()[0]);
        assertTrue(html, html.contains("Preview unavailable:"));
        assertFalse(html, html.contains("&lt;script&gt;"));
        var session = new ExtSession(mapper);
        var analyzed = session.analyzer.analyzeMethod(session.table(mapper).getValue(), session.model.method(mapper.getMethods()[0], session.model.type(mapper)));
        assertTrue(html, html.contains(StringUtil.escapeXmlEntities(analyzed.getDiagnostics().get(0).getMessage())));
    }

    public void testCrudDocumentationReportsUnresolvedMetadata() {
        PsiClass mapper = myFixture.addClass("package sample; @io.github.mybatisext.annotation.MapTable(Missing.class) interface BrokenMapper { Object find(); }");
        String html = documentation(mapper.getMethods()[0]);
        assertTrue(html, html.contains("Preview unavailable:"));
        assertTrue(html, html.contains("Unresolved"));
    }

    public void testUnresolvedMethodTypePreservesDiagnosticWithoutGutterOrReferences() {
        PsiMethod method = extMapper("Missing find();").getMethods()[0];
        var analyzed = new ExtSession(method).analyzeMethod(method);
        assertNotNull(analyzed);
        assertFalse(analyzed.isSuccess());
        assertEquals(Diagnostic.Code.UNRESOLVED_TYPE, analyzed.getDiagnostics().get(0).getCode());
        assertEmpty(ExtReferences.methodReferences(method));
        assertEmpty(markers(method, new ExtNavigation()));
        assertTrue(documentation(method).contains("Unresolved"));
    }

    public void testCrudDocumentationHonorsExplicitMappingsAndOrdinaryMethods() {
        PsiClass mapper = extMapper("User arbitrary(); @Select(\"select 1\") User explicit(); default User find() { return null; } User getByName(String name); User getByName(@Param(\"id\") Long id);");
        myFixture.addFileToProject("mapped.xml", "<mapper namespace=\"sample.UserMapper\"><select id=\"arbitrary\">select 1</select></mapper>");
        assertNull(documentation(mapper.findMethodsByName("arbitrary", false)[0]));
        assertNull(documentation(mapper.findMethodsByName("explicit", false)[0]));
        assertNull(documentation(mapper.findMethodsByName("find", false)[0]));
        PsiClass plain = myFixture.addClass("package sample; interface Plain { Object find(); }");
        assertNull(documentation(plain.getMethods()[0]));
        assertNull(new CrudDocumentation().generateDoc(mapper, mapper));
    }

    private String registeredDocumentation(PsiMethod method) {
        var provider = CompositeDocumentationProvider.wrapProviders(LanguageDocumentation.INSTANCE.allForLanguage(method.getLanguage()));
        return provider.generateDoc(method, method.getNameIdentifier());
    }

    public void testCrudDocumentationRegisteredWithPlatform() {
        PsiMethod method = extMapper("User findByName(String name);").getMethods()[0];
        String html = registeredDocumentation(method);
        assertNotNull(html);
        assertTrue(html, html.contains("mybatis-ext CRUD script"));
        PsiMethod plain = myFixture.addClass("package sample; interface Plain { String ordinary(); }").getMethods()[0];
        String ordinary = registeredDocumentation(plain);
        assertNotNull(ordinary);
        assertTrue(ordinary, ordinary.contains("ordinary"));
        assertFalse(ordinary, ordinary.contains("mybatis-ext CRUD script"));
    }

    public void testPreviewDialectPropertiesAndDocumentInvalidation() {
        PsiMethod method = extMapper("User find();").getMethods()[0];
        PsiFile config = myFixture.addFileToProject("application.properties", "spring.datasource.url=jdbc:postgresql://localhost/test\nspring.datasource.password=secret-do-not-display");
        assertInstanceOf(PreviewDialect.forMethod(method).dialect(), PostgreSqlDialect.class);
        assertTrue(documentation(method).contains("PostgreSQL"));
        assertFalse(documentation(method).contains("secret-do-not-display"));
        assertFalse(documentation(method).contains("localhost"));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            var document = PsiDocumentManager.getInstance(getProject()).getDocument(config);
            document.setText("spring.datasource.url=jdbc:oracle:thin:@localhost:1521:test");
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        });
        assertInstanceOf(PreviewDialect.forMethod(method).dialect(), OracleDialect.class);
    }

    public void testPreviewDialectYamlAndConflictingProfiles() {
        PsiMethod method = extMapper("User find();").getMethods()[0];
        myFixture.addFileToProject("application.yml", "spring:\n  datasource:\n    url: 'jdbc:h2:mem:test' # comment\n");
        assertInstanceOf(PreviewDialect.forMethod(method).dialect(), H2Dialect.class);
        myFixture.addFileToProject("application-prod.yaml", "datasource:\n  url: jdbc:postgresql://host/db\n");
        var selection = PreviewDialect.forMethod(method);
        assertInstanceOf(selection.dialect(), MySqlDialect.class);
        assertEquals("MySQL（默认，方言未确定）", selection.description());
        assertEquals("存在多种数据源方言", selection.reason());
    }

    public void testPreviewDialectXmlAndSameDialectDatasources() {
        PsiMethod method = extMapper("User find();").getMethods()[0];
        myFixture.addFileToProject("datasource.xml", "<beans><bean><property name=\"jdbcUrl\" value=\"jdbc:sqlserver://host;databaseName=test\"/></bean></beans>");
        myFixture.addFileToProject("application.properties", "spring.datasource.other.url=jdbc:sqlserver://other;databaseName=test");
        assertInstanceOf(PreviewDialect.forMethod(method).dialect(), SqlServerDialect.class);
    }

    public void testPreviewDialectFallbackForUnsupportedOrUnresolvedUrl() {
        PsiMethod method = extMapper("User find();").getMethods()[0];
        assertInstanceOf(PreviewDialect.forMethod(method).dialect(), MySqlDialect.class);
        myFixture.addFileToProject("application.properties", "spring.datasource.url=jdbc:unknown://host/db");
        assertEquals("MySQL（默认，方言未确定）", PreviewDialect.forMethod(method).description());
        assertEquals("数据源配置无法解析或方言不受支持", PreviewDialect.forMethod(method).reason());
        myFixture.addFileToProject("application-dev.yml", "spring:\n  datasource:\n    url: ${JDBC_URL}\n");
        assertInstanceOf(PreviewDialect.forMethod(method).dialect(), MySqlDialect.class);
        assertEquals("数据源配置无法解析或方言不受支持", PreviewDialect.forMethod(method).reason());
    }

    public void testPreviewDialectUnresolvedUrlFallsBackEvenWithKnownProtocol() {
        PsiMethod method = extMapper("User find();").getMethods()[0];
        myFixture.addFileToProject("application.properties", "spring.datasource.url=jdbc:postgresql://${DB_HOST}/test");
        var selection = PreviewDialect.forMethod(method);
        assertInstanceOf(selection.dialect(), MySqlDialect.class);
        assertEquals("MySQL（默认，方言未确定）", selection.description());
        String html = documentation(method);
        assertTrue(html, html.contains("title=\"数据源配置无法解析或方言不受支持\""));
        assertFalse(html, html.contains("Current method signature"));
    }

    public void testPreviewDialectCacheSharedAcrossFilesAndUnaffectedByJavaEdits() {
        PsiMethod first = extMapper("User find();").getMethods()[0];
        PsiMethod second = myFixture.addClass("package sample; interface OtherMapper { User find(); }").getMethods()[0];
        PsiFile unrelated = myFixture.addFileToProject("sample/Unrelated.java", "package sample; class Unrelated { int value = 1; }");
        myFixture.addFileToProject("application.properties", "spring.datasource.url=jdbc:postgresql://host/test");
        var selection = PreviewDialect.forMethod(first);
        assertEquals("PostgreSQL", selection.description());
        assertNull(selection.reason());
        assertSame(selection, PreviewDialect.forMethod(second));

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            var document = PsiDocumentManager.getInstance(getProject()).getDocument(unrelated);
            document.setText("package sample; class Unrelated { int value = 2; }");
            PsiDocumentManager.getInstance(getProject()).commitDocument(document);
        });
        assertSame(selection, PreviewDialect.forMethod(first));
    }

    public void testPreviewDialectCacheTracksConfigurationCreationRenameAndDeletion() {
        PsiMethod method = extMapper("User find();").getMethods()[0];
        assertEquals("MySQL（默认）", PreviewDialect.forMethod(method).description());
        PsiFile config = myFixture.addFileToProject("application.properties", "spring.datasource.url=jdbc:postgresql://host/test");
        assertEquals("PostgreSQL", PreviewDialect.forMethod(method).description());

        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            config.setName("application.txt");
        });
        assertEquals("MySQL（默认）", PreviewDialect.forMethod(method).description());
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            config.setName("application.properties");
        });
        assertEquals("PostgreSQL", PreviewDialect.forMethod(method).description());
        WriteCommandAction.runWriteCommandAction(getProject(), config::delete);
        assertEquals("MySQL（默认）", PreviewDialect.forMethod(method).description());
    }

    public void testIncompleteRequiredAnnotationMemberProducesDiagnostic() {
        PsiClass cls = myFixture.addClass("package sample; import io.github.mybatisext.annotation.*; @Table class Incomplete { @JoinRelation(table=User.class) String name; }");
        ExtSession session = new ExtSession(cls);
        var result = session.analyzer.analyzeMetadata(session.model.type(cls));
        assertFalse(result.isSuccess());
        assertEquals(Diagnostic.Code.UNRESOLVED_TYPE, result.getDiagnostics().get(0).getCode());
    }

    public void testOfflineDtdCdataAndXmlOnlyNamespace() {
        PsiClass mapper = mapper("User load(@Param(\"id\") Long id);");
        XmlFile file = (XmlFile) myFixture.configureByText("UserMapper.xml", "<!DOCTYPE mapper PUBLIC \"-//mybatis.org//DTD Mapper 3.0//EN\" \"https://mybatis.org/dtd/mybatis-3-mapper.dtd\"><mapper namespace=\"sample.UserMapper\"><select id=\"load\"><![CDATA[select 1 where 1 < 2 and id=#{id}]]></select></mapper>");
        XmlText text = file.getRootTag().findFirstSubTag("select").getValue().getTextElements()[0];
        assertTrue(MybatisReferences.references(text).stream().anyMatch(r -> r.resolve() instanceof PsiParameter));
        assertEquals(1, new MybatisProject(mapper).mappers(mapper).size());
        XmlFile fragments = (XmlFile) myFixture.configureByText("fragments.xml", "<mapper namespace=\"sql.fragments\"><sql id=\"columns\">id</sql></mapper>");
        assertTrue(MybatisReferences.references(attr(fragments.getRootTag(), "namespace")).stream().allMatch(PsiReference::isSoft));
    }
}
