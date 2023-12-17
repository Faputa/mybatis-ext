package io.github.mybatisext.util;

import javax.annotation.Nullable;

public class StringUtils {

    public static boolean isBlank(@Nullable String str) {
        return str == null || str.trim().length() == 0;
    }

    public static boolean isNotBlank(@Nullable String str) {
        return !isBlank(str);
    }

    public static String capitalize(String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public static String snakeToLowerCamel(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                continue;
            }
            if (i > 0 && str.charAt(i - 1) == '_' && sb.length() > 0) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static String snakeToUpperCamel(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '_') {
                continue;
            }
            if ((i > 0 && str.charAt(i - 1) == '_') || sb.length() == 0) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    public static String camelToSnake(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && (Character.isLowerCase(str.charAt(i - 1))
                        || (i < str.length() - 1 && Character.isLowerCase(str.charAt(i + 1))))) {
                    sb.append("_");
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
