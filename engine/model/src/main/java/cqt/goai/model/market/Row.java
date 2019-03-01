package cqt.goai.model.market;

import com.alibaba.fastjson.JSONArray;
import cqt.goai.model.To;
import cqt.goai.model.Util;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 档位 价格 和 数量
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class Row implements To, Serializable {

    private static final long serialVersionUID = -5070260547441227713L;

    /**
     * 交易所返回的原始数据, 防止有些用户需要一些特殊的数据
     */
    private final String data;

    /**
     * 价格
     */
    private final BigDecimal price;

    /**
     * 数量
     */
    private final BigDecimal amount;

    public Row(String data, BigDecimal price, BigDecimal amount) {
        this.data = data;
        this.price = Util.strip(price);
        this.amount = Util.strip(amount);
    }

    @Override
    public String to() {
        return '[' +
                Util.encode(this.data) +
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

    public static Row of(String data, Logger log) {
        return Util.of(data, Row::of, log);
    }

    static Row of(JSONArray r) {
        return new Row(
                Util.decode(r.getString(0)),
                r.getBigDecimal(1),
                r.getBigDecimal(2));
    }

    public static Row row(BigDecimal price, BigDecimal amount) {
        return new Row(null, price, amount);
    }

}
