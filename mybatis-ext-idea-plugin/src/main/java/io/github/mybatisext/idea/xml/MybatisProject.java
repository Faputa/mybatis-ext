package io.github.mybatisext.idea.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.type.Alias;
import org.apache.ibatis.type.TypeAliasRegistry;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.indexing.FileBasedIndex;

import io.github.mybatisext.idea.model.PsiModel;
import io.github.mybatisext.model.JavaBeanProperty;
import io.github.mybatisext.model.JavaTypes;
import io.github.mybatisext.model.UnresolvedTypeException;

public final class MybatisProject {

    public static final Set<String> STATEMENTS = Set.of("select", "insert", "update", "delete");
    public static final String PARAM = Param.class.getName();
    private final Project project;
    private final GlobalSearchScope scope;

    public MybatisProject(PsiElement context) {
        project = context.getProject();
        scope = context.getResolveScope();
    }

    public List<XmlTag> roots(String key) {
        if (DumbService.isDumb(project)) {
            return List.of();
        }
        List<XmlTag> result = new ArrayList<>();
        for (var file : FileBasedIndex.getInstance().getContainingFiles(MapperIndex.NAME, key, scope)) {
            ProgressManager.checkCanceled();
            if (PsiManager.getInstance(project).findFile(file) instanceof XmlFile xml && xml.getRootTag() != null) {
                result.add(xml.getRootTag());
            }
        }
        return result;
    }

    public List<XmlTag> mappers(PsiClass mapper) {
        return roots("mapper:" + PsiModel.binaryName(mapper));
    }

    public static XmlTag root(PsiElement element) {
        if (!(element.getContainingFile() instanceof XmlFile file)) {
            return null;
        }
        XmlTag root = file.getRootTag();
        return root != null && Set.of("mapper", "configuration").contains(root.getName()) ? root : null;
    }

    public PsiClass mapper(XmlTag root) {
        return root == null ? null : findClass(root.getAttributeValue("namespace"));
    }

    public PsiClass findClass(String name) {
        return name == null || name.isBlank() ? null : JavaPsiFacade.getInstance(project).findClass(name.replace('$', '.'), scope);
    }

    public List<XmlTag> statements(PsiClass mapper, String method, boolean inherited) {
        List<XmlTag> result = new ArrayList<>();
        collectStatements(mapper, method, inherited, new HashSet<>(), result);
        return result;
    }

    private void collectStatements(PsiClass mapper, String method, boolean inherited, Set<PsiClass> visited, List<XmlTag> result) {
        if (mapper == null || !visited.add(mapper)) {
            return;
        }
        for (XmlTag root : mappers(mapper)) {
            for (XmlTag tag : root.getSubTags()) {
                if (STATEMENTS.contains(tag.getName()) && (method == null || method.equals(tag.getAttributeValue("id")))) {
                    result.add(tag);
                }
            }
        }
        if (inherited) {
            for (PsiClass parent : mapper.getInterfaces()) {
                collectStatements(parent, method, true, visited, result);
            }
        }
    }

