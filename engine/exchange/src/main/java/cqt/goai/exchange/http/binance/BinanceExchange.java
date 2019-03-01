package cqt.goai.exchange.http.binance;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.ExchangeInfo;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.exchange.http.HttpExchange;
import cqt.goai.exchange.util.CommonUtil;
import cqt.goai.model.enums.Side;
import cqt.goai.model.enums.State;
import cqt.goai.model.enums.Type;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.*;
import dive.common.crypto.HmacUtil;
import dive.http.common.MimeRequest;
import dive.http.common.model.Method;
import dive.http.common.model.Parameter;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static dive.common.math.BigDecimalUtil.div;
import static dive.common.math.BigDecimalUtil.greater;
import static dive.common.util.Util.useful;
import static java.math.BigDecimal.ZERO;

/**
 * @author xxx
 */
public class BinanceExchange extends HttpExchange {

    private static final String SITE = "api.binance.com";
    private static final String ADDRESS = "https://" + SITE;

    private static final String TICKER = "/api/v1/ticker/24hr";
    private static final String KLINES = "/api/v1/klines";
    private static final String DEPTH = "/api/v1/depth";
    private static final String TRADES = "/api/v1/aggTrades";

    private static final String BALANCES = "/api/v3/account";

    private static final String PRECISIONS = "/api/v1/exchangeInfo";
    private static final String ORDER = "/api/v3/order";

    private static final String ORDERS = "/api/v3/openOrders";
    private static final String ORDERS_HISTORY = "/api/v3/allOrders";

    public BinanceExchange(Logger log) {
        super(ExchangeName.BINANCE, log);
    }

    @Override
    public String symbol(ExchangeInfo info) {
        return symbol(info, s -> s.replace("_", ""));
    }

