package io.github.mybatisext.idea.ext;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import javax.swing.JEditorPane;

import org.jetbrains.annotations.NotNull;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameIdentifierOwner;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.HTMLEditorKitBuilder;
import com.intellij.util.ui.JBUI;

import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.idea.model.PsiModel;
import io.github.mybatisext.idea.xml.MybatisIcons;
import io.github.mybatisext.model.UnresolvedTypeException;

public final class ExtNavigation extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {
        if (DumbService.isDumb(element.getProject())) {
            return;
        }
        if (!(element instanceof PsiIdentifier) || !(element.getParent() instanceof PsiMember member)
                || !(member instanceof PsiField || member instanceof PsiMethod)
                || ((PsiNameIdentifierOwner) member).getNameIdentifier() != element) {
            return;
        }
        PsiElement metadataTarget = metadataTarget(member);
        if (metadataTarget != null) {
            result.add(NavigationGutterIconBuilder.create(MybatisIcons.MAPPER).setTarget(metadataTarget)
                    .setTooltipText("Navigate to mybatis-ext mapped table or property").createLineMarkerInfo(element));
        }
        if (!(member instanceof PsiMethod method)) {
            return;
        }
        var analyzed = new ExtSession(method).analyzeMethod(method);
        if (analyzed != null && analyzed.isSuccess()) {
            result.add(new RelatedItemLineMarkerInfo<>(element, element.getTextRange(), MybatisIcons.ENHANCED,
                    e -> "查看 mybatis-ext 生成的 SQL 脚本",
                    this::showScript,
                    GutterIconRenderer.Alignment.CENTER, List::of));
        }
    }

    private void showScript(MouseEvent event, PsiElement identifier) {
        String html = new CrudDocumentation().generateDoc(identifier.getParent(), identifier);
        if (html == null) {
            return;
        }

        JEditorPane preview = new JEditorPane();
        preview.setEditable(false);
        preview.setEditorKit(new HTMLEditorKitBuilder().withWordWrapViewFactory().build());
        preview.setBorder(JBUI.Borders.empty(8));
        preview.setText(html);
        preview.setCaretPosition(0);
        JBScrollPane scrollPane = new JBScrollPane(preview);
        scrollPane.setPreferredSize(JBUI.size(600, 400));
        var popup = JBPopupFactory.getInstance().createComponentPopupBuilder(scrollPane, preview)
                .setProject(identifier.getProject()).setResizable(true).setMovable(true).setRequestFocus(true).createPopup();
        popup.show(new RelativePoint(event));
    }

    private PsiElement metadataTarget(PsiMember member) {
        PsiClass owner = member.getContainingClass();
        if (owner == null || !(owner.hasAnnotation(Table.class.getName()) || owner.hasAnnotation(TableRef.class.getName()))) {
            return null;
        }
        try {
            ExtSession session = new ExtSession(member);
            var table = session.analyzer.analyzeMetadata(session.model.type(owner));
            if (!table.isSuccess()) {
                return null;
            }
            for (var property : table.getValue().getNameToPropertyInfo().values()) {
                if (PsiModel.source(property.getSource()) == member) {
                    return PsiModel.source(property.getMappingSource());
                }
            }
            return null;
        } catch (UnresolvedTypeException e) {
            return null;
        }
    }
}
