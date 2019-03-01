package cqt.goai.exchange;

import cqt.goai.exchange.util.Seal;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.Row;
import lombok.Data;

import java.util.List;

/**
 * 封装请求信息
 * @author GOAi
 */
@Data
public class ExchangeInfo implements Cloneable {
    /**
     * 请求行为
     */
    private Action action;

    /**
     * 请求币对
     */
    private String symbol;

    /**
     * access
     */
    private String access;

    /**
     * secret
     */
    private String secret;


    /**
     * 获取Kline需要指定周期
     */
    private Period period;


    /**
     * 限价单 价格
     */
    private String price;
    /**
     * 限价单 数量
     */
    private String amount;
    /**
     * 市价买单 总价
     */
    private String quote;
    /**
     * 市价卖单数量
     */
    private String base;
    /**
     * 多个市价买单或卖单的价格数量信息
     */
    private List<Row> rows;
    /**
     * 取消订单id
     */
    private String cancelId;
    /**
     * 取消订单的id信息
     */
    private List<String> cancelIds;


    /**
     * 获取订单id
     */
    private String orderId;

    /**
     * 推送id
     */
    private String pushId;

    /**
     * 标识解析方式
     */
    private String extra;

    private ExchangeInfo(Action action, String symbol, String access, String secret) {
        this.action = action;
        this.symbol = symbol;
        this.access = access;
        this.secret = secret;
    }

    private ExchangeInfo(Action action, String symbol, String access, String secret, String pushId) {
        this(action, symbol, access, secret);
        this.pushId = pushId;
    }

    // 市场

    public static ExchangeInfo ticker(String symbol, String access, String secret) {
        return new ExchangeInfo(Action.TICKER, symbol, access, secret);
    }

    public static ExchangeInfo klines(String symbol, String access, String secret, Period period) {
        ExchangeInfo info = new ExchangeInfo(Action.KLINES, symbol, access, secret);
        info.period = period;
        return info;
    }

    public static ExchangeInfo depth(String symbol, String access, String secret) {
        return new ExchangeInfo(Action.DEPTH, symbol, access, secret);
    }

    public static ExchangeInfo trades(String symbol, String access, String secret) {
        return new ExchangeInfo(Action.TRADES, symbol, access, secret);
    }

    // 账户

    public static ExchangeInfo balances(String symbol, String access, String secret) {
        return new ExchangeInfo(Action.BALANCES, symbol, access, secret);
    }

    public static ExchangeInfo account(String symbol, String access, String secret) {
        return new ExchangeInfo(Action.ACCOUNT, symbol, access, secret);
    }

    // 交易

    public static ExchangeInfo precisions(String symbol, String access, String secret) {
        return new ExchangeInfo(Action.PRECISIONS, symbol, access, secret);
    }

    public static ExchangeInfo precision(String symbol, String access, String secret) {
        return new ExchangeInfo(Action.PRECISION, symbol, access, secret);
    }

    public static ExchangeInfo buyLimit(String symbol, String access, String secret, String price, String amount) {
        ExchangeInfo info = new ExchangeInfo(Action.BUY_LIMIT, symbol, access, secret);
        info.price = price;
        info.amount = amount;
        return info;
    }

    public static ExchangeInfo sellLimit(String symbol, String access, String secret, String price, String amount) {
        ExchangeInfo info = new ExchangeInfo(Action.SELL_LIMIT, symbol, access, secret);
        info.price = price;
        info.amount = amount;
        return info;
    }

    public static ExchangeInfo buyMarket(String symbol, String access, String secret, String quote) {
        ExchangeInfo info = new ExchangeInfo(Action.BUY_MARKET, symbol, access, secret);
        info.quote = quote;
        return info;
    }

    public static ExchangeInfo sellMarket(String symbol, String access, String secret, String base) {
        ExchangeInfo info = new ExchangeInfo(Action.SELL_MARKET, symbol, access, secret);
        info.base = base;
        return info;
    }

    public static ExchangeInfo multiBuy(String symbol, String access, String secret, List<Row> rows) {
        ExchangeInfo info = new ExchangeInfo(Action.MULTI_BUY, symbol, access, secret);
        info.rows = rows;
        return info;
    }

    public static ExchangeInfo multiSell(String symbol, String access, String secret, List<Row> rows) {
        ExchangeInfo info = new ExchangeInfo(Action.MULTI_SELL, symbol, access, secret);
        info.rows = rows;
        return info;
    }


    public static ExchangeInfo cancelOrder(String symbol, String access, String secret, String cancelId) {
        ExchangeInfo info = new ExchangeInfo(Action.CANCEL_ORDER, symbol, access, secret);
        info.cancelId = cancelId;
        return info;
    }

    public static ExchangeInfo cancelOrders(String symbol, String access, String secret, List<String> cancelIds) {
        ExchangeInfo info = new ExchangeInfo(Action.CANCEL_ORDERS, symbol, access, secret);
        info.cancelIds = cancelIds;
        return info;
    }

    // 订单接口

    public static ExchangeInfo orders(String symbol, String access, String secret) {
        return new ExchangeInfo(Action.ORDERS, symbol, access, secret);
    }

