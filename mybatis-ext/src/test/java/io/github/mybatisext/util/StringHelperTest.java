package io.github.mybatisext.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringHelperTest {
    
    @Test
    public void test(){
        assertEquals("my_variable_name", StringHelper.camelToUnderscore("myVariableName"));
        assertEquals("my_url_string", StringHelper.camelToUnderscore("myURLString"));
        assertEquals("my_url_string", StringHelper.camelToUnderscore("myUrlSTRING"));
        assertEquals("my_url_string", StringHelper.camelToUnderscore("MYUrlString"));
        assertEquals("myVariableName", StringHelper.underscoreToLowerCamel("my_variable_name"));
        assertEquals("myVariableName", StringHelper.underscoreToLowerCamel("MY_VARIABLE_NAME"));
        assertEquals("MyVariableName", StringHelper.underscoreToUpperCamel("my_variable_name"));
        assertEquals("MyVariableName", StringHelper.underscoreToUpperCamel("MY_VARIABLE_NAME"));
    }
}
