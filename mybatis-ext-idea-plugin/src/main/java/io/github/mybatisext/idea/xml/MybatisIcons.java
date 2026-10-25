package io.github.mybatisext.idea.xml;

import javax.swing.Icon;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.IconLoader;

public final class MybatisIcons {

    public static final Icon MAPPER = IconLoader.getIcon("/icons/mapper.svg", MybatisIcons.class);
    public static final Icon XML = IconLoader.getIcon("/icons/xml.svg", MybatisIcons.class);
    public static final Icon TABLE = IconLoader.getIcon("/icons/table.svg", MybatisIcons.class);
    public static final Icon TABLE_REF = IconLoader.getIcon("/icons/tableref.svg", MybatisIcons.class);
    public static final Icon ENHANCED = AllIcons.Nodes.Enum;

    private MybatisIcons() {
    }
}
