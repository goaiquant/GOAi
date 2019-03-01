package cqt.goai.run.exchange;

import com.alibaba.fastjson.JSON;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.exchange.http.HttpExchange;
import cqt.goai.exchange.util.RateLimit;
import cqt.goai.exchange.util.Seal;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.*;
import cqt.goai.run.exchange.model.LogAction;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

/**
 * @author GOAi
 */
public class ProxyExchange implements Exchange {

    /**
     * 被代理的交易所
     */
    private Exchange e;

    /**
     * 日志
     */
    private Logger log;

    public ProxyExchange(Exchange exchange) {
        this.e = exchange;
        this.log = this.e.getLog();
    }

    @Override
    public ExchangeName getExchangeName() {
        return this.e.getExchangeName();
    }

    @Override
    public String getName() {
        return this.e.getName();
    }

    @Override
    public String getSymbol() {
        return this.e.getSymbol();
    }

    @Override
    public String getAccess() {
        return this.e.getAccess();
    }

    @Override
    public String getSecret() {
        return this.e.getSecret();
    }

    @Override
    public Logger getLog() {
        return this.e.getLog();
    }

    @Override
    public HttpExchange getHttpExchange() {
        return this.e.getHttpExchange();
    }

    @Override
    public void destroy() {
        this.run(this.e::destroy);
    }

    @Override
    public BigDecimal checkCount(BigDecimal count, boolean ceil) {
        return this.get(this.e::checkCount, count, ceil);
    }

    @Override
    public BigDecimal checkBase(BigDecimal base) {
        return this.get(this.e::checkBase, base);
    }

    @Override
    public List<Row> checkRows(List<Row> rows, boolean ceil) {
        return this.get(this.e::checkRows, rows, ceil);
    }

    // =================== market =======================

    @Override
    public Ticker getTicker() {
        return this.get(this.e::getTicker);
    }

    @Override
    public Ticker getTicker(boolean latest) {
        return this.get(this.e::getTicker, latest);
    }

    @Override
    public Klines getKlines() {
        return this.get(this.e::getKlines);
    }

    @Override
    public Klines getKlines(Period period) {
        return this.get(this.e::getKlines, period);
    }

    @Override
    public Klines getKlines(boolean latest) {
        return this.get(this.e::getKlines, latest);
    }

    @Override
    public Klines getKlines(Period period, boolean latest) {
        return this.get(this.e::getKlines, period, latest);
    }

    @Override
    public Depth getDepth() {
        return this.get(this.e::getDepth);
    }

    @Override
    public Depth getDepth(boolean latest) {
        return this.get(this.e::getDepth, latest);
    }

    @Override
    public Trades getTrades() {
        return this.get(this.e::getTrades);
    }

    @Override
    public Trades getTrades(boolean latest) {
        return this.get(this.e::getTrades, latest);
    }

    // =================== account =======================

    @Override
    public Balances getBalances() {
        return this.get(this.e::getBalances);
    }

    @Override
    public Account getAccount() {
        return this.get(this.e::getAccount);
    }

    @Override
    public Account getAccount(boolean latest) {
        return this.get(this.e::getAccount, latest);
    }

    // =================== trade =======================

    @Override
    public Precisions getPrecisions() {
        return this.get(this.e::getPrecisions);
    }

    @Override
    public Precision getPrecision() {
        return this.get(this.e::getPrecision);
    }

    @Override
    public String buyLimit(BigDecimal price, BigDecimal amount) {
        Exception error = null;
        try {
            price = this.e.checkCount(price, false);
            amount = this.e.checkBase(amount);
        } catch (Exception e) {
            error = e;
        }
        if (null != error || null == price || null == amount) {
            this.log.error("{} {} {} buy limit: price: {} amount:{} error: {}",
                    this.getName(), this.getSymbol(), Seal.seal(this.getAccess()),
                    price, amount, error);
            return null;
        }

        LogAction la = LogAction.buyLimit(this.getName(), this.getSymbol(),
                Seal.seal(this.getAccess()), price, amount);
        return this.single(this.e::buyLimit, price, amount, la);
    }

