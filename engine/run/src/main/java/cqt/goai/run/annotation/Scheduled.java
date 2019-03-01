package cqt.goai.run.annotation;

import java.lang.annotation.*;

/**
 * 定时任务
 * @author GOAi
 */
@Repeatable(Schedules.class)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Scheduled {
    /**
     * cron方式 优先
     */
    String cron() default "";

    /**
     * 固定频率，毫秒
     */
    long fixedRate() default 0;

    /**
     * 延时，毫秒
     */
    long delay() default 0;

}
