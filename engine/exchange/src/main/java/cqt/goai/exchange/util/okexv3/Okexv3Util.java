package cqt.goai.exchange.util.okexv3;

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
import cqt.goai.model.trade.Orders;
import okio.ByteString;
import org.apache.commons.compress.compressors.deflate64.Deflate64CompressorInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static dive.common.math.BigDecimalUtil.div;
import static dive.common.math.BigDecimalUtil.greater;
import static java.math.BigDecimal.ZERO;

/**
 * @author GOAi
 */
public class Okexv3Util {

    /**
     * okexv3 的周期换算
     * @param period 周期
     * @return 周期长度 秒
     */
    public static Integer getPeriod(Period period) {
        switch (period) {
            case MIN1:
            case MIN3:
            case MIN5:
            case MIN15:
            case MIN30:
            case HOUR1:
            case HOUR2:
//                case HOUR3 : return 7200;
            case HOUR4:
            case HOUR6:
            case HOUR12:
            case DAY1:
//                case DAY3 : return 604800;
            case WEEK1:
                return period.getValue();
//                case WEEK2 : return 604800;
//                case MONTH1 : return 604800;
            default:
                return null;
        }
    }


    /**
     * http 和 ws 解析方式相同
     * @param result 原始文件
     * @param r json
     * @return Ticker
     */
    public static Ticker parseTicker(String result, JSONObject r) {
        /* {
         *  "instrument_id":"ETH-USDT",
         *  "last":"131.6299",
         *  "best_bid":"131.6299",
         *  "best_ask":"131.6857",
         *  "open_24h":"149.8823",
         *  "high_24h":"150.5",
         *  "low_24h":"130.9743",
         *  "base_volume_24h":"879069.0066515",
         *  "quote_volume_24h":"124634731.57238156",
         *  "timestamp":"2019-01-10T09:26:55.932Z"
         * }
         */
        Long time = r.getDate("timestamp").getTime();
        BigDecimal open = r.getBigDecimal("open_24h");
        BigDecimal high = r.getBigDecimal("high_24h");
        BigDecimal low = r.getBigDecimal("low_24h");
        BigDecimal last = r.getBigDecimal("last");
        BigDecimal volume = r.getBigDecimal("base_volume_24h");

        return new Ticker(result, time, open, high, low, last, volume);
    }

    /**
     * ws 解析方式
     * @param result 原始文件
     * @param r json
     * @return Kline
     */
    public static Kline parseKline(String result, JSONArray r) {
        /* {"candle":
         *  ["2019-01-15T09:51:00.000Z",
         *  "3594.0579",
         *  "3595.5917",
         *  "3593.9239",
         *  "3595.5917",
         *  "19.24375778"],
         *  "instrument_id":"BTC-USDT"}
         */
        Long time = r.getDate(0).getTime() / 1000;

        return CommonUtil.parseKlineByIndex(result, r, time, 1);
    }

    /**
     * 统一解析Depth
     */
    public static Depth parseDepth(JSONObject r) {
        Long time = r.getDate("timestamp").getTime();
        return CommonUtil.parseDepthByIndex(time, r.getJSONArray("asks"), r.getJSONArray("bids"));
    }

    /**
     * 统一解析Trades
     */
    public static Trades parseTrades(JSONArray r) {
        List<Trade> trades = new ArrayList<>(r.size());
        for (int i = 0, l = r.size(); i < l; i++) {
            /*
             * [
             *     {
             *         "time":"2019-01-11T14:02:02.536Z",
             *         "timestamp":"2019-01-11T14:02:02.536Z",
             *         "trade_id":"861471459",
             *         "price":"3578.8482",
             *         "size":"0.0394402",
             *         "side":"sell"
             *     },
             */
            JSONObject t = r.getJSONObject(i);
            Long time = t.getDate("timestamp").getTime();
            String id = t.getString("trade_id");
            Side side = Side.valueOf(t.getString("side").toUpperCase());
            BigDecimal price = t.getBigDecimal("price");
            BigDecimal amount = t.getBigDecimal("size");
            Trade trade = new Trade(r.getString(i), time, id, side, price, amount);
            trades.add(trade);
        }
        return new Trades(trades.stream()
                .sorted((t1, t2) -> t2.getTime().compareTo(t1.getTime()))
                .collect(Collectors.toList()));
    }

    /**
     * 解析单个币种余额
     *
     * @param result 原始数据
     * @param r      json
     * @return Balance
     */
    public static Balance parseBalance(String result, JSONObject r) {
        /*
         * {
         *     "frozen":"0",
         *     "hold":"0",
         *     "id":"6278097",
         *     "currency":"USDT",
         *     "balance":"127.613453580677953",
         *     "available":"127.613453580677953",
         *     "holds":"0"
         * }
         */
        return new Balance(result, r.getString("currency"),
                r.getBigDecimal("available"), r.getBigDecimal("hold"));
    }

    /**
     * 解析Order
     * @param result 原始数据
     * @param t      json
     * @return Order
     */
    public static Order parseOrder(String result, JSONObject t) {
        /*
         * {
         *     "order_id": "125678",
         *     "notional": "12.4",
         *     "price": "0.10000000",
         *     "size": "0.01000000",
         *     "instrument_id": "BTC-USDT",
         *     "side": "buy",
         *     "type": "limit",
         *     "timestamp": "2016-12-08T20:02:28.538Z",
         *     "filled_size": "0.00000000",
         *     "filled_notional": "0.0000000000000000",
         *     "status": "open"
         * }
         */
        Long time = t.getDate("timestamp").getTime();
        String id = t.getString("order_id");
        Side side = Side.valueOf(t.getString("side").toUpperCase());
        Type type = Type.valueOf(t.getString("type").toUpperCase());
        State state = null;
        switch (t.getString("status")) {
            case "open":
                state = State.SUBMIT;
                break;
            case "part_filled":
                state = State.PARTIAL;
                break;
            case "canceling":
                state = State.CANCEL;
                break;
            case "filled":
                state = State.FILLED;
                break;
            case "cancelled":
                state = State.CANCEL;
                break;
            case "failure":
                state = State.CANCEL;
                break;
            case "ordering":
                state = State.SUBMIT;
                break;
            default:
        }
        BigDecimal price = t.getBigDecimal("price");
        BigDecimal amount = t.getBigDecimal("size");
        BigDecimal deal = t.getBigDecimal("filled_size");
        BigDecimal average = ZERO;
        if (greater(deal, ZERO)) {
            average = div(t.getBigDecimal("filled_notional"), deal, 8, RoundingMode.HALF_UP);
            if (State.CANCEL.equals(state)) {
                state = State.UNDONE;
            }
        }
        return new Order(result, time, id, side, type, state, price, amount, deal, average);
    }

    /**
     * 统一解析orders
     * @param r json
     * @return orders
     */
    public static Orders parseOrders(JSONArray r) {
        List<Order> orders = new LinkedList<>();
        for (int i = 0; i< r.size(); i++) {
            orders.add(Okexv3Util.parseOrder(r.getString(i), r.getJSONObject(i)));
        }
        return new Orders(orders);
    }
    /**
     * WebSocket解压缩函数
     * @param bytes 压缩字符串
     * @return 解压缩后结果
     */
    public static String uncompress(ByteString bytes) {
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
             final ByteArrayInputStream in = new ByteArrayInputStream(bytes.toByteArray());
             final Deflate64CompressorInputStream zin = new Deflate64CompressorInputStream(in)) {
            final byte[] buffer = new byte[1024];
            int offset;
            while (-1 != (offset = zin.read(buffer))) {
                out.write(buffer, 0, offset);
            }
            return out.toString();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
