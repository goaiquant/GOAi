package cqt.goai.run.notice;

import org.slf4j.Logger;


/**
 * 通知对象
 *
 * @author GOAi
 */
public abstract class BaseNotice {

    protected final Logger log;

    final String strategyName;

    BaseNotice(Logger log, String strategyName) {
        this.log = log;
        this.strategyName = strategyName;
    }

    /**
     * 高级通知，发电报
     * @param message 消息
     */
    public abstract void notice(String message);

}
