package io.github.mybatisext.test;

import java.util.Objects;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.EmbedParent;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

/**
 * 岗位表 sys_post
 * 
 * @author ruoyi
 */
@Table
@EmbedParent
public class SysPost extends BaseEntity {

    /** 岗位序号 */
    @Id
    @Column
    private Long postId;

    /** 岗位编码 */
    @Column
    private String postCode;

    /** 岗位名称 */
    @Column
    private String postName;

    /** 岗位排序 */
    @Column
    private String postSort;

    /** 状态（0正常 1停用） */
    @Column
    private String status;

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getPostName() {
        return postName;
    }

    public void setPostName(String postName) {
        this.postName = postName;
    }

    public String getPostSort() {
        return postSort;
    }

    public void setPostSort(String postSort) {
        this.postSort = postSort;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SysPost sysPost = (SysPost) o;
        return Objects.equals(postId, sysPost.postId) && Objects.equals(postCode, sysPost.postCode) && Objects.equals(postName, sysPost.postName) && Objects.equals(postSort, sysPost.postSort) && Objects.equals(status, sysPost.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(postId, postCode, postName, postSort, status);
    }

    @Override
    public String toString() {
        return "SysPost{" +
                "postId=" + postId +
                ", postCode='" + postCode + '\'' +
                ", postName='" + postName + '\'' +
                ", postSort='" + postSort + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
