package io.github.mybatisext.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class SimpleStringTemplateTest {

    @Test
    public void rendersTemplateFromBean() {
        User user = new User();
        user.setName("John Doe");
        Address address = new Address();
        address.setCity("New York");
        address.setCountry("USA");
        user.setAddress(address);

        String template = "Hello {name}, you live in {address.city}, {address.country}.";
        String expected = "Hello John Doe, you live in New York, USA.";
        String result = SimpleStringTemplate.build(template, user);
        assertEquals(expected, result);
    }

    @Test
    public void rendersTemplateFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Jane Doe");
        map.put("age", 25);
        map.put("address", new HashMap<String, Object>() {
            {
                // 测试转义字符
                put("{ci.ty}", "Los Angeles");
                put("country", "USA");
            }
        });

        String template = "Hello {name}, you are {age} years old. You live in {address.\\{ci\\.ty\\}}, {address.country}.";
        String expected = "Hello Jane Doe, you are 25 years old. You live in Los Angeles, USA.";
        String result = SimpleStringTemplate.build(template, map);
        assertEquals(expected, result);
    }

    @Test
    public void rendersTemplateFromList() {
        List<String> list = Arrays.asList("Apple", "Banana", "Cherry");

        String template = "I have {0}, {1}, and {2}.";
        String expected = "I have Apple, Banana, and Cherry.";
        String result = SimpleStringTemplate.build(template, list);
        assertEquals(expected, result);
    }

    @Test
    public void rendersTemplateFromArray() {
        String[] array = {"Apple", "Banana", "Cherry"};

        String template = "I have {0}, {1}, and {2}.";
        String expected = "I have Apple, Banana, and Cherry.";
        String result = SimpleStringTemplate.build(template, array);
        assertEquals(expected, result);
    }

    @Test
    void rendersPrimitiveArraysAndGetterValues() {
        Getter<String> getter = key -> "name".equals(key) ? "Alice" : null;

        assertEquals("2", SimpleStringTemplate.build("{1}", new int[]{1, 2}));
        assertEquals("Hello Alice", SimpleStringTemplate.build("Hello {name}", getter));
    }

    @Test
    void supportsForcedBeanPropertiesForGetterImplementations() {
        GetterUser user = new GetterUser("bean name");

        assertEquals("getter name / bean name", SimpleStringTemplate.build("{name} / {##name}", user));
    }

    @Test
    void handlesMissingPathsAccordingToStrictMode() {
        Map<String, Object> values = new HashMap<>();
        values.put("items", Arrays.asList("first"));

        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> SimpleStringTemplate.build("{missing}", values));
        assertTrue(missing.getMessage().contains("param path not found: missing"));
        assertEquals("{missing}", SimpleStringTemplate.build("{missing}", values, false));
        assertEquals("{items.2}", SimpleStringTemplate.build("{items.2}", values, false));
        assertEquals("{items.-1}", SimpleStringTemplate.build("{items.-1}", values, false));
    }

    @Test
    void preservesIncompletePlaceholdersAndTrailingEscapes() {
        assertEquals("value {name", SimpleStringTemplate.build("value {name", new User()));
        assertEquals("value ", SimpleStringTemplate.build("value \\", new User()));
    }

    @Test
    void wrapsGetterInvocationFailures() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> SimpleStringTemplate.build("{broken}", new BrokenBean()));

        assertTrue(exception.getCause() instanceof java.lang.reflect.InvocationTargetException);
    }

    static class User {
        private String name;
        private Address address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    static class Address {
        private String city;
        private String country;

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCountry() {
            return country;
        }

        public void setCountry(String country) {
            this.country = country;
        }
    }

    static class GetterUser implements Getter<String> {
        private final String name;

        GetterUser(String name) {
            this.name = name;
        }

        @Override
        public String get(String key) {
            return "getter " + key;
        }

        public String getName() {
            return name;
        }
    }

    static class BrokenBean {
        public String getBroken() {
            throw new IllegalStateException("broken");
        }
    }
}
