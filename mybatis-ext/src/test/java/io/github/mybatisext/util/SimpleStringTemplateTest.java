package io.github.mybatisext.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class SimpleStringTemplateTest {

    @Test
    public void testWithUserObject() {
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
    public void testWithMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Jane Doe");
        map.put("age", 25);
        map.put("address", new HashMap<String, Object>() {
            {
                put("city", "Los Angeles");
                put("country", "USA");
            }
        });

        String template = "Hello {name}, you are {age} years old. You live in {address.city}, {address.country}.";
        String expected = "Hello Jane Doe, you are 25 years old. You live in Los Angeles, USA.";
        String result = SimpleStringTemplate.build(template, map);
        assertEquals(expected, result);
    }

    @Test
    public void testWithList() {
        List<String> list = Arrays.asList("Apple", "Banana", "Cherry");

        String template = "I have {0}, {1}, and {2}.";
        String expected = "I have Apple, Banana, and Cherry.";
        String result = SimpleStringTemplate.build(template, list);
        assertEquals(expected, result);
    }

    @Test
    public void testWithArray() {
        String[] array = { "Apple", "Banana", "Cherry" };

        String template = "I have {0}, {1}, and {2}.";
        String expected = "I have Apple, Banana, and Cherry.";
        String result = SimpleStringTemplate.build(template, array);
        assertEquals(expected, result);
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
}