    public static ExchangeInfo historyOrders(String symbol, String access, String secret) {
        return new ExchangeInfo(Action.HISTORY_ORDERS, symbol, access, secret);
    }

    public static ExchangeInfo order(String symbol, String access, String secret, String orderId) {
        ExchangeInfo info = new ExchangeInfo(Action.ORDER, symbol, access, secret);
        info.orderId = orderId;
        return info;
    }

    public static ExchangeInfo orderDetails(String symbol, String access, String secret, String orderId) {
        ExchangeInfo info = new ExchangeInfo(Action.ORDER_DETAILS, symbol, access, secret);
        info.orderId = orderId;
        return info;
    }

    public static ExchangeInfo orderDetailAll(String symbol, String access, String secret) {
        ExchangeInfo info = new ExchangeInfo(Action.ORDER_DETAILS, symbol, access, secret);
        return info;
    }

    // 推送接口

    public static ExchangeInfo onTicker(String symbol, String access, String secret, String pushId) {
        return new ExchangeInfo(Action.ON_TICKER, symbol, access, secret, pushId);
    }

    public static ExchangeInfo onKlines(String symbol, String access, String secret, Period period,
                                         String pushId) {
        ExchangeInfo info = new ExchangeInfo(Action.ON_KLINES, symbol, access, secret, pushId);
        info.period = period;
        return info;
    }

    public static ExchangeInfo onDepth(String symbol, String access, String secret, String pushId) {
        return new ExchangeInfo(Action.ON_DEPTH, symbol, access, secret, pushId);
    }

    public static ExchangeInfo onTrades(String symbol, String access, String secret, String pushId) {
        return new ExchangeInfo(Action.ON_TRADES, symbol, access, secret, pushId);
    }

    public static ExchangeInfo onAccount(String symbol, String access, String secret, String pushId) {
        return new ExchangeInfo(Action.ON_ACCOUNT, symbol, access, secret, pushId);
    }

    public static ExchangeInfo onOrders(String symbol, String access, String secret, String pushId) {
        return new ExchangeInfo(Action.ON_ORDERS, symbol, access, secret, pushId);
    }

    /**
     * 错误提示
     */
    public String tip() {
        switch (action) {
            case TICKER         : return action + " " + symbol + " " + Seal.seal(access);
            case KLINES        : return action + " " + symbol + " " + Seal.seal(access) + " " + period;
            case DEPTH          : return action + " " + symbol + " " + Seal.seal(access);
            case TRADES         : return action + " " + symbol + " " + Seal.seal(access);

            case BALANCES       : return action + " " + symbol + " " + Seal.seal(access);
            case ACCOUNT        : return action + " " + symbol + " " + Seal.seal(access);

            case PRECISIONS     : return action + " " + symbol + " " + Seal.seal(access);
            case PRECISION      : return action + " " + symbol + " " + Seal.seal(access);
            case BUY_LIMIT      : return action + " " + symbol + " " + Seal.seal(access) + " " + price + " " + amount;
            case SELL_LIMIT     : return action + " " + symbol + " " + Seal.seal(access) + " " + price + " " + amount;
            case BUY_MARKET     : return action + " " + symbol + " " + Seal.seal(access) + " " + quote;
            case SELL_MARKET    : return action + " " + symbol + " " + Seal.seal(access) + " " + base;
            case MULTI_BUY      : return action + " " + symbol + " " + Seal.seal(access) + " " + rows;
            case MULTI_SELL     : return action + " " + symbol + " " + Seal.seal(access) + " " + rows;
            case CANCEL_ORDER   : return action + " " + symbol + " " + Seal.seal(access) + " " + cancelId;
            case CANCEL_ORDERS  : return action + " " + symbol + " " + Seal.seal(access) + " " + cancelIds;

            case ORDERS         : return action + " " + symbol + " " + Seal.seal(access);
            case HISTORY_ORDERS : return action + " " + symbol + " " + Seal.seal(access);
            case ORDER          : return action + " " + symbol + " " + Seal.seal(access) + " " + orderId;
            case ORDER_DETAILS  : return action + " " + symbol + " " + Seal.seal(access) + " " + orderId;

            case ON_TICKER      : return action + " " + symbol + " " + Seal.seal(access) + " " + pushId;
            case ON_KLINES     : return action + " " + symbol + " " + Seal.seal(access) + " " + period + " " + pushId;
            case ON_DEPTH       : return action + " " + symbol + " " + Seal.seal(access) + " " + pushId;
            case ON_TRADES      : return action + " " + symbol + " " + Seal.seal(access) + " " + pushId;
            case ON_ACCOUNT     : return action + " " + symbol + " " + Seal.seal(access) + " " + pushId;
            case ON_ORDERS      : return action + " " + symbol + " " + Seal.seal(access) + " " + pushId;

            default:
        }
        return null;
    }

    public ExchangeInfo setPrice(String price) {
        this.price = price;
        return this;
    }

    public ExchangeInfo setAmount(String amount) {
        this.amount = amount;
        return this;
    }

    public ExchangeInfo setCancelId(String cancelId) {
        this.cancelId = cancelId;
        return this;
    }

    @Override
    public final ExchangeInfo clone() {
        try {
            return (ExchangeInfo) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return new ExchangeInfo(this.action, this.symbol, this.access, this.secret);
    }
}