    @Override
    public String sellLimit(BigDecimal price, BigDecimal amount) {
        Exception error = null;
        try {
            price = this.e.checkCount(price, true);
            amount = this.e.checkBase(amount);
        } catch (Exception e) {
            error = e;
        }
        if (null != error || null == price || null == amount) {
            this.log.error("{} {} {} sell limit: price: {} amount:{} error: {}",
                    this.getName(), this.getSymbol(), Seal.seal(this.getAccess()),
                    price, amount, error);
            return null;
        }

        LogAction la = LogAction.sellLimit(this.getName(), this.getSymbol(),
                Seal.seal(this.getAccess()), price, amount);
        return this.single(this.e::sellLimit, price, amount, la);
    }

    @Override
    public String buyMarket(BigDecimal count) {
        Exception error = null;
        try {
            count = this.e.checkCount(count, false);
        } catch (Exception e) {
            error = e;
        }
        if (null != error || null == count) {
            this.log.error("{} {} {} buy market: quote: {} error: {}",
                    this.getName(), this.getSymbol(), Seal.seal(this.getAccess()),
                    count, error);
            return null;
        }

        LogAction la = LogAction.buyMarket(this.getName(), this.getSymbol(),
                Seal.seal(this.getAccess()), count);
        return this.single(this.e::buyMarket, count, la);
    }

    @Override
    public String sellMarket(BigDecimal base) {
        Exception error = null;
        try {
            base = this.e.checkBase(base);
        } catch (Exception e) {
            error = e;
        }
        if (null != error || null == base) {
            this.log.error("{} {} {} sell market: base: {} error: {}",
                    this.getName(), this.getSymbol(), Seal.seal(this.getAccess()),
                    base, error);
            return null;
        }

        LogAction la = LogAction.sellMarket(this.getName(), this.getSymbol(),
                Seal.seal(this.getAccess()), base);
        return this.single(this.e::sellMarket, base, la);
    }

    @Override
    public List<String> multiBuy(List<Row> bids) {
        Exception error = null;
        try {
            bids = this.e.checkRows(bids, false);
        } catch (Exception e) {
            error = e;
        }
        if (null != error || null == bids || bids.isEmpty()) {
            this.log.error("{} {} {} multi buy: bids: {} error: {}",
                    this.getName(), this.getSymbol(), Seal.seal(this.getAccess()),
                    bids, error);
            return null;
        }

        LogAction la = LogAction.multiBuy(this.getName(), this.getSymbol(),
                Seal.seal(this.getAccess()), bids);
        return this.multi(this.e::multiBuy, bids, la);
    }

    @Override
    public List<String> multiSell(List<Row> asks) {
        Exception error = null;
        try {
            asks = this.e.checkRows(asks, true);
        } catch (Exception e) {
            error = e;
        }
        if (null != error || null == asks || asks.isEmpty()) {
            this.log.error("{} {} {} multi sell: asks: {} error: {}",
                    this.getName(), this.getSymbol(), Seal.seal(this.getAccess()),
                    asks, error);
            return null;
        }

        LogAction la = LogAction.multiSell(this.getName(), this.getSymbol(),
                Seal.seal(this.getAccess()), asks);
        return this.multi(this.e::multiSell, asks, la);
    }

    @Override
    public boolean cancelOrder(String id) {
        Boolean result = this.get(this.e::cancelOrder, id);
        if (null != result) {
            return result;
        }
        return false;
    }

    @Override
    public List<String> cancelOrders(List<String> ids) {
        return this.get(this.e::cancelOrders, ids);
    }

    // =================== order =======================

    @Override
    public Orders getOrders() {
        return this.get(this.e::getOrders);
    }

    @Override
    public Orders getHistoryOrders() {
        return this.get(this.e::getHistoryOrders);
    }

    @Override
    public Order getOrder(String id) {
        return this.get(this.e::getOrder, id);
    }

    @Override
    public OrderDetails getOrderDetails(String id) {
        return this.get(this.e::getOrderDetails, id);
    }

