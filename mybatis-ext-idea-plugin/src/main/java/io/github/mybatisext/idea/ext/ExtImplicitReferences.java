package io.github.mybatisext.idea.ext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.intellij.model.Symbol;
import com.intellij.model.psi.ImplicitReferenceProvider;
import com.intellij.model.psi.PsiSymbolReference;
import com.intellij.model.psi.PsiSymbolService;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;

import io.github.mybatisext.analysis.SymbolReference;

/** 将声明中的 CRUD 单词作为隐式引用，供 IDEA 按单词范围高亮和导航。 */
public final class ExtImplicitReferences implements ImplicitReferenceProvider {

    @Override
    public PsiSymbolReference getImplicitReference(@NotNull PsiElement element, int offsetInElement) {
        if (!(element instanceof PsiIdentifier) || !(element.getParent() instanceof PsiMethod method)
                || method.getNameIdentifier() != element || offsetInElement < 0 || offsetInElement >= element.getTextLength()) {
            return null;
        }

        TextRange range = null;
        Set<PsiElement> targets = new LinkedHashSet<>();
        for (SymbolReference reference : ExtReferences.methodReferences(method)) {
            if (offsetInElement < reference.getStart() || offsetInElement >= reference.getEnd()) {
                continue;
            }

            PsiElement target = ExtReferences.target(reference.getTarget());
            if (target != null) {
                range = new TextRange(reference.getStart(), reference.getEnd());
                targets.add(target);
            }
        }
        if (targets.isEmpty()) {
            return null;
        }

        TextRange referenceRange = range;
        List<Symbol> symbols = new ArrayList<>();
        for (PsiElement target : targets) {
            symbols.add(PsiSymbolService.getInstance().asSymbol(target));
        }
        return new PsiSymbolReference() {

            @Override
            public @NotNull PsiElement getElement() {
                return element;
            }

            @Override
            public @NotNull TextRange getRangeInElement() {
                return referenceRange;
            }

            @Override
            public @NotNull Collection<? extends Symbol> resolveReference() {
                return symbols;
            }
        };
    }
}
