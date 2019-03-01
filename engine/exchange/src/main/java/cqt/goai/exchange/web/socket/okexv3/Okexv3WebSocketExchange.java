package cqt.goai.exchange.web.socket.okexv3;

import cqt.goai.exchange.Action;
import cqt.goai.exchange.ExchangeInfo;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.exchange.util.RateLimit;
import cqt.goai.exchange.web.socket.BaseWebSocketClient;
import cqt.goai.exchange.web.socket.WebSocketExchange;
import cqt.goai.exchange.web.socket.WebSocketInfo;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.Klines;
import cqt.goai.model.market.Depth;
import cqt.goai.model.market.Ticker;
import cqt.goai.model.market.Trades;
import cqt.goai.model.trade.Account;
import cqt.goai.model.trade.Orders;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static dive.common.util.Util.exist;


/**
 * @author GOAi
 */
public class Okexv3WebSocketExchange extends WebSocketExchange {

    /**
     * ping的频率限制
     */
    private static final RateLimit LIMIT = RateLimit.limit(1000 * 17);

    /**
     * 订阅id和每个订阅基本信息  id -> info
     */
    private static final ConcurrentHashMap<String, WebSocketInfo> CONSUMERS
            = new ConcurrentHashMap<>();

    /**
     * 同一币对的连接
     */
    private static final ConcurrentHashMap<String, Okexv3WebSocketClient> CLIENTS
            = new ConcurrentHashMap<>();

    /**
     * ticker订阅信息 symbol -> info , Ticker来了，推送Ticker的地方
     */
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketInfo<Ticker>>> TICKERS
            = new ConcurrentHashMap<>();

    /**
     * klines订阅信息 symbol -> (period -> info)
     */
    private static final ConcurrentHashMap<String,
            ConcurrentHashMap<Period, ConcurrentLinkedQueue<WebSocketInfo<Klines>>>> KLINES
            = new ConcurrentHashMap<>();
    /**
     * depth订阅信息 symbol -> info , Depth来了，推送Depth的地方
     */
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketInfo<Depth>>> DEPTH
            = new ConcurrentHashMap<>();

    /**
     * trades订阅信息 symbol -> info , Trades来了，推送Trades的地方
     */
    private static final ConcurrentHashMap<String, ConcurrentLinkedQueue<WebSocketInfo<Trades>>> TRADES
            = new ConcurrentHashMap<>();

    /**
     * 每个实例的连接
     */
    private Okexv3WebSocketClient client;

    /**
     * 每个实例的账户余额推送
     */
    private final ConcurrentLinkedQueue<WebSocketInfo<Account>> accounts
            = new ConcurrentLinkedQueue<>();

    /**
     * 每个实例的订单
     */
    private final ConcurrentLinkedQueue<WebSocketInfo<Orders>> orders
            = new ConcurrentLinkedQueue<>();

    public Okexv3WebSocketExchange(Logger log) {
        super(ExchangeName.OKEXV3, log);
    }

    @Override
    protected void ping() {
        if (!Okexv3WebSocketExchange.LIMIT.timeout()) {
            return;
        }
        // 已经到了ping的时间了，就要遍历每个连接，ping一下
        Okexv3WebSocketExchange.CLIENTS.values().parallelStream().forEach(BaseWebSocketClient::ping);
        if (null != this.client) {
            this.client.ping();
        }
    }

    /**
     * 统一添加订阅
     * @param list 订阅list
     * @param onInfo 订阅信息
     * @param symbol 订阅币对
     */
    private <T> Okexv3WebSocketClient addInfo(ConcurrentLinkedQueue<WebSocketInfo<T>> list,
                                              WebSocketInfo<T> onInfo, String symbol) {
        if (!list.isEmpty()) {
            // 该币对已经有订阅, 加入订阅队列即可
            list.add(onInfo);
        } else { // 无订阅
            list.add(onInfo);
            // 取出这个币对的连接
            Okexv3WebSocketClient client = Okexv3WebSocketExchange.CLIENTS.get(symbol);
            if (!exist(client)) {
                // 该币对无连接，新建一个
                client = new Okexv3WebSocketClient(symbol, super.log);
                Okexv3WebSocketExchange.CLIENTS.put(symbol, client);
            }
            // 对这个连接订阅ticker
            return client;
        }
        return null;
    }

    @Override
    public boolean onTicker(ExchangeInfo info, Consumer<Ticker> onTicker) {
        String symbol = info.getSymbol();
        String access = info.getAccess();

        String id = info.getPushId();
        WebSocketInfo<Ticker> onInfo =
                new WebSocketInfo<>(id, Action.ON_TICKER, symbol, access, onTicker);

        ConcurrentLinkedQueue<WebSocketInfo<Ticker>> list = Okexv3WebSocketExchange.TICKERS
                .computeIfAbsent(symbol, k -> new ConcurrentLinkedQueue<>());
        Okexv3WebSocketClient client = this.addInfo(list, onInfo, symbol);
        if (null != client) {
            // 对这个连接订阅ticker
            client.onTicker(ticker -> list.stream()
                    .parallel()
                    .forEach(i -> i.getConsumer().accept(ticker)));
        }
        // 将这个订阅添加到总记录里
        Okexv3WebSocketExchange.CONSUMERS.put(id, onInfo);
        return true;
    }

