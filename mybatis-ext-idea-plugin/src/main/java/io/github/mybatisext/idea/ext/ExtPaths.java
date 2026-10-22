package io.github.mybatisext.idea.ext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.PsiTreeUtil;

import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;
import io.github.mybatisext.idea.xml.MybatisProject;
import io.github.mybatisext.metadata.TableInfo;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.JavaTypes;

/** 解析注解路径的归属，属性路径语法仍由核心处理。 */
public final class ExtPaths {

    public record Context(ExtSession session, TableInfo table, boolean parameterProperty) {

        public Context(ExtSession session, TableInfo table) {
            this(session, table, false);
        }
    }

    public static PsiElement leftAliasTarget(PsiLiteralExpression literal) {
        PsiNameValuePair pair = PsiTreeUtil.getParentOfType(literal, PsiNameValuePair.class);
        PsiAnnotation column = pair == null ? null : PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class);
        if (pair == null || pair.getValue() != literal || !"leftTableAlias".equals(pair.getName())
                || column == null || !JoinColumn.class.getName().equals(column.getQualifiedName())
                || !(literal.getValue() instanceof String alias) || alias.isBlank()) {
            return null;
        }

        PsiAnnotation relation = PsiTreeUtil.getParentOfType(column, PsiAnnotation.class);
        PsiClass owner = PsiTreeUtil.getParentOfType(column, PsiClass.class);
        PsiMember member = relation == null ? null : PsiTreeUtil.getParentOfType(relation, PsiField.class, PsiMethod.class);
        if (relation == null || !JoinRelation.class.getName().equals(relation.getQualifiedName())
                || owner == null || member == null || member.getModifierList() == null) {
            return null;
        }

        List<PsiAnnotation> declarations = new ArrayList<>();
        PsiAnnotation root = owner.getAnnotation(TableRef.class.getName());
        declarations.add(root == null ? owner.getAnnotation(Table.class.getName()) : root);
        declarations.add(owner.getAnnotation(JoinParent.class.getName()));
        for (PsiAnnotation previous : PsiTreeUtil.findChildrenOfType(member.getModifierList(), PsiAnnotation.class)) {
            if (previous.getTextOffset() < relation.getTextOffset() && JoinRelation.class.getName().equals(previous.getQualifiedName())) {
                declarations.add(previous);
            }
        }

        List<PsiElement> targets = new ArrayList<>();
        for (PsiAnnotation declaration : declarations) {
            if (declaration == null) {
                continue;
            }
            String attribute = declaration.getQualifiedName().endsWith("JoinRelation") ? "tableAlias" : "alias";
            if (alias.equals(MybatisProject.annotationString(declaration, attribute))) {
                targets.add(declaration.findDeclaredAttributeValue(attribute));
            }
        }

