package io.github.mybatisext.idea.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;

import io.github.mybatisext.idea.ext.AnalysisSettings;
import io.github.mybatisext.idea.model.PsiModel;
import io.github.mybatisext.idea.model.PsiTypePolicy;
import io.github.mybatisext.model.TypePolicy;

/** 解析 MyBatis 参数名及 foreach、bind 的词法作用域变量；未知的 Map 键使用软引用。 */
public final class XmlBindings {

    public record Binding(PsiElement source, PsiType type, boolean dynamic) {
    }

    public final Map<String, Binding> names = new LinkedHashMap<>();
    public PsiType singleType;
    public boolean dynamic;
    private Binding singleScalar;

    public XmlBindings(PsiElement context) {
        PsiModel model = new PsiModel(context.getProject());
        PsiTypePolicy typePolicy = new PsiTypePolicy(context.getProject().getService(AnalysisSettings.class).getState());
        MybatisProject project = new MybatisProject(context);
        XmlTag statement = MybatisProject.statement(context);
        if (statement == null) {
            dynamic = true;
            return;
        }
        PsiClass mapper = project.mapper(MybatisProject.root(statement));
        List<Pair<PsiMethod, PsiSubstitutor>> methods = mapper == null
                ? List.of()
                : mapper.findMethodsAndTheirSubstitutorsByName(Objects.toString(statement.getAttributeValue("id"), ""), true);
        for (Pair<PsiMethod, PsiSubstitutor> signature : methods) {
            PsiMethod method = signature.first;
            List<PsiParameter> parameters = Arrays.stream(method.getParameterList().getParameters()).filter(p -> !special(p.getType())).toList();
            Set<String> parameterNames = new HashSet<>();
            for (PsiParameter parameter : parameters) {
                String explicit = MybatisProject.annotationString(parameter.getAnnotation(MybatisProject.PARAM), "value");
                parameterNames.add(explicit == null ? parameter.getName() : explicit);
            }

            for (int i = 0; i < parameters.size(); i++) {
                PsiParameter p = parameters.get(i);
                String explicit = MybatisProject.annotationString(p.getAnnotation(MybatisProject.PARAM), "value");
                PsiType parameterType = signature.second.substitute(p.getType());
                Binding binding = new Binding(p, parameterType, map(parameterType));
                names.put(explicit == null ? p.getName() : explicit, binding);
                // 与 ParamNameResolver 一致，通用名称不能覆盖已有的参数名。
                String genericName = "param" + (i + 1);
                if (!parameterNames.contains(genericName)) {
                    names.put(genericName, binding);
                }
                if (parameters.size() == 1 && explicit == null) {
                    singleType = parameterType;
                    dynamic |= binding.dynamic;
                    names.put("_parameter", binding);
                    PsiClass cls = PsiUtil.resolveClassInType(singleType);
                    boolean collection = InheritanceUtil.isInheritor(cls, Collection.class.getName());
                    if (!(singleType instanceof PsiArrayType) && !collection && typePolicy.classify(model.type(singleType)) == TypePolicy.ScalarKind.SCALAR) {
                        singleScalar = binding;
                    }
                    if (parameterType instanceof PsiArrayType) {
                        names.put("array", binding);
                    }
                    if (collection) {
                        names.put("collection", binding);
                    }
                    if (InheritanceUtil.isInheritor(cls, List.class.getName())) {
                        names.put("list", binding);
                    }
                }
            }
        }
        if (methods.isEmpty()) {
            singleType = MybatisProject.classType(project.resolveType(statement.getAttributeValue("parameterType")));
            dynamic = singleType == null || map(singleType);
            if (singleType != null && typePolicy.classify(model.type(singleType)) == TypePolicy.ScalarKind.SCALAR) {
                singleScalar = new Binding(statement, singleType, false);
            }
        }
        names.put("_databaseId", new Binding(statement, null, true));
        // 先处理最外层作用域，内层变量覆盖同名的外层变量。
        List<XmlTag> parents = new ArrayList<>();
        for (XmlTag t = PsiTreeUtil.getParentOfType(context, XmlTag.class); t != null; t = t.getParentTag()) {
            parents.add(t);
            if (t == statement) {
                break;
            }
        }
        Collections.reverse(parents);
        for (XmlTag t : parents) {
            boolean ownAttribute = context instanceof XmlAttributeValue attr && attr.getParent().getParent() == t;
            if ("foreach".equals(t.getName()) && !ownAttribute) {
                Binding collection = resolve(Objects.toString(t.getAttributeValue("collection"), ""));
                String item = Objects.toString(t.getAttributeValue("item"), "item");
                PsiType itemType = collection == null ? null : MybatisProject.unwrap(collection.type);
                if (collection != null && map(collection.type) && collection.type instanceof PsiClassType c && c.getParameters().length == 2) {
                    itemType = c.getParameters()[1];
                }
                names.put(item, new Binding(t.getAttribute("item") == null ? t : t.getAttribute("item"), itemType, itemType == null || map(itemType)));
                names.put(Objects.toString(t.getAttributeValue("index"), "index"), new Binding(t, null, true));
            }
            for (XmlTag sibling : t.getSubTags()) {
                if (sibling.getTextOffset() >= context.getTextOffset()) {
                    break;
                }
                if ("bind".equals(sibling.getName()) && sibling.getAttributeValue("name") != null) {
                    names.put(sibling.getAttributeValue("name"), new Binding(sibling, null, true));
                }
            }
        }
    }

