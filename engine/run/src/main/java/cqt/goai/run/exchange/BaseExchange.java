package cqt.goai.run.exchange;

import cqt.goai.exchange.ExchangeException;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.exchange.http.HttpExchange;
import cqt.goai.exchange.util.RateLimit;
import cqt.goai.exchange.util.Seal;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.*;
import cqt.goai.run.exchange.model.ModelManager;
import lombok.EqualsAndHashCode;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static dive.common.math.BigDecimalUtil.greater;
import static dive.common.math.BigDecimalUtil.less;
import static dive.common.util.Util.exist;


/**
 * 基本获取交易所信息类，
 *  - 继承该类可直接调用Market和Trade数据，
 *  - 重载方法可实现主动推送（2种方式，主动注册有推送 和 被重载则推送）
 * @author GOAi
 */
@EqualsAndHashCode
public class BaseExchange implements Exchange {
    /**
     * 日志
     */
    protected final Logger log;
    /**
     * 交易所名
     */
    protected final ExchangeName name;
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
     * 是否可以进行主动推送
     */
    protected final Supplier<Boolean> ready;

    /**
     * 缓存的ticker，主动推送情况下，getTicker就直接返回这个
     */
    protected ModelManager<Ticker> ticker = new ModelManager<>(RateLimit.second3());

    protected ConcurrentHashMap<Period, ModelManager<Klines>> klines = new ConcurrentHashMap<>();

    protected ModelManager<Depth> depth = new ModelManager<>(RateLimit.second3());

    protected ModelManager<Trades> trades = new ModelManager<>(RateLimit.second3());

    protected ModelManager<Account> account = new ModelManager<>(RateLimit.second());

    protected ModelManager<Orders> orders = new ModelManager<>(RateLimit.NO);

    protected Precision precision;

    BaseExchange(Logger log, ExchangeName name, String symbol, String access, String secret,
                 Supplier<Boolean> ready) {
        this.log = log;
        this.name = name;
        this.symbol = symbol;
        this.access = access;
        this.secret = secret;
        this.ready = ready;
    }

    @Override
    public ExchangeName getExchangeName() {
        return name;
    }

