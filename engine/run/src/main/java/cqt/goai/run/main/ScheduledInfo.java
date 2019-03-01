package cqt.goai.run.main;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Trigger;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;

import java.text.ParseException;
import java.util.Date;

import static dive.common.util.Util.exist;
import static dive.common.util.Util.useful;

/**
 * 单个方法的定时信息
 * @author GOAi
 */
@Getter
@Slf4j
class ScheduledInfo {

    /**
     * cron定时
     */
    private String cron;

    /**
     * 固定频率
     */
    private long fixedRate;
    /**
     * 延时
     */
    private long delay;

    /**
     * 触发器
     */
    private Trigger trigger;

    /**
     * 提示信息
     */
    private String tip;

    /**
     * 构造器
     */
    ScheduledInfo(String cron, long fixedRate, long delay) {
        this.cron = cron;
        this.fixedRate = fixedRate;
        this.delay = delay;
        initTrigger();
    }

    /**
     * 设置触发器
     */
    private void initTrigger() {
        initCronTrigger();
        if (exist(trigger)) {
            return;
        }
        initSimpleTrigger();
        if (exist(trigger)) {
            return;
        }
        log.error("init trigger error --> cron: {}, fixedRate: {}, delay: {}",
                this.cron, this.fixedRate, this.delay);
    }

    /**
     * 重置触发器
     */
    void setCron(String cron) {
        this.cron = cron;
        this.trigger = null;
        initCronTrigger();
    }

    /**
     * 重置触发器
     */
    void setRate(long fixedRate) {
        this.fixedRate = fixedRate;
        this.delay = 0;
        initSimpleTrigger();
    }

    /**
     * 使用前应该复制一份
     */
    ScheduledInfo copy() {
        return new ScheduledInfo(this.cron, this.fixedRate, this.delay);
    }

    /**
     * 设置触发器
     */
    private void initCronTrigger() {
        if (!useful(this.cron)) {
            return;
        }
        try {
            CronTriggerImpl cronTrigger = new CronTriggerImpl();
            cronTrigger.setCronExpression(this.cron);
            this.tip = "cron: " + this.cron;
            this.trigger = cronTrigger;
            this.fixedRate = -1;
            this.delay = -1;
        } catch (ParseException e) {
            log.error("can not format {} to cron", this.cron);
        }
    }


    /**
     * 设置触发器
     */
    private void initSimpleTrigger() {
        if (this.fixedRate <= 0 || this.delay < 0) {
            return;
        }
        SimpleTriggerImpl simpleTrigger = new SimpleTriggerImpl();
        simpleTrigger.setRepeatInterval(this.fixedRate);
        this.tip = "fixedRate: " + this.fixedRate + " ms delay: " + this.delay + " ms";
        // 无限次
        simpleTrigger.setRepeatCount(-1);
        simpleTrigger.setStartTime(new Date(System.currentTimeMillis() + this.delay));
        this.trigger = simpleTrigger;
        this.cron = "";
    }

}
