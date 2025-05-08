package io.github.mybatisext.testcase;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.LoadStrategy;
import io.github.mybatisext.annotation.Table;

import java.util.List;

/**
 * 班级表
 */
@Table(name = "class")
public class ClassInfo {

    /** 主键 */
    @Id
    @Column
    private Integer id;
    /** 名称 */
    @Column
    private String name;
    /** 年级 */
    @Column
    private String grade;

    // ========回显字段========
    /** 学生 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "classId"))
    private List<Student> students;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }
}
