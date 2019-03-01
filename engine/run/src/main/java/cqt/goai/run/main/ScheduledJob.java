package cqt.goai.run.main;

import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.List;

import static dive.common.util.Util.exist;


/**
 * 从Configs中找到要执行的任务，执行
 * @author GOAi
 */
@Slf4j
public class ScheduledJob implements Job {

    private static Minister minister = Minister.getInstance();

    /**
     * Command Pattern 命令模式
     * 封装任务命令定时执行
     * @param context 任务内容
     */
    @Override
    public void execute(JobExecutionContext context) {
        if (!ScheduledJob.minister.isReady()) {
            return;
        }
        /*
         * Interpreter Patter 解释器模式
         * 定义规则，不同解释器解释结果不同
         * 运算表达式 统一的解释方法，加减乘除不同的处理方式
         */
        String description = context.getJobDetail().getDescription();
        String[] split = description.split("-");
        Integer id = Integer.valueOf(split[0]);
        Integer index = Integer.valueOf(split[2]);
        TaskManager manager = ScheduledJob.minister.getManagers().get(id);
        if (!exist(manager)) {
            log.error("can not find TaskManager: {}", description);
            return;
        }
        List<ScheduledMethod> methodTasks = manager.getMethodTasks();
        if (methodTasks.size() <= index) {
            manager.log.error("can not find MethodTask: {}, methodTasks.size: {}",
                    description, methodTasks.size());
            return;
        }
        ScheduledMethod mt = methodTasks.get(index);
        mt.invoke(description);
    }
}
