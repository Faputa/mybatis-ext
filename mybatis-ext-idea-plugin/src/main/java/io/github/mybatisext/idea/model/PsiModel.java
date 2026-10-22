package io.github.mybatisext.idea.model;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassObjectAccessExpression;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.util.TypeConversionUtil;

import io.github.mybatisext.model.JavaAnnotatedElement;
import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaBeanProperty;
import io.github.mybatisext.model.JavaField;
import io.github.mybatisext.model.JavaMethod;
import io.github.mybatisext.model.JavaParameter;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.UnresolvedTypeException;

/** 单次请求内使用的 PSI 适配器，不加载用户项目中的类。 */
public final class PsiModel {

    private final Project project;
    private final Map<PsiType, Type> types = new HashMap<>();

    public PsiModel(Project project) {
        this.project = project;
    }

    public Type type(PsiType type) {
        return type == null ? null : types.computeIfAbsent(type, Type::new);
    }

    public Type type(PsiClass type) {
        return type(JavaPsiFacade.getElementFactory(project).createType(type));
    }

    public Method method(PsiMethod method, Type context) {
        PsiClass owner = method.getContainingClass();
        PsiSubstitutor substitutor = context.substitutorFor(owner);
        return new Method(method, substitutor, owner == context.psiClass()
                ? context
                : type(JavaPsiFacade.getElementFactory(project).createType(owner, substitutor)));
    }

    public static PsiElement source(Object element) {
        return element instanceof Source s ? s.element() : null;
    }

    public interface Source {

        PsiElement element();
    }

    public abstract class Annotated implements JavaAnnotatedElement, Source {

        @Override
        public JavaAnnotation annotation(Class<? extends Annotation> cls) {
            JavaAnnotation[] all = annotations(cls);
            return all.length == 0 ? null : all[0];
        }

        @Override
        public JavaAnnotation[] annotations(Class<? extends Annotation> cls) {
            if (!(element() instanceof PsiModifierListOwner owner)) {
                return new JavaAnnotation[0];
            }
            List<JavaAnnotation> result = new ArrayList<>();
            Repeatable repeatable = cls.getAnnotation(Repeatable.class);
            for (PsiAnnotation a : owner.getAnnotations()) {
                if (cls.getName().equals(a.getQualifiedName())) {
                    result.add(new AnnotationValue(a, cls));
                } else if (repeatable != null && repeatable.value().getName().equals(a.getQualifiedName())) {
                    PsiAnnotationMemberValue v = a.findAttributeValue("value");
                    PsiAnnotationMemberValue[] values = v instanceof PsiArrayInitializerMemberValue arr ? arr.getInitializers() : new PsiAnnotationMemberValue[]{v};
                    for (PsiAnnotationMemberValue item : values) {
                        if (item instanceof PsiAnnotation nested) {
                            result.add(new AnnotationValue(nested, cls));
                        }
                    }
                }
            }
            if (result.isEmpty() && cls.isAnnotationPresent(Inherited.class) && this instanceof Type t) {
                Type parent = t.getGenericSuperclass();
                if (parent != null) {
                    return parent.annotations(cls);
                }
            }
            return result.toArray(JavaAnnotation[]::new);
        }
    }

    public final class Type extends Annotated implements JavaType {

        private final PsiType psi;

        private Type(PsiType psi) {
            this.psi = psi;
        }

        public PsiType psiType() {
            return psi;
        }

        public PsiClass psiClass() {
            return psi instanceof PsiClassType c ? c.resolve() : null;
        }

        @Override
        public PsiElement element() {
            return psiClass();
        }

        PsiSubstitutor substitutorFor(PsiClass owner) {
            if (!(psi instanceof PsiClassType c) || owner == null || c.resolve() == null) {
                return PsiSubstitutor.EMPTY;
            }
            return TypeConversionUtil.getSuperClassSubstitutor(owner, c.resolve(), c.resolveGenerics().getSubstitutor());
        }

        @Override
        public String getName() {
            if (psi instanceof PsiArrayType a) {
                return type(a.getComponentType()).getName() + "[]";
            }
            PsiClass cls = psiClass();
            return cls == null ? psi.getCanonicalText() : binaryName(cls);
        }

        @Override
        public String getSimpleName() {
            PsiClass c = psiClass();
            return c == null ? psi.getPresentableText() : c.getName();
        }

        @Override
        public String getTypeName() {
            return psi.getCanonicalText();
        }

        @Override
        public boolean isResolved() {
            if (psi instanceof PsiPrimitiveType) {
                return !PsiTypes.nullType().equals(psi);
            }
            if (psi instanceof PsiArrayType a) {
                return type(a.getComponentType()).isResolved();
            }
            return psiClass() != null && !(psiClass() instanceof PsiTypeParameter);
        }

