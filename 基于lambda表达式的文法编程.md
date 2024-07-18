```java
Symbol sentence=join(verb,optional(subject),optional(predicate),optional(qualifier));

Symbol verb=join(
    choice(
        keyword("find"),keyword("select"),keyword("list"),keyword("get"),
        keyword("update"),keyword("updateBatch"),keyword("modify"),keyword("modifyBatch"),
        keyword("delete"),keyword("deleteBatch"),keyword("remove"),keyword("removeBatch"),
        keyword("exists"),
        keyword("count"),
        join(keyword("save"),end),join(keyword("saveBatch"),end),join(keyword("insert"),end),join(keyword("insertBatch"),end),
        keyword("sum"),keyword("avg"),keyword("max"),keyword("min")
    ),
    optional(choice(keyword("All"),keyword("One"),join(keyword("Top"),choice(assign("num",num),assign("variable",variable)))))
);

Symbol subject=choice(
    noun,
    join(noun,keyword("And"),noun)
);

Symbol predicate=join(
    choice(keyword("By"),keyword("Where")),
    choice(
        noun,
        join(noun,optional(keyword("Not")),keyword("Is"),optional(assign("variable",variable))),
        join(noun,optional(keyword("Not")),keyword("Equals"),optional(assign("variable",variable))),
        join(noun,optional(keyword("Not")),keyword("Between"),optional(join(assign("begin",num),keyword("To"),assign("end",num)))),
        join(noun,optional(keyword("Not")),keyword("In"),optional(assign("variable",variable))),
        join(noun,optional(keyword("Not")),keyword("IsNull")),
        join(noun,optional(keyword("Not")),keyword("IsTrue")),
        join(noun,optional(keyword("Not")),keyword("IsFalse")),
        join(noun,optional(keyword("Not")),keyword("StartWith"),optional(assign("variable",variable))),
        join(noun,optional(keyword("Not")),keyword("EndWith"),optional(assign("variable",variable))),
        join(noun,keyword("Lowercase")),
        join(noun,keyword("Uppercase")),
        join(noun,keyword("Ignorecase")),
        join(predicate,keyword("And"),predicate),
        join(predicate,keyword("Or"),predicate)
    )
);

Symbol qualifier=choice(
    join(keyword("OrderBy"),noun),
    join(keyword("GroupBy"),noun),
    join(keyword("Limit"),assign("begin",num),optional(join(keyword("To"),assign("end",num)))),
    join(keyword("Limit"),assign("begin",variable),optional(join(keyword("To"),assign("end",variable)))),
    join(qualifier,qualifier)
);