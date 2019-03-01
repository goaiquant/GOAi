package cqt.goai.exchange.util.huobi.pro;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.util.CommonUtil;
import cqt.goai.model.enums.Period;
import cqt.goai.model.enums.Side;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.*;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author GOAi
 */
@Slf4j
public class HoubiProUtil {
    private static final String OK = "ok";
    private static final String STATUS = "status";
    private static final String DATA = "data";

    /**
     * 解析Ticker
     * {
     *     "status":"ok",
     *     "ch":"market.btcusdt.detail.merged",
     *     "ts":1549000056454,
     *     "tick":{
     *         "amount":15726.99857982195,
     *         "open":3465.23,
     *         "close":3420,
     *         "high":3479.57,
     *         "id":100158976057,
     *         "count":155478,
     *         "low":3402.52,
     *         "version":100158976057,
     *         "ask":[
     *             3420,
     *             16.61527792397661
     *         ],
     *         "vol":53984154.95959691,
     *         "bid":[
     *             3419.99,
     *             2.9407
     *         ]
     *     }
     * }
     * @param result 原始数据
     * @return Ticker
     */
    public static Ticker parseTicker(String result) {
        JSONObject r = JSON.parseObject(result);
        if (OK.equals(r.getString(STATUS))) {
            Long time = r.getLong("ts");
            return CommonUtil.parseTicker(result, r.getJSONObject("tick"), time,
                    "open", "high", "low", "close", "amount");
        }
        return null;
    }

    /**
     * 火币 的周期换算
     *
     * @param period 周期
     * @return 周期长度 秒
     */
    public static String getPeriod(Period period) {
        switch (period) {
            case MIN1: return "1min";
//            case MIN3: return "3min";
            case MIN5: return "5min";
            case MIN15: return "15min";
            case MIN30: return "30min";
            case HOUR1: return "60min";
//            case HOUR2: return "2hour";
//            case HOUR3: return "3hour";
//            case HOUR4: return "4hour";
//            case HOUR6: return "6hour";
//            case HOUR12: return "12hour";
            case DAY1: return "1day";
//            case DAY3: return "3day";
            case WEEK1: return "1week";
//            case WEEK2: return "2week";
            case MONTH1: return "1mon";
            default: return null;
        }
    }

    /**
     * 解析 K线
     * {
     *  "id":1549000800,
     *  "open":3418.43,
     *  "close":3418.44,
     *  "low":3418.43,
     *  "high":3418.44,
     *  "amount":0.33941184633926585,
     *  "vol":1160.258958,
     *  "count":11
     * }
     * @param result 原始数据
     * @return Klines
     */
    public static Klines parseKlines(String result) {
        JSONObject r = JSON.parseObject(result);

        if (OK.equals(r.getString(STATUS))) {
            JSONArray data = r.getJSONArray(DATA);

            List<Kline> klines = new ArrayList<>(data.size());
            for (int i = 0; i < data.size(); i++) {
                JSONObject t = data.getJSONObject(i);

                Long time = t.getLong("id");
                BigDecimal open = t.getBigDecimal("open");
                BigDecimal high = t.getBigDecimal("high");
                BigDecimal low = t.getBigDecimal("low");
                BigDecimal close = t.getBigDecimal("close");
                BigDecimal volume = t.getBigDecimal("amount");

                klines.add(new Kline(data.getString(i), time, open, high, low, close, volume));
            }
            return new Klines(klines);
        }
        return null;
    }

    /**
     * 解析 深度
     * {
     *     "status":"ok",
     *     "ch":"market.btcusdt.depth.step0",
     *     "ts":1549001152532,
     *     "tick":{
     *         "bids":[
     *             [
     *                 3415.41,
     *                 0.0183
     *             ], ...
     *         ],
     *         "asks":[
     *             [
     *                 3415.96,
     *                 0.0542
     *             ], ...
     *         ],
     *         "ts":1549001152017,
     *         "version":100159087527
     *     }
     * }
     * @param result 原始数据
     * @return Depth
     */
    public static Depth parseDepth(String result) {
        JSONObject r = JSON.parseObject(result);
        if (OK.equals(r.getString(STATUS))) {
            JSONObject tick = r.getJSONObject("tick");

            Long time = tick.getLong("ts");
            return CommonUtil.parseDepthByIndex(time,
                    tick.getJSONArray("asks"),
                    tick.getJSONArray("bids"));
        }
        return null;
    }

    /**
     * 解析 行情
     *
     * @param result 原始数据
     * @return Trades
     */
    public static Trades parseTrades(String result) {
        JSONObject r = JSON.parseObject(result);
        if (OK.equals(r.getString(STATUS))) {
            JSONArray data = r.getJSONArray(DATA);
            List<Trade> trades = new ArrayList<>(data.size());
            for (int i = 0; i < data.size(); i++) {
                JSONObject t = data.getJSONObject(i).getJSONArray(DATA).getJSONObject(0);
                /*
                 * {
                 *  "id":100159178954,
                 *  "ts":1549002100105,
                 *  "data":[
                 *      {
                 *       "amount":0.009000000000000000,
                 *       "ts":1549002100105,
                 *       "id":10015917895423497145394,
                 *       "price":3414.610000000000000000,
                 *       "direction":"buy"
                 *      }
                 *   ]
                 * }
                 */
                Long time = t.getLong("ts");
                String id = t.getString("id");
                Side side = Side.valueOf(t.getString("direction").toUpperCase());
                BigDecimal price = t.getBigDecimal("price");
                BigDecimal amount = t.getBigDecimal("amount");
                Trade trade = new Trade(data.getJSONObject(i).getString(DATA),
                        time, id, side, price, amount);
                trades.add(trade);
            }
            return new Trades(trades);
        }
        return null;
    }

