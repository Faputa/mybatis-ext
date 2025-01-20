package io.github.mybatisext.test.jpa;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

@SpringBootTest
public class SpringJdbcTest {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * @see org.springframework.jdbc.core.StatementCreatorUtils#setNull
     * @see com.mysql.cj.jdbc.ClientPreparedStatement#setNull
     */
    @Test
    public void test() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("a", 1);
        params.put("b", 103);
        jdbcTemplate.queryForList("select * from sys_user where user_id=:a and dept_id=:b", params);
    }
}
