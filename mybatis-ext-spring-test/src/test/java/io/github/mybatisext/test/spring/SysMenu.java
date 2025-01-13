package io.github.mybatisext.test.spring;

import java.util.ArrayList;
import java.util.List;

import io.github.mybatisext.annotation.Column;
import io.github.mybatisext.annotation.EmbedParent;
import io.github.mybatisext.annotation.FilterSpec;
import io.github.mybatisext.annotation.Id;
import io.github.mybatisext.annotation.JoinColumn;
import io.github.mybatisext.annotation.JoinRelation;
import io.github.mybatisext.annotation.LoadStrategy;
import io.github.mybatisext.annotation.LoadType;
import io.github.mybatisext.annotation.Table;
import io.github.mybatisext.jpa.CompareOperator;

/**
 * 菜单权限表 sys_menu
 * 
 * @author ruoyi
 */
@Table
@EmbedParent
public class SysMenu extends BaseEntity {

    /** 菜单ID */
    @Id
    @Column
    private Long menuId;

    /** 菜单名称 */
    @FilterSpec(operator = CompareOperator.Like)
    @Column
    private String menuName;

    /** 父菜单名称 */
    @FilterSpec(operator = CompareOperator.Like)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "parent_id", rightColumn = "menu_id"), table = SysMenu.class, column = "menu_name")
    private String parentName;

    /** 父菜单ID */
    @Column
    private Long parentId;

    /** 显示顺序 */
    @Column
    private String orderNum;

    /** 菜单URL */
    @Column
    private String url;

    /** 打开方式（menuItem页签 menuBlank新窗口） */
    @Column
    private String target;

    /** 类型（M目录 C菜单 F按钮） */
    @Column
    private String menuType;

    /** 菜单状态（0显示 1隐藏） */
    @Column
    private String visible;

    /** 是否刷新（0刷新 1不刷新） */
    @Column
    private String isRefresh;

    /** 权限字符串 */
    @Column
    private String perms;

    /** 菜单图标 */
    @Column
    private String icon;

    /** 子菜单 */
    @LoadStrategy(LoadType.FETCH_EAGER)
    @JoinRelation(joinColumn = @JoinColumn(leftColumn = "menu_id", rightColumn = "parent_id"))
    private List<SysMenu> children = new ArrayList<SysMenu>();

    public Long getMenuId() {
        return menuId;
    }

    public void setMenuId(Long menuId) {
        this.menuId = menuId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(String orderNum) {
        this.orderNum = orderNum;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getMenuType() {
        return menuType;
    }

    public void setMenuType(String menuType) {
        this.menuType = menuType;
    }

    public String getVisible() {
        return visible;
    }

    public void setVisible(String visible) {
        this.visible = visible;
    }

    public String getIsRefresh() {
        return isRefresh;
    }

    public void setIsRefresh(String isRefresh) {
        this.isRefresh = isRefresh;
    }

    public String getPerms() {
        return perms;
    }

    public void setPerms(String perms) {
        this.perms = perms;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<SysMenu> getChildren() {
        return children;
    }

    public void setChildren(List<SysMenu> children) {
        this.children = children;
    }
}
