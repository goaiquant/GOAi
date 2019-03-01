package cqt.goai.model.other;

import lombok.Data;

/**
 * 标记程序运行信息
 * run 落盘
 * node 读取
 * @author GOAi
 */
@Data
public class RunInfo {

    /**
     * 启动时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 程序当前状态
     */
    private RunState runState;

    /**
     * 程序pid
     */
    private Integer pid;

    public RunInfo(Long startTime, RunState runState) {
        this.startTime = startTime;
        this.runState = runState;
    }

    public RunInfo setRunState(RunState runState) {
        this.runState = runState;
        return this;
    }
}