    @Override
    public void noTicker(String pushId) {
        WebSocketInfo info = Okexv3WebSocketExchange.CONSUMERS.get(pushId);
        if (null != info) {
            String symbol = info.getSymbol();
            ConcurrentLinkedQueue<WebSocketInfo<Ticker>> list = Okexv3WebSocketExchange.TICKERS
                    .getOrDefault(symbol, null);
            if (null != list) {
                if (list.size() <= 1) {
                    // 这是最后一个订阅，需要取消订阅
                    Okexv3WebSocketClient client = Okexv3WebSocketExchange.CLIENTS.get(symbol);
                    if (null != client) {
                        client.noTicker();
                    }
                }
                list.remove(info);
            }
            Okexv3WebSocketExchange.CONSUMERS.remove(pushId);
        }
    }

    @Override
    public boolean onKlines(ExchangeInfo info, Consumer<Klines> onKlines) {
        String symbol = info.getSymbol();
        String access = info.getAccess();
        Period period = info.getPeriod();
        String id = info.getPushId();

        WebSocketInfo<Klines> onInfo =
                new WebSocketInfo<>(id, Action.ON_KLINES, symbol, access, onKlines, period);

        ConcurrentLinkedQueue<WebSocketInfo<Klines>> list = Okexv3WebSocketExchange.KLINES
                .computeIfAbsent(symbol, k -> new ConcurrentHashMap<>(16))
                .computeIfAbsent(period, p -> new ConcurrentLinkedQueue<>());
        Okexv3WebSocketClient client = this.addInfo(list, onInfo, symbol);
        if (null != client) {
            // 对这个连接订阅klines
            client.onKlines(klines -> list.stream().parallel()
                    .forEach(i -> i.getConsumer().accept(klines)), period);
        }
        // 将这个订阅添加到总记录里
        Okexv3WebSocketExchange.CONSUMERS.put(id, onInfo);
        return true;
    }

    @Override
    public void noKlines(Period period, String pushId) {
        WebSocketInfo info = Okexv3WebSocketExchange.CONSUMERS.get(pushId);
        if (null != info) {
            String symbol = info.getSymbol();
            ConcurrentHashMap<Period, ConcurrentLinkedQueue<WebSocketInfo<Klines>>> klines =
                    Okexv3WebSocketExchange.KLINES.getOrDefault(period, null);
            if (null != klines) {
                ConcurrentLinkedQueue<WebSocketInfo<Klines>> list
                        = klines.getOrDefault(symbol, null);
                if (null != list) {
                    if (list.size() <= 1) {
                        // 这是最后一个订阅，需要取消订阅
                        Okexv3WebSocketClient client = Okexv3WebSocketExchange.CLIENTS.get(symbol);
                        if (null != client) {
                            client.noKlines(period);
                        }
                    }
                    list.remove(info);
                }
            }
            Okexv3WebSocketExchange.CONSUMERS.remove(pushId);
        }
    }

    @Override
    public boolean onDepth(ExchangeInfo info, Consumer<Depth> onDepth) {
        String symbol = info.getSymbol();
        String access = info.getAccess();

        String id = info.getPushId();
        WebSocketInfo<Depth> onInfo = new WebSocketInfo<>(id, Action.ON_DEPTH, symbol, access, onDepth);

        ConcurrentLinkedQueue<WebSocketInfo<Depth>> list = Okexv3WebSocketExchange.DEPTH
                .computeIfAbsent(symbol, k -> new ConcurrentLinkedQueue<>());
        Okexv3WebSocketClient client = this.addInfo(list, onInfo, symbol);
        if (null != client) {
            // 对这个连接订阅depth
            client.onDepth(depth -> list.stream().parallel()
                    .forEach(i -> i.getConsumer().accept(depth)));
        }
        // 将这个订阅添加到总记录里
        Okexv3WebSocketExchange.CONSUMERS.put(id, onInfo);
        return true;
    }

    @Override
    public void noDepth(String pushId) {
        WebSocketInfo info = Okexv3WebSocketExchange.CONSUMERS.get(pushId);
        if (null != info) {
            String symbol = info.getSymbol();
            ConcurrentLinkedQueue<WebSocketInfo<Depth>> list = Okexv3WebSocketExchange.DEPTH
                    .getOrDefault(symbol, null);
            if (null != list) {
                if (list.size() <= 1) {
                    // 这是最后一个订阅，需要取消订阅
                    Okexv3WebSocketClient client = Okexv3WebSocketExchange.CLIENTS.get(symbol);
                    if (null != client) {
                        client.noDepth();
                    }
                }
                list.remove(info);
            }
            Okexv3WebSocketExchange.CONSUMERS.remove(pushId);
        }
    }

