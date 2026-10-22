package io.github.mybatisext.idea.model;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.chrono.JapaneseDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.session.Configuration;

import io.github.mybatisext.idea.ext.AnalysisSettings;
import io.github.mybatisext.model.JavaType;
import io.github.mybatisext.model.JavaTypes;
import io.github.mybatisext.model.TypePolicy;

/** 仅对框架和 JDK 类使用反射，应用类型始终通过 PSI 解析。 */
public final class PsiTypePolicy implements TypePolicy {

    private static final Set<String> BUILTINS;
    static {
        Set<String> names = new HashSet<>();
        var registry = new Configuration().getTypeHandlerRegistry();
        // 匹配 MyBatis 的默认注册规则，不加载任何项目类。
        for (Class<?> cls : new Class<?>[]{boolean.class, byte.class, short.class, int.class, long.class, float.class, double.class, char.class,
                Boolean.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Character.class,
                String.class, Object.class, byte[].class, Byte[].class, BigDecimal.class, BigInteger.class, java.util.Date.class,
                java.sql.Date.class, Time.class, Timestamp.class, Reader.class, InputStream.class,
                Instant.class, LocalDateTime.class, LocalDate.class, LocalTime.class,
                OffsetDateTime.class, OffsetTime.class, ZonedDateTime.class,
                Year.class, Month.class, YearMonth.class, JapaneseDate.class}) {
            if (registry.hasTypeHandler(cls)) {
                names.add(cls.getTypeName());
            }
        }
        BUILTINS = Collections.unmodifiableSet(names);
    }
    private final Set<String> configured;

    public PsiTypePolicy(AnalysisSettings.State settings) {
        configured = Set.copyOf(settings.scalarTypes);
    }

    @Override
    public ScalarKind classify(JavaType type) {
        if (type == null || !type.isResolved()) {
            return ScalarKind.UNKNOWN;
        }
        for (JavaType current = type; current != null; current = current.getGenericSuperclass()) {
            // MyBatis 会从父类继承类型处理器，但 java.lang.Object 除外。
            if (current != type && Object.class.getName().equals(current.getName())) {
                break;
            }
            if (BUILTINS.contains(current.getName()) || configured.contains(current.getName())) {
                return ScalarKind.SCALAR;
            }
        }
        return JavaTypes.isAssignableTo(type, Enum.class.getName()) ? ScalarKind.SCALAR : ScalarKind.OBJECT;
    }
}
