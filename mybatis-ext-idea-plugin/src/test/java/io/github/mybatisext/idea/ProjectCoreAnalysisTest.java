package io.github.mybatisext.idea;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;

import io.github.mybatisext.idea.ext.CrudDocumentation;
import io.github.mybatisext.idea.ext.ExtReferences;
import io.github.mybatisext.idea.ext.ExtSession;

/** 有效源码能否分析不应由项目的 Maven 元数据决定。 */
public final class ProjectCoreAnalysisTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_17;
    }

    private static String jar(Path path) {
        return "jar://" + path.toAbsolutePath().toString().replace('\\', '/') + "!/";
    }

    private void useCore(Path core) {
        ModuleRootModificationUtil.updateModel(getModule(), model -> {
            for (var entry : model.getOrderEntries()) {
                if (entry instanceof LibraryOrderEntry library && library.isModuleLevel()) {
                    model.removeOrderEntry(entry);
                }
            }
        });
        ModuleRootModificationUtil.addModuleLibrary(getModule(), "project-core", List.of(jar(core), jar(Path.of(System.getProperty("mybatisext.mybatis.jar")))), List.of());
        CodeInsightTestFixtureImpl.ensureIndexesUpToDate(getProject());
    }

    private Path artifact(String version) throws IOException {
        Path copy = Files.createTempFile("mybatisext-project-core-", ".jar");
        copy.toFile().deleteOnExit();
        String metadata = "META-INF/maven/io.github.mybatis-ext/mybatis-ext/pom.properties";
        try (ZipFile original = new ZipFile(System.getProperty("mybatisext.core.jar")); ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(copy))) {
            for (var entries = original.entries(); entries.hasMoreElements();) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().equals(metadata)) {
                    continue;
                }
                output.putNextEntry(new ZipEntry(entry.getName()));
                try (InputStream stream = original.getInputStream(entry)) {
                    stream.transferTo(output);
                }
                output.closeEntry();
            }
            if (version != null) {
                output.putNextEntry(new ZipEntry(metadata));
                output.write(("version=" + version + "\n").getBytes(StandardCharsets.ISO_8859_1));
                output.closeEntry();
            }
        }
        return copy;
    }

    public void testDifferentAndUnknownVersionsDoNotDisableValidAnalysis() throws Exception {
        PsiClass entity = myFixture.addClass("package sample; @io.github.mybatisext.annotation.Table public class User { @io.github.mybatisext.annotation.Column public String name; }");
        PsiClass mapper = myFixture.addClass("package sample; @io.github.mybatisext.annotation.MapTable(User.class) public interface UserMapper { User findByName(String name); }");
        PsiJavaFile view = (PsiJavaFile) myFixture.addFileToProject("sample/View.java", "package sample; @io.github.mybatisext.annotation.TableRef(User.class) class View { @io.github.mybatisext.annotation.ColumnRef(\"name\") String label; }");
        for (Path artifact : List.of(artifact("999.0.0"), artifact(null))) {
            useCore(artifact);
            ExtSession session = new ExtSession(entity);
            assertTrue(session.analyzer.analyzeMetadata(session.model.type(entity)).isSuccess());
            PsiMethod method = mapper.getMethods()[0];
            assertFalse(ExtReferences.methodReferences(method).isEmpty());
            String preview = new CrudDocumentation().generateDoc(method, method);
            assertNotNull(preview);
            assertTrue(preview, preview.contains("&lt;script&gt;"));
            assertFalse(preview, preview.contains("analysis paused"));
            PsiLiteralExpression path = PsiTreeUtil.findChildOfType(view, PsiLiteralExpression.class);
            assertTrue(Arrays.stream(ExtReferences.references(path)).anyMatch(r -> r.resolve() instanceof PsiField));
        }
    }

    public void testCoreSourceWithoutPomCanBeAnalyzed() {
        myFixture.addClass("package io.github.mybatisext.annotation; public @interface Table {}");
        myFixture.addClass("package io.github.mybatisext.annotation; public @interface Column {}");
        PsiClass entity = myFixture.addClass("package sample; @io.github.mybatisext.annotation.Table public class User { @io.github.mybatisext.annotation.Column public String name; }");
        ExtSession session = new ExtSession(entity);
        var metadata = session.analyzer.analyzeMetadata(session.model.type(entity));
        assertTrue(metadata.getDiagnostics().stream().map(d -> d.getMessage()).toList().toString(), metadata.isSuccess());
        assertNotNull(metadata.getValue().get("name"));
    }
}
