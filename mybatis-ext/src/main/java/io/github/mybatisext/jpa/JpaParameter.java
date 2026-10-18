package io.github.mybatisext.jpa;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import io.github.mybatisext.metadata.TableInfoFactory;
import io.github.mybatisext.model.JavaAnnotation;
import io.github.mybatisext.model.JavaParameter;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.JavaTypes;

/** 方法参数的静态绑定信息；不保存某个语法分支的参数消费状态。 */
final class JpaParameter {

    private final JavaParameter declaration;
    private final String name;
    private final boolean named;
    private final boolean single;
    private final boolean collection;

    JpaParameter(JavaParameter declaration, int index, int count) {
        this.declaration = declaration;
        JavaAnnotation param = declaration.annotation(Param.class);
        this.named = param != null;
        this.single = count == 1;
        JavaType type = getType();
        this.collection = type.isArray() || type.isAssignableTo(Collection.class);
        if (named) {
            this.name = param.value("value");
        } else if (!single) {
            this.name = "param" + (index + 1);
        } else if (type.isArray()) {
            this.name = "array";
        } else if (type.isAssignableTo(List.class)) {
            this.name = "list";
        } else {
            this.name = collection ? "collection" : "param1";
        }
    }

    JavaParameter getDeclaration() {
        return declaration;
    }

    String getName() {
        return name;
    }

    boolean isNamed() {
        return named;
    }

    boolean isCollection() {
        return collection;
    }

    JavaType getType() {
        return declaration.getGenericType();
    }

    JavaType getElementType() {
        return collection ? JavaTypes.unwrap(getType()) : getType();
    }

    boolean isTable(JavaType tableType) {
        return TableInfoFactory.isSameTableType(tableType, getElementType());
    }

    Variable getVariable() {
        return new Variable("", name, getType(), declaration);
    }

    /** 单个未命名实体直接访问属性，批量实体则访问 foreach 中的元素。 */
    Variable getValueVariable() {
        if (collection) {
            return getVariable().getItemVariable();
        }
        return new Variable("", single && !named ? "" : name, getType(), declaration);
    }
}
