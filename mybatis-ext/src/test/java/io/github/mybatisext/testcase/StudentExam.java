package io.github.mybatisext.testcase;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;

/**
 * 学生考试明细表
 */
@Table
public class StudentExam {

    /** 主键-学生id */
    @Id
    @Column
    private Integer studentId;
    /** 主键-考试id */
    @Id
    @Column
    private Integer examId;
    /** 考试成绩 */
    @Column
    private Integer score;

    // ========回显字段========
    /** 学生 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "studentId", rightColumn = "id"))
    private Student student;
    /** 考试 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "examId", rightColumn = "id"))
    private CourseExam courseExam;
    /** 课程 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "examId", rightColumn = "id"), table = CourseExam.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "courseId", rightColumn = "id"))
    private Course course;

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getExamId() {
        return examId;
    }

    public void setExamId(Integer examId) {
        this.examId = examId;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public CourseExam getCourseExam() {
        return courseExam;
    }

    public void setCourseExam(CourseExam courseExam) {
        this.courseExam = courseExam;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
