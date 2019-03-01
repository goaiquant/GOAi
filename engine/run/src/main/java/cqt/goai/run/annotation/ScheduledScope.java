package cqt.goai.run.annotation;

import cqt.goai.run.main.MethodScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法范围
 *
 * @author GOAi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ScheduledScope {
    /**
     * 默认实例范围
     * @return 方法运行范围
     */
    MethodScope value() default MethodScope.INSTANCE;
}
