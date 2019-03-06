package cqt.goai.run.main;

import lombok.Getter;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单个定时任务方法
 *
 * @author GOAi
 */
@Getter
class ScheduledMethod {

    private static Minister minister = Minister.getInstance();

    private final Logger log;

    private final Integer id;

    private final RunTask runTask;

    private final Method method;

    private final MethodScope methodScope;

    private final ReentrantLock lock;

    private final ScheduledInfo scheduledInfo;

    ScheduledMethod(Logger log, Integer id, RunTask runTask, Method method, ReentrantLock lock, ScheduledInfo scheduledInfo, MethodScope methodScope) {
        this.log = log;
        this.id = id;
        this.runTask = runTask;
        this.method = method;
        this.methodScope = methodScope;
        this.lock = lock;
        this.scheduledInfo = scheduledInfo;
    }

    void invoke(String description) {
        if (minister.isStopping()) {
            return;
        }
        try {
            if (null == lock || lock.tryLock(0, TimeUnit.MILLISECONDS)) {
                try {
                    method.invoke(runTask);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                    log.error("run method failed : {} --> {} exception: {}",
                            description, method.getName(), e);
                } finally {
                    if (null != lock && lock.isLocked()) {
                        lock.unlock();
                    }
                }
            } else {
                if (minister.isDebug()) {
                    log.error("{} lock is locked... {}", description, id);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
