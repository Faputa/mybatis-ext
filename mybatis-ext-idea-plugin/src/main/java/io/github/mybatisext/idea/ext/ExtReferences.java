package io.github.mybatisext.idea.ext;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;

import io.github.mybatisext.analysis.SymbolReference;
import io.github.mybatisext.idea.model.PsiModel;
import io.github.mybatisext.idea.model.SourceText;
import io.github.mybatisext.idea.xml.MappingReference;
import io.github.mybatisext.idea.xml.MybatisProject;
import io.github.mybatisext.jpa.Variable;
import io.github.mybatisext.metadata.PropertyInfo;
import io.github.mybatisext.model.JavaAnnotatedElement;
import io.github.mybatisext.model.UnresolvedTypeException;

public final class ExtReferences extends PsiReferenceContributor {

    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiLiteralExpression.class), new PsiReferenceProvider() {

            @Override
            public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                return references((PsiLiteralExpression) element);
            }
        });
    }

    public static PsiReference[] references(PsiLiteralExpression literal) {
        if (DumbService.isDumb(literal.getProject())) {
            return PsiReference.EMPTY_ARRAY;
        }
        try {
            SourceText source = SourceText.of(literal);
            if (source == null) {
                return PsiReference.EMPTY_ARRAY;
            }
            PsiElement aliasTarget = ExtPaths.leftAliasTarget(literal);
            if (aliasTarget != null) {
                var range = source.range(0, source.text.length());
                return range == null ? PsiReference.EMPTY_ARRAY : new PsiReference[]{new MappingReference(literal, range, true, "Unknown left table alias", () -> List.of(aliasTarget), () -> new Object[0])};
            }
            ExtPaths.Context context = ExtPaths.context(literal);
            if (context == null) {
                return PsiReference.EMPTY_ARRAY;
            }
            List<PsiReference> result = new ArrayList<>();
            if (context.parameterProperty()) {
                var analyzed = context.session().analyzer.resolveParameterProperty(context.table(), source.text);
                for (SymbolReference reference : analyzed.getReferences()) {
                    var range = source.range(reference.getStart(), reference.getEnd());
                    PsiElement target = target(reference.getTarget(), null);
                    if (range != null && target != null) {
                        result.add(new PsiReferenceBase<PsiLiteralExpression>(literal, range, true) {

                            @Override
                            public PsiElement resolve() {
                                return target;
                            }

                            @Override
                            public PsiElement handleElementRename(@NotNull String newName) {
                                String replacement = reference.getStart() == 0 ? newName : newName.substring(0, 1).toUpperCase() + newName.substring(1);
                                return ElementManipulators.handleContentChange(getElement(), getRangeInElement(), replacement);
                            }
                        });
                    }
                }
                return result.toArray(PsiReference[]::new);
            }
            var analyzed = context.session().analyzer.resolvePropertyPath(context.table(), source.text);
            int start = 0;
            for (String segment : source.text.split("\\.", -1)) {
                int offset = start;
                var range = source.range(start, start + segment.length());
                List<PsiElement> targets = analyzed.getReferences().stream().filter(r -> r.getStart() == offset).map(r -> target(r.getTarget(), null)).filter(Objects::nonNull).toList();
                if (range != null) {
                    result.add(new MappingReference(literal, range, true, "Unknown metadata path", () -> targets,
                            () -> context.session().analyzer.completePropertyPath(context.table(), source.text, offset).stream().map(c -> LookupElementBuilder.create(c.getText())).toArray()));
                }
                start += segment.length() + 1;
            }
            return result.toArray(PsiReference[]::new);
        } catch (UnresolvedTypeException e) {
            return PsiReference.EMPTY_ARRAY;
        }
    }

    public static PsiElement target(Object target, PsiMethod method) {
        if (target instanceof JavaAnnotatedElement) {
            return PsiModel.source(target);
        }
        if (target instanceof PropertyInfo property) {
            return PsiModel.source(property.getSource());
        }
        if (target instanceof Variable variable && method != null) {
            if (variable.getSource() != null) {
                return PsiModel.source(variable.getSource());
            }
            String root = variable.getFullName().split("\\.")[0];
            PsiParameter[] parameters = method.getParameterList().getParameters();
            for (int i = 0; i < parameters.length; i++) {
                PsiParameter p = parameters[i];
                String name = MybatisProject.annotationString(p.getAnnotation(MybatisProject.PARAM), "value");
                if (root.equals(name) || root.equals(p.getName()) || root.equals("param" + (i + 1))) {
                    return p;
                }
            }
            if (parameters.length == 1) {
                return parameters[0];
            }
        }
        return null;
    }

    public static List<SymbolReference> methodReferences(PsiMethod method) {
        if (DumbService.isDumb(method.getProject()) || method.getContainingClass() == null) {
            return List.of();
        }
        var analyzed = new ExtSession(method).analyzeMethod(method);
        return analyzed != null ? analyzed.getReferences() : List.of();
    }
}
