package io.github.mybatisext.idea.xml;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;

public final class MappingReference extends PsiPolyVariantReferenceBase<PsiElement> {

    private final Supplier<? extends Collection<? extends PsiElement>> targets;
    private final Supplier<Object[]> variants;
    public final String problem;

    public MappingReference(PsiElement element, TextRange range, boolean soft, String problem,
            Supplier<? extends Collection<? extends PsiElement>> targets, Supplier<Object[]> variants) {
        super(element, range, soft);
        this.targets = targets;
        this.variants = variants;
        this.problem = problem;
    }

    @Override
    public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        return targets.get().stream().filter(Objects::nonNull).distinct().map(PsiElementResolveResult::new).toArray(ResolveResult[]::new);
    }

    @Override
    public Object @NotNull [] getVariants() {
        return variants.get();
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newName) {
        if (myElement instanceof XmlToken) {
            XmlText text = PsiTreeUtil.getParentOfType(myElement, XmlText.class);
            if (text != null) {
                int offset = myElement.getTextOffset() - text.getTextOffset();
                TextRange physical = getRangeInElement().shiftRight(offset);
                // XmlText 的修改器接收解码后的偏移，不能直接传入包含 CDATA 标记或实体转义的物理偏移。
                TextRange decoded = new TextRange(text.physicalToDisplay(physical.getStartOffset()), text.physicalToDisplay(physical.getEndOffset()));
                return ElementManipulators.handleContentChange(text, decoded, newName);
            }
        }
        return ElementManipulators.handleContentChange(myElement, getRangeInElement(), newName);
    }
}
