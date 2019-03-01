package cqt.goai.model.market;

import com.alibaba.fastjson.JSONArray;
import cqt.goai.model.To;
import cqt.goai.model.Util;
import cqt.goai.model.enums.Side;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 成交信息
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class Trade implements To, Serializable {

    private static final long serialVersionUID = 514750454318716315L;

    /**
     * 交易所返回的原始数据, 防止有些用户需要一些特殊的数据
     */
    private final String data;

    /**
     * 时间戳
     */
    private final Long time;

    /**
     * 交易id
     */
    private final String id;

    /**
     * 方向
     */
    private final Side side;

    /**
     * 价格
     */
    private final BigDecimal price;

    /**
     * 数量
     */
    private final BigDecimal amount;

    public Trade(String data, Long time, String id, Side side, BigDecimal price, BigDecimal amount) {
        this.data = data;
        this.time = time;
        this.id = id;
        this.side = side;
        this.price = Util.strip(price);
        this.amount = Util.strip(amount);
    }

    @Override
    public String to() {
        return '[' +
                Util.encode(this.data) +
                ',' + Util.to(this.time) +
                ',' + Util.to(this.id) +
                ',' + Util.to(this.side) +
                ',' + Util.to(this.price) +
                ',' + Util.to(this.amount) +
                ']';
    }

    public double price() {
        Objects.requireNonNull(this.price);
        return this.price.doubleValue();
    }

    public double amount() {
        Objects.requireNonNull(this.amount);
        return this.amount.doubleValue();
    }

    public static Trade of(String data, Logger log) {
        return Util.of(data, Trade::of, log);
    }

    static Trade of(JSONArray r) {
        return new Trade(
                Util.decode(r.getString(0)),
                r.getLong(1),
                r.getString(2),
                Util.of(r.getString(3), Side::valueOf),
                r.getBigDecimal(4),
                r.getBigDecimal(5));
    }

}
