package io.github.mybatisext.metadata;

import java.util.LinkedHashMap;
import java.util.Set;

public class JoinTableInfo {

    private TableDef tableDef;
    private Set<JoinColumnInfo> leftJoinColumnInfos;
    private JoinNode joinNode;

    public TableDef getTableDef() {
        return tableDef;
    }

    public void setTableDef(TableDef tableDef) {
        this.tableDef = tableDef;
    }

    public Set<JoinColumnInfo> getLeftJoinColumnInfos() {
        return leftJoinColumnInfos;
    }

    public void setLeftJoinColumnInfos(Set<JoinColumnInfo> leftJoinColumnInfos) {
        this.leftJoinColumnInfos = leftJoinColumnInfos;
    }

    public JoinNode getJoinNode() {
        return joinNode;
    }

    public void setJoinNode(JoinNode joinNode) {
        this.joinNode = joinNode;
    }

    public TableName getTableName() {
        return tableDef.getTableName();
    }

    public String getAlias() {
        return joinNode.getAlias();
    }

    public void collectJoinTableInfo(LinkedHashMap<String, JoinTableInfo> orderJoinTableInfos) {
        if (orderJoinTableInfos.containsKey(joinNode.getAlias())) {
            return;
        }
        if (leftJoinColumnInfos != null) {
            for (JoinColumnInfo joinColumnInfo : leftJoinColumnInfos) {
                joinColumnInfo.getLeftJoinTableInfo().collectJoinTableInfo(orderJoinTableInfos);
            }
        }
        orderJoinTableInfos.put(joinNode.getAlias(), this);
    }

    @Override
    public String toString() {
        return tableDef.getTableName() + " AS " + joinNode.getAlias();
    }
}
