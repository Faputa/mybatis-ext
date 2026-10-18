package io.github.mybatisext.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.IdType;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.MapTable;
import io.github.mybatisext.annotation.OnlyById;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.diagnostic.DiagnosticLocation;
import io.github.mybatisext.dialect.H2Dialect;
import io.github.mybatisext.exception.MybatisExtException;
import io.github.mybatisext.idgenerator.IdGenerator;
import io.github.mybatisext.jpa.Condition;
import io.github.mybatisext.jpa.JpaParser;
import io.github.mybatisext.jpa.ParserException;
import io.github.mybatisext.jpa.Semantic;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.model.JavaAnnotatedElement;
import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaBeanProperty;
import io.github.mybatisext.model.JavaField;
import io.github.mybatisext.model.JavaMethod;
import io.github.mybatisext.model.JavaParameter;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.TypePolicy;
import io.github.mybatisext.reflect.GenericMethod;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;
import io.github.mybatisext.reflect.ReflectionTypePolicy;
import io.github.mybatisext.statement.MappedStatementHelper;
import io.github.mybatisext.statement.SemanticScriptHelper;

class MybatisExtAnalyzerTest {

    private final MemoryType text = new MemoryType("java.lang.String");
    private final MemoryType number = new MemoryType("java.lang.Long");
    private final MemoryType integer = new MemoryType("int");
    private final MemoryType address = new MemoryType(Address.class.getName());
    private final MemoryType department = new MemoryType(Department.class.getName());
    private final MemoryType parent = new MemoryType(Parent.class.getName());
    private final MemoryType record = new MemoryType(Record.class.getName());
    private final MemoryType view = new MemoryType(RecordView.class.getName());
    private final TypePolicy policy = type -> !type.isResolved()
            ? TypePolicy.ScalarKind.UNKNOWN
            : Arrays.asList("java.lang.String", "java.lang.Long", "int", "long", "boolean").contains(type.getName())
                    ? TypePolicy.ScalarKind.SCALAR
                    : TypePolicy.ScalarKind.OBJECT;
    private final MybatisExtAnalyzer memory = new MybatisExtAnalyzer(policy, new ExtContext());
    private final MybatisExtAnalyzer reflection = new MybatisExtAnalyzer(new ReflectionTypePolicy(new Configuration()), new ExtContext());

    MybatisExtAnalyzerTest() {
        address.fields.add(field("city", text).annotate(Column.class));
        parent.arguments = new JavaType[]{number};
        parent.fields.add(field("id", number).annotate(Column.class, "name", "record_id").annotate(Id.class));
        record.parent = parent;
        record.annotate(Table.class, "name", "analysis_record", "alias", "r");
        record.fields.add(field("name", text).annotate(Column.class));
        record.fields.add(field("address", address).annotate(Column.class));
        department.annotate(Table.class, "name", "department", "alias", "d");
        department.fields.add(field("id", number).annotate(Column.class).annotate(Id.class));
        department.fields.add(field("name", text).annotate(Column.class));
        record.fields.add(field("deptId", number).annotate(Column.class));
        JavaAnnotation join = new Node().annotate(JoinColumn.class, "leftColumn", "deptId", "rightColumn", "id").annotation(JoinColumn.class);
        record.fields.add(field("deptName", text).annotate(JoinRelation.class, "table", department, "column", "name", "joinColumn", new JavaAnnotation[]{join}));
        record.fields.add(field("dept", department).annotate(JoinRelation.class, "joinColumn", new JavaAnnotation[]{join}));
        MemoryMethod getter = new MemoryMethod("getName", text);
        getter.owner = record;
        getter.annotate(Column.class, "name", "ignored_getter_column");
        record.beans.add(new JavaBeanProperty("name", getter, true));
        MemoryMethod readonly = new MemoryMethod("getLabel", text);
        readonly.owner = record;
        readonly.annotate(Column.class, "name", "display_label");
        record.beans.add(new JavaBeanProperty("label", readonly, true));
        view.annotate(TableRef.class, "value", record);
        view.fields.add(field("displayName", text).annotate(ColumnRef.class, "value", "name"));
        view.fields.add(field("city", text).annotate(ColumnRef.class, "value", "address.city"));
        view.fields.add(field("deptLabel", text).annotate(ColumnRef.class, "value", "deptName"));
        view.fields.add(field("deptId", number).annotate(ColumnRef.class, "value", "deptId"));
    }

