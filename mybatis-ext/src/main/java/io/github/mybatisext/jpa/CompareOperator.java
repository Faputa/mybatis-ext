package io.github.mybatisext.jpa;

public enum CompareOperator {

    Equals(true, false),

    LessThan(true, false),

    LessThanEqual(true, false),

    GreaterThan(true, false),

    GreaterThanEqual(true, false),

    Like(true, false),

    StartWith(true, false),

    EndWith(true, false),

    Between(true, true),

    In(true, false),

    IsNull(false, false),

    IsNotNull(false, false),

    IsTrue(false, false),

    IsFalse(false, false),
    ;

    private final boolean requiredVariable;
    private final boolean requiredSecondVariable;

    private CompareOperator(boolean requiredVariable, boolean requiredSecondVariable) {
        this.requiredVariable = requiredVariable;
        this.requiredSecondVariable = requiredSecondVariable;
    }

    public boolean isRequiredVariable() {
        return requiredVariable;
    }

    public boolean isRequiredSecondVariable() {
        return requiredSecondVariable;
    }
}
