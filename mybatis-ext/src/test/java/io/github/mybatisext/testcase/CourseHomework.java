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
 * 课程作业表
 */
@Table
@JoinParent(joinColumn = @JoinColumn(leftColumn = "courseId", rightColumn = "id"))
public class CourseHomework extends Course {

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
    /** 截止时间 */
    @Column
    private String deadline;

    // ========回显字段========
    /** 学生作业 */
    @LoadStrategy
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "id", rightColumn = "homeworkId"))
    private List<StudentHomework> studentHomeworks;

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

    public String getDeadline() {
        return deadline;
    }

    public void setDeadline(String deadline) {
        this.deadline = deadline;
    }

    @Override
    public List<StudentHomework> getStudentHomeworks() {
        return studentHomeworks;
    }

    @Override
    public void setStudentHomeworks(List<StudentHomework> studentHomeworks) {
        this.studentHomeworks = studentHomeworks;
    }
}
