package cqt.goai.exchange.util.bitfinex;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.util.CommonUtil;
import cqt.goai.model.enums.Period;
import cqt.goai.model.enums.Side;
import cqt.goai.model.enums.State;
import cqt.goai.model.enums.Type;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.Balance;
import cqt.goai.model.trade.Order;
import cqt.goai.model.trade.OrderDetail;
import cqt.goai.model.trade.Precision;
import dive.common.math.BigDecimalUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static dive.common.math.BigDecimalUtil.*;


/**
 * @author
 */
public class BitfinexUtil {

    private static final String EXCHANGE_LIMIT = "exchange limit";
    private static final String EXCHANGE_MARKET = "exchange market";
    private static final String EXCHANGE_FOK = "exchange fill-or-kill";

    /**
     * bitfinex 的周期换算
     *
     * @param period 周期
     * @return 周期长度 秒
     */
    public static String getPeriod(Period period) {
        switch (period) {
            // '1m', '5m', '15m', '30m', '1h', '3h', '6h', '12h', '1D', '7D', '14D', '1M'
            case MIN1:
                return "1m";
//            case MIN3: return "3m";
            case MIN5:
                return "5m";
            case MIN15:
                return "15m";
            case MIN30:
                return "30m";
            case HOUR1:
                return "1h";
//            case HOUR2: return "2h";
            case HOUR3:
                return "3h";
//            case HOUR4: return "4h";
            case HOUR6:
                return "6h";
            case HOUR12:
                return "12h";
            case DAY1:
                return "1D";
//            case DAY3: return "3D";
            case WEEK1:
                return "7D";
            case WEEK2:
                return "14D";
            case MONTH1:
                return "1M";
            default:
                return null;
        }
    }

    /**
     * 统一解析Depth
     */
    public static Depth parseDepth(JSONObject r) {
        Long[] time = new Long[]{0L};
        List<Row> asks = BitfinexUtil.parseRows(r.getJSONArray("asks"), time);
        List<Row> bids = BitfinexUtil.parseRows(r.getJSONArray("bids"), time);
        return new Depth(time[0], new Rows(asks), new Rows(bids));
    }

    /**
     * 解析rows
     */
    private static List<Row> parseRows(JSONArray rs, Long[] time) {
        List<Row> rows = new ArrayList<>(rs.size());
        for (int i = 0, l = rs.size(); i < l; i++) {
            JSONObject t = rs.getJSONObject(i);
            rows.add(CommonUtil.parseRowByKey(rs.getString(i), t));
            Long timestamp = t.getBigDecimal("timestamp").multiply(CommonUtil.THOUSAND).longValue();
            if (time[0] < timestamp) {
                time[0] = timestamp;
            }
        }
        return rows;
    }

    /**
     * 统一解析Trades
     */
    public static Trades parseTrades(JSONArray r) {
        List<Trade> trades = new ArrayList<>(r.size());
        for (int i = 0; i < r.size(); i++) {
            JSONObject t = r.getJSONObject(i);
               /*{
                    "timestamp":1548042361,
                    "tid":333729405,
                    "price":"3584.3",
                    "amount":"0.015",
                    "exchange":"bitfinex",
                    "type":"sell"
                },*/
            Long time = t.getLong("timestamp");
            String id = t.getString("tid");
            Side side = Side.valueOf(t.getString("type").toUpperCase());
            BigDecimal price = t.getBigDecimal("price");
            BigDecimal amount = t.getBigDecimal("amount");
            trades.add(new Trade(r.getString(i), time, id, side, price, amount));
        }
        return new Trades(trades);
    }

    /**
     * 解析所有余额
     * @param array 数组
     * @return 余额列表
     */
    public static List<Balance> parseBalances(JSONArray array) {
        List<Balance> balances = new ArrayList<>(array.size());
        for (int i = 0, l = array.size(); i < l; i++) {
            balances.add(BitfinexUtil.parseBalance(array.getString(i), array.getJSONObject(i)));
        }
        return balances;
    }

    /**
     * 解析单个币种余额
     *
     * @param result 原始数据
     * @param r      json
     * @return Balance
     */
    private static Balance parseBalance(String result, JSONObject r) {
         /*
            {
                "type":"exchange",
                "currency":"ltc",
                "amount":"0.11996",
                "available":"0.11996"
            }
         */
        String coin = r.getString("currency").toUpperCase();
        BigDecimal free = r.getBigDecimal("available");
        BigDecimal frozen = sub(r.getBigDecimal("amount"), free);
        return new Balance(result, coin, free, frozen);
    }

    private static final String CANCELLED = "is_cancelled";
    private static final String LIVE = "is_live";

