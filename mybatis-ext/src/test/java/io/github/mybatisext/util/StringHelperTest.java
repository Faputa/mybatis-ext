package io.github.mybatisext.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringHelperTest {
    
    @Test
    public void test(){
        assertEquals("my_variable_name", StringHelper.camelToSnake("myVariableName"));
        assertEquals("my_url_string", StringHelper.camelToSnake("myURLString"));
        assertEquals("my_url_string", StringHelper.camelToSnake("myUrlSTRING"));
        assertEquals("my_url_string", StringHelper.camelToSnake("MYUrlString"));
        assertEquals("myVariableName", StringHelper.snakeToLowerCamel("my_variable_name"));
        assertEquals("myVariableName", StringHelper.snakeToLowerCamel("MY_VARIABLE_NAME"));
        assertEquals("MyVariableName", StringHelper.snakeToUpperCamel("my_variable_name"));
        assertEquals("MyVariableName", StringHelper.snakeToUpperCamel("MY_VARIABLE_NAME"));
    }
}
