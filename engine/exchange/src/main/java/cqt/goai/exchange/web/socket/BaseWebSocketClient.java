package cqt.goai.exchange.web.socket;

import cqt.goai.exchange.util.RateLimit;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.Klines;
import cqt.goai.model.market.Depth;
import cqt.goai.model.market.Ticker;
import cqt.goai.model.market.Trades;
import cqt.goai.model.trade.Account;
import cqt.goai.model.trade.Orders;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static dive.common.util.Util.exist;


/**
 * WebSocket连接
 * @author GOAi
 */
public abstract class BaseWebSocketClient {

    /**
     * 日志
     */
    protected final Logger log;

    /**
     * 币对
     */
    protected final String symbol;

    /**
     * access
     */
    protected final String access;

    /**
     * secret
     */
    protected final String secret;

    /**
     * 连接ping的频率 本链接最近更新频率, 高于该频率不发送ping
     */
    protected final RateLimit limit;

    /**
     * 消费函数
     */
    private Consumer<Ticker> onTicker;

    private ConcurrentHashMap<Period, Consumer<Klines>> onKlines = new ConcurrentHashMap<>();

    private Consumer<Depth> onDepth;

    private Consumer<Trades> onTrades;

    private Consumer<Account> onAccount;

    private Consumer<Orders> onOrders;

    public BaseWebSocketClient(String symbol, String access, String secret, RateLimit limit, Logger log) {
        this.symbol = symbol;
        this.access = access;
        this.secret = secret;
        this.limit = limit;
        this.log = log;
    }

    /**
     * 第一次连接需要做的
     */
    public abstract void open();

    /**
     * 连接断开
     */
    public abstract void closed();

    /**
     * 发送message
     * @param message message
     */
    protected abstract void send(String message);

    /**
     * ping
     */
    public void ping() {
        if (this.limit.timeout(true)) {
            this.send("ping");
        }
    }

    /**
     * 处理收到的新信息
     * @param message 收到信息
     */
    protected abstract void transform(String message);

    /**
     * 主动断开
     * @param code code
     * @param reason reason
     */
    public abstract void close(int code, String reason);

    /**
     * 命令日志
     * @param command 命令
     * @param url url
     */
    protected void commandLog(String command, String url) {
        this.log.info("websocket send: {} to {}", command, url);
    }
    // ===================== ticker =====================

    /**
     * 注册推送Ticker
     * @param onTicker 推送函数
     */
    public void onTicker(Consumer<Ticker> onTicker) {
        this.onTicker = onTicker;
        this.askTicker();
    }

    /**
     * 发送订阅信息
     */
    protected abstract void askTicker();

    /**
     * 接收到Ticker后
     * @param ticker ticker
     */
    protected void onTicker(Ticker ticker) {
        if (exist(this.onTicker)) {
            this.onTicker.accept(ticker);
        }
    }

    /**
     * 取消推送Ticker
     */
    public abstract void noTicker();

    // ===================== klines =====================

    /**
     * 注册推送Klines
     * @param onKlines 推送函数
     * @param period 周期
     */
    public void onKlines(Consumer<Klines> onKlines, Period period) {
        this.onKlines.put(period, onKlines);
        this.askKlines(period);
    }

    /**
     * 发送订阅信息
     * @param period 周期
     */
    protected abstract void askKlines(Period period);

    /**
     * 接收到Ticker后
     * @param klines klines
     * @param period 周期
     */
    protected void onKlines(Klines klines, Period period) {
        if (this.onKlines.containsKey(period)) {
            this.onKlines.get(period).accept(klines);
        }
    }

    /**
     * 取消推送Klines
     * @param period 周期
     */
    public abstract void noKlines(Period period);

    // ===================== depth =====================

    /**
     * 注册推送Depth
     * @param onDepth 推送函数
     */
    public void onDepth(Consumer<Depth> onDepth) {
        this.onDepth = onDepth;
        this.askDepth();
    }

    /**
     * 发送订阅信息
     */
    protected abstract void askDepth();

    /**
     * 接收到Depth后
     * @param depth depth
     */
    protected void onDepth(Depth depth) {
        if (exist(this.onDepth)) {
            this.onDepth.accept(depth);
        }
    }

    /**
     * 取消推送Ticker
     */
    public abstract void noDepth();


    // ===================== trades =====================

    /**
     * 注册推送Trades
     * @param onTrades 推送函数
     */
    public void onTrades(Consumer<Trades> onTrades) {
        this.onTrades = onTrades;
        this.askTrades();
    }

    /**
     * 发送订阅信息
     */
    protected abstract void askTrades();

    /**
     * 接收到Trades后
     * @param trades Trades
     */
    protected void onTrades(Trades trades) {
        if (exist(this.onTrades)) {
            this.onTrades.accept(trades);
        }
    }

    /**
     * 取消推送Trades
     */
    public abstract void noTrades();


    // ===================== account =====================

    /**
     * 注册推送Account
     * @param onAccount 推送函数
     */
    public void onAccount(Consumer<Account> onAccount) {
        this.onAccount = onAccount;
        this.askAccount();
    }

    /**
     * 发送订阅信息
     */
    protected abstract void askAccount();

    /**
     * 接收到Account后
     * @param account account
     */
    protected void onAccount(Account account) {
        if (exist(this.onAccount)) {
            this.onAccount.accept(account);
        }
    }

    /**
     * 取消推送Account
     */
    public abstract void noAccount();

    // ===================== orders =====================

    /**
     * 注册推送Orders
     * @param onOrders 推送函数
     */
    public void onOrders(Consumer<Orders> onOrders) {
        this.onOrders = onOrders;
        this.askOrders();
    }

    /**
     * 发送订阅信息
     */
    protected abstract void askOrders();

    /**
     * 接收到Orders后
     * @param orders orders
     */
    protected void onOrders(Orders orders) {
        if (exist(this.onOrders)) {
            this.onOrders.accept(orders);
        }
    }

    /**
     * 取消推送Orders
     */
    public abstract void noOrders();

}
