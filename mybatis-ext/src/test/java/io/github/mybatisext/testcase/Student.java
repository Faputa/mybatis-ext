package io.github.mybatisext.testcase;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinParent;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.LoadStrategy;
import io.github.mybatisext.annotation.Table;

import java.util.List;

/**
 * 学生表
 */
@Table
@JoinParent(joinColumn = @JoinColumn(leftColumn = "classId", rightColumn = "id"))
public class Student extends ClassInfo {

    /** 主键 */
    @Id
    @Column
    private Integer id;
    /** 外键-班级id */
    @Column
    private Integer classId;
    /** 姓名 */
    @Column
    private String name;
    /** 性别 */
    @Column
    private String gender;
    /** 年龄 */
    @Column
    private Integer age;

    // ========回显字段========
    /** 学生选课 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "studentId"))
    private List<StudentCourse> studentCourses;
    /** 学生考试 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "studentId"))
    private List<StudentExam> studentExams;
    /** 学生作业 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "studentId"))
    private List<StudentHomework> studentHomeworks;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public List<StudentCourse> getStudentCourses() {
        return studentCourses;
    }

    public void setStudentCourses(List<StudentCourse> studentCourses) {
        this.studentCourses = studentCourses;
    }

    public List<StudentExam> getStudentExams() {
        return studentExams;
    }

    public void setStudentExams(List<StudentExam> studentExams) {
        this.studentExams = studentExams;
    }

    public List<StudentHomework> getStudentHomeworks() {
        return studentHomeworks;
    }

    public void setStudentHomeworks(List<StudentHomework> studentHomeworks) {
        this.studentHomeworks = studentHomeworks;
    }
}
