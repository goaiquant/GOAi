package cqt.goai.run.main;

/**
 * 定时任务方法运行范围
 *
 * @author GOAi
 */
public enum MethodScope {

    /**
     * 全局唯一
     */
    GLOBAL,

    /**
     * 实例唯一
     */
    INSTANCE,

    /**
     * 循环任务唯一
     */
    SCHEDULED,

    /**
     * 不限制
     */
    NONE

}
