package cqt.goai.exchange.http.huobi.pro;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.ExchangeException;
import cqt.goai.exchange.ExchangeInfo;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.exchange.http.HttpExchange;
import cqt.goai.exchange.util.Seal;
import cqt.goai.exchange.util.huobi.pro.HoubiProUtil;
import cqt.goai.model.enums.Side;
import cqt.goai.model.enums.State;
import cqt.goai.model.enums.Type;
import cqt.goai.model.market.Klines;
import cqt.goai.model.market.Depth;
import cqt.goai.model.market.Ticker;
import cqt.goai.model.market.Trades;
import cqt.goai.model.trade.*;
import dive.common.crypto.Base64Util;
import dive.common.crypto.HmacUtil;
import dive.common.util.DateUtil;
import dive.http.common.MimeRequest;
import dive.http.common.model.Parameter;
import dive.cache.mime.PersistCache;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static dive.common.math.BigDecimalUtil.div;
import static dive.common.math.BigDecimalUtil.greater;
import static dive.common.util.Util.exist;
import static dive.common.util.Util.useful;
import static java.math.BigDecimal.ZERO;


/**
 * @author GOAi
 */
public class HuobiProExchange extends HttpExchange {

    private static final PersistCache<String, String> CACHE = new PersistCache<>("huobipro");

    private static final String OK = "ok";
    private static final String ERROR = "error";
    private static final String ERROR_CODE = "err-code";
    private static final String ORDER_ERROR = "order-orderstate-error";
    private static final String BASE_RECORD = "base-record-invalid";
    private static final String STATUS = "status";
    private static final String DATA = "data";

    protected String site = "api.huobi.pro";
    protected String address = "https://" + site;

    protected String apiTicker = "/market/detail/merged";
    protected String apiKlines = "/market/history/kline";
    protected String apiDepth = "/market/depth";
    protected String apiTrades = "/market/history/trade";

    protected String apiAccountId = "/v1/account/accounts";
    protected String apiBalances = "/v1/account/accounts/{account-id}/balance";


    protected String apiPrecisions = "/v1/common/symbols";
    protected String apiPlace = "/v1/order/orders/place";
    protected String apiCancel = "/v1/order/orders/{order-id}/submitcancel";
//    private static final String CANCEL_ALL = "/v1/order/orders/batchcancel";

    protected String apiOrders = "/v1/order/openOrders";
    protected String apiHistoryOrders = "/v1/order/orders";
    protected String apiOrder = "/v1/order/orders/{order-id}";
    protected String apiOrderDetails = "/v1/order/orders/{order-id}/matchresults";

    public HuobiProExchange(Logger log) {
        super(ExchangeName.HUOBIPRO, log);
    }

    protected HuobiProExchange(ExchangeName name, Logger log) {
        super(name, log);
    }

    @Override
    public String symbol(ExchangeInfo info) {
        return symbol(info, s -> s.replace("_", "").toLowerCase());
    }

