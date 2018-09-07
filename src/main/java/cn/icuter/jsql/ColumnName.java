package cn.icuter.jsql;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author edward
 * @since 2018-08-24
 */
@Target(FIELD)
@Retention(RUNTIME)
public @interface ColumnName {

    /**
     * match selecting column name
     * @return predefined column name
     */
    String value();

    /**
     * ignore mapping select column name to object filed
     *
     * @return true ignore, false don't ignore, default false
     */
    boolean ignore() default false;
}
