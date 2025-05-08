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
 * 学生课程明细表
 */
@Table(alias = "sc")
@JoinParent(alias = "s", joinColumn = @JoinColumn(leftColumn = "studentId", rightColumn = "id"))
public class StudentCourse extends Student {

    /** 主键-学生id */
    @Id
    @Column
    private Integer studentId;
    /** 主键-课程id */
    @Id
    @Column
    private Integer courseId;
    /** 选课时间 */
    @Column
    private Integer selectDate;

    // ========回显字段========
    /** 学生 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "studentId", rightColumn = "id"))
    private Student student;
    /** 课程 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "courseId", rightColumn = "id"))
    private Course course;
    /** 学生考试 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "courseId", rightColumn = "courseId"), table = CourseExam.class)
    @JoinRelation(joinColumn = {
            @JoinColumn(leftTableAlias = "sc", leftColumn = "studentId", rightColumn = "studentId"),
            @JoinColumn(leftColumn = "id", rightColumn = "examId")
    })
    private List<StudentExam> studentExams;
    /** 学生作业 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "courseId", rightColumn = "courseId"), table = CourseHomework.class, tableAlias = "ch")
    @JoinRelation(joinColumn = {
            @JoinColumn(leftTableAlias = "sc", leftColumn = "studentId", rightColumn = "studentId"),
            @JoinColumn(leftTableAlias = "ch", leftColumn = "id", rightColumn = "homeworkId")
    })
    private List<StudentHomework> studentHomeworks;

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public Integer getSelectDate() {
        return selectDate;
    }

    public void setSelectDate(Integer selectDate) {
        this.selectDate = selectDate;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
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
