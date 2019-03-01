package cqt.goai.model.other;

/**
 * 运行状态
 * STOPPED -> STARTING -> STARTED -> STOPPING -> STOPPED
 * @author GOAi
 */
public enum RunState {
    /**
     * 程序停止运行
     */
    STOPPED,
    /**
     * 启动中
     */
    STARTING,
    /**
     * 启动完毕
     */
    STARTED,
    /**
     * 停止中
     */
    STOPPING,

    /**
     * 意外停止
     */
    ERROR
}
