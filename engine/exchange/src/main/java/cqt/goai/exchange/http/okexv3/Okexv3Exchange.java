package cqt.goai.exchange.http.okexv3;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.ExchangeInfo;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.exchange.http.HttpExchange;
import cqt.goai.exchange.util.CommonUtil;
import cqt.goai.exchange.util.okexv3.Okexv3Util;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.*;
import dive.common.crypto.Base64Util;
import dive.common.crypto.HmacUtil;
import dive.common.math.RandomUtil;
import dive.http.common.MimeRequest;
import dive.http.common.model.Header;
import dive.http.common.model.Parameter;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

import static dive.common.util.DateUtil.SECOND;
import static dive.common.util.DateUtil.formatISO8601;
import static dive.common.util.UrlUtil.urlEncode;
import static dive.common.util.Util.exist;
import static dive.common.util.Util.useful;


/**
 * OKExV3Exchange
 *
 * @author GOAi
 */
public class Okexv3Exchange extends HttpExchange {

    private static final String SITE = "www.okex.com";
    private static final String ADDRESS = "https://" + SITE;

    private static final String TICKER = "/api/spot/v3/instruments/{instrumentId}/ticker";
    private static final String KLINES = "/api/spot/v3/instruments/{instrumentId}/candles";
    private static final String DEPTH = "/api/spot/v3/instruments/{instrumentId}/book?size=200";
    private static final String TRADES = "/api/spot/v3/instruments/{instrumentId}/trades";

    private static final String ACCOUNT = "/api/spot/v3/accounts/";
    private static final String BALANCES = "/api/spot/v3/accounts";
    private static final String ORDERS = "/api/spot/v3/orders_pending";
    private static final String HISTORY_ORDERS = "/api/spot/v3/orders";
    private static final String ORDER = "/api/spot/v3/orders/";
    private static final String ORDER_DETAILS = "/api/spot/v3/fills";

    private static final String PRECISIONS = "/api/spot/v3/instruments";
    private static final String CREATE_ORDER = "/api/spot/v3/orders";
    private static final String CREATE_ORDERS = "/api/spot/v3/batch_orders";
    private static final String CANCEL_ORDER = "/api/spot/v3/cancel_orders/";
    private static final String CANCEL_ORDERS = "/api/spot/v3/cancel_batch_orders";

    public Okexv3Exchange(Logger log) {
        super(ExchangeName.OKEXV3, log);
    }

    @Override
    public String symbol(ExchangeInfo info) {
        return symbol(info, s -> s.replace("_", "-").toLowerCase());
    }

