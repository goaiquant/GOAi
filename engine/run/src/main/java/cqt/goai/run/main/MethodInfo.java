package cqt.goai.run.main;

import lombok.Getter;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 方法运行信息
 *
 * @author GOAi
 */
@Getter
class MethodInfo {

    /**
     * 方法
     */
    private final Method method;

    /**
     * 方法运行范围
     */
    private final MethodScope methodScope;

    /**
     * 方法运行锁，重点是全局一个锁，不检查无锁，实例内只用一个，定时范围新生成
     */
    private final ReentrantLock globalLock;

    /**
     * 实例锁
     */
    private final ReentrantLock instanceLock;

    /**
     * 定时任务信息
     */
    private final List<ScheduledInfo> schedules;

    MethodInfo(Method method, MethodScope methodScope, List<ScheduledInfo> schedules) {
        this.method = method;
        this.methodScope = methodScope;
        this.schedules = schedules;
        if (methodScope == MethodScope.GLOBAL) {
            this.globalLock = new ReentrantLock();
            this.instanceLock = null;
        } else {
            this.globalLock = null;
            this.instanceLock = null;
        }
    }

    private MethodInfo(Method method, MethodScope methodScope, ReentrantLock globalLock, ReentrantLock instanceLock, List<ScheduledInfo> schedules) {
        this.method = method;
        this.methodScope = methodScope;
        this.globalLock = globalLock;
        this.instanceLock = instanceLock;
        this.schedules = schedules;
    }

    MethodInfo copy() {
        switch (methodScope) {
            case INSTANCE: return new MethodInfo(method, methodScope, null, new ReentrantLock(), schedules);
            case SCHEDULED: return new MethodInfo(method, methodScope, null, null, schedules);
            default: return this;
        }
    }

    ReentrantLock getLock() {
        switch (methodScope) {
            case GLOBAL: return this.globalLock;
            case INSTANCE: return this.instanceLock;
            case SCHEDULED: return new ReentrantLock();
            default: return null;
        }
    }

}
