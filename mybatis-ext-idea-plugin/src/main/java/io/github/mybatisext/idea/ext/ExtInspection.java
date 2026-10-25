package io.github.mybatisext.idea.ext;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;

import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.diagnostic.Diagnostic;
import io.github.mybatisext.model.UnresolvedTypeException;

public final class ExtInspection extends LocalInspectionTool {

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitClass(@NotNull PsiClass cls) {
                if (DumbService.isDumb(cls.getProject()) || cls.getQualifiedName() == null || PsiTreeUtil.findChildOfType(cls, PsiErrorElement.class) != null) {
                    return;
                }
                ExtSession session = new ExtSession(cls);
                try {
                    boolean mapper = session.enhanced(cls);
                    boolean metadata = cls.hasAnnotation(Table.class.getName()) || cls.hasAnnotation(TableRef.class.getName());
                    if (!mapper && !metadata) {
                        return;
                    }
                    var table = mapper ? session.table(cls) : session.analyzer.analyzeMetadata(session.model.type(cls));
                    report(table.getDiagnostics(), cls);
                    if (!mapper || !table.isSuccess()) {
                        return;
                    }
                    for (PsiMethod method : cls.getMethods()) {
                        if (method.getReturnType() == null || !session.derived(cls, method)) {
                            continue;
                        }
                        report(session.analyzer.analyzeMethod(table.getValue(), session.model.method(method, session.model.type(cls))).getDiagnostics(), method);
                    }
                } catch (UnresolvedTypeException e) {
                    report(List.of(e.getDiagnostic()), cls);
                }
            }

            private void report(List<Diagnostic> diagnostics, PsiElement fallback) {
                for (Diagnostic diagnostic : diagnostics) {
                    ExtLocation.Highlight highlight = ExtLocation.locate(diagnostic, fallback);
                    ProblemHighlightType level = diagnostic.getCode() == Diagnostic.Code.UNRESOLVED_TYPE ? ProblemHighlightType.WEAK_WARNING : ProblemHighlightType.GENERIC_ERROR;
                    holder.registerProblem(highlight.element(), diagnostic.getMessage(), level, highlight.range());
                }
            }
        };
    }
}