    @Override
    public final List<MimeRequest> tickerRequests(ExchangeInfo info, long delay) {
        String url = TICKER.replace("{instrumentId}", this.symbol(info));
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + url)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected final Ticker transformTicker(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            return Okexv3Util.parseTicker(result, r);
        }
        return null;
    }

    @Override
    public final List<MimeRequest> klinesRequests(ExchangeInfo info, long delay) {
        int size = 200;
        //granularity必须是[60 180 300 900 1800 3600 7200 14400 21600 43200 86400 604800]
        Integer granularity = period(info, Okexv3Util::getPeriod);
        Long now = System.currentTimeMillis();
        Long past = now - SECOND * granularity * size;
        String start = urlEncode(formatISO8601(past));
        String end = urlEncode(formatISO8601(now));
        String url = KLINES.replace("{instrumentId}", this.symbol(info))
                + "?end=" + end + "&granularity=" + granularity + "&start=" + start;
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + url)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected final Klines transformKlines(List<String> results, ExchangeInfo info) {
        // 默认200根
        int size = 200;
        String result = results.get(0);
        if (useful(result)) {
            JSONArray r = JSON.parseArray(result);
            List<Kline> list = new ArrayList<>(r.size());
            if (0 < r.size()) {
                for (int i = 0; i < r.size(); i++) {
                    Kline kline = Okexv3Util.parseKline(r.getString(i), r.getJSONArray(i));
                    list.add(kline);
                    if (size <= list.size()) {
                        break;
                    }
                }
            }
            return new Klines(list);
        }
        return null;
    }

    @Override
    public List<MimeRequest> depthRequests(ExchangeInfo info, long delay) {
        String url = DEPTH.replace("{instrumentId}", this.symbol(info));
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + url)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Depth transformDepth(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSONObject.parseObject(result) ;
            return Okexv3Util.parseDepth(r);
        }
        return null;
    }

    @Override
    public List<MimeRequest> tradesRequests(ExchangeInfo info, long delay) {
        Integer size = 100;

        String url = TRADES.replace("{instrumentId}", this.symbol(info)) + "?limit=" + size;
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + url)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Trades transformTrades(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONArray r = JSON.parseArray(result);
            return Okexv3Util.parseTrades(r);
        }
        return null;
    }


    @Override
    public List<MimeRequest> balancesRequests(ExchangeInfo info, long delay) {
        return Okexv3Exchange.get(delay, BALANCES, info);
    }

    @Override
    protected Balances transformBalances(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONArray r = JSON.parseArray(result);
            List<Balance> balances = new ArrayList<>(r.size());
            for (int i = 0, l = r.size(); i < l; i++) {
                balances.add(Okexv3Util.parseBalance(r.getString(i), r.getJSONObject(i)));
            }
            return new Balances(balances);
        }
        return null;
    }

    @Override
    public List<MimeRequest> accountRequests(ExchangeInfo info, long delay) {
        List<MimeRequest> requests = new ArrayList<>(2);
        String[] ss = CommonUtil.split(info.getSymbol().toLowerCase());
        for (String s : ss) {
            String url = ACCOUNT + s;
            Header header = sign(delay, "GET", url, null, info);
            // Builder Pattern 建造者模式
            MimeRequest request = new MimeRequest.Builder()
                    .url(ADDRESS + url)
                    .header(header)
                    .build();
            requests.add(request);
        }
        return requests;
    }

    @Override
    protected Account transformAccount(List<String> results, ExchangeInfo info) {
        String baseResult = results.get(0);
        String countResult = results.get(1);
        if (useful(baseResult) && useful(countResult)) {
            JSONObject br = JSON.parseObject(baseResult);
            JSONObject cr = JSON.parseObject(countResult);
            Balance base = Okexv3Util.parseBalance(baseResult, br);
            Balance count = Okexv3Util.parseBalance(countResult, cr);
            return new Account(System.currentTimeMillis(), base, count);
        }
        return null;
    }


    @Override
    public List<MimeRequest> precisionsRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + PRECISIONS)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Precisions transformPrecisions(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONArray r = JSON.parseArray(result);
            List<Precision> precisions = new ArrayList<>(r.size());
            for (int i = 0; i < r.size(); i++) {
                JSONObject t = r.getJSONObject(i);
                /*
                 * {
                 *     "base_currency":"DASH",  // 目标币
                 *     "product_id":"DASH-BTC", // 币对
                 *     "base_increment":"0.000001", // 目标币精度
                 *     "min_size":"0.001", // 目标币最小数量
                 *     "base_min_size":"0.001", // 目标币最小数量
                 *     "quote_increment":"0.00000001", // 计价币精度
                 *     "size_increment":"0.000001", // 目标币精度
                 *     "instrument_id":"DASH-BTC", // 币对
                 *     "quote_currency":"BTC",  // 计价币
                 *     "tick_size":"0.00000001" // 计价币精度
                 * }
                 */
                String symbol = t.getString("instrument_id").replace("-", "_");

                // 最小币步长
                BigDecimal stepBase = t.getBigDecimal("base_increment").stripTrailingZeros();
                // 最小价格步长
                BigDecimal stepCount = t.getBigDecimal("quote_increment").stripTrailingZeros();

                // 交易时币精度，amount
                Integer base = stepBase.scale();
                // 计价货币精度，price
                Integer count = stepCount.scale();

                // 最小买卖数量
                BigDecimal minBase = t.getBigDecimal("base_min_size");
                // 最小买价格
//                BigDecimal minCount = null;

                // 最大买卖数量
//                BigDecimal maxBase = null;
                // 最大买价格
//                BigDecimal maxCount = null;

                precisions.add(new Precision(r.getString(i), symbol, base, count, stepBase, stepCount,
                        minBase, null, null, null));
            }
            return new Precisions(precisions);
        }
        return null;
    }

    @Override
    public List<MimeRequest> precisionRequests(ExchangeInfo info, long delay) {
        return this.precisionsRequests(info, delay);
    }

    @Override
    protected Precision transformPrecision(List<String> results, ExchangeInfo info) {
        Precisions precisions = this.transformPrecisions(results, info);
        return precisions.stream()
                .filter(p -> info.getSymbol().equals(p.getSymbol()))
                .findAny()
                .orElse(null);
    }

    @Override
    public List<MimeRequest> buyLimitRequests(ExchangeInfo info, long delay) {
        return this.createOrder(info, delay, "limit", "buy",
                "price", info.getPrice(), "size", info.getAmount());
    }

    @Override
    public String transformBuyLimit(List<String> results, ExchangeInfo info) {
        return Okexv3Exchange.parseOrderId(results.get(0));
    }

    @Override
    public List<MimeRequest> sellLimitRequests(ExchangeInfo info, long delay) {
        return this.createOrder(info, delay, "limit", "sell",
                "price", info.getPrice(), "size", info.getAmount());
    }

    @Override
    public String transformSellLimit(List<String> results, ExchangeInfo info) {
        return Okexv3Exchange.parseOrderId(results.get(0));
    }

    @Override
    public List<MimeRequest> buyMarketRequests(ExchangeInfo info, long delay) {
        return this.createOrder(info, delay, "market", "buy",
                "notional", info.getQuote());
    }

    @Override
    public String transformBuyMarket(List<String> results, ExchangeInfo info) {
        return Okexv3Exchange.parseOrderId(results.get(0));
    }

    @Override
    public List<MimeRequest> sellMarketRequests(ExchangeInfo info, long delay) {
        return this.createOrder(info, delay, "market", "sell",
                "size", info.getBase());
    }

    @Override
    public String transformSellMarket(List<String> results, ExchangeInfo info) {
        return Okexv3Exchange.parseOrderId(results.get(0));
    }

    @Override
    public List<MimeRequest> multiBuyRequests(ExchangeInfo info, long delay) {
        return this.createOrders(info, delay, "buy");
    }

    @Override
    public List<String> transformMultiBuy(List<String> results, ExchangeInfo info) {
        List<String> rs = this.parseMultiOrderIds(results, info.getSymbol().toLowerCase());
        if (null != rs && !rs.isEmpty()) {
            return rs;
        }
        return null;
    }

    @Override
    public List<MimeRequest> multiSellRequests(ExchangeInfo info, long delay) {
        return this.createOrders(info, delay, "sell");
    }

    @Override
    public List<String> transformMultiSell(List<String> results, ExchangeInfo info) {
        return this.transformMultiBuy(results, info);
    }

    @Override
    public List<MimeRequest> cancelOrderRequests(ExchangeInfo info, long delay) {
        String symbol = this.symbol(info);
        String url = CANCEL_ORDER + info.getCancelId();
        Parameter parameter = Parameter.build("instrument_id", symbol);
        String body = parameter.json(JSON::toJSONString);
        return this.post(delay, body, info, url);
    }

    @Override
    protected Boolean transformCancelOrder(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            if (r.containsKey(RESULT)) {
                return r.getBoolean(RESULT);
            }
        }
        return null;
    }

    @Override
    public List<MimeRequest> cancelOrdersRequests(ExchangeInfo info, long delay) {
        String symbol = this.symbol(info);
        List<String> ids = info.getCancelIds();

        if (null == ids || ids.isEmpty()) {
            return Collections.emptyList();
        }

        int max = 4;

        List<List<Long>> lists = new LinkedList<>();
        for (int i = 0, l = ids.size() / max + 1; i < l; i++) {
            List<Long> list = ids.stream()
                    .skip(i * max)
                    .limit(max)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            if (!list.isEmpty()) {
                lists.add(list);
            }
        }

        return lists.stream()
                .map(s -> Parameter.build("instrument_id", symbol).add("order_ids", s))
                .map(p -> "[" + p.sort().json(JSON::toJSONString) + "]")
                .map(body -> MimeRequest.builder()
                        .url(ADDRESS + CANCEL_ORDERS)
                        .post()
                        .header(sign(delay, "POST", CANCEL_ORDERS, body, info))
                        .body(body)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> transformCancelOrders(List<String> results, ExchangeInfo info) {
        String symbol = this.symbol(info);
        return results.stream()
                .map(JSON::parseObject)
                .filter(o -> o.containsKey(symbol))
                .map(o -> o.getJSONObject(symbol))
                .filter(o -> o.containsKey(RESULT) && o.getBoolean(RESULT))
                .map(o -> o.getJSONArray("order_id"))
                .map(a -> a.toJavaList(String.class))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<MimeRequest> ordersRequests(ExchangeInfo info, long delay) {
        String url = ORDERS + "?instrument_id=" + this.symbol(info);
        return Okexv3Exchange.get(delay, url, info);
    }

    @Override
    protected Orders transformOrders(List<String> results, ExchangeInfo info) {
        List<Order> orders = parseOrders(results.get(0));
        return exist(orders) ? new Orders(orders) : null;
    }

    @Override
    public List<MimeRequest> historyOrdersRequests(ExchangeInfo info, long delay) {
        String symbol = this.symbol(info);

        Parameter parameter = Parameter.build();
        parameter.add("instrument_id", symbol);
        parameter.add("status", "all");
        parameter.add("limit", "100");

        String url = HISTORY_ORDERS + "?" + parameter.sort().concat();
        return Okexv3Exchange.get(delay, url, info);
    }

    @Override
    protected Orders transformHistoryOrders(List<String> results, ExchangeInfo info) {
        List<Order> orders = Okexv3Exchange.parseOrders(results.get(0));
        if (null == orders) {
            return null;
        }
        orders = orders.stream()
                .sorted((o1, o2) -> o2.getTime().compareTo(o1.getTime()))
                .collect(Collectors.toList());
        return new Orders(orders);
    }

    @Override
    public List<MimeRequest> orderRequests(ExchangeInfo info, long delay) {
        String url = ORDER + info.getOrderId() + "?instrument_id=" + this.symbol(info);
        return Okexv3Exchange.get(delay, url, info);
    }

    @Override
    protected Order transformOrder(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            return Okexv3Util.parseOrder(result, r);
        }
        return null;
    }

    @Override
    public List<MimeRequest> orderDetailsRequests(ExchangeInfo info, long delay) {
        List<MimeRequest> requests = new ArrayList<>(2);
        requests.addAll(this.orderRequests(info, delay));
        String url = ORDER_DETAILS + "?instrument_id=" + this.symbol(info) + "&order_id=" + info.getOrderId();
        requests.addAll(Okexv3Exchange.get(delay, url, info));
        return requests;
    }

    @Override
    public OrderDetails transformOrderDetails(List<String> results, ExchangeInfo info) {
        Order order = this.transformOrder(results, info);
        String side = order.getSide().name().toLowerCase();
        String result = results.get(1);
        if (useful(result)) {
            List<OrderDetail> details = Okexv3Exchange.parseOrderDetails(result, side);
            if (null != details) {
                return new OrderDetails(details);
            }
        }
        return null;
    }

    // ========== tools ==========

    /**
     * 统一签名
     *
     * @param delay  时间戳延时
     * @param method 请求类型
     * @param action url
     * @param body   请求参数
     * @param info   参数
     * @return 加密Header
     */
    private static Header sign(long delay, String method, String action, String body, ExchangeInfo info) {
        String access = info.getAccess();
        String secret = info.getSecret();

        String epoch = formatEpoch(System.currentTimeMillis() + delay);
        String[] ss = CommonUtil.split(secret);

        String sign = epoch + method + action + (useful(body) ? body : "");
        try {
            sign = Base64Util.base64EncodeToString(HmacUtil.hmac(sign.getBytes(), ss[0], HmacUtil.HMAC_SHA256));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }

        Header header = Header.build();
        header.add("OK-ACCESS-KEY", access);
        header.add("OK-ACCESS-SIGN", sign);
        header.add("OK-ACCESS-TIMESTAMP", epoch);
        header.add("OK-ACCESS-PASSPHRASE", ss[1]);
        return header;
    }

    public static String formatEpoch(Long time) {
        if (null == time) {
            return "";
        }
        return String.format("%s.%03d", time / 1000, time % 1000);
    }

    /**
     * get请求
     */
    private static List<MimeRequest> get(long delay, String url, ExchangeInfo info) {
        Header header = sign(delay, "GET", url, null, info);
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + url)
                .header(header)
                .build();
        return Collections.singletonList(request);
    }

    private static final String P = "{";
    private static final String CODE = "code";
    private static final int ERROR_CODE = 33007;
    private static final String RESULT = "result";
    private static final String TRUE = "true";

    /**
     * 解析多个订单
     * @param result 原始文件
     * @return 多个订单
     */
    private static List<Order> parseOrders(String result) {
        if (useful(result)) {
            if (result.trim().startsWith(P)) {
                JSONObject r = JSON.parseObject(result);
                if (r.containsKey(CODE) && ERROR_CODE == r.getInteger(CODE)) {
                    return Collections.emptyList();
                }
            }
            JSONArray r = JSON.parseArray(result);
            List<Order> orders = new ArrayList<>(r.size());
            for (int i = 0; i < r.size(); i++) {
                Order order = Okexv3Util.parseOrder(r.getString(i), r.getJSONObject(i));
                if (exist(order)) {
                    orders.add(order);
                } else {
                    return null;
                }
            }
            return orders;
        }
        return null;
    }

    /**
     * 解析订单明细
     *
     * @param result 原始数据
     * @param side   买卖方向
     * @return 订单明细
     */
    private static List<OrderDetail> parseOrderDetails(String result, String side) {
        if (useful(result)) {
            JSONArray r = JSON.parseArray(result);
            List<OrderDetail> details = new LinkedList<>();
            for (int i = 0; i < r.size(); i++) {
                JSONObject t = r.getJSONObject(i);
                /*
                 * [{
                 *
                 *     "exec_type": "T",
                 *     "fee": "0.018",
                 *     "instrument_id": "ETH-USDT",
                 *     "ledger_id": "1706",
                 *     "order_id": "1798782957193216",
                 *     "price": "90",
                 *     "side": "points_fee",
                 *     "size": "0",
                 *     "timestamp": "2018-11-14T08:14.09000Z"
                 * },
                 * {
                 *      "created_at":"2019-01-23T04:44:54.000Z",
                 *      "exec_type":"T",
                 *      "fee":"0",
                 *      "instrument_id":"LTC-USDT",
                 *      "ledger_id":"3463324607",
                 *      "liquidity":"T",
                 *      "order_id":"2194321788247040",
                 *      "price":"31.4824",
                 *      "product_id":"LTC-USDT",
                 *      "side":"sell",
                 *      "size":"0.03198611",
                 *      "timestamp":"2019-01-23T04:44:54.000Z"
                 *  }
                 */
                if (!side.equals(t.getString("side"))) {
                    continue;
                }
                Long time = t.getDate("timestamp").getTime();
                String orderId = t.getString("order_id");
                String detailId = t.getString("ledger_id");
                BigDecimal price = t.getBigDecimal("price");
                BigDecimal amount = t.getBigDecimal("size");
                BigDecimal fee = t.getBigDecimal("fee");
                details.add(new OrderDetail(r.getString(i), time, orderId, detailId, price, amount, fee, null));
            }
            return details;
        }
        return null;
    }

    /**
     * 创建订单
     *
     * @param info   请求信息
     * @param delay  延迟
     * @param type   订单类型 limit market
     * @param side   买卖方向 buy sell
     * @param others 其他参数
     * @return 订单请求
     */
    private List<MimeRequest> createOrder(ExchangeInfo info, long delay, String type, String side,
                                          String... others) {
        String symbol = this.symbol(info);
        Parameter parameter = Parameter.build();
        parameter.add("type", type);
        parameter.add("side", side);
        parameter.add("instrument_id", symbol);
        String body = CommonUtil.addOtherParameterToJson(parameter, others);
        return this.post(delay, body, info, CREATE_ORDER);
    }

    /**
     * post请求
     */
    private List<MimeRequest> post(long delay, String body, ExchangeInfo info, String url) {
        Header header = sign(delay, "POST", url, body, info);
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + url)
                .post()
                .header(header)
                .body(body)
                .build();
        return Collections.singletonList(request);
    }

    /**
     * 解析订单id
     */
    private static String parseOrderId(String result) {
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            if (r.containsKey(RESULT) && TRUE.equals(r.getString(RESULT))) {
                return r.getString("order_id");
            }
        }
        return null;
    }

    /**
     * 批量下单
     */
    private MimeRequest createOrders(ExchangeInfo info, long delay, String side, long startId,
                                     String... others) {
        String symbol = this.symbol(info);
        Parameter parameter = Parameter.build();
        parameter.add("type", "limit");
        parameter.add("side", side);
        parameter.add("instrument_id", symbol);
        List<String> multiBody = new LinkedList<>();
        for (int i = 0; i < others.length; i++) {
            parameter.put("client_oid", startId++);
            parameter.put("price", others[i]);
            parameter.put("size", others[++i]);
            multiBody.add(parameter.sort().json(JSON::toJSONString));
        }


        String body = "[" + String.join(",", multiBody) + "]";
        Header header = sign(delay, "POST", CREATE_ORDERS, body, info);
        return new MimeRequest.Builder()
                .url(ADDRESS + CREATE_ORDERS)
                .post()
                .header(header)
                .body(body)
                .build();
    }

    /**
     * 批量下单
     */
    private List<MimeRequest> createOrders(ExchangeInfo info, long delay, String side) {
        List<Row> rows = info.getRows();
        List<List<Row>> split = CommonUtil.split(rows, 4);
        return split.stream()
                .map(rs -> this.createOrders(info, delay, side, RandomUtil.uid(),
                        rs.stream()
                                .map(r -> Arrays.asList(r.getPrice().toPlainString(),
                                        r.getAmount().toPlainString()))
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList())
                                .toArray(new String[]{})))
                .collect(Collectors.toList());
    }

    /**
     * 批量下单的结果
     *
     * @param results 下单结果
     * @param symbol  币对，转换好的
     * @return ids
     */
    private List<String> parseMultiOrderIds(List<String> results, String symbol) {
        return results.stream()
                .map(JSON::parseObject)
                .filter(r -> r.containsKey(symbol))
                .map(r -> r.getJSONArray(symbol))
                .map(a -> a.toJavaList(JSONObject.class))
                .flatMap(Collection::stream)
                .filter(o -> o.containsKey(RESULT) && o.getBoolean(RESULT))
                .map(o -> o.getString("order_id"))
                .collect(Collectors.toList());
    }

}