        return targets.size() == 1 ? targets.get(0) : null;
    }

    public static Context context(PsiLiteralExpression literal) {
        PsiNameValuePair pair = PsiTreeUtil.getParentOfType(literal, PsiNameValuePair.class);
        PsiAnnotation annotation = pair == null ? null : PsiTreeUtil.getParentOfType(pair, PsiAnnotation.class);
        PsiClass owner = PsiTreeUtil.getParentOfType(literal, PsiClass.class);
        if (annotation == null || owner == null || !(literal.getValue() instanceof String)) {
            return null;
        }
        String member = pair.getName() == null ? "value" : pair.getName();
        String name = annotation.getQualifiedName();
        ExtSession session = new ExtSession(literal);
        if (MybatisProject.PARAM.equals(name) && member.equals("value") && pair.getValue() == literal) {
            PsiParameter parameter = PsiTreeUtil.getParentOfType(annotation, PsiParameter.class);
            PsiMethod method = parameter == null ? null : PsiTreeUtil.getParentOfType(parameter, PsiMethod.class);
            if (method == null || !session.derived(owner, method)) {
                return null;
            }
            var table = session.table(owner);
            if (!table.isSuccess()) {
                return null;
            }
            int index = method.getParameterList().getParameterIndex(parameter);
            TableInfo effective = session.analyzer.parameterPropertyTable(table.getValue(), session.model.method(method, session.model.type(owner)), index);
            return effective == null ? null : new Context(session, effective, true);
        }
        if (ColumnRef.class.getName().equals(name) && member.equals("value")) {
            TableInfo table = session.pathTable(owner, true);
            return table == null ? null : new Context(session, table);
        }
        PsiAnnotation relation = JoinRelation.class.getName().equals(name)
                ? annotation
                : PsiTreeUtil.getParentOfType(annotation, PsiAnnotation.class);
        if (relation == null || relation.getQualifiedName() == null) {
            return null;
        }
        if (JoinRelation.class.getName().equals(name) && !member.equals("column")) {
            return null;
        }
        if (JoinColumn.class.getName().equals(name) && !Set.of("leftColumn", "rightColumn").contains(member)) {
            return null;
        }
        if (!Set.of(JoinRelation.class.getName(), JoinParent.class.getName()).contains(relation.getQualifiedName())) {
            return null;
        }
        boolean right = member.equals("rightColumn") || member.equals("column");
        JavaType target = session.model.type(owner);
        if (right) {
            if (relation.getQualifiedName().endsWith("JoinParent")) {
                target = target.getGenericSuperclass();
            } else {
                PsiAnnotationMemberValue table = relation.findAttributeValue("table");
                if (table instanceof PsiClassObjectAccessExpression cls && !PsiTypes.voidType().equals(cls.getOperand().getType())) {
                    target = session.model.type(cls.getOperand().getType());
                } else {
                    PsiMember source = PsiTreeUtil.getParentOfType(relation, PsiField.class, PsiMethod.class);
                    PsiType type = source instanceof PsiField f ? f.getType() : source instanceof PsiMethod m ? m.getReturnType() : null;
                    target = JavaTypes.unwrap(session.model.type(type));
                }
            }
        } else {
            if (target.annotation(TableRef.class) != null) {
                target = target.annotation(TableRef.class).value("value");
            }
            Map<String, JavaType> aliases = new HashMap<>();
            for (String a : List.of(Table.class.getName(), TableRef.class.getName())) {
                PsiAnnotation root = owner.getAnnotation(a);
                String rootAlias = MybatisProject.annotationString(root, "alias");
                if (rootAlias != null && !rootAlias.isBlank()) {
                    aliases.put(rootAlias, target);
                }
            }
            PsiAnnotation parent = owner.getAnnotation(JoinParent.class.getName());
            String parentAlias = MybatisProject.annotationString(parent, "alias");
            if (parentAlias != null && !parentAlias.isBlank()) {
                aliases.put(parentAlias, session.model.type(owner).getGenericSuperclass());
            }
            PsiMember memberOwner = PsiTreeUtil.getParentOfType(relation, PsiField.class, PsiMethod.class);
            if (memberOwner != null && memberOwner.getModifierList() != null) {
                for (PsiAnnotation previous : PsiTreeUtil.findChildrenOfType(memberOwner.getModifierList(), PsiAnnotation.class)) {
                    if (previous == relation) {
                        break;
                    }
                    if (!JoinRelation.class.getName().equals(previous.getQualifiedName())) {
                        continue;
                    }
                    JavaType previousTable = relationTable(previous, session);
                    if (previousTable == null) {
                        return null;
                    }
                    target = previousTable;
                    String alias = MybatisProject.annotationString(previous, "tableAlias");
                    if (alias != null && !alias.isBlank()) {
                        aliases.put(alias, previousTable);
                    }
                }
            }
            String alias = MybatisProject.annotationString(annotation, "leftTableAlias");
            if (alias != null && !alias.isBlank()) {
                target = aliases.get(alias);
            }
        }
        if (target == null) {
            return null;
        }
        var result = session.analyzer.analyzeMetadata(target);
        return result.isSuccess() ? new Context(session, result.getValue()) : null;
    }

    private static JavaType relationTable(PsiAnnotation relation, ExtSession session) {
        PsiAnnotationMemberValue explicit = relation.findAttributeValue("table");
        if (explicit instanceof PsiClassObjectAccessExpression cls && !PsiTypes.voidType().equals(cls.getOperand().getType())) {
            return session.model.type(cls.getOperand().getType());
        }
        PsiMember member = PsiTreeUtil.getParentOfType(relation, PsiField.class, PsiMethod.class);
        PsiType type = member instanceof PsiField f ? f.getType() : member instanceof PsiMethod m ? m.getReturnType() : null;
        return JavaTypes.unwrap(session.model.type(type));
    }
}
