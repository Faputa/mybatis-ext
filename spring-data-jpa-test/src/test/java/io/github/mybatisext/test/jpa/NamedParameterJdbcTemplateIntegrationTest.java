package io.github.mybatisext.test.jpa;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@SpringBootTest
public class NamedParameterJdbcTemplateIntegrationTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @see org.springframework.jdbc.core.StatementCreatorUtils#setNull
     * @see com.mysql.cj.jdbc.ClientPreparedStatement#setNull
     */
    @Test
    public void returnsNoRowsForNonMatchingNamedParameters() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("a", 1);
        params.put("b", 103);
        List<Map<String, Object>> users = jdbcTemplate.queryForList("select * from sys_user where user_id=:a and dept_id=:b", params);
        assertTrue(users.isEmpty());
    }
}
