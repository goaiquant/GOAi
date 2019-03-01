package cqt.goai.run.exchange;

import cqt.goai.exchange.ExchangeException;
import cqt.goai.exchange.ExchangeInfo;
import cqt.goai.exchange.ExchangeManager;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.exchange.http.HttpExchange;
import cqt.goai.exchange.util.RateLimit;
import cqt.goai.exchange.util.Seal;
import cqt.goai.exchange.web.socket.WebSocketExchange;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.*;
import cqt.goai.run.exchange.model.ModelManager;
import cqt.goai.run.exchange.model.ModelObserver;
import cqt.goai.run.exchange.model.TradesObserver;
import dive.common.math.RandomUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 完全本地自主连接的Exchange
 * @author GOAi
 */
public class LocalExchange extends BaseExchange {

    /**
     * http方式
     */
    private HttpExchange httpExchange;
    /**
     * websocket方式
     */
    private WebSocketExchange webSocketExchange;

    public LocalExchange(Logger log, ExchangeName name, String symbol, String access, String secret,
                         Supplier<Boolean> ready) {
        super(log, name, symbol, access, secret, ready);
        this.httpExchange = ExchangeManager.getHttpExchange(name, log);
    }

    @Override
    public HttpExchange getHttpExchange() {
        return this.httpExchange;
    }

    /**
     * 检查WebSocketExchange
     */
    private void checkWebSocketExchange() {
        if (null == this.webSocketExchange) {
            synchronized (LocalExchange.class) {
                if (null == this.webSocketExchange) {
                    this.webSocketExchange = ExchangeManager.getWebSocketExchange(super.name, super.log);
                }
            }
        }
    }

    @Override
    public void destroy() {
        if (null == this.webSocketExchange) {
            return;
        }
        super.ticker.getObservers().values().forEach(o -> this.webSocketExchange.noTicker(o.getId()));
        super.klines.forEach((p, c) -> c.getObservers().values()
                        .forEach(o -> this.webSocketExchange.noKlines(p, o.getId())));
        super.depth.getObservers().values().forEach(o -> this.webSocketExchange.noDepth(o.getId()));
        super.trades.getObservers().values().forEach(o -> this.webSocketExchange.noTrades(o.getId()));
        super.account.getObservers().values().forEach(o -> this.webSocketExchange.noAccount(o.getId()));
        super.orders.getObservers().values().forEach(o -> this.webSocketExchange.noOrders(o.getId()));
    }

    @Override
    public Ticker getTicker(boolean latest) {
        if (super.ticker.on()) {
            // 如果有推送ticker 直接返回缓存的
            return super.ticker.getModel();
        } else {
            if (!latest && !super.ticker.getLimit().timeout(false)) {
                // 如果允许不是最新的，并且没有超时（3s） 可以返回缓存的
                return super.ticker.getModel();
            }
            // http 方式获取最新的
            Ticker ticker = this.httpExchange.getTicker(
                    ExchangeInfo.ticker(super.symbol, super.access, super.secret));
            // 更新缓存
            super.ticker.update(ticker);
            return ticker;
        }
    }

    @Override
    public Klines getKlines(Period period, boolean latest) {
        if (null == period) {
            period = Period.MIN1;
        }
        ModelManager<Klines> manager = super.klines.computeIfAbsent(period,
                p -> new ModelManager<>(RateLimit.second3()));
        if (manager.on()) {
            // 如果有推送klines 直接返回缓存的
            return manager.getModel();
        } else {
            if (!latest && !manager.getLimit().timeout(false)) {
                // 如果允许不是最新的，并且没有超时（3s） 可以返回缓存的
                return manager.getModel();
            }
            // http 方式获取最新的
            Klines klines = this.httpExchange.getKlines(
                    ExchangeInfo.klines(super.symbol, super.access, super.secret, period));
            // 更新缓存
            manager.update(klines);
            return klines;
        }
    }

    @Override
    public Depth getDepth(boolean latest) {
        if (super.depth.on()) {
            // 如果有推送depth 直接返回缓存的
            return super.depth.getModel();
        } else {
            if (!latest && !super.depth.getLimit().timeout(false)) {
                // 如果允许不是最新的，并且没有超时（3s） 可以返回缓存的
                return super.depth.getModel();
            }
            // http 方式获取最新的
            Depth depth = this.httpExchange.getDepth(
                    ExchangeInfo.depth(super.symbol, super.access, super.secret));
            // 更新缓存
            super.depth.update(depth);
            return depth;
        }
    }