    @Test
    void metadataAndCrudSqlAgreeAcrossReflectionAndMemoryModels() {
        TableInfo actual = table(memory, record);
        TableInfo expected = table(reflection, GenericTypeFactory.build(Record.class));
        assertEquals(snapshot(expected), snapshot(actual));
        assertEquals("record_id", actual.get("id").getColumnName());
        assertEquals("name", actual.get("name").getColumnName());
        assertTrue(actual.get("label").isReadonly());
        assertSame(record.fields.get(0), actual.get("name").getSource());
        assertEquals(snapshot(table(reflection, GenericTypeFactory.build(RecordView.class))), snapshot(table(memory, view)));

        Map<String, MemoryMethod> methods = new HashMap<>();
        methods.put("findByName", method("findByName", record, parameter(text).annotate(Param.class, "value", "name")));
        methods.put("findByAddressDotCity", method("findByAddressDotCity", record, parameter(text).annotate(Param.class, "value", "city")));
        methods.put("save", method("save", integer, parameter(record)));
        methods.put("updateIgnoreNull", method("updateIgnoreNull", integer, parameter(record).annotate(OnlyById.class)));
        methods.put("delete", method("delete", integer, parameter(record).annotate(OnlyById.class)));
        methods.put("count", method("count", new MemoryType("long"), parameter(record)));
        methods.put("exists", method("exists", new MemoryType("boolean"), parameter(record)));
        methods.put("listByName", method("listByName", listOf(record), parameter(text).annotate(Param.class, "value", "name")));
        methods.put("saveBatch", method("saveBatch", integer, parameter(listOf(record))));
        methods.put("updateBatch", method("updateBatch", integer, parameter(listOf(record)).annotate(OnlyById.class)));
        methods.put("deleteBatch", method("deleteBatch", integer, parameter(listOf(record)).annotate(OnlyById.class)));
        for (GenericMethod reflected : GenericTypeFactory.build(RecordMapper.class).getMethods()) {
            AnalysisResult<Semantic> left = memory.analyzeMethod(actual, methods.get(reflected.getName()));
            AnalysisResult<Semantic> right = reflection.analyzeMethod(expected, reflected);
            assertTrue(left.isSuccess(), reflected.getName() + ": " + messages(left));
            assertTrue(right.isSuccess(), messages(right));
            assertEquals(conditionSnapshot(right.getValue().getWhere()), conditionSnapshot(left.getValue().getWhere()));
            String expectedSql = SemanticScriptHelper.buildScriptContent(right.getValue(), new H2Dialect());
            String actualSql = SemanticScriptHelper.buildScriptContent(left.getValue(), new H2Dialect());
            // 默认条件使用集合保存，其遍历顺序不属于接口约定。
            if (reflected.getName().equals("count") || reflected.getName().equals("exists")) {
                assertEquals(expectedSql.substring(0, expectedSql.indexOf("<where>")), actualSql.substring(0, actualSql.indexOf("<where>")));
            } else {
                assertEquals(expectedSql, actualSql);
            }
        }
    }

    @Test
    void completesPartialTokensNestedPathsAndNamesWithMissingSignature() {
        TableInfo table = table(memory, record);
        assertTrue(candidates(table, "findByNa").contains("Name"));
        Set<String> nested = candidates(table, "findByAddressDot");
        assertTrue(nested.contains("City"));
        assertFalse(nested.contains("Name"));
        assertTrue(candidates(table, "findByDeptDot").contains("Name"));
        assertTrue(candidates(table, "findByName").contains("OrderBy"));
        assertTrue(candidates(table, "findByName").contains("And"));
        assertTrue(candidates(table, "findByNameOrderBy").contains("Id"));
        assertTrue(candidates(table, "saveIgnore").contains("IgnoreNull"));

        String name = "findByNameOrderById";
        AnalysisResult<List<CompletionItem>> result = memory.completeMethod(table, method(name, null), "findByNa".length());
        CompletionItem item = result.getValue().stream().filter(c -> c.getText().equals("Name")).findFirst().get();
        assertEquals("Name", name.substring(item.getReplaceStart(), item.getReplaceEnd()));
        assertEquals(name, name.substring(0, item.getReplaceStart()) + item.getText() + name.substring(item.getReplaceEnd()));
        assertTrue(memory.analyzeMethod(table, method("findByName", record)).getDiagnostics().stream()
                .anyMatch(d -> d.getCode() == Diagnostic.Code.SEMANTIC_ERROR));
    }

    @Test
    void referencesReturnTypeNameAndPreservesConditionBinding() {
        TableInfo table = table(memory, record);
        String typeName = record.getSimpleName();
        String name = "find" + typeName + "ByName";
        JavaParameter parameter = parameter(text).annotate(Param.class, "value", "name");
        AnalysisResult<Semantic> result = memory.analyzeMethod(table, method(name, record, parameter));
        assertTrue(result.isSuccess(), messages(result));
        assertEquals(2, result.getReferences().size());
        SymbolReference typeReference = result.getReferences().get(0);
        assertEquals(4, typeReference.getStart());
        assertEquals(4 + typeName.length(), typeReference.getEnd());
        assertEquals(typeName, name.substring(typeReference.getStart(), typeReference.getEnd()));
        assertSame(record, typeReference.getTarget());
        assertSame(parameter, result.getReferences().get(1).getTarget());
    }