        @Override
        public Type[] getTypeParameters() {
            if (!(psi instanceof PsiClassType c) || c.resolve() == null) {
                return new Type[0];
            }
            PsiSubstitutor sub = c.resolveGenerics().getSubstitutor();
            return Arrays.stream(c.resolve().getTypeParameters()).map(v -> {
                PsiType actual = sub.substitute(v);
                return type(actual != null ? actual : JavaPsiFacade.getElementFactory(project).createType(v));
            }).toArray(Type[]::new);
        }

        @Override
        public Type getComponentType() {
            return psi instanceof PsiArrayType a ? type(a.getComponentType()) : null;
        }

        @Override
        public boolean isInterface() {
            return psiClass() != null && psiClass().isInterface();
        }

        @Override
        public JavaField[] getDeclaredFields() {
            PsiClass c = psiClass();
            if (c == null) {
                return new JavaField[0];
            }
            PsiSubstitutor sub = substitutorFor(c);
            return Arrays.stream(c.getFields()).map(f -> new Field(f, sub)).toArray(JavaField[]::new);
        }

        @Override
        public JavaMethod[] getMethods() {
            PsiClass c = psiClass();
            if (c == null) {
                return new JavaMethod[0];
            }
            return Arrays.stream(c.getAllMethods()).filter(m -> m.hasModifierProperty(PsiModifier.PUBLIC) && !m.isConstructor())
                    .map(m -> method(m, this)).toArray(JavaMethod[]::new);
        }

        @Override
        public Type getGenericSuperclass() {
            if (psi instanceof PsiClassType c) {
                for (PsiType parent : c.getSuperTypes()) {
                    Type t = type(parent);
                    if (!t.isInterface()) {
                        return t;
                    }
                }
            }
            return null;
        }

        @Override
        public Type[] getGenericInterfaces() {
            if (!(psi instanceof PsiClassType c)) {
                return new Type[0];
            }
            return Arrays.stream(c.getSuperTypes()).map(PsiModel.this::type).filter(Type::isInterface).toArray(Type[]::new);
        }

        @Override
        public JavaBeanProperty[] getBeanProperties() {
            Map<String, Method> getters = new LinkedHashMap<>();
            Map<String, List<Method>> setters = new HashMap<>();
            for (JavaMethod method : getMethods()) {
                ProgressManager.checkCanceled();
                Method m = (Method) method;
                if (m.psi.hasModifierProperty(PsiModifier.STATIC)) {
                    continue;
                }
                String name = m.getName();
                if (name.startsWith("set") && name.length() > 3 && m.getParameters().length == 1 && PsiTypes.voidType().equals(m.getGenericReturnType().psiType())) {
                    setters.computeIfAbsent(Introspector.decapitalize(name.substring(3)), key -> new ArrayList<>()).add(m);
                }
                if (m.getParameters().length != 0) {
                    continue;
                }
                if (name.startsWith("is") && name.length() > 2 && "boolean".equals(m.getGenericReturnType().getName())) {
                    getters.put(Introspector.decapitalize(name.substring(2)), m);
                } else if (name.startsWith("get") && name.length() > 3 && !"void".equals(m.getGenericReturnType().getName())) {
                    getters.putIfAbsent(Introspector.decapitalize(name.substring(3)), m);
                }
            }
            return getters.entrySet().stream().map(e -> {
                PsiType readType = e.getValue().getGenericReturnType().psiType();
                boolean writable = setters.getOrDefault(e.getKey(), List.of()).stream().anyMatch(setter -> {
                    PsiType writeType = ((Type) setter.getParameters()[0].getGenericType()).psiType();
                    // JavaBeans 按类型继承关系匹配，不进行基本类型与包装类型之间的转换。
                    return readType.equals(writeType) || !(readType instanceof PsiPrimitiveType) && !(writeType instanceof PsiPrimitiveType)
                            && TypeConversionUtil.isAssignable(readType, writeType);
                });
                return new JavaBeanProperty(e.getKey(), e.getValue(), !writable);
            }).toArray(JavaBeanProperty[]::new);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof PsiModel.Type t && t.model() == PsiModel.this && psi.equals(t.psi);
        }

        private PsiModel model() {
            return PsiModel.this;
        }

        @Override
        public int hashCode() {
            return 31 * System.identityHashCode(PsiModel.this) + psi.hashCode();
        }
    }

    public final class Field extends Annotated implements JavaField {

        private final PsiField psi;
        private final PsiSubstitutor sub;

        Field(PsiField psi, PsiSubstitutor sub) {
            this.psi = psi;
            this.sub = sub;
        }

        @Override
        public PsiElement element() {
            return psi;
        }

