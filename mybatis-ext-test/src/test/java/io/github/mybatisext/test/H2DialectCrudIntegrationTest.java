package io.github.mybatisext.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import io.github.mybatisext.dialect.H2Dialect;

class H2DialectCrudIntegrationTest extends AbstractDialectCrudIntegrationTest {

    @Override
    protected DialectDataSource createDataSource() throws Exception {
        return newDataSource(new H2Dialect(), "");
    }

    @Test
    void updatesAndDeletesByRelationColumn() throws Exception {
        try (SqlSession session = createSqlSessionFactory().openSession()) {
            SysUserMapper mapper = session.getMapper(SysUserMapper.class);

            SysUser movedUser = new SysUser();
            movedUser.setUserId(2L);
            movedUser.setDeptId(108L);
            assertEquals(1, mapper.updateIgnoreNull(movedUser));

            SysUser update = new SysUser();
            update.setStatus("1");
            update.setParentId(101L);
            assertEquals(1, mapper.updateStatusByParentId(update));
            assertEquals("1", mapper.getByLoginName("admin").getStatus());
            assertEquals("0", mapper.getByLoginName("ry").getStatus());

            assertEquals(1, mapper.deleteByParentId(101L));
            assertNull(mapper.getByLoginName("admin"));
            assertNotNull(mapper.getByLoginName("ry"));
        }
    }

}