    @Override
    public Trades getTrades(boolean latest) {
        if (super.trades.on()) {
            // 如果有推送trades 直接返回缓存的
            return super.trades.getModel();
        } else {
            if (!latest && !super.trades.getLimit().timeout(false)) {
                // 如果允许不是最新的，并且没有超时（3s） 可以返回缓存的
                return super.trades.getModel();
            }
            // http 方式获取最新的
            Trades trades = this.httpExchange.getTrades(
                    ExchangeInfo.trades(super.symbol, super.access, super.secret));
            // 更新缓存
            super.trades.update(trades);
            return trades;
        }
    }


    @Override
    public Balances getBalances() {
        return this.httpExchange.getBalances(
                ExchangeInfo.balances(super.symbol, super.access, super.secret));
    }

    @Override
    public Account getAccount(boolean latest) {
        if (super.account.on()) {
            // 如果有推送account 直接返回缓存的
            return super.account.getModel();
        } else {
            if (!latest && !super.account.getLimit().timeout(false)) {
                // 如果允许不是最新的，并且没有超时（3s） 可以返回缓存的
                return super.account.getModel();
            }
            // http 方式获取最新的
            Account account = this.httpExchange.getAccount(
                    ExchangeInfo.account(super.symbol, super.access, super.secret));
            // 更新缓存
            super.account.update(account);
            return account;
        }
    }


    @Override
    public Precisions getPrecisions() {
        return this.httpExchange.getPrecisions(
                ExchangeInfo.precisions(super.symbol, super.access, super.secret));
    }

    @Override
    public Precision getPrecision() {
        if (null == super.precision) {
            synchronized (this) {
                if (null == super.precision) {
                    super.precision = this.httpExchange.getPrecision(
                            ExchangeInfo.precision(super.symbol, super.access, super.secret));
                }
            }
        }
        return super.precision;
    }

    @Override
    public String buyLimit(BigDecimal price, BigDecimal amount) {
        price = super.checkCount(price, false);
        amount = super.checkBase(amount);
        return this.httpExchange.buyLimit(
                ExchangeInfo.buyLimit(super.symbol, super.access, super.secret,
                        price.toPlainString(), amount.toPlainString()));
    }

    @Override
    public String sellLimit(BigDecimal price, BigDecimal amount) {
        price = super.checkCount(price, true);
        amount = super.checkBase(amount);
        return this.httpExchange.sellLimit(
                ExchangeInfo.sellLimit(super.symbol, super.access, super.secret,
                        price.toPlainString(), amount.toPlainString()));
    }

    @Override
    public String buyMarket(BigDecimal count) {
        count = super.checkCount(count, false);
        return this.httpExchange.buyMarket(
                ExchangeInfo.buyMarket(super.symbol, super.access, super.secret,
                        count.toPlainString()));
    }

    @Override
    public String sellMarket(BigDecimal base) {
        base = super.checkBase(base);
        return this.httpExchange.sellMarket(
                ExchangeInfo.sellMarket(super.symbol, super.access, super.secret,
                        base.toPlainString()));
    }

    @Override
    public List<String> multiBuy(List<Row> bids) {
        bids = super.checkRows(bids, false);
        if (bids.isEmpty()) {
            return Collections.emptyList();
        }
        return this.httpExchange.multiBuy(
                ExchangeInfo.multiBuy(super.symbol, super.access, super.secret, bids));
    }

    @Override
    public List<String> multiSell(List<Row> asks) {
        asks = super.checkRows(asks, true);
        if (asks.isEmpty()) {
            return Collections.emptyList();
        }
        return this.httpExchange.multiSell(
                ExchangeInfo.multiSell(super.symbol, super.access, super.secret, asks));
    }

    @Override
    public boolean cancelOrder(String id) {
        return this.httpExchange.cancelOrder(
                ExchangeInfo.cancelOrder(super.symbol, super.access, super.secret, id));
    }

