package io.github.mybatisext.idea;

import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.ParamNameResolver;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.apache.ibatis.session.Configuration;

import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlText;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.analysis.AnalysisResult;
import io.github.mybatisext.analysis.CompletionItem;
import io.github.mybatisext.analysis.MybatisExtAnalyzer;
import io.github.mybatisext.dialect.Dialect;
import io.github.mybatisext.dialect.MySqlDialect;
import io.github.mybatisext.dialect.PostgreSqlDialect;
import io.github.mybatisext.idea.ext.ExtPaths;
import io.github.mybatisext.idea.ext.ExtReferences;
import io.github.mybatisext.idea.ext.ExtSession;
import io.github.mybatisext.idea.fixtures.ContractFixture;
import io.github.mybatisext.idea.model.PsiModel;
import io.github.mybatisext.idea.xml.MybatisProject;
import io.github.mybatisext.idea.xml.MybatisReferences;
import io.github.mybatisext.idea.xml.XmlBindings;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.model.JavaAnnotatedElement;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.reflect.GenericField;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericParameter;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.reflect.ReflectionTypePolicy;
import io.github.mybatisext.statement.SemanticScriptHelper;

public final class CoreContractTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_17;
    }

    private PsiClass fixture;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        List<String> jars = List.of("mybatisext.core.jar", "mybatisext.mybatis.jar").stream()
                .map(key -> "jar://" + Path.of(System.getProperty(key)).toAbsolutePath().toString().replace('\\', '/') + "!/").toList();
        ModuleRootModificationUtil.addModuleLibrary(getModule(), "core-contract", jars, List.of());
        try (var source = getClass().getResourceAsStream("/fixtures/ContractFixture.java")) {
            PsiJavaFile file = (PsiJavaFile) myFixture.addFileToProject("io/github/mybatisext/idea/fixtures/ContractFixture.java", new String(source.readAllBytes(), StandardCharsets.UTF_8));
            fixture = file.getClasses()[0];
        }
    }

    private PsiClass psiClass(Class<?> type) {
        return Objects.requireNonNull(fixture.findInnerClassByName(type.getSimpleName(), false));
    }

    private static MybatisExtAnalyzer reflection() {
        return new MybatisExtAnalyzer(new ReflectionTypePolicy(new Configuration()), new ExtContext());
    }

    private static String source(JavaAnnotatedElement value) {
        if (value == null) {
            return "";
        }
        PsiElement psi = PsiModel.source(value);
        if (psi instanceof PsiParameter parameter) {
            PsiMethod method = (PsiMethod) parameter.getDeclarationScope();
            int index = Arrays.asList(method.getParameterList().getParameters()).indexOf(parameter);
            return PsiModel.binaryName(method.getContainingClass()) + "." + method.getName() + ":parameter" + index;
        }
        if (psi instanceof PsiClass cls) {
            return PsiModel.binaryName(cls);
        }
        if (psi instanceof PsiMember member) {
            return PsiModel.binaryName(member.getContainingClass()) + "." + member.getName();
        }
        if (value instanceof GenericType type) {
            return type.getName();
        }
        if (value instanceof GenericField field) {
            return field.getField().getDeclaringClass().getName() + "." + field.getName();
        }
        if (value instanceof GenericMethod method) {
            return method.getDeclaringClass().getName() + "." + method.getName();
        }
        if (value instanceof GenericParameter parameter) {
            var executable = parameter.getParameter().getDeclaringExecutable();
            int index = Arrays.asList(executable.getParameters()).indexOf(parameter.getParameter());
            return executable.getDeclaringClass().getName() + "." + executable.getName() + ":parameter" + index;
        }
        throw new AssertionError("Unhandled source: " + value);
    }

    private static Map<String, String> metadata(TableInfo table) {
        Map<String, String> result = new TreeMap<>();
        collect(table.getNameToPropertyInfo(), result);
        return result;
    }

    private static void collect(Map<String, PropertyInfo> properties, Map<String, String> result) {
        for (PropertyInfo property : properties.values()) {
            result.put(property.getFullName(), type(property.getJavaType()) + "|" + type(property.getOfType()) + "|" + property.getColumnName() + "|" + property.getPropertyType()
                    + "|" + property.getJdbcType() + "|" + property.getFetchType() + "|" + property.getIdType() + "|" + property.isOwnColumn()
                    + "|" + property.isReadonly() + "|" + source(property.getSource()) + "|" + source(property.getMappingSource()));
            collect(property.getNameToPropertyInfo(), result);
        }
    }

    private static String type(JavaType value) {
        return value == null ? "" : value.getName() + Arrays.stream(value.getTypeParameters()).map(CoreContractTest::type).toList();
    }

    public void testReflectionAndPsiMetadataAndFinalMappingsAgree() {
        for (Class<?> type : List.of(ContractFixture.User.class, ContractFixture.View.class, ContractFixture.ChainedView.class, ContractFixture.Joined.class, ContractFixture.Binary.class,
                ContractFixture.BeanProperties.class, ContractFixture.CollectionJoined.class)) {
            ExtSession session = new ExtSession(psiClass(type));
            var actual = session.analyzer.analyzeMetadata(session.model.type(psiClass(type)));
            var expected = reflection().analyzeMetadata(GenericTypeFactory.build(type));
            assertTrue(type + ": " + actual.getDiagnostics().stream().map(d -> d.format()).toList(), actual.isSuccess());
            assertTrue(type + ": " + expected.getDiagnostics().stream().map(d -> d.format()).toList(), expected.isSuccess());
            assertEquals(type.getName(), metadata(expected.getValue()), metadata(actual.getValue()));
            if (type == ContractFixture.View.class) {
                assertNotNull(actual.getValue().get("city"));
                assertEquals(ContractFixture.Address.class.getName() + ".city", source(actual.getValue().get("city").getMappingSource()));
            }
            if (type == ContractFixture.ChainedView.class) {
                assertEquals(ContractFixture.Address.class.getName() + ".city", source(actual.getValue().get("town").getMappingSource()));
            }
            if (type == ContractFixture.Joined.class) {
                assertEquals(ContractFixture.User.class.getName() + ".name", source(actual.getValue().get("label").getMappingSource()));
                assertEquals(ContractFixture.User.class.getName(), source(actual.getValue().get("users").getMappingSource()));
            }
        }
    }

    public void testReflectionAndPsiMetadataDiagnosticsAgree() {
        PsiClass type = psiClass(ContractFixture.BrokenView.class);
        ExtSession session = new ExtSession(type);
        var actual = session.analyzer.analyzeMetadata(session.model.type(type));
        var expected = reflection().analyzeMetadata(GenericTypeFactory.build(ContractFixture.BrokenView.class));
        assertFalse(actual.isSuccess());
        assertFalse(expected.isSuccess());
        assertEquals(expected.getDiagnostics().stream().map(d -> d.getCode() + ":" + d.getMessage()).toList(), actual.getDiagnostics().stream().map(d -> d.getCode() + ":" + d.getMessage()).toList());
        assertEquals(source(expected.getDiagnostics().get(0).getLocation().getSource()), source(actual.getDiagnostics().get(0).getLocation().getSource()));
    }

    public void testReflectionAndPsiCrudScriptsDiagnosticsAndReferencesAgree() {
        PsiClass mapper = psiClass(ContractFixture.Mapper.class);
        ExtSession session = new ExtSession(mapper);
        var psiTable = session.table(mapper);
        MybatisExtAnalyzer runtime = reflection();
        var runtimeTable = runtime.analyzeMetadata(GenericTypeFactory.build(ContractFixture.User.class));
        GenericType runtimeMapper = GenericTypeFactory.build(ContractFixture.Mapper.class);
        for (PsiMethod method : mapper.getMethods()) {
            GenericMethod counterpart = Arrays.stream(runtimeMapper.getMethods()).filter(m -> m.getName().equals(method.getName())).findFirst().orElseThrow();
            var expected = runtime.analyzeMethod(runtimeTable.getValue(), counterpart);
            var actual = session.analyzer.analyzeMethod(psiTable.getValue(), session.model.method(method, session.model.type(mapper)));
            assertEquals(method.getName(), expected.isSuccess(), actual.isSuccess());
            assertEquals(expected.getDiagnostics().stream().map(d -> d.getCode() + ":" + d.getMessage()).toList(), actual.getDiagnostics().stream().map(d -> d.getCode() + ":" + d.getMessage()).toList());
            assertEquals(references(expected), references(actual));
            if (method.getName().equals("findByName")) {
                var expectedCompletion = runtime.completeMethod(runtimeTable.getValue(), counterpart, 8);
                var actualCompletion = session.analyzer.completeMethod(psiTable.getValue(), session.model.method(method, session.model.type(mapper)), 8);
                assertNotNull(expectedCompletion.getValue());
                assertNotNull(actualCompletion.getValue());
                assertFalse(expectedCompletion.getValue().isEmpty());
                assertEquals(completions(expectedCompletion.getValue()), completions(actualCompletion.getValue()));
            }
            if (actual.isSuccess()) {
                for (Dialect dialect : List.of(new MySqlDialect(), new PostgreSqlDialect())) {
                    assertEquals(method.getName(), SemanticScriptHelper.buildScriptContent(expected.getValue(), dialect), SemanticScriptHelper.buildScriptContent(actual.getValue(), dialect));
                }
            }
        }
    }

    private static List<String> references(AnalysisResult<?> result) {
        List<String> references = new ArrayList<>();
        for (var reference : result.getReferences()) {
            Object target = reference.getTarget();
            String name;
            if (target instanceof PropertyInfo property) {
                name = property.getFullName();
            } else if (target instanceof Variable variable) {
                name = variable.getFullName();
            } else {
                name = source((JavaAnnotatedElement) target);
            }
            references.add(reference.getStart() + ":" + reference.getEnd() + ":" + name);
        }
        return references;
    }

    private static List<String> completions(List<CompletionItem> values) {
        return values.stream().map(c -> c.getText() + ":" + c.getKind() + ":" + c.getReplaceStart() + ":" + c.getReplaceEnd()).sorted().toList();
    }

    private XmlText parameterStatement(String method, String sql) {
        XmlFile file = (XmlFile) myFixture.configureByText("ParameterMapper.xml", "<mapper namespace=\"" + ContractFixture.ParameterMapper.class.getName()
                + "\"><select id=\"" + method + "\">" + sql + "</select></mapper>");
        return file.getRootTag().findFirstSubTag("select").getValue().getTextElements()[0];
    }

    public void testSingleScalarPlaceholderMatchesRuntimeBinding() {
        Configuration configuration = new Configuration();
        var sql = new XMLLanguageDriver().createSqlSource(configuration, "select #{value}", Integer.class);
        var statement = new MappedStatement.Builder(configuration, "scalar", sql, SqlCommandType.SELECT).build();
        List<Integer> boundValues = new ArrayList<>();
        PreparedStatement prepared = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{PreparedStatement.class}, (proxy, method, args) -> {
            if (method.getName().equals("setInt")) {
                boundValues.add((Integer) args[1]);
            }
            return null;
        });
        new DefaultParameterHandler(statement, 7, sql.getBoundSql(7)).setParameters(prepared);
        assertEquals(List.of(7), boundValues);

        XmlText text = parameterStatement("scalar", "select #{value}");
        var references = MybatisReferences.references(text);
        assertEquals(1, references.size());
        PsiElement target = references.get(0).resolve();
        assertInstanceOf(target, PsiParameter.class);
        assertEquals("id", ((PsiParameter) target).getName());
    }

    public void testNamedAndObjectParametersStillRejectUnknownProperties() {
        assertNull(new XmlBindings(parameterStatement("namedScalar", "select #{missing}")).resolve("missing"));
        assertNull(new XmlBindings(parameterStatement("object", "select #{missing}")).resolve("missing"));
        assertNotNull(new XmlBindings(parameterStatement("object", "select #{name}")).resolve("name"));
    }

    public void testSingleBinaryParameterRetainsArrayWrapping() throws Exception {
        byte[] bytes = new byte[]{1, 2};
        var method = ContractFixture.ParameterMapper.class.getMethod("binary", byte[].class);
        Map<?, ?> runtime = (Map<?, ?>) new ParamNameResolver(new Configuration(), method).getNamedParams(new Object[]{bytes});
        assertSame(bytes, runtime.get("array"));
        assertFalse(runtime.containsKey("missing"));

        XmlBindings bindings = new XmlBindings(parameterStatement("binary", "select #{array}"));
        assertNotNull(bindings.resolve("array"));
        assertNull(bindings.resolve("missing"));
    }

    public void testExplicitParameterNameTakesPrecedenceOverGenericAlias() throws Exception {
        ContractFixture.User user = new ContractFixture.User();
        var method = ContractFixture.ParameterMapper.class.getMethod("aliases", ContractFixture.User.class, int.class);
        Map<?, ?> runtime = (Map<?, ?>) new ParamNameResolver(new Configuration(), method).getNamedParams(new Object[]{user, 7});
        assertSame(user, runtime.get("param2"));
        assertSame(user, runtime.get("param1"));

        XmlBindings bindings = new XmlBindings(parameterStatement("aliases", "select #{param2.name}"));
        assertEquals(PsiModel.binaryName(psiClass(ContractFixture.User.class)), new PsiModel(getProject()).type(bindings.resolve("param2").type()).getName());
        assertNotNull(bindings.resolve("param2.name"));
        assertSame(bindings.names.get("param2"), bindings.names.get("param1"));
        assertFalse(bindings.names.containsKey("first"));
        assertEquals("int", bindings.resolve("other").type().getCanonicalText());
    }

    public void testCollectionSubclassNavigationUsesCoreUnwrap() {
        PsiClass users = psiClass(ContractFixture.Users.class);
        PsiType unwrapped = MybatisProject.unwrap(JavaPsiFacade.getElementFactory(getProject()).createType(users));
        assertEquals(ContractFixture.User.class.getName(), new PsiModel(getProject()).type(unwrapped).getName());

        PsiClass joined = psiClass(ContractFixture.CollectionJoined.class);
        PsiLiteralExpression column = PsiTreeUtil.findChildrenOfType(joined, PsiLiteralExpression.class).stream()
                .filter(literal -> "id".equals(literal.getValue())).findFirst().orElseThrow();
        ExtPaths.Context context = ExtPaths.context(column);
        assertNotNull(context);
        assertEquals(ContractFixture.User.class.getName(), context.table().getClassType().getName());
        assertEquals(1, ExtReferences.references(column).length);
        assertNotNull(ExtReferences.references(column)[0].resolve());
    }
}