    @Test
    void returnsExactReferencesAndDiagnosticsWithoutWritingToStderr() {
        TableInfo table = table(memory, record);
        String name = "findByAddressDotCity";
        JavaParameter city = parameter(text).annotate(Param.class, "value", "city");
        AnalysisResult<Semantic> result = memory.analyzeMethod(table,
                method(name, record, city));
        assertTrue(result.isSuccess(), messages(result));
        assertEquals(2, result.getReferences().size());
        assertEquals(Arrays.asList("Address", "City"), result.getReferences().stream()
                .map(r -> name.substring(r.getStart(), r.getEnd())).collect(Collectors.toList()));
        assertTrue(result.getReferences().get(0).getTarget() instanceof PropertyInfo);
        assertSame(address.fields.get(0), result.getValue().getWhere().getPropertyInfo().getSource());
        SymbolReference bindingReference = result.getReferences().get(1);
        assertSame(city, bindingReference.getTarget());
        assertEquals("City", name.substring(bindingReference.getStart(), bindingReference.getEnd()));

        AnalysisResult<Semantic> invalid = memory.analyzeMethod(table, method("findByMissing", record));
        assertEquals(Diagnostic.Code.SYNTAX_ERROR, invalid.getDiagnostics().get(0).getCode());
        assertEquals(6, invalid.getDiagnostics().get(0).getStart());
        AnalysisResult<Semantic> oversizedLimit = memory.analyzeMethod(table, method("findTop999999999999999999999999", record));
        assertEquals(Diagnostic.Code.SYNTAX_ERROR, oversizedLimit.getDiagnostics().get(0).getCode());
        MemoryType unresolved = new MemoryType("not.compiled.Yet");
        unresolved.resolved = false;
        assertEquals(Diagnostic.Code.UNRESOLVED_TYPE, memory.analyzeMetadata(unresolved).getDiagnostics().get(0).getCode());
        assertEquals(Diagnostic.Code.UNRESOLVED_TYPE, memory.analyzeMethod(table, method("find", unresolved)).getDiagnostics().get(0).getCode());
        assertTrue(candidates(table, "findByNa").contains("Name"));
        assertTrue(memory.completeMethod(table, method("findByNa", unresolved, parameter(unresolved)), 8).getValue().stream()
                .anyMatch(c -> c.getText().equals("Name")));
    }

    @Test
    void resolvesAndCompletesMetadataReferencesAtTheCaret() {
        TableInfo table = table(memory, record);
        AnalysisResult<PropertyInfo> resolved = memory.resolvePropertyPath(table, "address.city");
        assertTrue(resolved.isSuccess());
        assertEquals("city", resolved.getValue().getColumnName());
        assertEquals(2, resolved.getReferences().size());
        AnalysisResult<PropertyInfo> invalid = memory.resolvePropertyPath(table, "address.missing");
        assertEquals(8, invalid.getDiagnostics().get(0).getStart());
        assertEquals(1, invalid.getReferences().size());
        List<CompletionItem> items = memory.completePropertyPath(table, "address.city", 10);
        assertEquals(1, items.size());
        assertEquals("city", items.get(0).getText());
        assertEquals(8, items.get(0).getReplaceStart());
        assertEquals(12, items.get(0).getReplaceEnd());
        assertTrue(memory.completePropertyPath(table, "missing.", 8).isEmpty());
    }

    @Test
    void isolatesSnapshotsAndTypeHandlerPolicies() {
        TableInfo embedded = table(memory, record);
        TypePolicy scalarAddress = type -> type == address ? TypePolicy.ScalarKind.SCALAR : policy.classify(type);
        TableInfo scalar = table(new MybatisExtAnalyzer(scalarAddress, new ExtContext()), record);
        assertNull(embedded.get("address").getColumnName());
        assertEquals("address", scalar.get("address").getColumnName());
        assertNotSame(embedded.getJoinGraph(), scalar.getJoinGraph());
        assertSame(embedded, table(memory, record));
        TypePolicy unknownAddress = type -> type == address ? TypePolicy.ScalarKind.UNKNOWN : policy.classify(type);
        assertEquals(Diagnostic.Code.UNRESOLVED_TYPE,
                new MybatisExtAnalyzer(unknownAddress, new ExtContext()).analyzeMetadata(record).getDiagnostics().get(0).getCode());

        ExtContext context = new ExtContext();
        context.setDefaultFilterable(false);
        TableInfo withoutDefaultFilters = table(new MybatisExtAnalyzer(policy, context), record);
        assertNotNull(embedded.get("name").getFilterableInfo());
        assertNull(withoutDefaultFilters.get("name").getFilterableInfo());
    }

    @Test
    void sharesMapperEligibilityAndExistingStatementPrecedence() {
        MemoryType base = new MemoryType("io.github.mybatisext.mapper.ExtMapper");
        base.interfaceType = true;
        base.arguments = new JavaType[]{record};
        MemoryType mapper = new MemoryType("example.RecordMapper");
        mapper.interfaceType = true;
        mapper.interfaces = new JavaType[]{base};
        assertTrue(MapperResolver.isEnhancedMapper(mapper));
        assertSame(record, MapperResolver.entityType(mapper));
        MemoryMethod method = method("findByName", record);
        assertTrue(MapperResolver.isDerivedMethod(mapper, method, id -> false));
        assertFalse(MapperResolver.isDerivedMethod(mapper, method, id -> id.equals("example.RecordMapper.findByName")));
        assertFalse(MapperResolver.isDerivedMethod(mapper, method, id -> id.equals("io.github.mybatisext.mapper.ExtMapper.findByName")));
        mapper.annotate(MapTable.class, "value", view);
        assertSame(view, MapperResolver.entityType(mapper));
    }