    /**
     * 统一解析order
     * @param result 原始文件
     * @param o json
     * @return Order
     */
    public static Order parseOrder(String result, JSONObject o) {
        /*
        {
            "id":21597779146,
            "cid":26899733859,
            "cid_date":"2019-01-18",
            "gid":null,
            "symbol":"ltcusd",
            "exchange":"bitfinex",
            "price":"26.0",
            "avg_execution_price":"0.0",
            "side":"buy",
            "type":"exchange limit",
            "timestamp":"1547796500.0",
            "is_live":true,
            "is_cancelled":false,
            "is_hidden":false,
            "oco_order":null,
            "was_forced":false,
            "original_amount":"0.4",
            "remaining_amount":"0.4",
            "executed_amount":"0.0",
            "src":"api"
        }
        */
        Long time = o.getBigDecimal("timestamp").multiply(CommonUtil.THOUSAND).longValue();
        String id = o.getString("id");
        Side side = Side.valueOf(o.getString("side").toUpperCase());
        String t = o.getString("type");
        Type type = null;
        if (EXCHANGE_LIMIT.equals(t)) {
            type = Type.LIMIT;
        } else if (EXCHANGE_MARKET.equals(t)) {
            type = Type.MARKET;
        } else if (EXCHANGE_FOK.equals(t)) {
            type = Type.FILL_OR_KILL;
        }
        State state;
        BigDecimal price = o.getBigDecimal("price");
        BigDecimal amount = o.getBigDecimal("original_amount");
        BigDecimal deal = o.getBigDecimal("executed_amount");
        BigDecimal average = o.getBigDecimal("avg_execution_price");

        if (o.getBoolean(CANCELLED)) {
            state = State.CANCEL;
            if (greater(deal, BigDecimalUtil.ZERO)) {
                state = State.UNDONE;
            }
        } else if (o.getBoolean(LIVE)) {
            state = State.SUBMIT;
            if (greater(deal, BigDecimalUtil.ZERO)) {
                state = State.PARTIAL;
            }
            if (greaterOrEqual(deal, amount)) {
                state = State.FILLED;
            }
        } else {
            state = State.FILLED;
        }
        return new Order(result, time, id, side, type, state, price, amount, deal, average);
    }

    /**
     * [
     *  21597693840,                ID	int64	Order ID
     *  null,                       GID	int	Group ID
     *  26665933660,                CID	int	Client Order ID
     *  "tLTCUSD",                  SYMBOL	string	Pair (tBTCUSD, …)
     *  1547796266000,              MTS_CREATE	int	Millisecond timestamp of creation
     *  1547796266000,              MTS_UPDATE	int	Millisecond timestamp of update
     *  0,                          AMOUNT	float	Remaining amount.
     *  -0.4,                       AMOUNT_ORIG	float	Original amount, positive means buy, negative means sell.
     *  "EXCHANGE LIMIT",           TYPE	string	The type of the order: LIMIT, MARKET, STOP, TRAILING STOP, EXCHANGE MARKET, EXCHANGE LIMIT, EXCHANGE STOP, EXCHANGE TRAILING STOP, FOK, EXCHANGE FOK.
     *  null,                       TYPE_PREV	string	Previous order type
     *  null,                       _PLACEHOLDER,
     *  null,                       _PLACEHOLDER,
     *  "0",                        FLAGS	int	Upcoming Params Object (stay tuned)
     *  "EXECUTED @ 31.962(-0.4)",  ORDER_STATUS	string	Order Status: ACTIVE, EXECUTED, PARTIALLY FILLED, CANCELED
     *  null,                       _PLACEHOLDER,
     *  null,                       _PLACEHOLDER,
     *  30,                         PRICE	float	Price
     *  31.962,                     PRICE_AVG	float	Average price
     *  0,                          PRICE_TRAILING	float	The trailing price
     *  0,                          PRICE_AUX_LIMIT	float	Auxiliary Limit price (for STOP LIMIT)
     *  null,                       _PLACEHOLDER,
     *  null,                       _PLACEHOLDER,
     *  null,                       _PLACEHOLDER,
     *  0,                          NOTIFY	int	1 if Notify flag is active, 0 if not
     *  0,                          HIDDEN	int	1 if Hidden, 0 if not hidden
     *  null,                       PLACED_ID	int	If another order caused this order to be placed (OCO) this will be that other order's ID
     *  null,
     *  null,
     *  "API>BFX",
     *  null,
     *  null,
     *  null
     * ]
     * @param result 原始数据
     * @return 订单列表
     */
    public static List<Order> parseOrders(String result) {
        JSONArray r = JSON.parseArray(result);
        List<Order> orders = new ArrayList<>(r.size());
        for (int i = 0; i < r.size(); i++) {

            JSONArray t = r.getJSONArray(i);

            String data = r.getString(i);
            Long time = t.getLong(4);
            String id = t.getString(0);
            Side side = null;
            Double s = t.getDouble(7);
            if (0 < s) {
                side = Side.BUY;
            } else if (s < 0) {
                side = Side.SELL;
            }
            Type type = null;
            String orderType = t.getString(8);
            if ("EXCHANGE LIMIT".equals(orderType)) {
                type = Type.LIMIT;
            } else if ("EXCHANGE MARKET".equals(orderType)) {
                type = Type.MARKET;
            } else if ("EXCHANGE FOK".equals(orderType)) {
                type = Type.FILL_OR_KILL;
            }
            State state = null;
            String orderState = t.getString(13);
            if (orderState.contains("ACTIVE")) {
                state = State.SUBMIT;
            } else if (orderState.contains("EXECUTED")) {
                state = State.FILLED;
            } else if (orderState.contains("PARTIALLY FILLED")) {
                state = State.UNDONE;
            } else if (orderState.contains("CANCELED")) {
                state = State.CANCEL;
            }

            BigDecimal price = t.getBigDecimal(16);
            BigDecimal amount = t.getBigDecimal(7).abs();
            BigDecimal deal = amount.subtract(t.getBigDecimal(6).abs());
            BigDecimal average = t.getBigDecimal(17);

            if (state == State.SUBMIT && greater(deal, BigDecimal.ZERO)) {
                state = State.PARTIAL;
            }
            if (state == State.CANCEL && greater(deal, BigDecimal.ZERO)) {
                state = State.UNDONE;
            }

            orders.add(new Order(data, time, id, side, type, state, price, amount, deal, average));
        }
        return orders;
    }

