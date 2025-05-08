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
 * 课程考试表
 */
@Table
@JoinParent(joinColumn = @JoinColumn(leftColumn = "courseId", rightColumn = "id"))
public class CourseExam extends Course {

    /** 主键 */
    @Id
    @Column
    private Integer id;
    /** 外键-课程id */
    @Column
    private Integer courseId;
    /** 名称 */
    @Column
    private String name;
    /** 开始时间 */
    @Column
    private String startTime;
    /** 结束时间 */
    @Column
    private String endTime;

    // ========回显字段========
    /** 学生考试 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "examId"))
    private List<StudentExam> studentExams;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getCourseId() {
        return courseId;
    }

    public void setCourseId(Integer courseId) {
        this.courseId = courseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public List<StudentExam> getStudentExams() {
        return studentExams;
    }

    public void setStudentExams(List<StudentExam> studentExams) {
        this.studentExams = studentExams;
    }
}
