package io.github.mybatisext.test;

import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.TableRef;

@TableRef(SysPost.class)
public class SysPostVO {

    @ColumnRef
    private Long postId;
    @ColumnRef
    private String postCode;
    @ColumnRef
    private String postName;
    @ColumnRef
    private String postSort;
    @ColumnRef
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
}
