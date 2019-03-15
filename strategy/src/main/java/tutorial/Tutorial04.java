package tutorial;

import cqt.goai.exchange.util.RateLimit;
import cqt.goai.model.market.Ticker;
import cqt.goai.run.exchange.Exchange;
import cqt.goai.run.main.RunTask;
import cqt.goai.run.notice.NoticeType;

import java.util.Arrays;

/**
 * 教程2：本教程你将了解 如何接受主动推送信息。
 *     onTicker 主动推送ticker
 *     onKlines 主动推送klines
 *     onDepth 主动推送depth
 *     onTrades 主动推送trades
 *     onAccount 主动推送account
 *     onOrders 主动推送orders
 *
 *     以ticker为例，2种推送方式：
 *          1. 名为 属性名OnTicker(Ticker ticker) 的方法
 *          2. 调用对应Exchange的setOnTicker(Consumer<Ticker> consumer[, RateLimit limit])方法,
 *              该方法可限制推送频率不高于某个秒数, RateLimit.limit(mills) 或使用默认的 RateLimit.second3()等
 *
 */

public class Tutorial04 extends RunTask {

    /**
     * 有这个Exchange才会有推送
     */
    private Exchange huobiE;

    @Override
    protected void init() {

        // 这种方式设置的主动推送，可以设置间隔时间，例：second3 表明两次推送的最小间隔不超过3秒
        huobiE.setOnTicker(ticker -> {
            log.info("huobi e 2 ticker -> {}", ticker);
        }, RateLimit.second3());

    }

    /**
     * 属于参数 huobiE 的主动推送，
     * 以 huobiE 开头，并且只有一个Ticker参数的方法会默认设置推送
     * @param ticker
     */
    protected void huobiEOnTicker(Ticker ticker) {
        log.info("huobi e 1 ticker -> {}", ticker);
    }

    /**
     * 默认为 e 的主动推送
     * @param ticker
     */
    @Override
    protected void onTicker(Ticker ticker) {
        log.info("e ticker -> {}", ticker);
    }
}
