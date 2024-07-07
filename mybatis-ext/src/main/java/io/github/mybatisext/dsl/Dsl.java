package io.github.mybatisext.dsl;

public class Dsl {

    public static <T> SelectAll<T> select(Class<T> tableClass) {
        return new SelectAll<>();
    }

    public static Select select(String ...column) {
        return new Select();
    }
}