    @Override
    public OrderDetails getOrderDetailAll() {
        return this.get(this.e::getOrderDetailAll);
    }

    // =================== push =======================

    @Override
    public void setOnTicker(Consumer<Ticker> onTicker) {
        this.run(this.e::setOnTicker, onTicker);
    }

    @Override
    public void setOnTicker(Consumer<Ticker> onTicker, RateLimit limit) {
        this.run(this.e::setOnTicker, onTicker, limit);
    }

    @Override
    public void setOnKlines(Consumer<Klines> onKlines) {
        this.run(this.e::setOnKlines, onKlines);
    }

    @Override
    public void setOnKlines(Consumer<Klines> onKlines, Period period, RateLimit limit) {
        this.run(this.e::setOnKlines, onKlines, period, limit);
    }

    @Override
    public void setOnDepth(Consumer<Depth> onDepth) {
        this.run(this.e::setOnDepth, onDepth);
    }

    @Override
    public void setOnDepth(Consumer<Depth> onDepth, RateLimit limit) {
        this.run(this.e::setOnDepth, onDepth, limit);
    }

    @Override
    public void setOnTrades(Consumer<Trades> onTrades) {
        this.run(this.e::setOnTrades, onTrades);
    }

    @Override
    public void setOnTrades(Consumer<Trades> onTrades, RateLimit limit) {
        this.run(this.e::setOnTrades, onTrades, limit);
    }

    @Override
    public void setOnAccount(Consumer<Account> onAccount) {
        this.run(this.e::setOnAccount, onAccount);
    }

    @Override
    public void setOnOrders(Consumer<Orders> onOrders) {
        this.run(this.e::setOnOrders, onOrders);
    }


    // =================== tools =======================

    @Override
    public String toString() {
        return "ProxyExchange{" +
                "e=" + e +
                '}';
    }

    private void run(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception ignored) { }
    }

    private <T> void run(Consumer<T> consumer, T t) {
        try {
            consumer.accept(t);
        } catch (Exception ignored) { }
    }

    private <T, K> void run(BiConsumer<T, K> consumer, T t, K k) {
        try {
            consumer.accept(t, k);
        } catch (Exception ignored) { }
    }

    private <T, U, K> void run(TrConsumer<T, U, K> consumer, T t, U u, K k) {
        try {
            consumer.accept(t, u, k);
        } catch (Exception ignored) { }
    }

    private <R> R get(Supplier<R> supplier) {
        try {
            return supplier.get();
        } catch (Exception ignored) { }
        return null;
    }

    private <T, R> R get(Function<T, R> function, T t) {
        try {
            return function.apply(t);
        } catch (Exception ignored) { }
        return null;
    }

    private <T, K, R> R get(BiFunction<T, K, R> function, T t, K k) {
        try {
            return function.apply(t, k);
        } catch (Exception ignored) { }
        return null;
    }

    private <T, U, K, L, R> R get(FourFunction<T, U, K, L, R> function, T t, U u, K k, L l) {
        try {
            return function.apply(t, u, k, l);
        } catch (Exception ignored) { }
        return null;
    }

    private List<String> multi(Function<List<Row>, List<String>> multi,
                               List<Row> rows, LogAction la) {
        List<String> ids = this.get(multi, rows);
        if (null != ids) {
            la.setIds(new ArrayList<>(ids));
        }
        this.log.info("{} {}", la.getAction(), JSON.toJSONString(la));
        return ids;
    }

    private String single(Function<BigDecimal, String> single, BigDecimal number, LogAction la) {
        String id = this.get(single, number);
        return this.checkId(id, la);
    }

    private String checkId(String id, LogAction la) {
        if (null != id) {
            la.setId(id);
        }
        this.log.info("{} {}", la.getAction(), JSON.toJSONString(la));
        return id;
    }

    private String single(BiFunction<BigDecimal, BigDecimal, String> single,
                          BigDecimal price, BigDecimal amount, LogAction la) {
        String id = this.get(single, price, amount);
        return this.checkId(id, la);
    }

}