    @Override
    public boolean onTrades(ExchangeInfo info, Consumer<Trades> onTrades) {
        String symbol = info.getSymbol();
        String access = info.getAccess();

        String id = info.getPushId();
        WebSocketInfo<Trades> onInfo =
                new WebSocketInfo<>(id, Action.ON_TRADES, symbol, access, onTrades);

        ConcurrentLinkedQueue<WebSocketInfo<Trades>> list = Okexv3WebSocketExchange.TRADES
                .computeIfAbsent(symbol, k -> new ConcurrentLinkedQueue<>());
        Okexv3WebSocketClient client = this.addInfo(list, onInfo, symbol);
        if (null != client) {
            // 对这个连接订阅ticker
            client.onTrades(trades -> list.stream().parallel()
                    .forEach(i -> i.getConsumer().accept(trades)));
        }
        // 将这个订阅添加到总记录里
        Okexv3WebSocketExchange.CONSUMERS.put(id, onInfo);
        return true;
    }

    @Override
    public void noTrades(String pushId) {
        WebSocketInfo info = Okexv3WebSocketExchange.CONSUMERS.get(pushId);
        if (null != info) {
            String symbol = info.getSymbol();
            ConcurrentLinkedQueue<WebSocketInfo<Trades>> list = Okexv3WebSocketExchange.TRADES
                    .getOrDefault(symbol, null);
            if (null != list) {
                if (list.size() <= 1) {
                    // 这是最后一个订阅，需要取消订阅
                    Okexv3WebSocketClient client = Okexv3WebSocketExchange.CLIENTS.get(symbol);
                    if (null != client) {
                        client.noTrades();
                    }
                }
                list.remove(info);
            }
            Okexv3WebSocketExchange.CONSUMERS.remove(pushId);
        }
    }

    @Override
    public boolean onAccount(ExchangeInfo info, Consumer<Account> onAccount) {
        String symbol = info.getSymbol();
        String access = info.getAccess();
        String secret = info.getSecret();

        String id = info.getPushId();
        WebSocketInfo<Account> onInfo =
                new WebSocketInfo<>(id, Action.ON_ACCOUNT, symbol, access, onAccount);
        if (null == this.client) {
            this.client = new Okexv3WebSocketClient(symbol, access, secret, super.log);
        }
        this.accounts.add(onInfo);
        // 对这个连接订阅account
        this.client.onAccount(account -> this.accounts.stream().parallel()
                .forEach(i -> i.getConsumer().accept(account)));
        // 将这个订阅添加到总记录里
        Okexv3WebSocketExchange.CONSUMERS.put(id, onInfo);
        return true;
    }

    @Override
    public void noAccount(String pushId) {
        WebSocketInfo info = Okexv3WebSocketExchange.CONSUMERS.get(pushId);
        if (null != info) {
            if (!this.accounts.isEmpty()) {
                if (this.accounts.size() <= 1) {
                    // 这是最后一个订阅，需要取消订阅
                    this.client.noAccount();
                }
                this.accounts.remove(info);
            }
            Okexv3WebSocketExchange.CONSUMERS.remove(pushId);
        }
    }

    @Override
    public boolean onOrders(ExchangeInfo info, Consumer<Orders> onOrders) {
        String symbol = info.getSymbol();
        String access = info.getAccess();
        String secret = info.getSecret();

        String id = info.getPushId();
        WebSocketInfo<Orders> onInfo =
                new WebSocketInfo<>(id, Action.ON_ORDERS, symbol, access, onOrders);
        if (null == this.client) {
            this.client = new Okexv3WebSocketClient(symbol, access, secret, super.log);
        }
        this.orders.add(onInfo);
        // 对这个连接订阅orders
        this.client.onOrders(orders -> this.orders.stream().parallel()
                .forEach(i -> i.getConsumer().accept(orders)));
        // 将这个订阅添加到总记录里
        Okexv3WebSocketExchange.CONSUMERS.put(id, onInfo);
        return true;
    }

    @Override
    public void noOrders(String pushId) {
        WebSocketInfo info = Okexv3WebSocketExchange.CONSUMERS.get(pushId);
        if (null != info) {
            if (!this.orders.isEmpty()) {
                if (this.orders.size() <= 1) {
                    // 这是最后一个订阅，需要取消订阅
                    this.client.noOrders();
                }
                this.orders.remove(info);
            }
            Okexv3WebSocketExchange.CONSUMERS.remove(pushId);
        }
    }
}
