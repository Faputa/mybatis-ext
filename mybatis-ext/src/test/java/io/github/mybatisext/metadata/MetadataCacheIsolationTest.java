package io.github.mybatisext.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.ObjectTypeHandler;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.adapter.ExtContext;
import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.reflect.GenericType;
import io.github.mybatisext.reflect.GenericTypeFactory;

class MetadataCacheIsolationTest {

    @Test
    void isolatesTypeHandlersAndJoinGraphsWithSharedExtContext() {
        ExtContext context = new ExtContext();
        Configuration embeddedConfiguration = new Configuration();
        Configuration scalarConfiguration = new Configuration();
        scalarConfiguration.getTypeHandlerRegistry().register(Value.class, ObjectTypeHandler.class);
        TableInfoFactory embeddedFactory = new TableInfoFactory(embeddedConfiguration, context);
        TableInfoFactory scalarFactory = new TableInfoFactory(scalarConfiguration, context);
        GenericType type = GenericTypeFactory.build(Record.class);

        TableInfo embedded = embeddedFactory.buildTableInfo(type);
        TableInfo scalar = scalarFactory.buildTableInfo(type);
        assertSame(embedded, embeddedFactory.buildTableInfo(type));
        assertSame(scalar, scalarFactory.buildTableInfo(type));
        assertNotSame(embedded.getJoinGraph(), scalar.getJoinGraph());
        assertNotSame(embedded.getJoinTableInfo().getTableDef(), scalar.getJoinTableInfo().getTableDef());
        assertNull(embedded.getNameToPropertyInfo().get("value").getColumnName());
        assertEquals("code", embedded.getNameToPropertyInfo().get("value").getNameToPropertyInfo().get("code").getColumnName());
        assertEquals("value", scalar.getNameToPropertyInfo().get("value").getColumnName());
    }

    @Table
    static class Record {
        @Column
        private Value value;
    }

    static class Value {
        @Column
        private String code;
    }
}
