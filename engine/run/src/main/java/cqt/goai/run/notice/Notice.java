package cqt.goai.run.notice;

import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * 通知管理
 *
 * @author goai
 */
public class Notice {

    protected final Logger log;

    private final List<BaseNotice> list;

    public Notice(Logger log, List<BaseNotice> notices) {
        this.log = log;
        list = notices;
    }

    /**
     * 高级通知，发电报
     * @param message 消息
     */
    public void noticeHigh(String message, NoticeType... noticeTypes) {
        this.log.info("NOTICE_HIGH {}", message);
        List<NoticeType> types = Arrays.asList(noticeTypes);
        list.stream().filter(n -> {
            if (types.isEmpty()) {
                return true;
            }
            Class c = n.getClass();
            return (c == EmailNotice.class && types.contains(NoticeType.Email))
                    || (c == TelegramNotice.class && types.contains(NoticeType.Telegram));
        }).forEach(n -> n.notice(message));
    }

    /**
     * 低级通知，打日志
     * @param message 消息
     */
    public void noticeLow(String message) {
        this.log.info("NOTICE_LOW {}", message);
    }

}
