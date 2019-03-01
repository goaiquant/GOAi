package cqt.goai.exchange.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.ExchangeException;
import cqt.goai.model.enums.Side;
import cqt.goai.model.enums.State;
import cqt.goai.model.enums.Type;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.Order;
import cqt.goai.model.trade.OrderDetail;
import cqt.goai.model.trade.Precision;
import dive.common.crypto.HexUtil;
import dive.http.common.model.Parameter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static dive.common.math.BigDecimalUtil.greater;
import static dive.common.util.Util.useful;
import static java.math.BigDecimal.ZERO;


/**
 * 公共工具类
 *
 * @author GOAi

 */
public class CommonUtil {

    public static final BigDecimal THOUSAND = new BigDecimal("1000");

    private static final String ALGORITHM_SHA256 = "SHA-256";

    /**
     * 统一获取Ticker
     *
     * @param result 原始文件
     * @param r      json
     * @param time   时间
     * @param open   开 键名
     * @param high   高 键名
     * @param low    低 键名
     * @param last   收 键名
     * @param volume 量 键名
     * @return Ticker
     */
    public static Ticker parseTicker(String result, JSONObject r, Long time, String open,
                                     String high, String low, String last, String volume) {
        return new Ticker(result,
                time,
                getBigDecimal(r, open),
                getBigDecimal(r, high),
                getBigDecimal(r, low),
                getBigDecimal(r, last),
                getBigDecimal(r, volume));
    }

    /**
     * 读取指定属性
     *
     * @param r    json
     * @param name 名称
     * @return BigDecimal
     */
    private static BigDecimal getBigDecimal(JSONObject r, String name) {
        return null != name && r.containsKey(name) ? r.getBigDecimal(name) : null;
    }

    /**
     * 根据序号解析Kline
     *
     * @param result 原始文件
     * @param r      array
     * @param time   时间
     * @return Kline
     */
    public static Kline parseKlineByIndex(String result, JSONArray r, Long time, int first) {
        BigDecimal open = r.getBigDecimal(first);
        BigDecimal high = r.getBigDecimal(first + 1);
        BigDecimal low = r.getBigDecimal(first + 2);
        BigDecimal close = r.getBigDecimal(first + 3);
        BigDecimal volume = r.getBigDecimal(first + 4);
        return new Kline(result, time, open, high, low, close, volume);
    }

    /**
     * 解析档位
     *
     * @param result 元素数据
     * @param row    数组
     * @return 档位
     */
    public static Row parseRowByIndex(String result, JSONArray row) {
        BigDecimal price = row.getBigDecimal(0);
        BigDecimal amount = row.getBigDecimal(1);
        return new Row(result, price, amount);
    }

    /**
     * 解析rows
     */
    public static List<Row> parseRowsByIndex(JSONArray rs) {
        List<Row> rows = new ArrayList<>(rs.size());
        for (int i = 0, l = rs.size(); i < l; i++) {
            rows.add(CommonUtil.parseRowByIndex(rs.getString(i), rs.getJSONArray(i)));
        }
        return rows;
    }

    /**
     * 统一解析Depth
     * @param time 时间
     * @param asks 卖盘
     * @param bids 买盘
     * @return Depth
     */
    public static Depth parseDepthByIndex(Long time, JSONArray asks, JSONArray bids) {
        return new Depth(time,
                new Rows(CommonUtil.parseRowsByIndex(asks)),
                new Rows(CommonUtil.parseRowsByIndex(bids)));
    }

    /**
     * 解析档位
     *
     * @param result 元素数据
     * @param row    数组
     * @return 档位
     */
    public static Row parseRowByKey(String result, JSONObject row) {
        return new Row(result, row.getBigDecimal("price"), row.getBigDecimal("amount"));
    }

    /**
     * 解析订单
     * @param result 原始数据
     * @param r json
     * @param time 时间
     * @param side 方向
     * @param type 类型
     * @param state 状态
     * @param idKey id键名
     * @param priceKey 价格键名
     * @param amountKey 数量键名
     * @param dealKey 已成交数量键名
     * @param averageKey 均价键名
     * @return Order
     */
    public static Order parseOrder(String result, JSONObject r, Long time, Side side, Type type, State state,
                                   String idKey, String priceKey, String amountKey,
                                   String dealKey, String averageKey) {
        String id = r.getString(idKey);
        BigDecimal price = getBigDecimal(r, priceKey);
        BigDecimal amount = getBigDecimal(r, amountKey);
        BigDecimal deal = getBigDecimal(r, dealKey);
        BigDecimal average = getBigDecimal(r, averageKey);
        if (state == State.CANCEL && null != deal && greater(deal, ZERO)) {
            state = State.UNDONE;
        }
        return new Order(result, time, id, side, type, state, price, amount, deal, average);
    }

