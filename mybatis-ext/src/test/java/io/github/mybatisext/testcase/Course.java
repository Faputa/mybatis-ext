package io.github.mybatisext.testcase;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.LoadStrategy;
import io.github.mybatisext.annotation.Table;

import java.util.List;

/**
 * 课程表
 */
@Table
public class Course {

    /** 主键 */
    @Id
    @Column
    private Integer id;
    /** 名称 */
    @Column
    private String name;
    /** 学分 */
    @Column
    private Integer credit;

    // ========回显字段========
    /** 学生 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "courseId"), table = StudentCourse.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "studentId", rightColumn = "id"))
    private List<Student> students;
    /** 学生考试 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "courseId"), table = CourseExam.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "examId"))
    private List<StudentExam> studentExams;
    /** 学生作业 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "courseId"), table = CourseHomework.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "homeworkId"))
    private List<StudentHomework> studentHomeworks;

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

    public Integer getCredit() {
        return credit;
    }

    public void setCredit(Integer credit) {
        this.credit = credit;
    }

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
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
