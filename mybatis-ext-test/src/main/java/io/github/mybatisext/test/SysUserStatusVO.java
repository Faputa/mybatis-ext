package io.github.mybatisext.test;

import io.github.mybatisext.annotation.ColumnRef;
import io.github.mybatisext.annotation.TableRef;

@TableRef(SysUser.class)
public class SysUserStatusVO {

    @ColumnRef
    private Long deptId;

    @ColumnRef
    private String status;

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
