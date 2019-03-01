package cqt.goai.model.market;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
 * Ticker信息
 *
 * Prototype Pattern 原型模式
 * 本来想用原型模式，每个推送都clone一份
 * 最后决定还是用不可变类，这样并发更好
 *
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class Ticker implements To, Serializable {

    private static final long serialVersionUID = 8856908960657104056L;

    /**
     * 交易所返回的原始数据, 防止有些用户需要一些特殊的数据
     */
    private final String data;

    /**
     * 时间戳
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
    private final BigDecimal last;

    /**
     * 交易量
     */
    private final BigDecimal volume;

    public Ticker(String data, Long time, BigDecimal open, BigDecimal high,
                  BigDecimal low, BigDecimal last, BigDecimal volume) {
        this.data = data;
        this.time = time;
        this.open = Util.strip(open);
        this.high = Util.strip(high);
        this.low = Util.strip(low);
        this.last = Util.strip(last);
        this.volume = Util.strip(volume);
    }

    @Override
    public String to() {
        return '{' +
                "\"data\":" + Util.encode(this.data) +
                ",\"time\":" + Util.to(this.time) +
                ",\"open\":" + Util.to(this.open) +
                ",\"high\":" + Util.to(this.high) +
                ",\"low\":" + Util.to(this.low) +
                ",\"last\":" + Util.to(this.last) +
                ",\"volume\":" + Util.to(this.volume) +
                '}';
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

    public double last() {
        Objects.requireNonNull(this.last);
        return this.last.doubleValue();
    }

    public double volume() {
        Objects.requireNonNull(this.volume);
        return this.volume.doubleValue();
    }

    public static Ticker of(String data, Logger log) {
        try {
            JSONObject r = JSON.parseObject(data);
            return new Ticker(
                    Util.decode(r.getString("data")),
                    r.getLong("time"),
                    r.getBigDecimal("open"),
                    r.getBigDecimal("high"),
                    r.getBigDecimal("low"),
                    r.getBigDecimal("last"),
                    r.getBigDecimal("volume"));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            log.error("of error -> {}", data);
        }
        return null;
    }

}
