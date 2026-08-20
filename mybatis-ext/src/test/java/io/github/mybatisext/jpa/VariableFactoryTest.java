package io.github.mybatisext.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.reflect.GenericTypeFactory;

class VariableFactoryTest {

    private final Configuration configuration = new Configuration();

    @Test
    void addsFieldsGettersAndInheritedMembers() {
        Variable variable = new Variable("parameter", GenericTypeFactory.build(ChildBean.class));

        VariableFactory.addChildren(configuration, variable);

        assertEquals("parameter.parentField", variable.get("parentField").getFullName());
        assertEquals("parameter.childField", variable.get("childField").getFullName());
        assertEquals("parameter.derivedValue", variable.get("derivedValue").getFullName());
    }

    @Test
    void skipsScalarContainerAndArrayTypes() {
        assertFalse(VariableFactory.hasSubVariable(configuration, String.class));
        assertFalse(VariableFactory.hasSubVariable(configuration, Map.class));
        assertFalse(VariableFactory.hasSubVariable(configuration, Collection.class));
        assertFalse(VariableFactory.hasSubVariable(configuration, Object[].class));
        assertTrue(VariableFactory.hasSubVariable(configuration, ChildBean.class));
    }

    @Test
    void doesNotRebuildAnAlreadyInitializedVariable() {
        Variable variable = new Variable("parameter", GenericTypeFactory.build(ChildBean.class));
        Variable existing = new Variable("parameter", "existing", GenericTypeFactory.build(String.class));
        variable.getNameToVariable().put("existing", existing);

        VariableFactory.addChildren(configuration, variable);

        assertEquals(1, variable.getNameToVariable().size());
        assertSame(existing, variable.get("existing"));
    }

    @Test
    void buildsJdbcAwarePlaceholdersAndCollectionItems() throws Exception {
        Field names = GenericHolder.class.getDeclaredField("names");
        Variable variable = new Variable("request", "names", GenericTypeFactory.build(names.getGenericType()));
        variable.setJdbcType(JdbcType.VARCHAR);

        Variable item = variable.getItemVariable();

        assertEquals("#{request.names, jdbcType=VARCHAR}", variable.getPlaceholder());
        assertEquals("__names__bind", variable.getBindName());
        assertEquals("#{__names__bind, jdbcType=VARCHAR}", variable.getBindPlaceholder());
        assertEquals("__names__item", item.getName());
        assertEquals(String.class, item.getJavaType().getType());
        assertEquals("#{__names__item, jdbcType=VARCHAR}", item.getPlaceholder());
    }

    static class ParentBean {
        private String parentField;
    }

    static class ChildBean extends ParentBean {
        private Long childField;

        public String getDerivedValue() {
            return "value";
        }
    }

    static class GenericHolder {
        private List<String> names;
    }
}