    public boolean statementExists(String id) {
        int dot = id.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        String name = id.substring(dot + 1);
        for (XmlTag root : roots("mapper:" + id.substring(0, dot))) {
            for (XmlTag tag : root.getSubTags()) {
                if (STATEMENTS.contains(tag.getName()) && name.equals(tag.getAttributeValue("id"))) {
                    return true;
                }
            }
        }
        PsiClass mapper = findClass(id.substring(0, dot));
        if (mapper != null) {
            for (PsiMethod method : mapper.findMethodsByName(name, false)) {
                if (hasSqlAnnotation(method)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasSqlAnnotation(PsiMethod method) {
        return hasSqlAnnotation(method.getModifierList());
    }

    public static boolean hasSqlAnnotation(PsiModifierList modifiers) {
        for (PsiAnnotation a : modifiers.getAnnotations()) {
            String n = a.getQualifiedName();
            if (n != null && n.startsWith("org.apache.ibatis.annotations.") && Set.of("Select", "Insert", "Update", "Delete", "SelectProvider", "InsertProvider", "UpdateProvider", "DeleteProvider").contains(n.substring(n.lastIndexOf('.') + 1))) {
                return true;
            }
        }
        return false;
    }

    public List<XmlTag> declarations(XmlTag context, String id, String kind) {
        XmlTag root = root(context);
        if (root == null) {
            return List.of();
        }
        String ns = root.getAttributeValue("namespace");
        String local = id;
        int dot = id.lastIndexOf('.');
        if (dot >= 0) {
            ns = id.substring(0, dot);
            local = id.substring(dot + 1);
        }
        List<XmlTag> result = new ArrayList<>();
        for (XmlTag r : roots("mapper:" + ns)) {
            for (XmlTag tag : r.getSubTags()) {
                if (tag.getName().equals(kind) && local.equals(tag.getAttributeValue("id"))) {
                    result.add(tag);
                }
            }
        }
        return result;
    }

    public Map<String, PsiClass> aliases() {
        Map<String, PsiClass> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        new TypeAliasRegistry().getTypeAliases().forEach((name, cls) -> {
            PsiClass c = findClass(cls.getName());
            if (c != null) {
                result.put(name, c);
            }
        });
        for (XmlTag configuration : roots("configuration")) {
            XmlTag aliases = configuration.findFirstSubTag("typeAliases");
            if (aliases == null) {
                continue;
            }
            for (XmlTag a : aliases.getSubTags()) {
                if ("typeAlias".equals(a.getName())) {
                    PsiClass c = findClass(a.getAttributeValue("type"));
                    if (c != null) {
                        result.put(Optional.ofNullable(a.getAttributeValue("alias")).orElse(c.getName()), c);
                    }
                } else if ("package".equals(a.getName())) {
                    PsiPackage pkg = JavaPsiFacade.getInstance(project).findPackage(Objects.toString(a.getAttributeValue("name"), ""));
                    if (pkg != null) {
                        collectPackageAliases(pkg, result);
                    }
                }
            }
        }
        PsiClass alias = findClass(Alias.class.getName());
        if (alias != null) {
            AnnotatedElementsSearch.searchPsiClasses(alias, scope).forEach(cls -> {
                String value = annotationString(cls.getAnnotation(Alias.class.getName()), "value");
                if (value != null) {
                    result.put(value, cls);
                }
                return true;
            });
        }
        return result;
    }

    private void collectPackageAliases(PsiPackage pkg, Map<String, PsiClass> result) {
        ProgressManager.checkCanceled();
        for (PsiClass c : pkg.getClasses(scope)) {
            if (!c.isInterface()) {
                String alias = annotationString(c.getAnnotation(Alias.class.getName()), "value");
                result.put(alias == null ? c.getName() : alias, c);
            }
        }
        for (PsiPackage child : pkg.getSubPackages(scope)) {
            collectPackageAliases(child, result);
        }
    }

    public PsiClass resolveType(String name) {
        PsiClass direct = findClass(name);
        return direct != null || name == null ? direct : aliases().get(name);
    }

    public Object[] classVariants(boolean interfacesOnly) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!interfacesOnly) {
            aliases().forEach((name, cls) -> result.put(name, LookupElementBuilder.create(cls, name)));
        }
        AllClassesSearch.search(scope.intersectWith(GlobalSearchScope.projectScope(project)), project).forEach(cls -> {
            ProgressManager.checkCanceled();
            if (cls.getQualifiedName() != null && (!interfacesOnly || cls.isInterface())) {
                String name = PsiModel.binaryName(cls);
                result.putIfAbsent(name, LookupElementBuilder.create(cls, name));
            }
            return true;
        });
        return result.values().toArray();
    }

    public static String annotationString(PsiAnnotation a, String member) {
        if (a == null) {
            return null;
        }
        PsiAnnotationMemberValue expression = a.findAttributeValue(member);
        if (expression == null) {
            return null;
        }
        Object v = JavaPsiFacade.getInstance(a.getProject()).getConstantEvaluationHelper().computeConstantExpression(expression);
        return v instanceof String s ? s : null;
    }

    public static Map<String, PsiElement> properties(PsiType type) {
        if (type == null) {
            return Map.of();
        }
        PsiClass cls = PsiUtil.resolveClassInType(type);
        if (cls == null) {
            return Map.of();
        }
        Map<String, PsiElement> result = new TreeMap<>();
        PsiModel model = new PsiModel(cls.getProject());
        for (JavaBeanProperty p : model.type(type).getBeanProperties()) {
            if (!"class".equals(p.getName())) {
                result.put(p.getName(), PsiModel.source(p.getReadMethod()));
            }
        }
        for (PsiField f : cls.getAllFields()) {
            if (!f.hasModifierProperty(PsiModifier.STATIC)) {
                result.putIfAbsent(f.getName(), f);
            }
        }
        return result;
    }

    /** 导航优先定位 getter 所属类的对应字段，属性类型仍由原始声明推导。 */
    public static PsiElement propertyTarget(String name, PsiElement property) {
        if (property instanceof PsiMethod method) {
            PsiClass declaring = method.getContainingClass();
            PsiField field = declaring == null ? null : declaring.findFieldByName(name, true);
            if (field != null && !field.hasModifierProperty(PsiModifier.STATIC)) {
                return field;
            }
        }
        return property;
    }

    public static PsiType propertyType(PsiType owner, PsiElement property) {
        PsiClass cls = PsiUtil.resolveClassInType(owner);
        if (cls == null) {
            return null;
        }
        PsiClass declaring = property instanceof PsiField f ? f.getContainingClass() : property instanceof PsiMethod m ? m.getContainingClass() : null;
        PsiType raw = property instanceof PsiField f ? f.getType() : property instanceof PsiMethod m ? m.getReturnType() : null;
        if (declaring == null || raw == null || !(owner instanceof PsiClassType c)) {
            return raw;
        }
        return TypeConversionUtil.getSuperClassSubstitutor(declaring, cls, c.resolveGenerics().getSubstitutor()).substitute(raw);
    }

    public static PsiType classType(PsiClass cls) {
        return cls == null ? null : JavaPsiFacade.getElementFactory(cls.getProject()).createType(cls);
    }

    public static PsiType unwrap(PsiType type) {
        if (type instanceof PsiArrayType array) {
            return array.getComponentType();
        }
        if (type instanceof PsiClassType c) {
            PsiClass cls = c.resolve();
            if (cls != null) {
                PsiModel.Type javaType = new PsiModel(cls.getProject()).type(type);
                try {
                    var unwrapped = JavaTypes.unwrap(javaType);
                    // XML 的 foreach 还允许非 Collection 的 Iterable，泛型解析仍交给核心。
                    if (unwrapped == javaType && javaType.isAssignableTo(Iterable.class)) {
                        unwrapped = JavaTypes.typeArgument(javaType, Iterable.class, 0);
                    }
                    return ((PsiModel.Type) unwrapped).psiType();
                } catch (UnresolvedTypeException ignored) {
                    // 不完整的类型保留原样，由后续引用解析决定是否能够定位。
                }
            }
        }
        return type;
    }

    public static XmlTag statement(PsiElement element) {
        for (XmlTag tag = PsiTreeUtil.getParentOfType(element, XmlTag.class); tag != null; tag = tag.getParentTag()) {
            if (STATEMENTS.contains(tag.getName())) {
                return tag;
            }
        }
        return null;
    }
}
