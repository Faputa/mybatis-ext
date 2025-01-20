package io.github.mybatisext.test;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.Table;

/**
 * 用户和岗位关联 sys_user_post
 * 
 * @author ruoyi
 */
@Table
public class SysUserPost {

    /** 用户ID */
    @Id
    @Column
    private Long userId;

    /** 岗位ID */
    @Id
    @Column
    private Long postId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPostId() {
        return postId;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
    }
}
