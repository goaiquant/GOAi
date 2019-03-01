package cqt.goai.exchange.http.bitfinex;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.ExchangeException;
import cqt.goai.exchange.ExchangeInfo;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.exchange.http.HistoryOrdersByTimestamp;
import cqt.goai.exchange.http.HistoryOrdersDetailsByTimestamp;
import cqt.goai.exchange.http.HttpExchange;
import cqt.goai.exchange.http.TradeByFillOrKill;
import cqt.goai.exchange.util.CommonUtil;
import cqt.goai.exchange.util.bitfinex.BitfinexUtil;
import cqt.goai.model.enums.State;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.*;
import dive.http.common.MimeHttp;
import dive.http.common.MimeRequest;
import dive.http.common.model.Parameter;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static dive.common.util.Util.useful;

/**
 * @author
 */
public class BitfinexExchange extends HttpExchange
        implements HistoryOrdersByTimestamp, HistoryOrdersDetailsByTimestamp,
        TradeByFillOrKill {

    private static final String SITE = "api.bitfinex.com";
    private static final String ADDRESS = "https://" + SITE;

    private static final String TICKER = "/v1/pubticker/";
    private static final String KLINES = "/v2/candles/trade";
    private static final String DEPTH = "/v1/book/";
    private static final String TRADES = "/v1/trades/";

    private static final String ACCOUNT = "/v1/balances";

    private static final String ORDERS = "/v1/orders";
    private static final String HISTORY_ORDERS = "/v2/auth/r/orders/{Symbol}/hist";
    private static final String ORDER = "/v1/order/status";
    private static final String ORDER_DETAILS = "/v2/auth/r/order/{Symbol}:{OrderId}/trades";

    private static final String PRECISIONS = "/v1/symbols_details";
    private static final String ORDER_NEW = "/v1/order/new";
    private static final String MULTI_ORDERS = "/v1/order/new/multi";
    private static final String ORDER_CANCEL = "/v1/order/cancel";
    private static final String CANCEL_MULTI_ORDERS = "/v1/order/cancel/multi";

    private static final String HISTORY_ORDERS_DETAILS = "/v2/auth/r/trades/{Symbol}/hist";


    public BitfinexExchange(Logger log) {
        super(ExchangeName.BITFINEX, log);
    }

    @Override
    protected void postRequest(ExchangeInfo info) {
        switch (info.getAction()) {
            case TICKER:
            case KLINES:
            case DEPTH:
            case TRADES:
            case PRECISION:
            case PRECISIONS:
                return;
            default: this.unlock(info);
        }
    }

    @Override
    public String symbol(ExchangeInfo info) {
        return symbol(info, s -> s.replace("_", ""));
    }

    @Override
    public List<MimeRequest> tickerRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + TICKER + this.symbol(info))
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Ticker transformTicker(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject d = JSON.parseObject(result);
                /*{
                    "mid":"3953.05",
                    "bid":"3953.0",
                    "ask":"3953.1",
                    "last_price":"3953.1",
                    "low":"3902.0",
                    "high":"4227.0",
                    "volume":"37560.79069276",
                    "timestamp":"1545458269.236464"
                }*/
            BigDecimal timestamp = d.getBigDecimal("timestamp");

            Long time = timestamp.multiply(CommonUtil.THOUSAND).longValue();
//            BigDecimal open = null;
            BigDecimal high = d.getBigDecimal("high");
            BigDecimal low = d.getBigDecimal("low");
            BigDecimal close = d.getBigDecimal("last_price");
            BigDecimal volume = d.getBigDecimal("volume");

            return new Ticker(result, time, null, high, low, close, volume);
        }
        return null;
    }

    @Override
    public List<MimeRequest> klinesRequests(ExchangeInfo info, long delay) {
        String timeFrame = period(info, BitfinexUtil::getPeriod);
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + KLINES + ":{TimeFrame}:t{Symbol}/{Section}")
        //":" + timeFrame + ":t" + this.symbol(info) + "/hist?limit=" + 200 + "&sortOrder=" + 1)
                .replace("TimeFrame", timeFrame)
                .replace("Symbol", this.symbol(info))
                .replace("Section", "hist")
                .body("limit", 200)
                .body("sortOrder", -1)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Klines transformKlines(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONArray array = JSON.parseArray(result);
            List<Kline> klines = new ArrayList<>(array.size());

            long step = info.getPeriod().getValue();
            Long lastTime = null;
            BigDecimal lastClose = null;
            // new ... old
            for (int i = array.size() - 1; 0 <= i; i--) {
                JSONArray t = array.getJSONArray(i);
                    /*
                        [
                          MTS 1364824380000,
                          OPEN 99.01,
                          CLOSE 99.01,
                          HIGH 99.01,
                          LOW 99.01,
                          VOLUME 6
                        ]
                    */
                Long time = t.getLong(0) / 1000;

                if (null != lastTime) {
                    while (lastTime + step < time) {
                        //
                        lastTime = lastTime + step;
                        klines.add(new Kline(String.format("[%s,%s,%s,%s,%s,%s]",
                                lastTime * 1000, lastClose, lastClose, lastClose, lastClose, 0),
                                lastTime, lastClose, lastClose, lastClose, lastClose, BigDecimal.ZERO));
                    }
                }

                Kline kline = CommonUtil.parseKlineByIndex(result, t, time, 1);
                klines.add(kline);
                lastTime = time;
                lastClose = kline.getClose();
            }
            Collections.reverse(klines);
            return new Klines(klines);
        }
        return null;
    }

    @Override
    public List<MimeRequest> depthRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + DEPTH + this.symbol(info) + "?limit_bids=200&limit_asks=200")
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Depth transformDepth(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSONObject.parseObject(result);
            return BitfinexUtil.parseDepth(r);
        }
        return null;
    }

    @Override
    public List<MimeRequest> tradesRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + TRADES + this.symbol(info) + "?limit_trades=200")
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Trades transformTrades(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONArray r = JSON.parseArray(result);
            return BitfinexUtil.parseTrades(r);
        }
        return null;
    }


    @Override
    public List<MimeRequest> balancesRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        return this.post(info, ACCOUNT, "request", ACCOUNT,
                "nonce", nonce);
    }

    @Override
    protected Balances transformBalances(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        String result = results.get(0);
        if (useful(result)) {
            JSONArray r = JSON.parseArray(result);
            List<Balance> balances = BitfinexUtil.parseBalances(r);
            return new Balances(balances);
        }
        return null;
    }

    @Override
    public List<MimeRequest> accountRequests(ExchangeInfo info, long delay) {
        return this.balancesRequests(info, delay);
    }

    @Override
    protected Account transformAccount(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        Account account = super.transformAccount(results, info);
        Balance base = account.getBase();
        Balance count = account.getQuote();

        String[] symbols = CommonUtil.split(info.getSymbol());

        if (null == base) {
            base = new Balance(String.format("{\"type\":\"exchange\"," +
                            "\"currency\":\"%s\"," +
                            "\"amount\":\"%s\"," +
                            "\"available\":\"%s\"}",
                    symbols[0].toLowerCase(), 0, 0), symbols[0], BigDecimal.ZERO, BigDecimal.ZERO);
        }
        if (null == count) {
            count = new Balance(String.format("{\"type\":\"exchange\"," +
                            "\"currency\":\"%s\"," +
                            "\"amount\":\"%s\"," +
                            "\"available\":\"%s\"}",
                    symbols[1].toLowerCase(), 0, 0), symbols[1], BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return new Account(System.currentTimeMillis(), base, count);
    }

    @Override
    public List<MimeRequest> precisionsRequests(ExchangeInfo info, long delay) {
        if (System.currentTimeMillis() < lastRequest + MIN_GAP) {
            return Collections.emptyList();
        }
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + PRECISIONS)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Precisions transformPrecisions(List<String> results, ExchangeInfo info) {
        String result;
        if (System.currentTimeMillis() < lastRequest + MIN_GAP) {
            result = precisionsResult;
        } else {
            result = results.get(0);
            precisionsResult = result;
            lastRequest = System.currentTimeMillis();
        }
        return new Precisions(BitfinexUtil.parsePrecisions(result));
    }

    @Override
    public List<MimeRequest> precisionRequests(ExchangeInfo info, long delay) {
        List<MimeRequest> requests = new LinkedList<>(this.depthRequests(info, delay));
        requests.addAll(this.precisionsRequests(info, delay));
        return requests;
    }

    @Override
    protected Precision transformPrecision(List<String> results, ExchangeInfo info) {
        Precisions precisions;
        if (1 < results.size()) {
            // 有请求币对信息
            precisions = this.transformPrecisions(results.subList(1, 2), info);
        } else {
            precisions = new Precisions(BitfinexUtil.parsePrecisions(precisionsResult));
        }
        Precision precision = null;
        for (Precision p : precisions) {
            if (info.getSymbol().equals(p.getSymbol())) {
                precision = p;
                break;
            }
        }
        if (null == precision) {
            return null;
        }
        // 解析深度信息获取 数量最小精度
        Depth depth = this.transformDepth(results, info);
        Precision p = CommonUtil.parsePrecisionByDepth(depth, info.getSymbol());
        return new Precision(precision.getData(), precision.getSymbol(),
                p.getBase(), p.getQuote(),
                precision.getBaseStep(), precision.getQuoteStep(),
                precision.getMinBase(), precision.getMinQuote(),
                precision.getMaxBase(), precision.getMaxQuote());
    }

    @Override
    public List<MimeRequest> buyLimitRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        return this.post(info, ORDER_NEW, "request", ORDER_NEW,
                "nonce", nonce,
                "symbol", this.symbol(info),
                "amount", info.getAmount(),
                "price", info.getPrice(),
                "side", "buy",
                "type", "exchange limit",
                "ocoorder ", "false");
    }

    @Override
    public String transformBuyLimit(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        return this.parseId(results.get(0));
    }

    @Override
    public List<MimeRequest> sellLimitRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        return this.post(info, ORDER_NEW, "request", ORDER_NEW,
                "nonce", nonce,
                "symbol", this.symbol(info),
                "amount", info.getAmount(),
                "price", info.getPrice(),
                "side", "sell",
                "type", "exchange limit",
                "ocoorder ", "false");
    }

    @Override
    public String transformSellLimit(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        return this.parseId(results.get(0));
    }

    @Override
    public List<MimeRequest> buyMarketRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        return this.post(info, ORDER_NEW, "request", ORDER_NEW,
                "nonce", nonce,
                "symbol", this.symbol(info),
                "amount", info.getQuote(),
                "price", info.getQuote(),
                "side", "buy",
                "type", "exchange market",
                "ocoorder ", "false");
    }

    @Override
    public String transformBuyMarket(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        return this.parseId(results.get(0));
    }

    @Override
    public List<MimeRequest> sellMarketRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        return this.post(info, ORDER_NEW, "request", ORDER_NEW,
                "nonce", nonce,
                "symbol", this.symbol(info),
                "amount", info.getBase(),
                "price", info.getBase(),
                "side", "sell",
                "type", "exchange market",
                "ocoorder ", "false");
    }

    @Override
    public String transformSellMarket(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        return this.parseId(results.get(0));
    }

    @Override
    public List<MimeRequest> multiBuyRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        List<Map<String, Object>> orders = this.getMultiOrders(info, "buy");

        return this.post(info, MULTI_ORDERS, "request", MULTI_ORDERS,
                "nonce", nonce,
                "orders", orders);
    }

    @Override
    public List<String> transformMultiBuy(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        return this.parseOrderIds(results.get(0));
    }

    @Override
    public List<MimeRequest> multiSellRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        List<Map<String, Object>> orders = this.getMultiOrders(info, "sell");

        return this.post(info, MULTI_ORDERS, "request", MULTI_ORDERS,
                "nonce", nonce,
                "orders", orders);
    }

    @Override
    public List<String> transformMultiSell(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        return this.parseOrderIds(results.get(0));
    }

    @Override
    public List<MimeRequest> cancelOrderRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        return this.post(info, ORDER_CANCEL, "request", ORDER_CANCEL,
                "nonce", nonce,
                "symbol", this.symbol(info),
                "order_id", Long.parseLong(info.getCancelId()));
    }

    @Override
    protected Boolean transformCancelOrder(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        String result = results.get(0);
        if (useful(result)) {
            BitfinexUtil.parseOrder(result, JSON.parseObject(result));
            return true;
        }
        return null;
    }

    @Override
    public List<MimeRequest> cancelOrdersRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        return this.post(info, CANCEL_MULTI_ORDERS, "request", CANCEL_MULTI_ORDERS,
                "nonce", nonce,
                "symbol", this.symbol(info),
                "order_ids", info.getCancelIds().stream()
                        .map(Long::parseLong)
                        .collect(Collectors.toList()));
    }

    /**
     * 一个都没取消
     */
    private static final String NONE = "None to cancel";
    @Override
    public List<String> transformCancelOrders(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        log.info("results -> {}", results);

        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSON.parseArray(result).getJSONObject(0);
            result = r.getString("result");
            if (null == result) {
                return null;
            }
            if (NONE.equals(result)) {
                return Collections.emptyList();
            }
            if (result.contains(String.valueOf(info.getCancelIds().size()))) {
                return info.getCancelIds();
            }
        }
        return null;
    }

    @Override
    public List<MimeRequest> ordersRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        return this.post(info, ORDERS, "request", ORDERS,
                "nonce", nonce);
    }

    @Override
    protected Orders transformOrders(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        String result = results.get(0);
        if (useful(result)) {
            List<Order> orders = this.parseOrders(result, this.symbol(info).toLowerCase());
            orders = orders.stream()
                    .filter(o -> o.getState() == State.SUBMIT || o.getState() == State.PARTIAL)
                    .sorted((o1, o2) -> o2.getTime().compareTo(o1.getTime()))
                    .collect(Collectors.toList());
            return new Orders(orders);
        }
        return null;
    }

    @Override
    public List<MimeRequest> historyOrdersRequests(ExchangeInfo info, long delay) {
        return this.historyOrdersRequests(info.getSymbol(), info.getAccess(),  info.getSecret(),
                null, System.currentTimeMillis(), delay);
    }

    @Override
    public Orders transformHistoryOrders(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        String result = results.get(0);
        if (useful(result)) {
            List<Order> orders = BitfinexUtil.parseOrders(result);
            orders = orders.stream()
                    .sorted((o1, o2) -> o2.getTime().compareTo(o1.getTime()))
                    .collect(Collectors.toList());
            return new Orders(orders);
        }
        return null;
    }

    @Override
    public List<MimeRequest> orderRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        return this.post(info, ORDER, "request", ORDER,
                "nonce", nonce,
                "symbol", this.symbol(info),
                "order_id", Long.parseLong(info.getOrderId()));
    }

    @Override
    protected Order transformOrder(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        String result = results.get(0);
        if (useful(result)) {
            return BitfinexUtil.parseOrder(result, JSON.parseObject(result));
        }
        return null;
    }

    @Override
    public List<MimeRequest> orderDetailsRequests(ExchangeInfo info, long delay) {
        String nonce = this.lock(info);

        String symbol = this.symbol(info);

        String path = ORDER_DETAILS.replace("{Symbol}", "t" + symbol)
                .replace("{OrderId}", info.getOrderId());
        Parameter para = Parameter.build();

        String parameter = para.sort().json(JSON::toJSONString);
        // signature = `/api/${apiPath}${nonce}${JSON.stringify(body)}`
        String signature = CommonUtil.hmacSha384("/api" + path + nonce + parameter, info.getSecret());
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + path)
                .post()
                .header("bfx-nonce", nonce)
                .header("bfx-apikey", info.getAccess())
                .header("bfx-signature", signature)
                .body(parameter)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    public OrderDetails transformOrderDetails(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        String result = results.get(0);
        if (useful(result)) {
            List<OrderDetail> details = BitfinexUtil.parseOrderDetails(result);
            details = details.stream()
                    .sorted((d1, d2) -> d2.getTime().compareTo(d1.getTime()))
                    .collect(Collectors.toList());
            return new OrderDetails(details);
        }
        return null;
    }


    // ======================== tools =====================

    /**
     * 上次币对请求结果
     */
    private static String precisionsResult;

    /**
     * 上次币对请求时间
     */
    private static long lastRequest;

    /**
     * 最小间隔12s
     */
    private static final long MIN_GAP = 1000 * 12;

    /**
     * 签名算法需要用nonce，必须要求递增，所以在高并发下，不能能够使用当前时间戳
     * 用map存储每次使用过的nonce
     * key --> symbol:access
     */
    private static final ConcurrentHashMap<String, Long> NONCE = new ConcurrentHashMap<>();

    /**
     * 高并发下，两次请求无法确定请求的先后，只好对所有的要求签名方法进行加锁，等一个请求结果回来了，再请求下一个
     * key --> symbol:access
     */
    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    /**
     * 尝试等待获取锁的时间，毫秒
     */
    private static final int WAIT = 3000;

    /**
     * 最大锁时间，超过该时间，强制解锁
     */
    private static final int MAX_LOCK_TIME = 1000 * 20;

    private String getKey(ExchangeInfo info) {
        // 不清楚用什么做key合适，symbol:access 还是会报nonce异常，搞不好是根据ip来的，那就连map也不用了
        return info.getSymbol() + ":" + info.getAccess();
    }

    /**
     * 获取nonce
     * @param info 请求信息
     * @return nonce
     */
    private String getNonce(ExchangeInfo info) {
        String key = this.getKey(info);

        Long nonce = NONCE.get(key);
        long now = System.currentTimeMillis();
        if (null == nonce) {
            nonce = now;
        } else if (nonce == now) {
            nonce++;
        } else if (nonce < now) {
            nonce = now;
        }
        NONCE.put(key, nonce);

        return String.valueOf(nonce);
    }

    /**
     * 进行加锁
     * @param info 请求信息
     */
    private String lock(ExchangeInfo info) {
        String key = this.getKey(info);

        Lock lock = LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
        try {
            if (lock.tryLock(WAIT, TimeUnit.MILLISECONDS)) {
                return this.getNonce(info);
            } else {
                Long nonce = NONCE.get(key);
                if (nonce + MAX_LOCK_TIME < System.currentTimeMillis()) {
                    this.unlock(info);
                }
                throw new ExchangeException("there are too many request. locks already!!");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 解锁
     * @param info 请求信息
     */
    private void unlock(ExchangeInfo info) {
        String key = this.getKey(info);
        ReentrantLock lock = LOCKS.get(key);
        if (null != lock && lock.isLocked()) {
            lock.unlock();
        }
    }

    /**
     * post请求
     * @param info 请求信息
     * @param path 请求路径
     * @param others 其他参数
     * @return 封装请求信息
     */
    private List<MimeRequest> post(ExchangeInfo info, String path, Object... others) {
        String access = info.getAccess();
        String secret = info.getSecret();

        String parameter = CommonUtil.addOtherParameterToJson(Parameter.build(), others);

        String payload = Base64.getEncoder().encodeToString(parameter.getBytes());

        String signature = CommonUtil.hmacSha384(payload, secret);
        return Collections.singletonList(new MimeRequest.Builder()
                .url(ADDRESS + path)
                .post()
                .header("X-BFX-APIKEY", access)
                .header("X-BFX-PAYLOAD", payload)
                .header("X-BFX-SIGNATURE", signature)
                .build());
    }

    /**
     * 解析orders
     * @param result 请求结果
     * @return orders
     */
    private List<Order> parseOrders(String result, String symbol) {
        JSONArray r = JSON.parseArray(result);
        List<Order> orders = new LinkedList<>();
        for (int i = 0; i < r.size(); i++) {
            JSONObject o = r.getJSONObject(i);
            if (symbol.equalsIgnoreCase(o.getString("symbol"))) {
                orders.add(BitfinexUtil.parseOrder(r.getString(i), o));
            }
        }
        return orders;
    }

    /**
     * 解析订单id
     * @param result 原始数据
     * @return 订单id
     */
    private String parseId(String result) {
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            return r.getString("id");
        }
        return null;
    }

    /**
     * 统一定制多笔订单的请求信息
     * @param info 请信息
     * @param side 买卖方向
     * @return 请求信息
     */
    private List<Map<String, Object>> getMultiOrders(ExchangeInfo info, String side) {
        String symbol = this.symbol(info);

        return info.getRows().stream().map(r -> {
            /*
             * symbol: 'BTCUSD',
             *   amount: '0.011',
             *   price: '1000',
             *   exchange: 'bitfinex',
             *   side: 'buy',
             *   type: 'exchange market'
             */
            Map<String, Object> map = new HashMap<>(6);
            map.put("symbol", symbol);
            map.put("amount", r.getAmount());
            map.put("price", r.getPrice());
            map.put("exchange", "bitfinex");
            map.put("side", side);
            map.put("type", "exchange limit");
            return map;
        }).collect(Collectors.toList());
    }

    private static final String SUCCESS = "success";
    private static final String STATUS = "status";
    /**
     * 取出多个下单结果的id
     * @param result 请求结果
     * @return 订单id
     */
    private List<String> parseOrderIds(String result) {
        JSONObject r = JSON.parseObject(result);
        if (!SUCCESS.equals(r.getString(STATUS))) {
            return null;
        }
        JSONArray array = r.getJSONArray("order_ids");
        List<String> ids = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            ids.add(array.getJSONObject(i).getString("id"));
        }
        return ids;
    }


    // ======================== special api ==========================

    /**
     * v2版本的post请求封装
     * @param info 请求信息 access secret symbol
     * @param nonce nonce
     * @param path 路径
     * @param others 其他参数
     * @return 请求信息
     */
    private static List<MimeRequest> postV2(ExchangeInfo info, String nonce, String path, Object... others) {
        String access = info.getAccess();
        String secret = info.getSecret();

        String parameter = CommonUtil.addOtherParameterToJson(Parameter.build(), others);

        // signature = `/api/${apiPath}${nonce}${JSON.stringify(body)}`
        String signature = CommonUtil.hmacSha384("/api" + path + nonce + parameter, secret);
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + path)
                .post()
                .header("bfx-nonce", nonce)
                .header("bfx-apikey", access)
                .header("bfx-signature", signature)
                .body(parameter)
                .build();
        return Collections.singletonList(request);
    }

    /**
     * 获取参数列表 获取历史订单和历史订单明细都需要这些
     * @param start 开始时间
     * @param end 结束时间
     * @param limit 限制个数
     * @return 参数列表
     */
    private static List<Object> getParameterList(Long start, Long end, Integer limit) {
        List<Object> list = new LinkedList<>();
        if (null != start) {
            list.add("start");
            list.add(start);
        }
        if (null != end) {
            list.add("end");
            list.add(end);
        }
        list.add("limit");
        list.add(limit);
        return list;
    }

    @Override
    public MimeHttp getHttp() {
        return super.http;
    }

    @Override
    public ExchangeName getExchangeName() {
        return this.name;
    }

    @Override
    public Logger getLog() {
        return this.log;
    }

    @Override
    public List<MimeRequest> historyOrdersRequests(String symbol, String access, String secret,
                                                   Long start, Long end, long delay) {
        ExchangeInfo info = ExchangeInfo.historyOrders(symbol, access, secret);

        String nonce = this.lock(info);

        symbol = this.symbol(info);

        String path = HISTORY_ORDERS.replace("{Symbol}", "t" + symbol);

        List<Object> list = BitfinexExchange.getParameterList(start, end, 500);
        list.add("sortOrder");
        list.add(-1);

        return BitfinexExchange.postV2(info, nonce, path, list.toArray(new Object[]{}));
    }

    @Override
    public List<MimeRequest> historyOrdersDetailsRequests(String symbol, String access, String secret,
                                                   Long start, Long end, long delay) {
        ExchangeInfo info = ExchangeInfo.historyOrders(symbol, access, secret);

        String nonce = this.lock(info);

        symbol = this.symbol(info);

        String path = HISTORY_ORDERS_DETAILS.replace("{Symbol}", "t" + symbol);

        List<Object> list = BitfinexExchange.getParameterList(start, end, 1000);

        return BitfinexExchange.postV2(info, nonce, path, list.toArray(new Object[]{}));
    }

    /**
     * 解析历史订单明细
     * [
     *  334330310,      ID	integer	Trade database id
     *  "tVSYBTC",      PAIR	string	Pair (BTCUSD, …)
     *  1548318500000,  MTS_CREATE	integer	Execution timestamp
     *  21775101099,    ORDER_ID	integer	Order id
     *  109.5576846,    EXEC_AMOUNT	float	Positive means buy, negative means sell
     *  0.0000081,      EXEC_PRICE	float	Execution price
     *  null,           ORDER_TYPE	string	Order type
     *  null,           ORDER_PRICE	float	Order price
     *  -1,             MAKER	int	1 if true, 0 if false
     *  -0.21911537,    FEE	float	Fee
     *  "VSY"           FEE_CURRENCY	string	Fee currency
     * ]
     *
     *
     * @param results 请求结果
     * @return 历史订单明细
     */
    @Override
    public OrderDetails transformHistoryOrdersDetails(List<String> results, ExchangeInfo info) {
        return this.transformOrderDetails(results, info);
    }

    @Override
    public List<MimeRequest> buyFillOrKillRequests(String symbol, String access, String secret, String price, String amount, long delay) {
        ExchangeInfo info = ExchangeInfo.buyLimit(symbol, access, secret, price, amount);

        String nonce = this.lock(info);

        return this.post(info, ORDER_NEW, "request", ORDER_NEW,
                "nonce", nonce,
                "symbol", this.symbol(info),
                "amount", info.getAmount(),
                "price", info.getPrice(),
                "side", "buy",
                "type", "exchange fill-or-kill",
                "ocoorder ", "false");
    }

    @Override
    public String transformBuyFillOrKill(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        return this.parseId(results.get(0));
    }

    @Override
    public List<MimeRequest> sellFillOrKillRequests(String symbol, String access, String secret, String price, String amount, long delay) {
        ExchangeInfo info = ExchangeInfo.sellLimit(symbol, access, secret, price, amount);

        String nonce = this.lock(info);

        return this.post(info, ORDER_NEW, "request", ORDER_NEW,
                "nonce", nonce,
                "symbol", this.symbol(info),
                "amount", info.getAmount(),
                "price", info.getPrice(),
                "side", "sell",
                "type", "exchange fill-or-kill",
                "ocoorder ", "false");
    }

    @Override
    public String transformSellFillOrKill(List<String> results, ExchangeInfo info) {
        this.unlock(info);

        return this.parseId(results.get(0));
    }

}
