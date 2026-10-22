package io.github.mybatisext.idea.fixtures;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.MapTable;
import io.github.mybatisext.annotation.OnlyById;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.annotation.TableRef;

/** 契约测试将同一份源码编译后用于反射，并以 PSI 形式加载。 */
public class ContractFixture {

    @Table
    public static class Binary {

        @Column
        public byte[] payload;
        @Column
        public Byte[] boxedPayload;
        @Column
        public InputStream stream;
        @Column
        public ByteArrayInputStream bufferedStream;
        @Column
        public Object value;
    }

    public static class Parent<T> {

        @Id
        @Column
        public T id;
    }

    @Table(name = "contract_user", alias = "u")
    public static class User extends Parent<Long> {

        @Column(name = "display_name")
        public String name;
        @Column
        public Address address;
    }

    public static class Address {

        @Column
        public String city;
    }

    public static class GenericSetter<T> {

        public void setGeneric(T value) {
        }
    }

    @Table
    public static class BeanProperties extends GenericSetter<String> {

        @Column
        public String getMismatch() {
            return null;
        }

        public void setMismatch(int value) {
        }

        @Column
        public String getFluent() {
            return null;
        }

        public BeanProperties setFluent(String value) {
            return this;
        }

        @Column
        public Object getNarrow() {
            return null;
        }

        public void setNarrow(String value) {
        }

        @Column
        public String getWide() {
            return null;
        }

        public void setWide(Object value) {
        }

        @Column
        public Integer getBoxed() {
            return null;
        }

        public void setBoxed(int value) {
        }

        @Column
        public String getGeneric() {
            return null;
        }

        @Column
        public String getOverloaded() {
            return null;
        }

        public void setOverloaded(int value) {
        }

        public void setOverloaded(String value) {
        }
    }

    public static class Users extends ArrayList<User> {
    }

    @Table
    public static class CollectionJoined {

        @Column
        public Long userId;
        @JoinRelation(joinColumn = @JoinColumn(leftColumn = "userId", rightColumn = "id"))
        public Users users;
    }

    public interface ParameterMapper {

        int scalar(int id);

        int namedScalar(@Param("id") int id);

        int binary(byte[] bytes);

        int object(User user);

        int aliases(@Param("param2") User first, @Param("other") int second);
    }

    @TableRef(User.class)
    public static class View {

        @ColumnRef("address.city")
        public String city;
        @ColumnRef("name")
        public String label;

        @ColumnRef
        public Long getId() {
            return null;
        }
    }

    @TableRef(View.class)
    public static class ChainedView {

        @ColumnRef("city")
        public String town;
    }

    @TableRef(User.class)
    public static class BrokenView {

        @ColumnRef("address.missing")
        public String broken;
    }

    @Table
    public static class Joined {

        @Column
        public Long userId;
        @JoinRelation(table = User.class, column = "id", joinColumn = @JoinColumn(leftColumn = "userId", rightColumn = "id"))
        @JoinRelation(table = User.class, column = "name", joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "id"))
        public String label;
        @JoinRelation(joinColumn = @JoinColumn(leftColumn = "userId", rightColumn = "id"))
        public List<User> users;
    }

    @MapTable(User.class)
    public interface Mapper {

        List<User> findByName(@Param("name") String name);

        List<User> findByNameIn(@Param("name") List<String> name);

        int updateIgnoreNull(@OnlyById @Param("user") User user);

        int deleteById(@Param("id") Long id);

        User findByMissing();
    }
}
