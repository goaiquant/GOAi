package cqt.goai.run.exchange;

import cqt.goai.exchange.ExchangeName;
import cqt.goai.exchange.http.HttpExchange;
import cqt.goai.exchange.util.RateLimit;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.*;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

/**
 * 交易所接口
 * @author GOAi
 */
public interface Exchange {

    /**
     * 所属交易所信息
     * @return 交易所信息
     */
    ExchangeName getExchangeName();

    /**
     * 交易所名
     * @return 交易所名
     */
    String getName();

    /**
     * 币对
     * @return 币对
     */
    String getSymbol();

    /**
     * access
     * @return access
     */
    String getAccess();

    /**
     * secret
     * @return secret
     */
    String getSecret();

    /**
     * 日志
     * @return 日志
     */
    Logger getLog();

    /**
     * 获取http请求对象 方便调用特殊api
     * @return HttpExchange
     */
    HttpExchange getHttpExchange();

    /**
     * 取消所有订阅
     */
    void destroy();

    /**
     * 检查价格
     * @param count 价格
     * @param ceil 取整方向
     * @return 调整后
     */
    BigDecimal checkCount(BigDecimal count, boolean ceil);

    /**
     * 检查数量
     * @param base 数量
     * @return 调整后
     */
    BigDecimal checkBase(BigDecimal base);

    /**
     * 检查多个精度
     * @param rows 多个
     * @param ceil 取整方向
     * @return 调整后
     */
    List<Row> checkRows(List<Row> rows, boolean ceil);

    // =================== ticker =======================

    /**
     * 获取Ticker
     * @return Ticker
     */
    Ticker getTicker();

    /**
     * 获取Ticker
     * @param latest http方式, false -> 允许不是最新的 true -> 必须最新的
     * @return Ticker
     */
    Ticker getTicker(boolean latest);

    // =================== klines =======================

    /**
     * 获取Klines
     * @return Klines
     */
    Klines getKlines();

    /**
     * 获取Klines 默认可以取不是最新的
     * @param period K线周期
     * @return Klines
     */
    Klines getKlines(Period period);

    /**
     * 获取Klines 默认周期为MIN1
     * @param latest http方式, 允许不是最新的
     * @return Klines
     */
    Klines getKlines(boolean latest);

    /**
     * 获取Klines
     * @param period K线周期
     * @param latest http方式, 允许不是最新的
     * @return Klines
     */
    Klines getKlines(Period period, boolean latest);

    // =================== Depth =======================

    /**
     * 获取Depth
     * @return Depth
     */
    Depth getDepth();

    /**
     * 获取Depth
     * @param latest http方式, 允许不是最新的
     * @return Depth
     */
    Depth getDepth(boolean latest);

    // =================== trades =======================

    /**
     * 获取Trades
     * @return Trades
     */
    Trades getTrades();

    /**
     * 获取Trades
     * @param latest http方式, 允许不是最新的
     * @return Trades
     */
    Trades getTrades(boolean latest);

    // =================== account =======================

    /**
     * 获取账户所有余额
     * @return Balances
     */
    Balances getBalances();

    /**
     * 获取Account
     * @return Account
     */
    Account getAccount();

    /**
     * 获取Account
     * @param latest http方式, 允许不是最新的
     * @return Account
     */
    Account getAccount(boolean latest);

    // =================== trade =======================

    /**
     * 获取所有币对精度信息
     * @return Precisions
     */
    Precisions getPrecisions();

    /**
     * 获取币对精度信息
     * @return Precision
     */
    Precision getPrecision();

    /**
     * 限价买
     * 自动结算精度，价格向下截断，数量向下截断
     * @param price 订单价格
     * @param amount 订单数量
     * @return 订单id
     */
    String buyLimit(BigDecimal price, BigDecimal amount);

    /**
     * 限价买
     * 自动结算精度，价格向下截断，数量向下截断
     * @param price 订单价格
     * @param amount 订单数量
     * @return 订单id
     */
    default String buyLimit(double price, double amount) {
        return this.buyLimit(new BigDecimal(price), new BigDecimal(amount));
    }

    /**
     * 限价卖
     * 自动结算精度，价格向上截断，数量向上截断
     * @param price 订单价格
     * @param amount 订单数量
     * @return 订单id
     */
    String sellLimit(BigDecimal price, BigDecimal amount);

