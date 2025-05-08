package io.github.mybatisext.testcase;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.Table;

/**
 * 学生作业明细表
 */
@Table
public class StudentHomework {

    /** 主键-学生id */
    @Id
    @Column
    private Integer studentId;
    /** 主键-作业id */
    @Id
    @Column
    private Integer homeworkId;
    /** 评价等级 */
    @Column
    private String evaluation_grade;
    /** 评语 */
    @Column
    private String evaluation_comment;

    // ========回显字段========
    /** 学生 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "studentId", rightColumn = "id"))
    private Student student;
    /** 作业 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "homeworkId", rightColumn = "id"))
    private CourseHomework courseExam;
    /** 课程 */
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "homeworkId", rightColumn = "id"), table = CourseHomework.class)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "courseId", rightColumn = "id"))
    private Course course;

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public Integer getHomeworkId() {
        return homeworkId;
    }

    public void setHomeworkId(Integer homeworkId) {
        this.homeworkId = homeworkId;
    }

    public String getEvaluation_grade() {
        return evaluation_grade;
    }

    public void setEvaluation_grade(String evaluation_grade) {
        this.evaluation_grade = evaluation_grade;
    }

    public String getEvaluation_comment() {
        return evaluation_comment;
    }

    public void setEvaluation_comment(String evaluation_comment) {
        this.evaluation_comment = evaluation_comment;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public CourseHomework getCourseExam() {
        return courseExam;
    }

    public void setCourseExam(CourseHomework courseExam) {
        this.courseExam = courseExam;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