    @Override
    public List<String> cancelOrders(List<String> ids) {
        if (null == ids || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return this.httpExchange.cancelOrders(
                ExchangeInfo.cancelOrders(super.symbol, super.access, super.secret, ids));
    }


    @Override
    public Orders getOrders() {
        return this.httpExchange.getOrders(
                ExchangeInfo.orders(super.symbol, super.access, super.secret));
    }

    @Override
    public Orders getHistoryOrders() {
        return this.httpExchange.getHistoryOrders(
                ExchangeInfo.historyOrders(super.symbol, super.access, super.secret));
    }

    @Override
    public Order getOrder(String id) {
        return this.httpExchange.getOrder(
                ExchangeInfo.order(super.symbol, super.access, super.secret, id));
    }

    @Override
    public OrderDetails getOrderDetails(String id) {
        return this.httpExchange.getOrderDetails(
                ExchangeInfo.orderDetails(super.symbol, super.access, super.secret, id));
    }

    @Override
    public OrderDetails getOrderDetailAll() {
        return this.httpExchange.getOrderDetailAll(
                ExchangeInfo.orderDetailAll(super.symbol, super.access, super.secret));
    }

    @Override
    public void setOnTicker(Consumer<Ticker> onTicker, RateLimit limit) {
        checkWebSocketExchange();
        ModelObserver<Ticker> mo = new ModelObserver<>(onTicker, limit, RandomUtil.token());
        boolean success = this.webSocketExchange.onTicker(
                ExchangeInfo.onTicker(super.symbol, super.access, super.secret, mo.getId()),
                ticker -> super.onTicker(ticker, mo.getId()));
        if (!success) {
            throw new ExchangeException("can not register ticker: " + super.getName() + " "
                    + super.getSymbol() + " " + Seal.seal(super.getAccess()));
        }
        super.ticker.observe(mo);
        log.info("observe successful! {} {} {} {}", name.getName(), symbol, "onTicker", limit);
    }

    @Override
    public void setOnKlines(Consumer<Klines> onKlines, Period period, RateLimit limit) {
        checkWebSocketExchange();
        ModelObserver<Klines> mo = new ModelObserver<>(onKlines, limit, RandomUtil.token());
        boolean success = this.webSocketExchange.onKlines(
                ExchangeInfo.onKlines(super.symbol, super.access, super.secret, period, mo.getId()),
                klines -> super.onKlines(period, klines, mo.getId()));
        if (!success) {
            throw new ExchangeException("can not register klines: " + super.getName() + " "
                    + super.getSymbol() + " " + Seal.seal(super.getAccess()));
        }
        super.klines.computeIfAbsent(period, p -> new ModelManager<>(RateLimit.second3()))
                .observe(mo);
        log.info("observe successful! {} {} {} {}", name.getName(), symbol, "onKlines", period);
    }

    @Override
    public void setOnDepth(Consumer<Depth> onDepth, RateLimit limit) {
        checkWebSocketExchange();
        ModelObserver<Depth> mo = new ModelObserver<>(onDepth, limit, RandomUtil.token());
        boolean success = this.webSocketExchange.onDepth(
                ExchangeInfo.onDepth(super.symbol, super.access, super.secret, mo.getId()),
                depth -> super.onDepth(depth, mo.getId()));
        if (!success) {
            throw new ExchangeException("can not register depth: " + super.getName() + " "
                    + super.getSymbol() + " " + Seal.seal(super.getAccess()));
        }
        super.depth.observe(mo);
        log.info("observe successful! {} {} {} {}", name.getName(), symbol, "onDepth", limit);
    }

    @Override
    public void setOnTrades(Consumer<Trades> onTrades, RateLimit limit) {
        checkWebSocketExchange();
        TradesObserver to = new TradesObserver(onTrades, limit, RandomUtil.token(), 200);
        boolean success = this.webSocketExchange.onTrades(
                ExchangeInfo.onTrades(super.symbol, super.access, super.secret, to.getId()),
                trades -> super.onTrades(trades, to.getId()));
        if (!success) {
            throw new ExchangeException("can not register trades: " + super.getName() + " "
                    + super.getSymbol() + " " + Seal.seal(super.getAccess()));
        }
        super.trades.observe(to);
        log.info("observe successful! {} {} {} {}", name.getName(), symbol, "onTrades", limit);
    }

    @Override
    public void setOnAccount(Consumer<Account> onAccount) {
        checkWebSocketExchange();
        ModelObserver<Account> mo = new ModelObserver<>(onAccount, RateLimit.NO, RandomUtil.token());
        boolean success = this.webSocketExchange.onAccount(
                ExchangeInfo.onAccount(super.symbol, super.access, super.secret, mo.getId()),
                account -> super.onAccount(account, mo.getId()));
        if (!success) {
            throw new ExchangeException("can not register account: " + super.getName() + " "
                    + super.getSymbol() + " " + Seal.seal(super.getAccess()));
        }
        super.account.observe(mo);
        log.info("observe successful! {} {} {} {}", name.getName(), symbol, "onAccount", RateLimit.NO);
    }

    @Override
    public void setOnOrders(Consumer<Orders> onOrders) {
        checkWebSocketExchange();
        ModelObserver<Orders> mo = new ModelObserver<>(onOrders, RateLimit.NO, RandomUtil.token());
        boolean success = this.webSocketExchange.onOrders(
                ExchangeInfo.onOrders(super.symbol, super.access, super.secret, mo.getId()),
                orders -> super.onOrders(orders, mo.getId()));
        if (!success) {
            throw new ExchangeException("can not register orders: " + super.getName() + " "
                    + super.getSymbol() + " " + Seal.seal(super.getAccess()));
        }
        super.orders.observe(mo);
        log.info("observe successful! {} {} {} {}", name.getName(), symbol, "onOrders", RateLimit.NO);
    }


    // ===================== tools ========================

    @Override
    public String toString() {
        return "LocalExchange{" + super.toString() + '}';
    }
}
