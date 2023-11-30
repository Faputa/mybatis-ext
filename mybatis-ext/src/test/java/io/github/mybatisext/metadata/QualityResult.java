// 设计demo

// package io.github.mybatisext.test.spring;

// @Table(name="dida_quality_result")
// @RelationTable(QualityRule.class, relations=@Relation(localColumn="rule_id", remoteColumn="id"))
// @RelationTable(QualityXxxx.class, relations=@Relation(localColumn="xxxx_id", remoteColumn="id"))
// @RelationTable(QualityYyyy.class, relations={
//     @Relation(localColumn="yyyy_a1", remoteColumn="a1"),
//     @Relation(localColumn="yyyy_b2", remoteColumn="b2")
// })
// public class QualityResult {

//     @Id
//     @Column
//     private String id;
//     @Column
//     private String ruleId;
//     @Column
//     private String executeId;
//     @Column
//     private String executeDate;
//     @Column
//     private Integer totalNum;
//     @Column
//     private Integer correctNum;
//     @Column
//     private java.math.BigDecimal score;
//     @Column
//     private Integer scoreWeight;
//     @Column
//     private String otherResultJson;
//     @Column
//     private String checkSql;
//     @Column
//     private java.sql.Timestamp executeTime;

//     @RelationColumn(tableClass=QualityRule.class, name="name")
//     private String ruleName;
//     @RelationColumn(tableClass=DidaMetaDataTable.class, name="name")
//     private String targetTableName;
//     @RelationColumn(tableClass=DidaDataSourceConnection.class, name="name")
//     private String datasourceName;
//     @RelationColumn(tableClass=QualityRule.class)
//     private String paramDefineJson;
//     @RelationColumn(tableClass=QualityRule.class)
//     private String targetColumns;
//     @RelationColumn
//     private Xxxx xxxx;

//     public String getId() {
//         return id;
//     }

//     public void setId(String id) {
//         this.id = id;
//     }

//     public String getRuleId() {
//         return ruleId;
//     }

//     public void setRuleId(String ruleId) {
//         this.ruleId = ruleId;
//     }

//     public String getExecuteId() {
//         return executeId;
//     }

//     public void setExecuteId(String executeId) {
//         this.executeId = executeId;
//     }

//     public String getExecuteDate() {
//         return executeDate;
//     }

//     public void setExecuteDate(String executeDate) {
//         this.executeDate = executeDate;
//     }

//     public Integer getTotalNum() {
//         return totalNum;
//     }

//     public void setTotalNum(Integer totalNum) {
//         this.totalNum = totalNum;
//     }

//     public Integer getCorrectNum() {
//         return correctNum;
//     }

//     public void setCorrectNum(Integer correctNum) {
//         this.correctNum = correctNum;
//     }

//     public java.math.BigDecimal getScore() {
//         return score;
//     }

//     public void setScore(java.math.BigDecimal score) {
//         this.score = score;
//     }

//     public Integer getScoreWeight() {
//         return scoreWeight;
//     }

//     public void setScoreWeight(Integer scoreWeight) {
//         this.scoreWeight = scoreWeight;
//     }

//     public String getOtherResultJson() {
//         return otherResultJson;
//     }

//     public void setOtherResultJson(String otherResultJson) {
//         this.otherResultJson = otherResultJson;
//     }

//     public String getCheckSql() {
//         return checkSql;
//     }

//     public void setCheckSql(String checkSql) {
//         this.checkSql = checkSql;
//     }

//     public java.sql.Timestamp getExecuteTime() {
//         return executeTime;
//     }

//     public void setExecuteTime(java.sql.Timestamp executeTime) {
//         this.executeTime = executeTime;
//     }

//     public String getRuleName() {
//         return ruleName;
//     }

//     public void setRuleName(String ruleName) {
//         this.ruleName = ruleName;
//     }

//     public String getTargetTableName() {
//         return targetTableName;
//     }

//     public void setTargetTableName(String targetTableName) {
//         this.targetTableName = targetTableName;
//     }

//     public String getDatasourceName() {
//         return datasourceName;
//     }

//     public void setDatasourceName(String datasourceName) {
//         this.datasourceName = datasourceName;
//     }

//     public String getParamDefineJson() {
//         return paramDefineJson;
//     }

//     public void setParamDefineJson(String paramDefineJson) {
//         this.paramDefineJson = paramDefineJson;
//     }

//     public String getTargetColumns() {
//         return targetColumns;
//     }

//     public void setTargetColumns(String targetColumns) {
//         this.targetColumns = targetColumns;
//     }

// }