    @Override
    public List<MimeRequest> tickerRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + TICKER + "?symbol=" + this.symbol(info))
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Ticker transformTicker(List<String> results, ExchangeInfo info) {
        String result = results.get(0).trim();
        if (useful(result)) {
            JSONObject r;
            if (result.startsWith(LEFT_SQUARE_BRACKETS)) {
                r = JSON.parseArray(result).getJSONObject(0);
            } else {
                r = JSON.parseObject(result);
            }
            Long time = r.getLong("closeTime");
            return CommonUtil.parseTicker(r.toJSONString(), r, time,
                    "openPrice", "highPrice", "lowPrice",
                    "lastPrice", "volume");
        }
        return null;
    }

    @Override
    public List<MimeRequest> klinesRequests(ExchangeInfo info, long delay) {
        Integer size = 400;

        String type = period(info, period -> {
            switch (period) {
                case MIN1: return "1m";
                case MIN3: return "3m";
                case MIN5: return "5m";
                case MIN15: return "15m";
                case MIN30: return "30m";
                case HOUR1: return "1h";
                case HOUR2: return "2h";
//                case HOUR3: return "3h";
                case HOUR4: return "4h";
                case HOUR6: return "6h";
                case HOUR12: return "12h";
                case DAY1: return "1d";
                case DAY3: return "3d";
                case WEEK1: return "1w";
//                case WEEK2: return "2w";
                case MONTH1: return "1M";
                default: return null;
            }
        });

        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + KLINES + "?symbol=" + this.symbol(info)
                        + "&limit=" + size + "&interval=" + type)
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Klines transformKlines(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONArray array = JSON.parseArray(result);
            List<Kline> klines = new ArrayList<>(array.size());
            for (int i = array.size() - 1; 0 <= i; i--) {
                JSONArray t = array.getJSONArray(i);
                /*
                 * 1499040000000,      // Open time
                 * "0.01634790",       // Open
                 * "0.80000000",       // High
                 * "0.01575800",       // Low
                 * "0.01577100",       // Close
                 * "148976.11427815",  // Volume
                 * 1499644799999,      // Close time
                 * "2434.19055334",    // Quote asset volume
                 * 308,                // Number of trades
                 * "1756.87402397",    // Taker buy base asset volume
                 * "28.46694368",      // Taker buy quote asset volume
                 * "17928899.62484339" // Ignore.
                 */
                Long time = t.getLong(0) / 1000;
                Kline kline = CommonUtil.parseKlineByIndex(array.getString(i), t, time, 1);
                klines.add(kline);
//                    if (size <= records.size()) break;
            }
            return new Klines(klines);
        }
        return null;
    }

    @Override
    public List<MimeRequest> depthRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + DEPTH + "?symbol=" + this.symbol(info))
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Depth transformDepth(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSONObject.parseObject(result);
            JSONArray asks = r.getJSONArray("asks");
            JSONArray bids = r.getJSONArray("bids");
            Long time = System.currentTimeMillis();
            return CommonUtil.parseDepthByIndex(time, asks, bids);
        }
        return null;
    }

    @Override
    public List<MimeRequest> tradesRequests(ExchangeInfo info, long delay) {
        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + TRADES + "?symbol=" + this.symbol(info) + "&limit=100")
                .build();
        return Collections.singletonList(request);
    }

    @Override
    protected Trades transformTrades(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            try {
                JSONArray r = JSON.parseArray(result);
                List<Trade> trades = new ArrayList<>(r.size());
                for (int i = r.size() - 1; 0 <= i; i--) {
                    JSONObject t = r.getJSONObject(i);
                    /*
                     * "a": 26129,         // Aggregate tradeId
                     * "p": "0.01633102",  // Price
                     * "q": "4.70443515",  // Quantity
                     * "f": 27781,         // First tradeId
                     * "l": 27781,         // Last tradeId
                     * "T": 1498793709153, // Timestamp
                     * "m": true,          // Was the buyer the maker?
                     * "M": true           // Was the trade the best price match?
                     */
                    Long time = t.getLong("T");
                    String id = t.getString("f") + "_" + t.getString("l");
                    Side side = t.getBoolean("m") ? Side.SELL : Side.BUY;
                    BigDecimal price = t.getBigDecimal("p");
                    BigDecimal amount = t.getBigDecimal("q");
                    Trade trade = new Trade(r.getString(i), time, id, side, price, amount);
                    trades.add(trade);
                }

                return new Trades(trades);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public List<MimeRequest> balancesRequests(ExchangeInfo info, long delay) {
        return this.get(info, delay, BALANCES);
    }

    @Override
    protected Balances transformBalances(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONArray r = JSON.parseObject(result).getJSONArray("balances");
            List<Balance> balances = new ArrayList<>(r.size());
            for (int i = 0; i < r.size(); i++) {
                JSONObject t = r.getJSONObject(i);
                String currency = t.getString("asset");
                BigDecimal free = t.getBigDecimal("free");
                BigDecimal used = t.getBigDecimal("locked");
                balances.add(new Balance(r.getString(i), currency, free, used));
            }
            return new Balances(balances);
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

    /**
     * {
     *  "symbol":"ETHBTC",
     *  "status":"TRADING",
     *  "baseAsset":"ETH",
     *  "baseAssetPrecision":8,
     *  "quoteAsset":"BTC",
     *  "quotePrecision":8,
     *  "orderTypes":Array[5],
     *  "icebergAllowed":true,
     *  "filters":[
     *      {
     *          "filterType":"PRICE_FILTER",
     *          "minPrice":"0.00000000",
     *          "maxPrice":"0.00000000",
     *          "tickSize":"0.00000100"
     *      },
     *      {
     *          "filterType":"PERCENT_PRICE",
     *          "multiplierUp":"10",
     *          "multiplierDown":"0.1",
     *          "avgPriceMins":5
     *      },
     *      {
     *          "filterType":"LOT_SIZE",
     *          "minQty":"0.00100000",
     *          "maxQty":"100000.00000000",
     *          "stepSize":"0.00100000"
     *      },
     *      {
     *          "filterType":"MIN_NOTIONAL",
     *          "minNotional":"0.00100000",
     *          "applyToMarket":true,
     *          "avgPriceMins":5
     *      },
     *      {
     *          "filterType":"ICEBERG_PARTS",
     *          "limit":10
     *      },
     *      {
     *          "filterType":"MAX_NUM_ALGO_ORDERS",
     *          "maxNumAlgoOrders":5
     *      }
     *  ]
     * }
     */
    @Override
    protected Precisions transformPrecisions(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            JSONArray symbols = r.getJSONArray("symbols");
            List<Precision> precisions = new ArrayList<>(symbols.size());
            for (int i = 0; i < symbols.size(); i++) {

                JSONObject t = symbols.getJSONObject(i);
                if (!"TRADING".equals(t.getString("status"))) {
                    continue;
                }

                String data = symbols.getString(i);
                String symbol = t.getString("baseAsset") + "_" + t.getString("quoteAsset");
                Integer base = t.getInteger("baseAssetPrecision");
                Integer quote = t.getInteger("quotePrecision");

                JSONArray filters = t.getJSONArray("filters");
                JSONObject baseFilter = null;
                JSONObject priceFilter = null;
                for (int j = 0; j < filters.size(); j++) {
                    JSONObject temp = filters.getJSONObject(j);
                    if ("PRICE_FILTER".equals(temp.getString("filterType"))) {
                        priceFilter = temp;
                    } else if ("LOT_SIZE".equals(temp.getString("filterType"))){
                        baseFilter = temp;
                    }
                }

                BigDecimal baseStep = this.getBigDecimal(baseFilter, "stepSize");
                BigDecimal quoteStep = this.getBigDecimal(priceFilter, "tickSize");
                BigDecimal minBase = this.getBigDecimal(baseFilter, "minQty");
                BigDecimal minQuote = this.getBigDecimal(priceFilter, "minPrice");
                BigDecimal maxBase = this.getBigDecimal(baseFilter, "maxQty");
                BigDecimal maxQuote = this.getBigDecimal(priceFilter, "maxPrice");

                Precision precision = new Precision(data, symbol, base, quote,
                        baseStep, quoteStep, minBase, minQuote, maxBase, maxQuote);
                precisions.add(precision);
            }
            return new Precisions(precisions);
        }
        return null;
    }


    @Override
    public List<MimeRequest> buyLimitRequests(ExchangeInfo info, long delay) {
        return this.postOrder(info, delay,
                "symbol", this.symbol(info),
                "side", "BUY",
                "type", "LIMIT",
                "timeInForce", "GTC",
                "quantity", info.getAmount(),
                "price", info.getPrice());
    }

    @Override
    public String transformBuyLimit(List<String> results, ExchangeInfo info) {
        return this.getId(results);
    }

    @Override
    public List<MimeRequest> sellLimitRequests(ExchangeInfo info, long delay) {
        return this.postOrder(info, delay,
                "symbol", this.symbol(info),
                "side", "SELL",
                "type", "LIMIT",
                "timeInForce", "GTC",
                "quantity", info.getAmount(),
                "price", info.getPrice());
    }

    @Override
    public String transformSellLimit(List<String> results, ExchangeInfo info) {
        return this.getId(results);
    }

    @Override
    public List<MimeRequest> buyMarketRequests(ExchangeInfo info, long delay) {
        return this.postOrder(info, delay,
                "symbol", this.symbol(info),
                "side", "BUY",
                "type", "MARKET",
                "timeInForce", "GTC",
                "quantity", info.getQuote());
    }

    @Override
    public String transformBuyMarket(List<String> results, ExchangeInfo info) {
        return this.getId(results);
    }

    @Override
    public List<MimeRequest> sellMarketRequests(ExchangeInfo info, long delay) {
        return this.postOrder(info, delay,
                "symbol", this.symbol(info),
                "side", "SELL",
                "type", "MARKET",
                "timeInForce", "GTC",
                "quantity", info.getBase());
    }

    @Override
    public String transformSellMarket(List<String> results, ExchangeInfo info) {
        return this.getId(results);
    }

    @Override
    public List<MimeRequest> cancelOrderRequests(ExchangeInfo info, long delay) {
        return this.request(info, delay, ORDER, Method.DELETE,
                "orderId", info.getCancelId(),
                "symbol", this.symbol(info));
    }

    @Override
    protected Boolean transformCancelOrder(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            this.parseOrder(null, JSON.parseObject(result));
            return true;
        }
        return null;
    }


    @Override
    public List<MimeRequest> ordersRequests(ExchangeInfo info, long delay) {
        return this.get(info, delay, ORDERS,
                "symbol", this.symbol(info));
    }

    @Override
    protected Orders transformOrders(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            JSONArray r = JSON.parseArray(result);
            List<Order> orders = new LinkedList<>();
            for (int i = 0; i < r.size(); i++) {
                orders.add(this.parseOrder(r.getString(i), r.getJSONObject(i)));
            }
            orders = orders.stream().sorted(CommonUtil::sortOrder).collect(Collectors.toList());
            return new Orders(orders);
        }
        return null;
    }

    @Override
    public List<MimeRequest> historyOrdersRequests(ExchangeInfo info, long delay) {
        return this.get(info, delay, ORDERS_HISTORY,
                "symbol", this.symbol(info), "limit", 500);
    }

    @Override
    protected Orders transformHistoryOrders(List<String> results, ExchangeInfo info) {
        return this.transformOrders(results, info);
    }

    @Override
    public List<MimeRequest> orderRequests(ExchangeInfo info, long delay) {
        return this.get(info, delay, ORDER,
                "symbol", this.symbol(info),
                "orderId", info.getOrderId());
    }

    @Override
    protected Order transformOrder(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return this.parseOrder(result, JSON.parseObject(result));
        }
        return null;
    }

    @Override
    public List<MimeRequest> orderDetailsRequests(ExchangeInfo info, long delay) {
        return this.get(info, delay, ORDER,
                "symbol", this.symbol(info),
                "orderId", info.getOrderId());
    }

    @Override
    protected OrderDetails transformOrderDetails(List<String> results, ExchangeInfo info) {
        String result = results.get(0);
        if (useful(result)) {
            return this.parseOrderDetails(JSON.parseObject(result));
        }
        return null;
    }

    // ====================== tools ============================

    private static final String LEFT_SQUARE_BRACKETS = "[";
    private static final String FILLS = "fills";

    private List<MimeRequest> request(ExchangeInfo info, long delay, String api, Method method, Object... others) {
        String access = info.getAccess();
        String secret = info.getSecret();

        Parameter parameter = Parameter.build("timestamp", System.currentTimeMillis() + delay);
        CommonUtil.addOtherParameter(parameter, others);

        String para = parameter.concat();
        String sign = HmacUtil.HmacSHA256(para, secret);

        MimeRequest request = new MimeRequest.Builder()
                .url(ADDRESS + api + "?" + para + "&signature=" + sign)
                .header("X-MBX-APIKEY", access)
                .method(method)
                .body(Method.POST == method ? "" : null)
                .build();
        return Collections.singletonList(request);
    }

    private List<MimeRequest> get(ExchangeInfo info, long delay, String api, Object... others) {
        return this.request(info, delay, api, Method.GET, others);
    }

    private List<MimeRequest> postOrder(ExchangeInfo info, long delay, Object... others) {
        return this.request(info, delay, BinanceExchange.ORDER, Method.POST, others);
    }

    private Order parseOrder(String result, JSONObject t) {
        /*
         * "symbol": "LTCBTC",
         * "orderId": 1,
         * "clientOrderId": "myOrder1",
         * "price": "0.1",
         * "origQty": "1.0",
         * "executedQty": "0.0",
         * "cummulativeQuoteQty": "0.0",
         * "status": "NEW",
         * "timeInForce": "GTC",
         * "type": "LIMIT",
         * "side": "BUY",
         * "stopPrice": "0.0",
         * "icebergQty": "0.0",
         * "time": 1499827319559,
         * "updateTime": 1499827319559,
         * "isWorking": true
         */
        Long time = t.getLong("updateTime");
        String id = t.getString("orderId");
        State state = null;
        switch (t.getString("status")) {
            case "NEW": state = State.SUBMIT; break;
            case "PARTIALLY_FILLED": state = State.PARTIAL; break;
            case "FILLED": state = State.FILLED; break;
            case "CANCELED": state = State.CANCEL; break;
            case "PENDING_CANCEL": state = State.CANCEL; break;
            case "REJECTED": state = State.CANCEL; break;
            case "EXPIRED": state = State.CANCEL; break;
            default:
        }
        Side side = Side.valueOf(t.getString("side"));
        Type type = null;
        //STOP_LOSS
        //STOP_LOSS_LIMIT
        //TAKE_PROFIT
        //TAKE_PROFIT_LIMIT
        //LIMIT_MAKER
        switch (t.getString("type")) {
            case "LIMIT" : type = Type.LIMIT; break;
            case "MARKET" : type = Type.MARKET; break;
            default:
        }

        BigDecimal price = t.getBigDecimal("price");
        BigDecimal amount = t.getBigDecimal("origQty");
        BigDecimal deal = t.getBigDecimal("executedQty");
        BigDecimal average = greater(deal, ZERO) ?
                div(t.getBigDecimal("cummulativeQuoteQty"), deal) : ZERO;
        return new Order(result, time, id, side, type,
                greater(deal, ZERO) && state == State.CANCEL ? State.UNDONE : state,
                price, amount, deal, average);
    }

    private OrderDetails parseOrderDetails(JSONObject t) {
        /*
         * "symbol": "LTCBTC",
         * "orderId": 1,
         * "clientOrderId": "myOrder1",
         * "price": "0.1",
         * "origQty": "1.0",
         * "executedQty": "0.0",
         * "cummulativeQuoteQty": "0.0",
         * "status": "NEW",
         * "timeInForce": "GTC",
         * "type": "LIMIT",
         * "side": "BUY",
         * "stopPrice": "0.0",
         * "icebergQty": "0.0",
         * "time": 1499827319559,
         * "updateTime": 1499827319559,
         * "isWorking": true
         */
        Long time = t.getLong("updateTime");
        String id = t.getString("orderId");
        Side side = Side.valueOf(t.getString("side"));

        List<OrderDetail> details = new LinkedList<>();

        if (t.containsKey(FILLS)) {
            JSONArray fills = t.getJSONArray(FILLS);
            for (int i = 0; i < fills.size(); i++) {
                JSONObject tt = fills.getJSONObject(i);
                BigDecimal price = tt.getBigDecimal("price");
                BigDecimal amount = tt.getBigDecimal("qty");
                BigDecimal fee = tt.getBigDecimal("commission");
                String feeCurrency = tt.getString("commissionAsset");
                details.add(new OrderDetail(fills.getString(i), time, id, null, price, amount, fee, feeCurrency, side));
            }
        }
        return new OrderDetails(details);
    }

    private BigDecimal getBigDecimal(JSONObject t, String name) {
        if (null == t) {
            return null;
        }
        BigDecimal number = t.getBigDecimal(name);
        if (greater(number, ZERO)) {
            return number;
        }
        return null;
    }

    private String getId(List<String> results) {
        String result = results.get(0);
        if (useful(result)) {
            JSONObject r = JSON.parseObject(result);
            return r.getString("orderId");
        }
        return null;
    }

}