    public Binding resolve(String path) {
        if (path.isEmpty()) {
            return null;
        }
        String[] segments = path.split("\\.");
        Binding value = names.get(segments[0]);
        // 单个标量参数由 TypeHandler 直接绑定，占位符名称不必与形参名一致。
        if (singleScalar != null && (value == null || value == singleScalar)) {
            return singleScalar;
        }
        int first = value == null ? 0 : 1;
        if (value == null && singleType != null) {
            value = new Binding(null, singleType, map(singleType));
        }
        for (int i = first; i < segments.length && value != null; i++) {
            if (value.dynamic) {
                return new Binding(value.source, null, true);
            }
            PsiElement property = MybatisProject.properties(value.type).get(segments[i]);
            if (property == null) {
                return null;
            }
            PsiType type = MybatisProject.propertyType(value.type, property);
            value = new Binding(MybatisProject.propertyTarget(segments[i], property), type, map(type));
        }
        return value;
    }

    public Map<String, Binding> candidates(String parent) {
        Map<String, Binding> result = new LinkedHashMap<>();
        if (parent.isEmpty()) {
            result.putAll(names);
            if (singleType != null) {
                MybatisProject.properties(singleType).forEach((n, p) -> result.putIfAbsent(n, new Binding(MybatisProject.propertyTarget(n, p), MybatisProject.propertyType(singleType, p), false)));
            }
        } else {
            Binding binding = resolve(parent);
            if (binding != null) {
                MybatisProject.properties(binding.type).forEach((n, p) -> result.put(n, new Binding(MybatisProject.propertyTarget(n, p), MybatisProject.propertyType(binding.type, p), false)));
            }
        }
        return result;
    }

    private static boolean special(PsiType type) {
        PsiClass cls = PsiUtil.resolveClassInType(type);
        return cls != null && (RowBounds.class.getName().equals(cls.getQualifiedName()) || InheritanceUtil.isInheritor(cls, ResultHandler.class.getName()));
    }

    public static boolean map(PsiType type) {
        PsiClass cls = PsiUtil.resolveClassInType(type);
        return cls != null && (Object.class.getName().equals(cls.getQualifiedName()) || Map.class.getName().equals(cls.getQualifiedName()) || InheritanceUtil.isInheritor(cls, Map.class.getName()));
    }
}