    @Test
    void doesNotInstantiateCustomGeneratorsDuringAnalysis() {
        TableInfo table = table(reflection, GenericTypeFactory.build(Generated.class));
        assertEquals(CountingGenerator.class.getName(), table.get("id").getCustomIdGenerator().getName());
        assertEquals(0, CountingGenerator.constructions);
    }

    @Test
    void runtimeAndAnalysisShareSyntaxAndBindingDiagnostics() {
        TableInfo table = table(memory, record);
        JpaParser parser = new JpaParser(policy, new TableInfoFactory(policy, new ExtContext()));
        MemoryType mapper = new MemoryType("example.RecordMapper");
        MemoryMethod invalid = method("findByMissing", record);
        invalid.owner = mapper;
        ParserException syntax = assertThrows(ParserException.class, () -> parser.parse(table, invalid, record));
        Diagnostic syntaxDiagnostic = memory.analyzeMethod(table, invalid).getDiagnostics().get(0);
        assertSameDiagnostic(syntaxDiagnostic, syntax.getDiagnostic());
        assertSame(invalid, syntaxDiagnostic.getLocation().getSource());
        assertSame(mapper, syntaxDiagnostic.getLocation().getOwner());
        assertTrue(syntax.getMessage().contains("example.RecordMapper#findByMissing"));
        assertEquals(6, syntaxDiagnostic.getStart());

        MemoryMethod missingParameter = method("findByName", record);
        missingParameter.owner = mapper;
        MybatisExtException binding = assertThrows(MybatisExtException.class, () -> parser.parse(table, missingParameter, record));
        Diagnostic bindingDiagnostic = memory.analyzeMethod(table, missingParameter).getDiagnostics().get(0);
        assertSameDiagnostic(bindingDiagnostic, binding.getDiagnostic());
        assertEquals(Diagnostic.Code.SEMANTIC_ERROR, bindingDiagnostic.getCode());
        assertTrue(binding.getMessage().contains("condition: name"));
        assertTrue(binding.getMessage().contains("supplied parameters: 0"));

        MemoryMethod valid = method("findByName", record, parameter(text).annotate(Param.class, "value", "name"));
        valid.owner = mapper;
        assertEquals(conditionSnapshot(memory.analyzeMethod(table, valid).getValue().getWhere()),
                conditionSnapshot(parser.parse(table, valid, record).getWhere()));
    }

    @Test
    void metadataErrorsRetainAnnotationSourceAndPathInBothConsumers() {
        MemoryType broken = new MemoryType("example.BrokenView");
        broken.annotate(TableRef.class, "value", record);
        MemoryField reference = field("zip", text).annotate(ColumnRef.class, "value", "address.missing");
        broken.fields.add(reference);
        MybatisExtException error = assertThrows(MybatisExtException.class,
                () -> new TableInfoFactory(policy, new ExtContext()).buildTableInfo(broken));
        Diagnostic diagnostic = memory.analyzeMetadata(broken).getDiagnostics().get(0);
        assertSameDiagnostic(diagnostic, error.getDiagnostic());
        assertSame(broken, diagnostic.getLocation().getOwner());
        assertSame(reference, diagnostic.getLocation().getSource());
        assertEquals(ColumnRef.class, diagnostic.getLocation().getAnnotationType());
        assertEquals("value", diagnostic.getLocation().getAnnotationMember());
        assertEquals("address.missing", diagnostic.getLocation().getPath());
        assertEquals(8, diagnostic.getStart());
        assertEquals(15, diagnostic.getEnd());
        assertTrue(error.getMessage().contains("example.BrokenView"));
        assertTrue(error.getMessage().contains("@ColumnRef.value"));

        // 方法分析必须保留注解文本的坐标，不能将其转换为相对于方法名的坐标。
        TableInfo table = table(memory, record);
        Diagnostic nested = memory.analyzeMethod(table, method("find", broken)).getDiagnostics().get(0);
        assertSameDiagnostic(diagnostic, nested);
    }

    @Test
    void unknownPropertyTypesHaveDeclarationLocationsWithoutInventedCharacterRanges() {
        MemoryType unknown = new MemoryType("not.compiled.Yet");
        unknown.resolved = false;
        MemoryField source = field("unknown", unknown).annotate(Column.class);
        record.fields.add(source);
        Diagnostic diagnostic = memory.analyzeMetadata(record).getDiagnostics().get(0);
        assertEquals(Diagnostic.Code.UNRESOLVED_TYPE, diagnostic.getCode());
        assertSame(source, diagnostic.getLocation().getSource());
        assertSame(record, diagnostic.getLocation().getOwner());
        assertEquals("unknown", diagnostic.getLocation().getPath());
        assertFalse(diagnostic.hasRange());
        assertEquals(-1, diagnostic.getStart());
    }