    /**
     * 解析所有币的余额
     *
     * @param result 原始数据
     * @return Balances
     */
    public static Balances parseBalances(String result) {
        JSONObject r = JSON.parseObject(result);
        if (OK.equals(r.getString(STATUS))) {
            JSONArray list = r.getJSONObject("data").getJSONArray("list");
            int size = list.size() / 2;

            List<Balance> balances = new ArrayList<>(size);
            Map<String, JSONObject> free = new HashMap<>(size);
            Map<String, JSONObject> frozen = new HashMap<>(size);

            for (int i = 0; i < list.size(); i++) {
                JSONObject t = list.getJSONObject(i);
                String coin = t.getString("currency").toUpperCase();
                String type = t.getString("type");
                if ("trade".equals(type)) {
                    free.put(coin, t);
                } else if ("frozen".equals(type)) {
                    frozen.put(coin, t);
                }
            }

            for (String coin : free.keySet()) {
                JSONObject f1 = free.get(coin);
                JSONObject f2 = frozen.get(coin);
                balances.add(new Balance("[" + f1.toJSONString() + "," + f2.toJSONString() + "]",
                        coin, f1.getBigDecimal("balance"), f2.getBigDecimal("balance")));
            }
            return new Balances(balances);
        }
        return null;
    }


    /**
     * 解析 币对余额
     * 单独写 不用解析所有的币了
     * @param result 原始数据
     * @param symbol 币对
     * @return Account
     */
    public static Account parseAccount(String result, String symbol) {
        JSONObject r = JSON.parseObject(result);

        if (OK.equals(r.getString(STATUS))) {
            JSONArray list = r.getJSONObject("data").getJSONArray("list");

            JSONObject freeBase = null;
            JSONObject frozenBase = null;
            JSONObject freeCount = null;
            JSONObject frozenCount = null;

            String[] symbols = symbol.split("_");

            String base = symbols[0].toLowerCase();
            String count = symbols[1].toLowerCase();

            for (int i = 0; i < list.size(); i++) {
                JSONObject t = list.getJSONObject(i);

                String coin = t.getString("currency");
                String type = t.getString("type");

                if (base.equals(coin)) {
                    if ("trade".equals(type)) {
                        freeBase = t;
                    } else if ("frozen".equals(type)) {
                        frozenBase = t;
                    }
                } else if (count.equals(coin)) {
                    if ("trade".equals(type)) {
                        freeCount = t;
                    } else if ("frozen".equals(type)) {
                        frozenCount = t;
                    }
                }
            }
            if (null != freeBase && null != frozenBase && null != freeCount && null != frozenCount) {
                return new Account(System.currentTimeMillis(),
                        new Balance("[" + freeBase.toJSONString() + "," + frozenBase.toJSONString() + "]",
                                base.toUpperCase(), freeBase.getBigDecimal("balance"), frozenBase.getBigDecimal("balance")),
                        new Balance("[" + freeCount.toJSONString() + "," + frozenCount.toJSONString() + "]",
                                count.toUpperCase(), freeCount.getBigDecimal("balance"), frozenCount.getBigDecimal("balance")));
            }
        }
        return null;
    }

    /**
     * 解析 精度
     *
     * @param result 原始信息
     * @return 解析结果
     */
    public static Precisions parsePrecisions(String result) {
        JSONObject r = JSON.parseObject(result);
        if (OK.equals(r.getString(STATUS))) {
            JSONArray data = r.getJSONArray(DATA);
            List<Precision> precisions = new ArrayList<>(data.size());
            for (int i = 0; i < data.size(); i++) {
                JSONObject t = data.getJSONObject(i);

                String symbol = t.getString("base-currency") + "_" + t.getString("quote-currency");
                symbol = symbol.toUpperCase();
                Integer base = t.getInteger("amount-precision");
                Integer count = t.getInteger("price-precision");

                Precision precision = new Precision(data.getString(i), symbol,
                        base, count,
                        null, null,
                        null, null,
                        null, null);

                precisions.add(precision);
            }
            return new Precisions(precisions);
        }
        return null;
    }

    /**
     * 解析单个精度
     * @param result 请求结果
     * @param symbol 解析币对
     * @return 精度
     */
    public static Precision parsePrecision(String result, String symbol) {
        JSONObject r = JSON.parseObject(result);
        if (OK.equals(r.getString(STATUS))) {
            JSONArray data = r.getJSONArray(DATA);
            for (int i = 0; i < data.size(); i++) {
                JSONObject t = data.getJSONObject(i);
                if (symbol.equalsIgnoreCase(t.getString("symbol"))) {

                    symbol = t.getString("base-currency") + "_" + t.getString("quote-currency");
                    symbol = symbol.toUpperCase();
                    Integer base = t.getInteger("amount-precision");
                    Integer count = t.getInteger("price-precision");

                    return new Precision(data.getString(i), symbol,
                            base, count,
                            null, null,
                            null, null,
                            null, null);
                }
            }
        }
        return null;
    }

}
