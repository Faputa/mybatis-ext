package io.github.mybatisext.dialect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.github.mybatisext.jpa.Limit;

class DialectSyntaxTest {

    @Test
    void escapesDialectSpecificIdentifierDelimiters() {
        assertEquals("`a``b`", new MySqlDialect().quote("a`b"));
        assertEquals("\"a\"\"b\"", new PostgreSqlDialect().quote("a\"b"));
        assertEquals("\"a\"\"b\"", new OracleDialect().quote("a\"b"));
        assertEquals("\"a\"\"b\"", new DmDialect().quote("a\"b"));
        assertEquals("\"a\"\"b\"", new H2Dialect().quote("a\"b"));
        assertEquals("[a]]b]", new SqlServerDialect().quote("a]b"));
        assertEquals("[a]]b]", new SqlServer2008Dialect().quote("a]b"));
    }

    @Test
    void buildsDialectSpecificLimitClauses() {
        Limit limit = limit(2, 3);

        assertEquals("SELECT id FROM test LIMIT 2, 3", new MySqlDialect().buildLimit(limit, "SELECT id FROM test"));
        assertEquals("SELECT id FROM test LIMIT 2, 3", new H2Dialect().buildLimit(limit, "SELECT id FROM test"));
        assertEquals("SELECT id FROM test LIMIT 3 OFFSET 2", new PostgreSqlDialect().buildLimit(limit, "SELECT id FROM test"));
        assertEquals("SELECT id FROM test OFFSET 2 ROWS FETCH NEXT 3 ROWS ONLY", new SqlServerDialect().buildLimit(limit, "SELECT id FROM test"));

        String oracle = new OracleDialect().buildLimit(limit, "SELECT id FROM test");
        assertTrue(oracle.contains("<bind name=\"__endRow\" value=\"2 + 3\"/> #{__endRow}"));
        assertTrue(oracle.endsWith("WHERE PAGEHELPER_ROW_ID &gt; 2"));
        assertEquals(oracle, new DmDialect().buildLimit(limit, "SELECT id FROM test"));
    }

    @Test
    void buildsExistsAndBooleanExpressions() {
        assertEquals("SELECT EXISTS (SELECT 1)", new MySqlDialect().buildExists("SELECT 1"));
        assertEquals("SELECT EXISTS (SELECT 1)", new PostgreSqlDialect().buildExists("SELECT 1"));
        assertEquals("SELECT EXISTS (SELECT 1)", new H2Dialect().buildExists("SELECT 1"));
        assertEquals("SELECT CASE WHEN EXISTS (SELECT 1) THEN 1 ELSE 0 END FROM DUAL", new OracleDialect().buildExists("SELECT 1"));
        assertEquals("SELECT CASE WHEN EXISTS (SELECT 1) THEN 1 ELSE 0 END FROM DUAL", new DmDialect().buildExists("SELECT 1"));
        assertEquals("SELECT CASE WHEN EXISTS (SELECT 1) THEN 1 ELSE 0 END", new SqlServerDialect().buildExists("SELECT 1"));

        assertEquals("IS TRUE", new MySqlDialect().isTrue());
        assertEquals("IS NOT TRUE", new PostgreSqlDialect().isFalse());
        assertEquals("!= 0", new OracleDialect().isTrue());
        assertEquals("= 0", new DmDialect().isFalse());
        assertEquals("= 1", new SqlServerDialect().isTrue());
        assertEquals("= 0", new SqlServer2008Dialect().isFalse());
    }

    private Limit limit(int offset, int rowCount) {
        Limit limit = new Limit();
        limit.setOffset(offset);
        limit.setRowCount(rowCount);
        return limit;
    }
}