    @Test
    void propertyLookupSharesValidationAndDoesNotAcceptEmptyPathSegments() {
        TableInfo table = table(memory, record);
        for (String path : Arrays.asList("address.missing", "name.city", "address.", "address..city", "")) {
            MybatisExtException error = assertThrows(MybatisExtException.class, () -> TableInfoFactory.getPropertyInfo(table, path));
            AnalysisResult<PropertyInfo> result = memory.resolvePropertyPath(table, path);
            assertSameDiagnostic(result.getDiagnostics().get(0), error.getDiagnostic());
        }
        assertSame(TableInfoFactory.getPropertyInfo(table, "address.city"), memory.resolvePropertyPath(table, "address.city").getValue());
    }

    @Test
    void analysisDoesNotSwallowUnexpectedAdapterFailuresAndExceptionsKeepTheirCauses() {
        IllegalStateException cause = new IllegalStateException("broken adapter");
        MybatisExtAnalyzer analyzer = new MybatisExtAnalyzer(type -> {
            throw cause;
        }, new ExtContext());
        assertSame(cause, assertThrows(IllegalStateException.class, () -> analyzer.analyzeMetadata(record)));
        MybatisExtException metadata = new MybatisExtException("metadata failure", cause);
        ParserException syntax = new ParserException("syntax failure", cause);
        assertSame(cause, metadata.getCause());
        assertSame(cause, syntax.getCause());
        assertNull(metadata.getDiagnostic());
        assertNull(syntax.getDiagnostic());
    }

