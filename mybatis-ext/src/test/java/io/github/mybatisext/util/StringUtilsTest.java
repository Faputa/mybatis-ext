package io.github.mybatisext.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringUtilsTest {

    @Test
    public void test() {
        assertEquals("my_variable_name", StringUtils.camelToSnake("myVariableName"));
        assertEquals("my_url_string", StringUtils.camelToSnake("myURLString"));
        assertEquals("my_url_string", StringUtils.camelToSnake("myUrlSTRING"));
        assertEquals("my_url_string", StringUtils.camelToSnake("MYUrlString"));
        assertEquals("myVariableName", StringUtils.snakeToLowerCamel("my_variable_name"));
        assertEquals("myVariableName", StringUtils.snakeToLowerCamel("MY_VARIABLE_NAME"));
        assertEquals("MyVariableName", StringUtils.snakeToUpperCamel("my_variable_name"));
        assertEquals("MyVariableName", StringUtils.snakeToUpperCamel("MY_VARIABLE_NAME"));
    }
}