    /**
     * 解析订单详情
     * [
     *  333168795,      ID	integer	Trade database id
     *  "tLTCUSD",      PAIR	string	Pair (BTCUSD, …)
     *  1547796266000,  MTS_CREATE	integer	Execution timestamp
     *  21597693840,    ORDER_ID	integer	Order id
     *  -0.4,           EXEC_AMOUNT	float	Positive means buy, negative means sell
     *  31.962,         EXEC_PRICE	float	Execution price
     *  null,           _PLACEHOLDER,
     *  null,           _PLACEHOLDER,
     *  -1,             MAKER	int	1 if true, -1 if false
     *  -0.0255696,     FEE	float	Fee
     *  "USD"           FEE_CURRENCY	string	Fee currency
     * ]
     * @param result 元素数据
     * @return 订单详情列表
     */
    public static List<OrderDetail> parseOrderDetails(String result) {
        JSONArray r = JSON.parseArray(result);
        List<OrderDetail> details = new ArrayList<>(r.size());
        for (int i = 0; i < r.size(); i++) {
            JSONArray t = r.getJSONArray(i);

            String data = r.getString(i);
            Long time = t.getLong(2);
            String orderId = t.getString(3);
            String detailId = t.getString(0);
            BigDecimal price = t.getBigDecimal(5);
            BigDecimal amount = t.getBigDecimal(4).abs();
            BigDecimal fee = t.getBigDecimal(9).abs();
            String feeCurrency = t.getString(10);
            details.add(new OrderDetail(data, time, orderId, detailId, price, amount, fee, feeCurrency));
        }
        return details;
    }


    /**
     * 解析币对精度信息
     * @param result 原始数据
     * @return 精度列表
     */
    public static List<Precision> parsePrecisions(String result) {
        JSONArray r = JSON.parseArray(result);
        List<Precision> precisions = new ArrayList<>(r.size());
        for (int i = 0; i < r.size(); i++) {
            /*
             * {
             *  "pair":"vsyusd",
             *  "price_precision":5,
             *  "initial_margin":"30.0",
             *  "minimum_margin":"15.0",
             *  "maximum_order_size":"50000.0",
             *  "minimum_order_size":"0.001",
             *  "expiration":"NA",
             *  "margin":false
             * }
             *  "pair":"btcusd",
                "price_precision":5,
                "initial_margin":"30.0",
                "minimum_margin":"15.0",
                "maximum_order_size":"2000.0",
                "minimum_order_size":"0.004",
                "expiration":"NA",
                "margin":true
             */
            JSONObject t = r.getJSONObject(i);
            String data = r.getString(i);
            String symbol = new StringBuilder(t.getString("pair").toUpperCase())
                    .insert(3, "_").toString();
//            Integer base = null;
            Integer count = t.getInteger("price_precision");
//            BigDecimal baseStep = null;
//            BigDecimal countStep = null;
            BigDecimal minBase = t.getBigDecimal("minimum_order_size");
//            BigDecimal minCount = null;
            BigDecimal maxBase = t.getBigDecimal("maximum_order_size");
//            BigDecimal maxCount = null;
            precisions.add(new Precision(data, symbol, null, count,
                    null, null, minBase, null, maxBase, null));
        }
        return precisions;
    }

}
