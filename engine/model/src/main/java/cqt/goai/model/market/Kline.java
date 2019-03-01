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
 * K线信息
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class Kline implements To, Serializable {

    private static final long serialVersionUID = 3739670180018865992L;

    /**
     * 交易所返回的原始数据, 防止有些用户需要一些特殊的数据
     */
    private final String data;

    /**
     * 时间, 精确到秒
     */
    private final Long time;

    /**
     * 开盘价
     */
    private final BigDecimal open;

    /**
     * 最高价
     */
    private final BigDecimal high;

    /**
     * 最低价
     */
    private final BigDecimal low;

    /**
     * 收盘价
     */
    private final BigDecimal close;

    /**
     * 交易量
     */
    private final BigDecimal volume;

    public Kline(String data, Long time, BigDecimal open, BigDecimal high,
                 BigDecimal low, BigDecimal close, BigDecimal volume) {
        this.data = data;
        this.time = time;
        this.open = Util.strip(open);
        this.high = Util.strip(high);
        this.low = Util.strip(low);
        this.close = Util.strip(close);
        this.volume = Util.strip(volume);
    }

    @Override
    public String to() {
        return '[' +
                Util.encode(this.data) +
                ',' + Util.to(this.time) +
                ',' + Util.to(this.open) +
                ',' + Util.to(this.high) +
                ',' + Util.to(this.low) +
                ',' + Util.to(this.close) +
                ',' + Util.to(this.volume) +
                ']';
    }

    public double open() {
        Objects.requireNonNull(this.open);
        return this.open.doubleValue();
    }

    public double high() {
        Objects.requireNonNull(this.high);
        return this.high.doubleValue();
    }

    public double low() {
        Objects.requireNonNull(this.low);
        return this.low.doubleValue();
    }

    public double close() {
        Objects.requireNonNull(this.close);
        return this.close.doubleValue();
    }

    public double volume() {
        Objects.requireNonNull(this.volume);
        return this.volume.doubleValue();
    }

    public static Kline of(String data, Logger log) {
        return Util.of(data, Kline::of, log);
    }

    static Kline of(JSONArray r) {
        return new Kline(
                Util.decode(r.getString(0)),
                r.getLong(1),
                r.getBigDecimal(2),
                r.getBigDecimal(3),
                r.getBigDecimal(4),
                r.getBigDecimal(5),
                r.getBigDecimal(6));
    }

}
