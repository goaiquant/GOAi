package cqt.goai.exchange.web.socket;

import cqt.goai.exchange.BaseExchange;
import cqt.goai.exchange.ExchangeException;
import cqt.goai.exchange.ExchangeInfo;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.Klines;
import cqt.goai.model.market.Depth;
import cqt.goai.model.market.Ticker;
import cqt.goai.model.market.Trades;
import cqt.goai.model.trade.Account;
import cqt.goai.model.trade.Orders;
import dive.common.math.RandomUtil;
import org.slf4j.Logger;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * WebSocket请求，每个交易所的WebSocket请求方式都要继承该类
 * @author GOAi
 */
public class WebSocketExchange extends BaseExchange {

    /**
     * 每个连接加入ping的id，取消时用
     */
    private String id;

    /**
     * 需要ping的连接
     */
    private static final ConcurrentHashMap<String, WebSocketExchange> EXCHANGES = new ConcurrentHashMap<>();

    static {
        ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1, new ThreadPoolExecutor.DiscardPolicy());
        timer.scheduleAtFixedRate(() -> EXCHANGES.values().stream().parallel().forEach(WebSocketExchange::ping), 7, 7, TimeUnit.SECONDS);
    }

    public WebSocketExchange(ExchangeName name, Logger log) {
        super(name, log);
        this.ping(this);
    }

    /**
     * 定时某些交易所需要定时向服务器发送ping
     */
    protected void ping() {}

    /**
     * 加入ping
     * @param e ping对象
     */
    private void ping(WebSocketExchange e) {
        if (null != this.id && EXCHANGES.containsKey(this.id)) {
            return;
        }
        this.id = RandomUtil.token();
        EXCHANGES.put(this.id, e);
    }

    /**
     * 取消ping
     */
    protected void noPing() {
        EXCHANGES.remove(this.id);
    }

    // =============== ticker ===============

    /**
     * 获取Ticker
     * @param info 请求信息
     * @param onTicker 消费函数
     * @return 订阅id
     */
    public boolean onTicker(ExchangeInfo info, Consumer<Ticker> onTicker) {
        throw new ExchangeException(super.name + " " + info.getSymbol() + " onTicker is not supported.");
    }

    public void noTicker(String pushId) {
        throw new ExchangeException(super.name + " noTicker is not supported. pushId: " + pushId);
    }

    // =============== klines ===============

    /**
     * 获取Klines
     * @param info 请求信息
     * @param onKlines 消费函数
     * @return 订阅id
     */
    public boolean onKlines(ExchangeInfo info, Consumer<Klines> onKlines) {
        throw new ExchangeException(super.name + " " + info.getSymbol() + " onKlines " + info.getPeriod() + " is not supported.");
    }

    public void noKlines(Period period, String pushId) {
        throw new ExchangeException(super.name + " noKlines " + period + " is not supported. pushId: " + pushId);
    }

    // =============== depth ===============

    /**
     * 获取Depth
     * @param info 请求信息
     * @param onDepth 消费函数
     * @return 订阅id
     */
    public boolean onDepth(ExchangeInfo info, Consumer<Depth> onDepth) {
        throw new ExchangeException(super.name + " " + info.getSymbol() + " onDepth is not supported.");
    }

    public void noDepth(String pushId) {
        throw new ExchangeException(super.name + " noDepth is not supported. pushId: " + pushId);
    }

    // =============== trades ===============

    /**
     * 获取Trades
     * @param info 请求信息
     * @param onTrades 消费函数
     * @return 订阅id
     */
    public boolean onTrades(ExchangeInfo info, Consumer<Trades> onTrades) {
        throw new ExchangeException(super.name + " " + info.getSymbol() + " onTrades is not supported.");
    }

    public void noTrades(String pushId) {
        throw new ExchangeException(super.name + " noTrades is not supported. pushId: " + pushId);
    }

    // =============== account ===============

    /**
     * 获取Account
     * @param info 请求信息
     * @param onAccount 消费函数
     * @return 订阅id
     */
    public boolean onAccount(ExchangeInfo info, Consumer<Account> onAccount) {
        throw new ExchangeException(super.name + " " + info.getSymbol() + " onAccount is not supported.");
    }

    public void noAccount(String pushId) {
        throw new ExchangeException(super.name + " noAccount is not supported. pushId: " + pushId);
    }

    // =============== orders ===============

    /**
     * 获取Orders
     * @param info 请求信息
     * @param onOrders 消费函数
     * @return 订阅id
     */
    public boolean onOrders(ExchangeInfo info, Consumer<Orders> onOrders) {
        throw new ExchangeException(super.name + " " + info.getSymbol() + " onOrders is not supported.");
    }

    public void noOrders(String pushId) {
        throw new ExchangeException(super.name + " noOrders is not supported. pushId: " + pushId);
    }

}
