package cqt.goai.run.main;

import com.alibaba.fastjson.JSONObject;
import cqt.goai.model.market.Klines;
import cqt.goai.model.market.Depth;
import cqt.goai.model.market.Ticker;
import cqt.goai.model.market.Trades;
import cqt.goai.model.trade.Account;
import cqt.goai.model.trade.Orders;
import cqt.goai.run.exchange.Exchange;
import dive.common.util.TryUtil;
import org.slf4j.Logger;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 抽象任务类
 *
 * @author GOAi
 */
public class RunTask {
    /**
     * 是否准备好推送，init方法结束后设置为true
     */
    private Boolean ready = false;

    /**
     * 准备好了才推送
     */
    final Supplier<Boolean> isReady = () -> this.ready;


    /**
     * 用户日志
     */
    protected Logger log;

    /**
     * 通知对象
     */
    protected Notice notice;

    /**
     * 配置名称
     */
    protected String strategyName;

    /**
     * 本实例id
     */
    protected Integer id;

    /**
     * 本实例配置
     */
    protected JSONObject config;

    /**
     * 默认e
     */
    protected Exchange e;

    /**
     * 获取exchange 参数为json字符串
     *      {"name":"xxx","symbol":"BTC_USD","access":"xxx","secret":"xxx"}
     */
    protected Function<String, Exchange> getExchange;

    public RunTask() {}

    /**
     * 初始化设置
     * @param strategyName 配置名称
     * @param id 实例id
     * @param config 实例配置
     */
    void init(String strategyName, Integer id, JSONObject config, Logger log) {
        this.log = new UserLogger(log);
        this.notice = new Notice(log, config.getString("telegramGroup"), config.getString("telegramToken"));
        this.strategyName = strategyName;
        this.id = id;
        this.config = config;
    }

    /**
     * 初始化方法
     */
    protected void init() {}

    /**
     * 默认循环
     */
    protected void loop() {}

    /**
     * 退出方法
     */
    protected void destroy() {}


    /**
     * 重写该方法，可自动对e进行主动推送Ticker
     * @param ticker 主动推送的ticker
     */
    protected void onTicker(Ticker ticker) { }

    /**
     * 重写该方法，可自动对e进行主动推送Klines
     * K线是1分钟K线，100个
     * @param klines K线
     */
    protected void onKlines(Klines klines) { }

    /**
     * 重写该方法，可自动对e进行主动推送Depth
     * @param depth 盘口深度
     */
    protected void onDepth(Depth depth) { }

    /**
     * 重写该方法，可自动对e进行主动推送Trades
     * @param trades 交易信息
     */
    protected void onTrades(Trades trades) { }

    /**
     * 重写该方法，可自动对e进行主动推送币对余额
     * @param account 币对余额
     */
    protected void onAccount(Account account) { }

    /**
     * 重写该方法，可自动对e进行主动推送Orders
     * @param orders 订单信息
     */
    protected void onOrders(Orders orders) { }


    void setGetExchange(Function<String, Exchange> getExchange) {
        this.getExchange = getExchange;
    }

    /**
     * 睡眠一段时间
     * @param millis 毫秒
     */
    protected static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重试获取
     * @param supplier 获取函数
     * @param times 次数
     * @param millis 失败暂停时间
     * @param <T> 结果对象
     * @return 结果
     */
    protected static <T> T retry(Supplier<T> supplier, int times, long millis) {
        return TryUtil.retry(supplier, times, () -> sleep(millis));
    }

    /**
     * 重试获取
     * @param supplier 获取函数
     * @param times 次数
     * @param <T> 结果对象
     * @return 结果
     */
    protected static <T> T retry(Supplier<T> supplier, int times) {
        return retry(supplier, times, 3000);
    }

    /**
     * 重试获取
     * @param supplier 获取函数
     * @param <T> 结果对象
     * @return 结果
     */
    protected static <T> T retry(Supplier<T> supplier) {
        return retry(supplier, 5, 1000);
    }

}