    /**
     * 限价卖
     * 自动结算精度，价格向上截断，数量向上截断
     * @param price 订单价格
     * @param amount 订单数量
     * @return 订单id
     */
    default String sellLimit(double price, double amount) {
        return this.sellLimit(new BigDecimal(price), new BigDecimal(amount));
    }

    /**
     * 市价买
     * 自动结算精度，价格向下截断
     * @param count 订单总价
     * @return 订单id
     */
    String buyMarket(BigDecimal count);

    /**
     * 市价卖
     * 自动结算精度，数量向上截断
     * @param base 订单数量
     * @return 订单id
     */
    String sellMarket(BigDecimal base);

    /**
     * 多买
     * @param bids 多对买单
     * @return 订单id
     */
    List<String> multiBuy(List<Row> bids);

    /**
     * 多卖
     * @param asks 多对卖单
     * @return 订单id
     */
    List<String> multiSell(List<Row> asks);

    /**
     * 取消单个订单
     * @param id 订单id
     * @return 取消成功与否
     */
    boolean cancelOrder(String id);

    /**
     * 取消多个订单
     * @param ids 多个订单id
     * @return 成功取消的订单id
     */
    List<String> cancelOrders(List<String> ids);

    // =================== order =======================

    /**
     * 获取活跃订单
     * @return Orders
     */
    Orders getOrders();

    /**
     * 获取历史订单
     * @return Orders
     */
    Orders getHistoryOrders();


    /**
     * 获取历史订单
     * @param id 订单id
     * @return Order
     */
    Order getOrder(String id);

    /**
     * 获取订单成交明细
     * @param id 订单id
     * @return OrderDetails
     */
    OrderDetails getOrderDetails(String id);

    /**
     * 获取所有订单成交明细
     * @return getOrderDetailAll
     */
    OrderDetails getOrderDetailAll();

    // =================== push =======================

    /**
     * 设置onTicker, 默认无频率限制
     * @param onTicker 消费函数
     */
    void setOnTicker(Consumer<Ticker> onTicker);

    /**
     * 设置onTicker
     * @param onTicker 消费函数
     * @param limit 推送频率限制
     */
    void setOnTicker(Consumer<Ticker> onTicker, RateLimit limit);

    /**
     * 设置onTicker, 默认1MIN, 频率无频率限制
     * @param onKlines 消费函数
     */
    void setOnKlines(Consumer<Klines> onKlines);

    /**
     * 设置onTicker
     * @param onKlines 消费函数
     * @param period 推送K线周期
     * @param limit 频率限制
     */
    void setOnKlines(Consumer<Klines> onKlines, Period period, RateLimit limit);

    /**
     * 设置onDepth, 默认无频率限制
     * @param onDepth 消费函数
     */
    void setOnDepth(Consumer<Depth> onDepth);

    /**
     * 设置onDepth
     * @param onDepth 消费函数
     * @param limit 推送频率限制
     */
    void setOnDepth(Consumer<Depth> onDepth, RateLimit limit);

    /**
     * 设置onTrades, 默认无频率限制
     * @param onTrades 消费函数
     */
    void setOnTrades(Consumer<Trades> onTrades);

    /**
     * 设置onTrades
     * @param onTrades 消费函数
     * @param limit 推送频率限制
     */
    void setOnTrades(Consumer<Trades> onTrades, RateLimit limit);

    /**
     * 设置onAccount, 默认无频率限制
     * @param onAccount 消费函数
     */
    void setOnAccount(Consumer<Account> onAccount);

    /**
     * 设置onOrders, 默认无频率限制
     * @param onOrders 消费函数
     */
    void setOnOrders(Consumer<Orders> onOrders);


    /**
     * 获取Exchange json
     * @param name 交易所名
     * @param symbol 币对
     * @param access access
     * @param secret secret
     * @return 交易所json
     */
    static String getExchangeJson(String name, String symbol, String access, String secret) {
        return String.format("{\"name\":\"%s\",\"symbol\":\"%s\",\"access\":\"%s\",\"secret\":\"%s\"}",
                name, symbol, access, secret);
    }

}