    /**
     * 经常需要对下划线分割
     *
     * @param content 分割内容
     * @param first   true 第一个_分割，false 最后一个_分割
     */
    public static String[] split(String content, boolean first) {
        if (useful(content)) {
            int i = first ? content.indexOf("_") : content.lastIndexOf("_");
            if (0 < i) {
                return new String[]{content.substring(0, i), content.substring(i + 1)};
            }
        }
        throw new ExchangeException("can not split content: " + content);
    }

    /**
     * 经常需要对下划线分割
     */
    public static String[] split(String content) {
        return CommonUtil.split(content, true);
    }

    /**
     * 订单排序方法
     *
     * @param o1 o1
     * @param o2 o2
     * @return 排序
     */
    public static int sortOrder(Order o1, Order o2) {
        return o2.getTime().compareTo(o1.getTime());
    }

    /**
     * 订单排序方法
     *
     * @param d1 d1
     * @param d2 d2
     * @return 排序
     */
    public static int sortOrderDetail(OrderDetail d1, OrderDetail d2) {
        return d2.getTime().compareTo(d1.getTime());
    }

    /**
     * 添加参数
     *
     * @param parameter 参数对象
     * @param others    其他内容
     */
    public static void addOtherParameter(Parameter parameter, Object[] others) {
        for (int i = 0; i < others.length; i++) {
            parameter.add((String) others[i], others[++i]);
        }
    }

    /**
     * 添加其他参数
     *
     * @param parameter 参数对象
     * @param others    其他内容
     * @return 排序后json
     */
    public static String addOtherParameterToJson(Parameter parameter, Object[] others) {
        CommonUtil.addOtherParameter(parameter, others);
        return parameter.sort().json(JSON::toJSONString);
    }


    /**
     * hmac sha384
     *
     * @param message 信息
     * @param secret  秘钥
     * @return 加密结果
     */
    public static String hmacSha384(String message, String secret) {
        Exception exception = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA384");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA384");
            mac.init(secretKeySpec);
            return HexUtil.hexEncode(mac.doFinal(message.getBytes()), false);
        } catch (NoSuchAlgorithmException ignored) {
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            exception = e;
        }
        throw new ExchangeException("hmacSha384 error.", exception);
    }

    /**
     * hmac HmacSHA256
     *
     * @param message 信息
     * @param secret  秘钥
     * @return 加密结果
     */
    public static String hmacSha256(String message, String secret) {
        Exception exception = null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            return HexUtil.hexEncode(mac.doFinal(message.getBytes()), false);
        } catch (NoSuchAlgorithmException ignored) {
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            exception = e;
        }
        throw new ExchangeException("hmacSha256 error.", exception);
    }

    /**
     * 从深度信息解析精度
     *
     * @param depth  深度
     * @param symbol 币对
     * @return 精度
     */
    public static Precision parsePrecisionByDepth(Depth depth, String symbol) {
        int base = 0;
        int count = 0;
        List<Row> rows = depth.getAsks().getList();
        rows.addAll(depth.getBids().getList());
        for (Row r : rows) {
            int c = r.getPrice().scale();
            int b = r.getAmount().scale();
            if (count < c) {
                count = c;
            }
            if (base < b) {
                base = b;
            }
        }
        return new Precision(null, symbol, base, count, null, null,
                null, null, null, null);
    }

    /**
     * 按大小分割
     * @param list 列表
     * @param max 最大数量
     * @param <T> 基础类型
     * @return 分割后流
     */
    public static <T> List<List<T>> split(List<T> list, int max) {
        List<List<T>> split = new LinkedList<>();
        for (int i = 0; i < list.size() / max + 1; i++) {
            List<T> temp = list.stream()
                    .skip(i * max)
                    .limit(max)
                    .collect(Collectors.toList());
            if (!temp.isEmpty()) {
                split.add(temp);
            }
        }
        return split;
    }

}
