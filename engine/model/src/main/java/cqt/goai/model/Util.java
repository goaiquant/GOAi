package cqt.goai.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import cqt.goai.model.trade.Balance;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工具类
 * @author GOAi
 */
public class Util {

    /**
     * 去除末尾的0, 保存时应该检查，有些交易所的数据末尾0乱七八糟的，不如都不要
     * @param number 待结尾数字
     * @return 结尾后数字
     */
    public static BigDecimal strip(BigDecimal number) {
        return null != number ?
                new BigDecimal(number.stripTrailingZeros().toPlainString()) : null;
    }

    /**
     * 处理Long数据
     */
    public static String to(Long time) {
        if (null == time) {
            return "null";
        }
        return String.valueOf(time);
    }

    /**
     * 处理BigDecimal数据
     */
    public static String to(BigDecimal number) {
        if (null == number) {
            return "null";
        }
        return number.toPlainString();
    }

    /**
     * 处理String数据
     */
    public static String to(String content) {
        if (null == content) {
            return "null";
        }
        return '\"' + content + '\"';
    }

    /**
     * 处理ModelList数据
     */
    public static String to(BaseModelList list) {
        if (null == list) {
            return "null";
        }
        return list.to();
    }

    /**
     * 处理枚举数据
     */
    public static String to(Enum e) {
        if (null == e) {
            return "null";
        }
        return '\"' + e.name() + '\"';
    }

    /**
     * 处理整型数据
     */
    public static String to(Integer number) {
        if (null == number) {
            return "null";
        }
        return String.valueOf(number);
    }

    /**
     * 处理Balance数据
     */
    public static String to(Balance balance) {
        if (null == balance) {
            return "null";
        }
        return balance.to();
    }

    /**
     * list 转换字符串数组
     * @param list list
     * @param <E> 元素类型
     * @return 字符串数组
     */
    public static <E extends To> String to(List<E> list) {
        if (null == list) {
            return null;
        }
        if (0 == list.size()) {
            return "[]";
        }
        return '[' +
                list.stream()
                        .map(To::to)
                        .collect(Collectors.joining(",")) +
                ']';
    }

    /**
     * data数据传输时变成Base64编码
     * @param data 原始数据
     * @return Base64编码
     */
    public static String encode(String data) {
        return null == data || "null".equals(data) ?
                "null" : ('\"' + Base64.getEncoder().encodeToString(data.getBytes()) + '\"');
    }

    /**
     * data数据存入时解码
     * @param data Base64编码
     * @return 原始数据
     */
    public static String decode(String data) {
        return null == data || "null".equals(data) ?
                null : new String(Base64.getDecoder().decode(data.getBytes()));
    }

    /**
     * 单个值还原
     * @param data 文本数据
     * @param of 转化方法
     * @param <E> 结果对象
     * @return 转化结果
     */
    public static <E> E of(String data, Function<String, E> of) {
        return null == data || "null".equals(data) ? null : of.apply(data);
    }

    /**
     * 可能组合成ModelList的元素，在上级就已经转变成JSONArray了
     * 本身的of(String data)不能使用，则重载个of(JSONArray r)方法
     * @param data 数组数据
     * @param of 解析JSONArray对象
     * @param log 错误输出
     * @return 解析结果
     */
    public static <T> T of(String data, Function<JSONArray, T> of, Logger log) {
        try {
            return of.apply(JSON.parseArray(data));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            log.error("of error -> {}", data);
        }
        return null;
    }

    /**
     * 解析list数据并添加到ModelList对象中
     * @param data 原始数据
     * @param fresh 构造新的实例
     * @param of JSONArray解析of
     * @param log 错误输出
     * @param <E> 元素对象
     * @return 解析成功与否
     */
    public static <E, T> T of(String data, Function<List<E>, T> fresh,
                              Function<JSONArray, E> of, Logger log) {
        try {
            JSONArray r = JSON.parseArray(data);
            return Util.of(r, fresh, of, log);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            log.error("of error -> {}", data);
        }
        return null;
    }

    /**
     * 解析JSONArray数据并添加到ModelList对象中
     * @param r JSONArray数据
     * @param fresh 构造新的实例
     * @param of JSONArray解析of
     * @param log 错误输出
     * @param <E> 元素对象
     * @return 解析成功与否
     */
    public static <E, T> T of(JSONArray r, Function<List<E>, T> fresh,
                                    Function<JSONArray, E> of, Logger log) {
        if (null == r) {
            return null;
        }
        try {
            List<E> list = new ArrayList<>(r.size());
            for (int i = 0; i < r.size(); i++) {
                list.add(of.apply(r.getJSONArray(i)));
            }
            return fresh.apply(list);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            log.error("of error -> {}", r);
        }
        return null;
    }

}
