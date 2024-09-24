package io.github.mybatisext.dialect;

import io.github.mybatisext.jpa.Limit;

import java.util.ArrayList;
import java.util.List;

public class PostgreSqlDialect extends MySqlDialect {

    @Override
    public String limit(Limit limit, String select) {
        List<String> ss = new ArrayList<>();
        ss.add(select);
        if (limit.getOffset() == null && limit.getOffsetVariable() != null) {
            ss.add("LIMIT");
            ss.add(limit.getRowCount() != null ? limit.getRowCount().toString() : "#{" + limit.getRowCountVariable() + "}");
        } else {
            ss.add("LIMIT");
            ss.add(limit.getOffset() != null ? limit.getOffset().toString() : "#{" + limit.getOffsetVariable() + "}");
            ss.add("OFFSET");
            ss.add(limit.getRowCount() != null ? limit.getRowCount().toString() : "#{" + limit.getRowCountVariable() + "}");
        }
        return String.join(" ", ss);
    }
}
