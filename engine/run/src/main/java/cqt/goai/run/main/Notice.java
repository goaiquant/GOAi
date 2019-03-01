package cqt.goai.run.main;

import cqt.goai.exchange.ExchangeUtil;
import dive.http.common.MimeRequest;
import org.slf4j.Logger;

import static dive.common.util.Util.useful;


/**
 * 通知对象
 *
 * @author GOAi
 */
public class Notice {

    private final Logger log;

    private final String telegramGroup;

    private final String token;

    public Notice(Logger log, String telegramGroup, String token) {
        this.log = log;
        this.telegramGroup = telegramGroup;
        this.token = token;
    }

    /**
     * 高级通知，发电报
     * @param message 消息
     */
    public void noticeHigh(String message) {
        this.log.info("NOTICE_HIGH {}", message);

        // FIX ME 发电报通知
        if (useful(this.telegramGroup)) {
            new MimeRequest.Builder()
                    .url("https://api.telegram.org/bot" + token + "/sendMessage")
                    .post()
                    .body("chat_id", this.telegramGroup)
                    .body("text", message)
                    .body("parse_mode", "HTML")
                    .execute(ExchangeUtil.OKHTTP);
        }
    }

    /**
     * 低级通知，打日志
     * @param message 消息
     */
    public void noticeLow(String message) {
        this.log.info("NOTICE_LOW {}", message);
    }

}