    @Override
    public String getName() {
        return name.getName();
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public String getAccess() {
        return access;
    }

    @Override
    public String getSecret() {
        return secret;
    }

    @Override
    public Logger getLog() {
        return log;
    }

    @Override
    public HttpExchange getHttpExchange() {
        throw new ExchangeException("getHttpExchange is not supported.");
    }

    /**
     * 取消所有订阅
     */
    @Override
    public void destroy() {
        throw new ExchangeException("destroy is not supported.");
    }


    /**
     * 检查目标币精度
     * @param count 被检查的值
     * @param ceil 是否向上取整
     * @return 检查后的值
     */
    @Override
    public BigDecimal checkCount(BigDecimal count, boolean ceil) {
        this.checkPrecision();
        if (null == this.precision) {
            return count;
        }
        return this.checkPrecision(count, ceil ? BigDecimal.ROUND_CEILING : BigDecimal.ROUND_FLOOR,
                this.precision.getMinQuote(), this.precision.getMaxQuote(),
                this.precision.getQuoteStep(), this.precision.getQuote());
    }

    /**
     * 检查目标币精度
     * @param base 被检查的值
     * @return 检查后的值
     */
    @Override
    public BigDecimal checkBase(BigDecimal base) {
        this.checkPrecision();
        if (null == this.precision) {
            return base;
        }
        return this.checkPrecision(base, BigDecimal.ROUND_FLOOR,
                this.precision.getMinBase(), this.precision.getMaxBase(),
                this.precision.getBaseStep(), this.precision.getBase());
    }

    /**
     * 检查精度
     * @param value 被检查数字
     * @param roundingMode 精度不对时处理方式
     * @param min 最小值
     * @param max 最大值
     * @param step 最小递增精度
     * @param precision 最小精度
     * @return 检查后结果
     */
    private BigDecimal checkPrecision(BigDecimal value, int roundingMode,
                                      BigDecimal min, BigDecimal max,
                                      BigDecimal step, Integer precision) {
        if (null != min && less(value, min)) {
            String message = String.format("precision is wrong. value is %s but the min require is %s",
                    value, min);
            log.error(message);
            throw new ExchangeException(message);
        }
        if (null != max && greater(value, max)) {
            String message = String.format("precision is wrong. value is %s but the max require is %s",
                    value, max);
            log.error(message);
            throw new ExchangeException(message);
        }
        if (null != step) {
            BigDecimal times = value.divide(step, 32, BigDecimal.ROUND_HALF_UP).stripTrailingZeros();
            int scale = times.scale();
            if (0 < scale) {
                // 没有被整除
                times = times.setScale(0, roundingMode);
                return step.multiply(times);
            } else {
                return value;
            }
        }
        if (null != precision) {
            value = value.setScale(precision, roundingMode);
            return this.checkPrecision(value, roundingMode, min, max, null, null);
        }
        return value;
    }

    /**
     * 本地缓存一份精度数据
     */
    private void checkPrecision() {
        if (null == this.precision) {
            synchronized (HttpExchange.class) {
                if (null == this.precision) {
                    this.precision = this.getPrecision();
                }
            }
        }
    }

    /**
     * 精度调整
     */
    @Override
    public List<Row> checkRows(List<Row> rows, boolean ceil) {
        return rows.stream()
                .map(r -> {
                    BigDecimal price;
                    BigDecimal amount;
                    try {
                        price = this.checkCount(r.getPrice(), ceil);
                        amount = this.checkBase(r.getAmount());
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error(e.getMessage());
                        return Row.row(null, null);
                    }
                    return Row.row(price, amount);
                }).filter(r -> exist(r.getPrice(), r.getAmount()))
                .collect(Collectors.toList());
    }
    // =================== ticker =======================

    /**
     * 获取Ticker
     */
    @Override
    public final Ticker getTicker() {
        return this.getTicker(false);
    }

    /**
     * 获取Ticker
     * @param latest http方式, 允许不是最新的
     */
    @Override
    public Ticker getTicker(boolean latest) {
        throw new ExchangeException("getTicker is not supported. latest:"  + latest);
    }

    // =================== klines =======================

    /**
     * 获取Klines
     */
    @Override
    public final Klines getKlines() {
        return this.getKlines(Period.MIN1, false);
    }

    /**
     * 获取Klines 默认可以取不是最新的
     * @param period K线周期
     */
    @Override
    public Klines getKlines(Period period) {
        return this.getKlines(Period.MIN1, false);
    }

    /**
     * 获取Klines 默认周期为MIN1
     * @param latest http方式, 允许不是最新的
     */
    @Override
    public Klines getKlines(boolean latest) {
        return this.getKlines(Period.MIN1, latest);
    }

    /**
     * 获取Klines
     * @param period K线周期
     * @param latest http方式, 允许不是最新的
     */
    @Override
    public Klines getKlines(Period period, boolean latest) {
        throw new ExchangeException("getKlines is not supported. latest:"  + latest);
    }

    // =================== Depth =======================

    /**
     * 获取Depth
     */
    @Override
    public final Depth getDepth() {
        return this.getDepth(false);
    }

    /**
     * 获取Depth
     * @param latest http方式, 允许不是最新的
     */
    @Override
    public Depth getDepth(boolean latest) {
        throw new ExchangeException("getDepth is not supported. latest:"  + latest);
    }

    // =================== trades =======================

    /**
     * 获取Trades
     */
    @Override
    public final Trades getTrades() {
        return this.getTrades(false);
    }

    /**
     * 获取Trades
     * @param latest http方式, 允许不是最新的
     */
    @Override
    public Trades getTrades(boolean latest) {
        throw new ExchangeException("getTrades is not supported. latest:"  + latest);
    }


    // =================== account =======================

    /**
     * 获取账户所有余额
     */
    @Override
    public Balances getBalances() {
        throw new ExchangeException("getBalances is not supported.");
    }

    /**
     * 获取Account
     */
    @Override
    public final Account getAccount() {
        return this.getAccount(false);
    }

    /**
     * 获取Account
     * @param latest http方式, 允许不是最新的
     */
    @Override
    public Account getAccount(boolean latest) {
        throw new ExchangeException("getAccount is not supported. latest:"  + latest);
    }

    // =================== reminder =======================

    /**
     * 获取所有币对精度信息
     */
    @Override
    public Precisions getPrecisions() {
        throw new ExchangeException("getPrecisions is not supported.");
    }

    /**
     * 获取币对精度信息
     */
    @Override
    public Precision getPrecision() {
        throw new ExchangeException("getPrecision is not supported.");
    }

    /**
     * 限价买
     * 自动结算精度，价格向下截断，数量向下截断
     * @return 订单id
     */
    @Override
    public String buyLimit(BigDecimal price, BigDecimal amount) {
        throw new ExchangeException("buyByLimit is not supported.");
    }

    /**
     * 限价卖
     * 自动结算精度，价格向上截断，数量向上截断
     * @return 订单id
     */
    @Override
    public String sellLimit(BigDecimal price, BigDecimal amount) {
        throw new ExchangeException("sellByLimit is not supported.");
    }

    /**
     * 市价买
     * 自动结算精度，价格向下截断
     * @return 订单id
     */
    @Override
    public String buyMarket(BigDecimal count) {
        throw new ExchangeException("buyByMarket is not supported.");
    }

    /**
     * 市价卖
     * 自动结算精度，数量向上截断
     * @return 订单id
     */
    @Override
    public String sellMarket(BigDecimal base) {
        throw new ExchangeException("sellByMarket is not supported.");
    }

    /**
     * 多买
     * @param bids 多对买单
     * @return 订单id
     */
    @Override
    public List<String> multiBuy(List<Row> bids) {
        throw new ExchangeException("multiBuy is not supported.");
    }

    /**
     * 多卖
     * @param asks 多对卖单
     * @return 订单id
     */
    @Override
    public List<String> multiSell(List<Row> asks) {
        throw new ExchangeException("multiSell is not supported.");
    }

    /**
     * 取消单个订单
     * @param id 订单id
     * @return 取消成功与否
     */
    @Override
    public boolean cancelOrder(String id) {
        throw new ExchangeException("cancelOrder is not supported.");
    }

    /**
     * 取消多个订单
     * @param ids 多个订单id
     * @return 成功取消的订单id
     */
    @Override
    public List<String> cancelOrders(List<String> ids) {
        throw new ExchangeException("cancelOrders is not supported.");
    }



    /**
     * 获取活跃订单
     */
    @Override
    public Orders getOrders() {
        throw new ExchangeException("getOrders is not supported.");
    }

    /**
     * 获取历史订单
     */
    @Override
    public Orders getHistoryOrders() {
        throw new ExchangeException("getHistoryOrders is not supported.");
    }

    /**
     * 获取指定订单
     */
    @Override
    public Order getOrder(String id) {
        throw new ExchangeException("getOrder is not supported.");
    }

    /**
     * 获取订单成交明细
     */
    @Override
    public OrderDetails getOrderDetails(String id) {
        throw new ExchangeException("getOrderDetails is not supported.");
    }

    /**
     * 获取所有订单成交明细
     */
    @Override
    public OrderDetails getOrderDetailAll() {
        throw new ExchangeException("getOrderDetailAll is not supported.");
    }



    // ========================== push ===========================

    /**
     * 设置onTicker, 默认无频率限制
     * @param onTicker 消费函数
     */
    @Override
    public final void setOnTicker(Consumer<Ticker> onTicker) {
        this.setOnTicker(onTicker, RateLimit.NO);
    }

    /**
     * 设置onTicker
     * @param onTicker 消费函数
     * @param limit 推送频率限制
     */
    @Override
    public void setOnTicker(Consumer<Ticker> onTicker, RateLimit limit) {
        throw new ExchangeException("setOnTicker is not supported.");
    }

    /**
     * 更新本地ticker并推送Ticker
     */
    void onTicker(Ticker ticker, String id) {
        this.ticker.on(this.ready.get(), ticker, id);
    }


    /**
     * 设置onTicker, 默认1MIN, 频率无频率限制
     * @param onKlines 消费函数
     */
    @Override
    public final void setOnKlines(Consumer<Klines> onKlines) {
        this.setOnKlines(onKlines, Period.MIN1, RateLimit.NO);
    }

    /**
     * 设置onTicker
     * @param onKlines 消费函数
     * @param period 推送K线周期
     */
    @Override
    public void setOnKlines(Consumer<Klines> onKlines, Period period, RateLimit limit) {
        throw new ExchangeException("setOnKlines is not supported.");
    }

    /**
     * 更新本地ticker并推送Ticker
     */
    void onKlines(Period period, Klines klines, String id) {
        if (this.klines.containsKey(period)) {
            this.klines.get(period).on(this.ready.get(), klines, id);
        }
    }


    /**
     * 设置onDepth, 默认无频率限制
     * @param onDepth 消费函数
     */
    @Override
    public final void setOnDepth(Consumer<Depth> onDepth) {
        this.setOnDepth(onDepth, RateLimit.NO);
    }

    /**
     * 设置onDepth
     * @param onDepth 消费函数
     * @param limit 推送频率限制
     */
    @Override
    public void setOnDepth(Consumer<Depth> onDepth, RateLimit limit) {
        throw new ExchangeException("setOnDepth is not supported.");
    }

    /**
     * 更新本地depth并推送depth
     */
    void onDepth(Depth depth, String id) {
        this.depth.on(this.ready.get(), depth, id);
    }


    /**
     * 设置onTrades, 默认无频率限制
     * @param onTrades 消费函数
     */
    @Override
    public final void setOnTrades(Consumer<Trades> onTrades) {
        this.setOnTrades(onTrades, RateLimit.NO);
    }

    /**
     * 设置onTrades
     * @param onTrades 消费函数
     * @param limit 推送频率限制
     */
    @Override
    public void setOnTrades(Consumer<Trades> onTrades, RateLimit limit) {
        throw new ExchangeException("setOnTrades is not supported.");
    }

    /**
     * 更新本地trades并推送trades
     */
    void onTrades(Trades trades, String id) {
        this.trades.on(this.ready.get(), trades, id);
    }


    /**
     * 设置onAccount, 默认无频率限制
     * @param onAccount 消费函数
     */
    @Override
    public void setOnAccount(Consumer<Account> onAccount) {
        throw new ExchangeException("setOnAccount is not supported.");
    }

    /**
     * 更新本地account并推送account
     */
    void onAccount(Account account, String id) {
        this.account.on(this.ready.get(), account, id);
    }


    /**
     * 设置onOrders, 默认无频率限制
     * @param onOrders 消费函数
     */
    @Override
    public void setOnOrders(Consumer<Orders> onOrders) {
        throw new ExchangeException("setOnOrders is not supported.");
    }

    /**
     * 更新本地account并推送account
     */
    void onOrders(Orders orders, String id) {
        this.orders.on(this.ready.get(), orders, id);
    }


    @Override
    public String toString() {
        return "BaseExchange{" +
                "name='" + this.name + '\'' +
                ", symbol='" + this.symbol + '\'' +
                ", access='" + Seal.seal(this.access) + '\'' +
                ", secret='" + Seal.seal(this.secret) + '\'' +
                '}';
    }

}