    @Override
    public List<MimeRequest> tickerRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(address + apiTicker)
                .body("symbol", this.symbol(info))
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Ticker transformTicker(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return HoubiProUtil.parseTicker(result);
        }
        return null;
    }

    @Override
    public List<MimeRequest> klinesRequests(ExchangeInfo info, long delay) {
        String period = super.period(info, HoubiProUtil::getPeriod);
        MimeRequest request = new MimeRequest.Builder()
                .url(address + apiKlines)
                .body("symbol", this.symbol(info))
                .body("period", period)
                .body("size", 400)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Klines transformKlines(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return HoubiProUtil.parseKlines(result);
        }
        return null;
    }

    @Override
    public List<MimeRequest> depthRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(address + apiDepth)
                .body("symbol", this.symbol(info))
                .body("type", "step0")
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Depth transformDepth(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return HoubiProUtil.parseDepth(result);
        }
        return null;
    }

    @Override
    public List<MimeRequest> tradesRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(address + apiTrades)
                .body("symbol", this.symbol(info))
                .body("size", 100)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Trades transformTrades(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return HoubiProUtil.parseTrades(result);
        }
        return null;
    }



    @Override
    public List<MimeRequest> balancesRequests(ExchangeInfo info, long delay) {
        String access = info.getAccess();
        String secret = info.getSecret();

        String accountId = this.getAccountId(access, secret);

        String url = apiBalances.replace("{account-id}", accountId);
        Parameter parameter = this.addParam(access, delay);

        return this.get(parameter, secret, url);
    }

    @Override
    protected Balances transformBalances(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return HoubiProUtil.parseBalances(result);
        }
        return null;
    }

    @Override
    public List<MimeRequest> accountRequests(ExchangeInfo info, long delay) {
        return this.balancesRequests(info, delay);
    }

    @Override
    protected Account transformAccount(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return HoubiProUtil.parseAccount(result, info.getSymbol());
        }
        return null;
    }



    @Override
    public List<MimeRequest> precisionsRequests(ExchangeInfo info, long delay) {
        String url = address + apiPrecisions;
        MimeRequest request = new MimeRequest.Builder()
                .url(url)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Precisions transformPrecisions(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return HoubiProUtil.parsePrecisions(result);
        }
        return null;
    }


    @Override
    public List<MimeRequest> precisionRequests(ExchangeInfo info, long delay) {
        return this.precisionsRequests(info, delay);
    }

    @Override
    protected Precision transformPrecision(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return HoubiProUtil.parsePrecision(result, symbol(info));
        }
        return null;
    }


    @Override
    public List<MimeRequest> buyLimitRequests(ExchangeInfo info, long delay) {
        return this.place(info, delay, info.getPrice(), info.getAmount(), BUY_LIMIT);
    }

    @Override
    public String transformBuyLimit(List<String> results, ExchangeInfo info) {
        return this.getOrderId(results.get(0));
    }

    @Override
    public List<MimeRequest> sellLimitRequests(ExchangeInfo info, long delay) {
        return this.place(info, delay, info.getPrice(), info.getAmount(), SELL_LIMIT);
    }

    @Override
    public String transformSellLimit(List<String> results, ExchangeInfo info) {
        return this.getOrderId(results.get(0));
    }


    @Override
    public List<MimeRequest> buyMarketRequests(ExchangeInfo info, long delay) {
        return this.place(info, delay, null, info.getQuote(), BUY_MARKET);
    }

    @Override
    public String transformBuyMarket(List<String> results, ExchangeInfo info) {
        return this.getOrderId(results.get(0));
    }

    @Override
    public List<MimeRequest> sellMarketRequests(ExchangeInfo info, long delay) {
        return this.place(info, delay, null, info.getBase(), SELL_MARKET);
    }

    @Override
    public String transformSellMarket(List<String> results, ExchangeInfo info) {
        return this.getOrderId(results.get(0));
    }


    @Override
    public List<MimeRequest> cancelOrderRequests(ExchangeInfo info, long delay) {
        String access = info.getAccess();
        String secret = info.getSecret();
        String id = info.getCancelId();

        String url = apiCancel.replace("{order-id}", id);
        Parameter parameter = this.addParam(access, delay);
        parameter.add("Signature", encode(this.sign(secret, "POST", site, url, parameter)));
        MimeRequest request = new MimeRequest.Builder()
                .url(address + url + "?" + parameter.concat())
                .post()
                .header("Accept-Language", "zh-CN")
                .body("")
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Boolean transformCancelOrder(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            if (OK.equals(r.getString(STATUS))) {
                return true;
            }
            if (ERROR.equals(r.getString(STATUS)) && ORDER_ERROR.equals(r.getString(ERROR_CODE))) {
                return false;
            }
        }
        return null;
    }

    /*@Override
    public List<MimeRequest> cancelOrdersRequests(ExchangeInfo info, long delay) {
        String access = info.getAccess();
        String secret = info.getSecret();
        List<String> ids = info.getCancelIds();

        List<MimeRequest> list = new ArrayList<>();
        if (!exist(ids) || 0 == ids.size()) {
            return list;
        }
        List<List<String>> split = CommonUtil.split(ids, 50);
        return split.stream().map(group -> {
            Parameter parameter = this.addParam(access, delay, "order-ids", group);
            parameter.add("Signature", encode(this.sign(secret, "POST", site, CANCEL_ALL, parameter)));
            parameter.put("Timestamp", parameter.get("Timestamp").toString().replace("%3A", ":"));
            parameter.put("Signature", parameter.get("Signature").toString().replace("%2B", "+").replace("%3D", "="));
            return new MimeRequest.Builder()
                    .url(address + CANCEL_ALL)
                    .post()
                    .header("Accept-Language", "zh-CN")
                    .body(parameter.json(JSON::toJSONString))
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> transformCancelOrders(List<String> results, ExchangeInfo info) {
        List<String> ids = new LinkedList<>();
        for (int i = 0; i < results.size(); i++) {
            String result = results.get(i);
            JSONObject r = JSON.parseObject(result);
            if (OK.equals(r.getString(STATUS))) {
                JSONArray temp = r.getJSONObject("data").getJSONArray("success");
                for (int j = 0; j < temp.size(); i++) {
                    ids.add(temp.getString(j));
                }
            } else {
                return null;
            }
        }
        return ids;
    }*/

    @Override
    public List<MimeRequest> ordersRequests(ExchangeInfo info, long delay) {
        String access = info.getAccess();
        String secret = info.getSecret();

        Parameter parameter = this.addParam(access, delay,
                "account-id", this.getAccountId(access, secret),
                "symbol", this.symbol(info),
                "size", String.valueOf(500));
        return this.get(parameter, secret, apiOrders);
    }

    @Override
    protected Orders transformOrders(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        List<Order> orders = this.parseOrders(result, info.tip());
        return exist(orders) ? new Orders(orders) : null;
    }

    @Override
    public List<MimeRequest> historyOrdersRequests(ExchangeInfo info, long delay) {
        String access = info.getAccess();
        String secret = info.getSecret();
        Integer size = 100;

        String s = "submitting" +
                "%2Csubmitted" +
                "%2Cpartial-filled" +
                "%2Cpartial-canceled" +
                "%2Cfilled" +
                "%2Ccanceled";
        Parameter parameter = this.addParam(access, delay,
                "symbol", symbol(info),
                "states", s,
                "size", String.valueOf(size));
        return this.get(parameter, secret, apiHistoryOrders);
    }

    @Override
    protected Orders transformHistoryOrders(List<String> results, ExchangeInfo info) {
        return this.transformOrders(results, info);
    }

    @Override
    public List<MimeRequest> orderRequests(ExchangeInfo info, long delay) {
        return this.getRequests(info, delay, apiOrder);
    }

    @Override
    protected Order transformOrder(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            if (OK.equals(r.getString(STATUS))) {
                JSONObject data = r.getJSONObject("data");
                return parseOrder(r.getString("data"), data, info.tip());
            }
        }
        return null;
    }

    @Override
    public List<MimeRequest> orderDetailsRequests(ExchangeInfo info, long delay) {
        return this.getRequests(info, delay, apiOrderDetails);
    }

    @Override
    public OrderDetails transformOrderDetails(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            if (OK.equals(r.getString(STATUS))) {
                return new OrderDetails(this.parseOrderDetails(r));
            }
            if (BASE_RECORD.equals(r.get(ERROR_CODE))) {
                return new OrderDetails(Collections.emptyList());
            }
        }
        return null;
    }

    // =================================== tools ======================================

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final String BUY_LIMIT = "buy-limit";
    private static final String BUY_MARKET = "buy-market";
    private static final String SELL_LIMIT = "sell-limit";
    private static final String SELL_MARKET = "sell-market";

    private static final String SUBMITTING = "submitting";
    private static final String SUBMITTED = "submitted";
    private static final String PARTIAL_FILLED = "partial-filled";
    private static final String PARTIAL_CANCELED = "partial-canceled";
    private static final String FILLED = "filled";
    private static final String CANCELED = "canceled";
    private static final String CANCELLING = "cancelling";

    /**
     * 获取UTC时间
     * @param delay 延迟
     * @return UTC时间
     */
    private static String getUTCTimeString(long delay) {
        // 1、取得本地时间：
        Calendar cal = Calendar.getInstance();
        // 2、取得时间偏移量：
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
        // 3、取得夏令时差：
        int dstOffset = cal.get(Calendar.DST_OFFSET);
        // 4、本地时间扣除差量，即可以取得UTC时间：
        cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        // 5、网络延迟时间
        cal.add(Calendar.MILLISECOND, (int) delay);
        return DateUtil.format(new Date(cal.getTime().getTime()), DTF);
    }

    /**
     * 编码转换
     * @param value 值
     * @return 编码后
     */
    private static String encode(String value) {
        return value.replace(":", "%3A")
                .replace("=", "%3D")
                .replace("/", "%2F")
                .replace(" ", "%20")
                .replace(",", "%2C")
                .replace("+", "%2B");
    }

    /**
     * 添加参数
     * @param access access
     * @param delay 延迟
     * @param kv 其他参数
     * @return 请求参数
     */
    private Parameter addParam(String access, long delay, Object... kv) {
        Parameter map = Parameter.build();
        map.put("AccessKeyId", access);
        map.put("SignatureMethod", "HmacSHA256");
        map.put("SignatureVersion", "2");
        map.put("Timestamp", HuobiProExchange.encode(HuobiProExchange.getUTCTimeString(delay)));
        for (int i = 0; i < kv.length; i++) {
            map.put(kv[i].toString(), kv[++i]);
        }
        return map;
    }

    /**
     * 签名
     * @param secret 秘钥
     * @param method 请求方法
     * @param address 请求地址
     * @param api 请求路径
     * @param map 参数
     * @return 签名
     */
    private String sign(String secret, String method, String address, String api, Parameter map) {
        StringBuilder builder = new StringBuilder();
        builder.append(method).append("\n");
        builder.append(address).append("\n");
        this.addSignParam(builder, api);
        List<String> keys = new ArrayList<>(map.size());
        keys.addAll(map.keySet());
        Collections.sort(keys);
        StringBuilder sb = new StringBuilder();
        if (keys.size() >= 1) {
            sb.append(keys.get(0)).append("=")
                    .append(encode(map.get(keys.get(0)).toString()));
            for (int i = 1, s = keys.size(); i < s; i++) {
                sb.append("&").append(keys.get(i)).append("=")
                        .append(encode(map.get(keys.get(i)).toString()));
            }
        }
        builder.append(sb.toString());
        try {
            byte[] sign = HmacUtil.hmac(builder.toString().getBytes(), secret, HmacUtil.HMAC_SHA256);
            return Base64Util.base64EncodeToString(sign);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
        throw new ExchangeException(super.name.getName() + " sign error.");
    }

    /**
     * exshell 不需要这个
     * @param builder 签名串
     * @param api api
     */
    protected void addSignParam(StringBuilder builder, String api) {
        builder.append(api).append("\n");
    }

    /**
     * 获取account id
     * @param access access
     * @param secret secret
     * @return 请求结果
     */
    private synchronized String getAccounts(String access, String secret) {
        Parameter parameter = this.addParam(access, 0);
        parameter.add("Signature", encode(this.sign(secret, "GET", site, apiAccountId, parameter)));
        return new MimeRequest.Builder()
                .url(address + apiAccountId + "?" + parameter.concat())
                .build()
                .execute(super.http);
    }

    /**
     * 获取account id
     * @param access access
     * @param secret secret
     * @return account id
     */
    private String getAccountId(String access, String secret) {
        String id = this.getCache().get(access);
        if (!useful(id)) {
            String result = this.getAccounts(access, secret);
            if (useful(result)) {
                JSONObject r = JSON.parseObject(result);
                if (r.containsKey(STATUS) && OK.equals(r.getString(STATUS))) {
                    JSONArray data = r.getJSONArray(DATA);
                    for (int i = 0, s = data.size(); i < s; i++) {
                        JSONObject t = data.getJSONObject(i);
                        String accountId = t.getString("id");
                        String type = t.getString("type");
                        if ("spot".equals(type)) {
                            id = accountId;
                            this.getCache().set(access, id);
                            return id;
                        }
                    }
                }
            }
            throw new ExchangeException(super.name.getName() + " " + Seal.seal(access)
                    + " account_id: result is " + result);
        }
        return id;
    }

    /**
     * 获取缓存
     * @return 缓存
     */
    protected PersistCache<String, String> getCache() {
        return CACHE;
    }

    /**
     * get请求
     * @param parameter 请求参数
     * @param secret 秘钥
     * @param api 请求路径
     * @return 请求信息
     */
    private List<MimeRequest> get(Parameter parameter, String secret, String api) {
        parameter.add("Signature", encode(this.sign(secret, "GET", site, api, parameter)));

        MimeRequest request = new MimeRequest.Builder()
                .url(address + api + "?" + parameter.concat())
                .build();
        return Collections.singletonList(request);
    }

    /**
     * 解析订单
     * @param result 订单结果
     * @param data json
     * @param tip 错误提示
     * @return 解析订单
     */
    protected Order parseOrder(String result, JSONObject data, String tip) {
        Long time = data.getLong("created-at");
        String id = data.getString("id");
        String[] types = data.getString("type").split("-");
        Side side = Side.valueOf(types[0].toUpperCase());
        Type type = null;
        switch (types[1]) {
            case "limit": type = Type.LIMIT; break;
            case "market": type = Type.MARKET; break;
            case "ioc": type = Type.IMMEDIATE_OR_CANCEL; break;
            default:
        }
        State state;
        switch (data.getString("state")) {
            case SUBMITTING:
            case SUBMITTED: state = State.SUBMIT; break;
            case PARTIAL_FILLED: state = State.PARTIAL; break;
            case CANCELLING: state = State.SUBMIT; break;
            case FILLED: state = State.FILLED; break;
            case CANCELED: state = State.CANCEL; break;
            case PARTIAL_CANCELED: state = State.UNDONE; break;
            default: log.error("{} {} parseOrder state --> {}", name, tip, data); return null;
        }
        BigDecimal price = data.getBigDecimal("price");
        BigDecimal amount = data.getBigDecimal("amount");
        BigDecimal deal = data.getBigDecimal("field-amount");
        BigDecimal average = exist(deal) && greater(deal, ZERO) ?
                div(data.getBigDecimal("field-cash-amount"), deal, 16) : null;
        if (state == State.SUBMIT && null != deal && greater(deal, ZERO)) {
            state = State.PARTIAL;
        }
        return new Order(result, time, id, side, type, state, price, amount, deal, average);
    }

    /**
     * 解析订单列表
     * @param result 请求结果
     * @param tip 错误提示
     * @return 解析结果
     */
    private List<Order> parseOrders(String result, String tip) {
        if (useful(result)) {
            try {
                JSONObject r = JSON.parseObject(result);
                if (OK.equals(r.getString(STATUS))) {
                    JSONArray data = r.getJSONArray("data");
                    List<Order> orders = new ArrayList<>(data.size());
                    for (int i = 0, l = data.size(); i < l; i++) {
                        Order order = this.parseOrder(data.getString(i), data.getJSONObject(i), tip);
                        if (exist(order)) {
                            orders.add(order);
                        }
                    }
                    return orders;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 订单 和 订单明细
     * @param info 请求信息
     * @param delay 延时
     * @param api api
     * @return 请求信息
     */
    private List<MimeRequest> getRequests(ExchangeInfo info, long delay, String api) {
        String access = info.getAccess();
        String secret = info.getSecret();
        String id = info.getOrderId();

        String url = api.replace("{order-id}", id);
        Parameter parameter = this.addParam(access, delay);
        return this.get(parameter, secret, url);
    }

    /**
     * 下单
     * @param info 请求信息
     * @param delay 延时
     * @param price 价格
     * @param amount 数量
     * @param type 类型
     * @return 请求信息
     */
    private List<MimeRequest> place(ExchangeInfo info, long delay,
                                    String price, String amount, String type) {
        String symbol = this.symbol(info);
        String access = info.getAccess();
        String secret = info.getSecret();

        String accountId = this.getAccountId(access, secret);
        Parameter parameter = this.addParam(access, delay);
        String sign = this.sign(secret, "POST", site, apiPlace, parameter);
        parameter.add("Signature", encode(sign));
        Parameter param = Parameter.build()
                .add("account-id", accountId)
                .add("amount", amount)
                .add("source", "api")
                .add("symbol", symbol)
                .add("type", type);
        if (useful(price)) {
            param.add("price", price);
        }
        MimeRequest request = new MimeRequest.Builder()
                .url(address + apiPlace + "?" + parameter.concat())
                .post()
                .header("Accept-Language", "zh-CN")
//                .header("Content-Type", "application/json;charset=UTF-8")
                .body(param.json(JSON::toJSONString))
                .build();
        return Collections.singletonList(request);
    }

    /**
     * 解析id
     * @param result 请求结果
     * @return 订单id
     */
    private String getOrderId(String result) {
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            if (OK.equals(r.getString(STATUS))) {
                return r.getString("data");
            }
        }
        return null;
    }

    /**
     * 解析订单明细
     * @param r json
     * @return 订单明细
     */
    private List<OrderDetail> parseOrderDetails(JSONObject r) {
        JSONArray data = r.getJSONArray("data");
        /*
         * "data": [
         *     {
         *       "id": 29553,
         *       "order-id": 59378,
         *       "match-id": 59335,
         *       "symbol": "ethusdt",
         *       "type": "buy-limit",
         *       "source": "api",
         *       "price": "100.1000000000",
         *       "filled-amount": "9.1155000000",
         *       "filled-fees": "0.0182310000",
         *       "created-at": 1494901400435
         *     }
         *   ]
         */
        List<OrderDetail> details = new ArrayList<>(data.size());
        for (int i = 0; i < data.size(); i++) {
            JSONObject t = data.getJSONObject(i);
            Long time = t.getLong("created-at");
            String orderId = t.getString("order-id");
            String detailId = t.getString("match-id");
            BigDecimal price = t.getBigDecimal("price");
            BigDecimal amount = t.getBigDecimal("filled-amount");
            BigDecimal fee = t.getBigDecimal("filled-fees");
            OrderDetail detail = new OrderDetail(data.getString(i), time, orderId, detailId, price, amount, fee, null);
            details.add(detail);
        }
        return details;
    }

}