        @Override
        public String getName() {
            return psi.getName();
        }

        @Override
        public Type getGenericType() {
            return type(sub.substitute(psi.getType()));
        }
    }

    public final class Method extends Annotated implements JavaMethod {

        private final PsiMethod psi;
        private final PsiSubstitutor sub;
        private final Type declaring;

        Method(PsiMethod psi, PsiSubstitutor sub, Type declaring) {
            this.psi = psi;
            this.sub = sub;
            this.declaring = declaring;
        }

        @Override
        public PsiElement element() {
            return psi;
        }

        @Override
        public String getName() {
            return psi.getName();
        }

        @Override
        public Type getDeclaringType() {
            return declaring;
        }

        @Override
        public Type getGenericReturnType() {
            return type(sub.substitute(psi.getReturnType()));
        }

        @Override
        public JavaParameter[] getParameters() {
            return Arrays.stream(psi.getParameterList().getParameters()).map(p -> new Parameter(p, sub)).toArray(JavaParameter[]::new);
        }

        @Override
        public boolean isBridge() {
            return false;
        }

        @Override
        public boolean isDefault() {
            return psi.hasModifierProperty(PsiModifier.DEFAULT);
        }
    }

    public final class Parameter extends Annotated implements JavaParameter {

        private final PsiParameter psi;
        private final PsiSubstitutor sub;

        Parameter(PsiParameter psi, PsiSubstitutor sub) {
            this.psi = psi;
            this.sub = sub;
        }

        @Override
        public PsiElement element() {
            return psi;
        }

        @Override
        public Type getGenericType() {
            return type(sub.substitute(psi.getType()));
        }
    }

    private final class AnnotationValue implements JavaAnnotation {

        private final PsiAnnotation psi;
        private final Class<? extends Annotation> cls;

        AnnotationValue(PsiAnnotation psi, Class<? extends Annotation> cls) {
            this.psi = psi;
            this.cls = cls;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T value(String name) {
            try {
                java.lang.reflect.Method member = cls.getMethod(name);
                PsiAnnotationMemberValue v = psi.findDeclaredAttributeValue(name);
                if (v == null && member.getDefaultValue() == null) {
                    throw new UnresolvedTypeException("annotation member " + cls.getName() + "." + name);
                }
                return (T) (v == null ? defaultValue(member.getDefaultValue()) : convert(v, member.getReturnType()));
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException("Unsupported framework annotation member: " + cls.getName() + "." + name, e);
            }
        }

        private Object defaultValue(Object v) {
            if (v instanceof Class<?> c) {
                return namedType(c.getName());
            }
            if (v instanceof Annotation a) {
                return new JavaAnnotation() {

                    @SuppressWarnings("unchecked")
                    public <T> T value(String name) {
                        try {
                            return (T) defaultValue(a.annotationType().getMethod(name).invoke(a));
                        } catch (ReflectiveOperationException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                };
            }
            if (v instanceof Annotation[] a) {
                return Arrays.stream(a).map(this::defaultValue).toArray(JavaAnnotation[]::new);
            }
            return v;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private Object convert(PsiAnnotationMemberValue v, Class<?> expected) {
            if (expected.isArray()) {
                PsiAnnotationMemberValue[] items = v instanceof PsiArrayInitializerMemberValue a ? a.getInitializers() : new PsiAnnotationMemberValue[]{v};
                Class<?> component = expected.getComponentType();
                Object result = Array.newInstance(component.isAnnotation() ? JavaAnnotation.class : component == Class.class ? JavaType.class : component, items.length);
                for (int i = 0; i < items.length; i++) {
                    Array.set(result, i, convert(items[i], component));
                }
                return result;
            }
            if (v instanceof PsiClassObjectAccessExpression c) {
                return type(c.getOperand().getType());
            }
            if (v instanceof PsiAnnotation a) {
                return new AnnotationValue(a, (Class<? extends Annotation>) expected);
            }
            if (expected.isEnum() && v instanceof PsiReferenceExpression r && r.resolve() instanceof PsiEnumConstant e) {
                return Enum.valueOf((Class) expected, e.getName());
            }
            Object result = JavaPsiFacade.getInstance(project).getConstantEvaluationHelper().computeConstantExpression(v);
            if (result == null) {
                throw new UnresolvedTypeException("annotation value " + v.getText());
            }
            return result;
        }
    }

    private Type namedType(String name) {
        return type(JavaPsiFacade.getElementFactory(project).createTypeFromText(name.replace('$', '.'), null));
    }

    public static String binaryName(PsiClass cls) {
        return cls.getContainingClass() == null ? cls.getQualifiedName() : binaryName(cls.getContainingClass()) + "$" + cls.getName();
    }
}
