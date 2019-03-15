package cqt.goai.run.notice;

import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.ExchangeUtil;
import dive.http.common.MimeRequest;
import org.slf4j.Logger;


/**
 * 通知对象
 *
 * @author GOAi
 */
public class TelegramNotice extends BaseNotice {

    private final String token;

    private final String chatId;

    public TelegramNotice(Logger log, String strategyName, JSONObject config) {
        super(log, strategyName);
        this.token = config.getString("token");
        this.chatId = config.getString("chatId");
    }

    /**
     * 高级通知，发电报
     * @param message 消息
     */
    @Override
    public void notice(String message) {
        new MimeRequest.Builder()
                .url("https://api.telegram.org/bot" + this.token + "/sendMessage")
                .post()
                .body("chat_id", this.chatId)
                .body("text", strategyName + "\n" + message)
                .body("parse_mode", "HTML")
                .execute(ExchangeUtil.OKHTTP);
    }

}
