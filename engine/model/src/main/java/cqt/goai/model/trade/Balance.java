package cqt.goai.model.trade;

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
 * 某个币的余额
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class Balance implements To, Serializable {

    private static final long serialVersionUID = -8006911245239428396L;

    /**
     * 交易所返回的原始数据, 防止有些用户需要一些特殊的数据
     */
    private final String data;

    /**
     * 币的名称 BTC USD 等
     */
    private final String currency;

    /**
     * 可用数量
     */
    private final BigDecimal free;

    /**
     * 冻结数量
     */
    private final BigDecimal used;

    public Balance(String data, String currency, BigDecimal free, BigDecimal used) {
        this.data = data;
        this.currency = currency;
        this.free = Util.strip(free);
        this.used = Util.strip(used);
    }

    @Override
    public String to() {
        return '[' +
                Util.encode(data) +
                "," + Util.to(currency) +
                "," + Util.to(free) +
                "," + Util.to(used) +
                ']';
    }

    public double free() {
        Objects.requireNonNull(this.free);
        return this.free.doubleValue();
    }

    public double used() {
        Objects.requireNonNull(this.used);
        return this.used.doubleValue();
    }

    public static Balance of(String data, Logger log) {
        return Util.of(data, Balance::of, log);
    }

    static Balance of(JSONArray r) {
        return new Balance(
                Util.decode(r.getString(0)),
                r.getString(1),
                r.getBigDecimal(2),
                r.getBigDecimal(3));
    }
}