    @Test
    void structuredExceptionsRemainSerializableWithoutSerializingSourceSnapshots() throws Exception {
        Diagnostic diagnostic = new Diagnostic(Diagnostic.Code.SYNTAX_ERROR, "invalid method",
                new DiagnosticLocation(record, record, null, null, null).inText("findByBad", 6, 9));
        ParserException original = new ParserException(diagnostic);
        original.initCause(new IllegalArgumentException("cause"));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(original);
        }
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            ParserException restored = (ParserException) input.readObject();
            assertEquals(diagnostic.getCode(), restored.getDiagnostic().getCode());
            assertEquals(6, restored.getDiagnostic().getStart());
            assertEquals("cause", restored.getCause().getMessage());
            assertNull(restored.getDiagnostic().getLocation().getOwner());
            assertNull(restored.getDiagnostic().getLocation().getSource());
            assertEquals("findByBad", restored.getDiagnostic().getLocation().getText());
            assertEquals(original.getMessage(), restored.getMessage());
        }
    }

    @Test
    void statementRegistrationKeepsTheMapperMethodDiagnostic() {
        Configuration configuration = new Configuration();
        ExtContext context = new ExtContext();
        TableInfoFactory tables = new TableInfoFactory(configuration, context);
        GenericType entity = GenericTypeFactory.build(Record.class);
        GenericMethod method = Arrays.stream(GenericTypeFactory.build(InvalidMapper.class).getMethods())
                .filter(m -> m.getName().equals("findByMissing")).findFirst().get();
        MappedStatementHelper statements = new MappedStatementHelper(configuration, context, tables);
        ParserException error = assertThrows(ParserException.class, () -> statements.build(
                InvalidMapper.class.getName() + ".findByMissing", entity, Collections.singletonList(method), entity, false));
        Diagnostic analyzed = reflection.analyzeMethod(table(reflection, entity), method).getDiagnostics().get(0);
        assertSameDiagnostic(analyzed, error.getDiagnostic());
        assertTrue(error.getMessage().contains(InvalidMapper.class.getName() + "#findByMissing"));
    }

    @Test
    void joinColumnErrorsPointToTheReferencingDeclarationAndCorrectSide() {
        for (String side : Arrays.asList("leftColumn", "rightColumn")) {
            MemoryType owner = new MemoryType("example.InvalidJoin" + side);
            owner.annotate(Table.class, "name", "invalid_join");
            owner.fields.add(field("id", number).annotate(Column.class));
            JavaAnnotation join = new Node().annotate(JoinColumn.class,
                    "leftColumn", side.equals("leftColumn") ? "missing" : "id",
                    "rightColumn", side.equals("rightColumn") ? "missing" : "id").annotation(JoinColumn.class);
            MemoryField source = field("label", text).annotate(JoinRelation.class, "table", department,
                    "column", "name", "joinColumn", new JavaAnnotation[]{join});
            owner.fields.add(source);
            Diagnostic diagnostic = memory.analyzeMetadata(owner).getDiagnostics().get(0);
            MybatisExtException error = assertThrows(MybatisExtException.class,
                    () -> new TableInfoFactory(policy, new ExtContext()).buildTableInfo(owner));
            assertSameDiagnostic(diagnostic, error.getDiagnostic());
            assertSame(source, diagnostic.getLocation().getSource());
            assertSame(owner, diagnostic.getLocation().getOwner());
            assertEquals("joinColumn." + side, diagnostic.getLocation().getAnnotationMember());
            assertEquals("missing", diagnostic.getLocation().getPath());
            assertEquals(0, diagnostic.getStart());
            assertEquals(7, diagnostic.getEnd());
        }
    }

    @Test
    void tableMismatchPointsAtTheActualMappingAnnotation() {
        address.annotate(Table.class, "name", "another_table");
        Diagnostic diagnostic = memory.analyzeMetadata(record).getDiagnostics().get(0);
        assertTrue(diagnostic.getMessage().contains("Table name mismatch"));
        assertSame(address, diagnostic.getLocation().getSource());
        assertSame(address, diagnostic.getLocation().getOwner());
        assertEquals(Table.class, diagnostic.getLocation().getAnnotationType());
        assertEquals("name", diagnostic.getLocation().getAnnotationMember());
    }

    @Test
    void infrastructureExceptionsAreNotConvertedToSemanticDiagnostics() {
        MybatisExtException failure = new MybatisExtException("infrastructure failure", new SQLException("connection refused"));
        MybatisExtAnalyzer analyzer = new MybatisExtAnalyzer(type -> {
            throw failure;
        }, new ExtContext());
        assertSame(failure, assertThrows(MybatisExtException.class, () -> analyzer.analyzeMetadata(record)));
        TableInfo table = table(memory, record);
        assertSame(failure, assertThrows(MybatisExtException.class, () -> analyzer.analyzeMethod(table,
                method("findByName", record, parameter(text)))));
        assertSame(failure, assertThrows(MybatisExtException.class, () -> analyzer.completeMethod(table,
                method("findByNa", record, parameter(text)), 8)));
        assertNull(failure.getDiagnostic());
        assertEquals("infrastructure failure", failure.getMessage());
    }

    @Test
    void databaseConnectionFailuresKeepTheirCauseWithoutSemanticClassification() {
        Configuration configuration = new Configuration();
        SQLException unavailable = new SQLException("unavailable");
        DataSource dataSource = (DataSource) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{DataSource.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getConnection")) {
                        throw unavailable;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        configuration.setEnvironment(new Environment("test",
                new JdbcTransactionFactory(), dataSource));
        ExtContext context = new ExtContext();
        TableInfoFactory tables = new TableInfoFactory(configuration, context);
        GenericType entity = GenericTypeFactory.build(Record.class);
        GenericMethod method = Arrays.stream(GenericTypeFactory.build(RecordMapper.class).getMethods())
                .filter(m -> m.getName().equals("findByName")).findFirst().get();
        MybatisExtException error = assertThrows(MybatisExtException.class, () -> new MappedStatementHelper(configuration, context, tables)
                .build("test.findByName", entity, Collections.singletonList(method), entity, false));
        assertSame(unavailable, error.getCause());
        assertNull(error.getDiagnostic());
    }

    @Test
    void internallyPrefixedPathsAreMappedBackToTheAnnotationText() {
        MemoryType embedded = new MemoryType("example.Embedded");
        JavaAnnotation join = new Node().annotate(JoinColumn.class,
                "leftColumn", "missing", "rightColumn", "id").annotation(JoinColumn.class);
        MemoryField source = field("label", text).annotate(JoinRelation.class, "table", department,
                "column", "name", "joinColumn", new JavaAnnotation[]{join});
        embedded.fields.add(source);
        record.fields.add(field("value", embedded).annotate(Column.class));
        Diagnostic diagnostic = memory.analyzeMetadata(record).getDiagnostics().get(0);
        assertEquals("value.missing", diagnostic.getLocation().getPath());
        assertEquals("missing", diagnostic.getLocation().getText());
        assertEquals(0, diagnostic.getStart());
        assertEquals(7, diagnostic.getEnd());
        assertSame(embedded, diagnostic.getLocation().getOwner());
        assertSame(source, diagnostic.getLocation().getSource());
    }

    @Test
    void inferredColumnReferenceNamesDoNotInventAnnotationRanges() {
        MemoryType broken = new MemoryType("example.InferredView");
        broken.annotate(TableRef.class, "value", record);
        MemoryField source = field("missing", text).annotate(ColumnRef.class, "value", "");
        broken.fields.add(source);
        Diagnostic diagnostic = memory.analyzeMetadata(broken).getDiagnostics().get(0);
        assertEquals("missing", diagnostic.getLocation().getPath());
        assertSame(source, diagnostic.getLocation().getSource());
        assertNull(diagnostic.getLocation().getText());
        assertFalse(diagnostic.hasRange());
    }

    @Test
    void columnReferenceFailuresKeepTheDtoSourceOwnerRatherThanTheMappedEntity() {
        MemoryType unsupported = new MemoryType("java.util.Map");
        unsupported.arguments = new JavaType[]{text, text};
        MemoryField source = field("displayName", unsupported).annotate(ColumnRef.class, "value", "name");
        view.fields.set(0, source);
        Diagnostic diagnostic = memory.analyzeMetadata(view).getDiagnostics().get(0);
        assertTrue(diagnostic.getMessage().contains("Unsupported property type"));
        assertSame(view, diagnostic.getLocation().getOwner());
        assertSame(source, diagnostic.getLocation().getSource());
        assertFalse(diagnostic.hasRange());
    }

    private static void assertSameDiagnostic(Diagnostic expected, Diagnostic actual) {
        assertEquals(expected.getCode(), actual.getCode());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getStart(), actual.getStart());
        assertEquals(expected.getEnd(), actual.getEnd());
        assertEquals(expected.getLocation().getOwner(), actual.getLocation().getOwner());
        assertSame(expected.getLocation().getSource(), actual.getLocation().getSource());
        assertEquals(expected.getLocation().getAnnotationType(), actual.getLocation().getAnnotationType());
        assertEquals(expected.getLocation().getAnnotationMember(), actual.getLocation().getAnnotationMember());
        assertEquals(expected.getLocation().getPath(), actual.getLocation().getPath());
        assertEquals(expected.getLocation().getText(), actual.getLocation().getText());
    }

    private Set<String> candidates(TableInfo table, String name) {
        AnalysisResult<List<CompletionItem>> result = memory.completeMethod(table, method(name, null), name.length());
        assertTrue(result.getDiagnostics().isEmpty(), messages(result));
        return result.getValue().stream().map(CompletionItem::getText).collect(Collectors.toSet());
    }

    private static TableInfo table(MybatisExtAnalyzer analyzer, JavaType type) {
        AnalysisResult<TableInfo> result = analyzer.analyzeMetadata(type);
        assertTrue(result.isSuccess(), messages(result));
        return result.getValue();
    }

    private static String messages(AnalysisResult<?> result) {
        return result.getDiagnostics().stream().map(Diagnostic::getMessage).collect(Collectors.joining("; "));
    }

    private static String conditionSnapshot(Condition condition) {
        if (condition == null) {
            return "null";
        }
        return condition.getType() + ":" + condition.getLogicalOperator() + ":" + condition.getCompareOperator()
                + ":" + (condition.getPropertyInfo() == null ? "" : condition.getPropertyInfo().getFullName())
                + ":" + condition.getVariable() + ":" + condition.getSecondVariable() + ":" + condition.getCollectionVariable()
                + ":" + condition.getTestMode() + ":" + condition.isIgnorecase() + ":" + condition.isNot()
                + ":" + condition.getTestTemplate() + ":" + condition.getExprTemplate()
                + condition.getSubConditions().stream().map(MybatisExtAnalyzerTest::conditionSnapshot).sorted().collect(Collectors.toList());
    }

    private static Map<String, String> snapshot(TableInfo table) {
        Map<String, String> result = new TreeMap<>();
        snapshot(table.getNameToPropertyInfo(), result);
        return result;
    }

    private static void snapshot(Map<String, PropertyInfo> properties, Map<String, String> target) {
        for (PropertyInfo property : properties.values()) {
            target.put(property.getFullName(), property.getColumnName() + ":" + property.getPropertyType() + ":" + property.isReadonly());
            snapshot(property.getNameToPropertyInfo(), target);
        }
    }

    // 这些模型只包含名称和源码事实，不持有应用的 Class、Field 或 Method。
    private static class Node implements JavaAnnotatedElement {

        final Map<Class<? extends Annotation>, List<JavaAnnotation>> annotations = new HashMap<>();

        Node annotate(Class<? extends Annotation> type, Object... values) {
            Map<String, Object> effective = new HashMap<>();
            effective.put("name", "");
            effective.put("schema", "");
            effective.put("alias", "");
            effective.put("jdbcType", JdbcType.UNDEFINED);
            effective.put("idType", IdType.NONE);
            effective.put("table", new MemoryType("void"));
            effective.put("tableAlias", "");
            effective.put("column", "");
            effective.put("leftTableAlias", "");
            for (int i = 0; i < values.length; i += 2) {
                effective.put((String) values[i], values[i + 1]);
            }
            JavaAnnotation view = new JavaAnnotation() {

                @SuppressWarnings("unchecked")
                public <T> T value(String key) {
                    return (T) effective.get(key);
                }
            };
            annotations.computeIfAbsent(type, key -> new ArrayList<>()).add(view);
            return this;
        }

        public JavaAnnotation annotation(Class<? extends Annotation> type) {
            JavaAnnotation[] values = annotations(type);
            return values.length == 0 ? null : values[0];
        }

        public JavaAnnotation[] annotations(Class<? extends Annotation> type) {
            return annotations.getOrDefault(type, Collections.emptyList()).toArray(new JavaAnnotation[0]);
        }
    }

    private static final class MemoryType extends Node implements JavaType {

        final String name;
        final List<JavaField> fields = new ArrayList<>();
        final List<JavaBeanProperty> beans = new ArrayList<>();
        JavaType parent;
        JavaType[] interfaces = new JavaType[0];
        JavaType[] arguments = new JavaType[0];
        boolean resolved = true;
        boolean interfaceType;

        MemoryType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public String getSimpleName() {
            return name.substring(Math.max(name.lastIndexOf('.'), name.lastIndexOf('$')) + 1);
        }

        public JavaType[] getTypeParameters() {
            return arguments;
        }

        public JavaType getComponentType() {
            return null;
        }

        public JavaField[] getDeclaredFields() {
            return fields.toArray(new JavaField[0]);
        }

        public JavaMethod[] getMethods() {
            return new JavaMethod[0];
        }

        public JavaType getGenericSuperclass() {
            return parent;
        }

        public JavaType[] getGenericInterfaces() {
            return interfaces;
        }

        public JavaBeanProperty[] getBeanProperties() {
            return beans.toArray(new JavaBeanProperty[0]);
        }

        public boolean isResolved() {
            return resolved;
        }

        public boolean isInterface() {
            return interfaceType;
        }

        public boolean isAssignableTo(Class<?> target) {
            if (name.equals(target.getName()) || parent != null && parent.isAssignableTo(target)) {
                return true;
            }
            for (JavaType type : interfaces) {
                if (type.isAssignableTo(target)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static final class MemoryField extends Node implements JavaField {

        final String name;
        final JavaType type;

        MemoryField(String name, JavaType type) {
            this.name = name;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public JavaType getGenericType() {
            return type;
        }

        @Override
        MemoryField annotate(Class<? extends Annotation> type, Object... values) {
            super.annotate(type, values);
            return this;
        }
    }

    private static final class MemoryParameter extends Node implements JavaParameter {

        final JavaType type;

        MemoryParameter(JavaType type) {
            this.type = type;
        }

        public JavaType getGenericType() {
            return type;
        }

        @Override
        MemoryParameter annotate(Class<? extends Annotation> type, Object... values) {
            super.annotate(type, values);
            return this;
        }
    }

    private static final class MemoryMethod extends Node implements JavaMethod {

        final String name;
        final JavaType result;
        JavaType owner;
        JavaParameter[] parameters = new JavaParameter[0];

        MemoryMethod(String name, JavaType result) {
            this.name = name;
            this.result = result;
        }

        public String getName() {
            return name;
        }

        public JavaType getDeclaringType() {
            return owner;
        }

        public JavaType getGenericReturnType() {
            return result;
        }

        public JavaParameter[] getParameters() {
            return parameters;
        }

        public boolean isBridge() {
            return false;
        }

        public boolean isDefault() {
            return false;
        }
    }

    private static MemoryField field(String name, JavaType type) {
        return new MemoryField(name, type);
    }

    private static MemoryType listOf(JavaType element) {
        MemoryType collection = new MemoryType("java.util.Collection");
        collection.arguments = new JavaType[]{element};
        MemoryType list = new MemoryType("java.util.List");
        list.arguments = new JavaType[]{element};
        list.interfaces = new JavaType[]{collection};
        return list;
    }

    private static MemoryParameter parameter(JavaType type) {
        return new MemoryParameter(type);
    }

    private static MemoryMethod method(String name, JavaType result, JavaParameter... parameters) {
        MemoryMethod method = new MemoryMethod(name, result);
        method.parameters = parameters;
        return method;
    }

    static class Parent<T> {

        @Column(name = "record_id")
        @Id
        T id;
    }

    static class Address {

        @Column
        String city;
    }

    @Table(name = "department", alias = "d")
    static class Department {

        @Column
        @Id
        Long id;
        @Column
        String name;
    }

    @Table(name = "analysis_record", alias = "r")
    static class Record extends Parent<Long> {

        @Column
        String name;
        @Column
        Address address;
        @Column
        Long deptId;
        @JoinRelation(table = Department.class, column = "name", joinColumn = @JoinColumn(leftColumn = "deptId", rightColumn = "id"))
        String deptName;
        @JoinRelation(joinColumn = @JoinColumn(leftColumn = "deptId", rightColumn = "id"))
        Department dept;

        @Column(name = "ignored_getter_column")
        public String getName() {
            return name;
        }

        @Column(name = "display_label")
        public String getLabel() {
            return name;
        }
    }

    @TableRef(Record.class)
    static class RecordView {

        @ColumnRef("name")
        String displayName;
        @ColumnRef("address.city")
        String city;
        @ColumnRef("deptName")
        String deptLabel;
        @ColumnRef("deptId")
        Long deptId;
    }

    interface InvalidMapper {

        Record findByMissing();
    }

    interface RecordMapper {

        Record findByName(@Param("name") String name);

        Record findByAddressDotCity(@Param("city") String city);

        int save(Record value);

        int updateIgnoreNull(@OnlyById Record value);

        int delete(@OnlyById Record value);

        long count(Record value);

        boolean exists(Record value);

        List<Record> listByName(@Param("name") String name);

        int saveBatch(List<Record> values);

        int updateBatch(@OnlyById List<Record> values);

        int deleteBatch(@OnlyById List<Record> values);
    }

    @Table
    static class Generated {

        @Column
        @Id(idType = IdType.CUSTOM, customIdGenerator = CountingGenerator.class)
        String id;
    }

    public static class CountingGenerator implements IdGenerator<String> {

        static int constructions;

        public CountingGenerator() {
            constructions++;
        }

        public String getId() {
            return "id";
        }
    }
}
