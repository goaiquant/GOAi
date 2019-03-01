package cqt.goai.model.trade;

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
 * 某笔订单的一条成交明细记录
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class OrderDetail implements To, Serializable {

    private static final long serialVersionUID = 8135946558399552447L;

    /**
     * 交易所返回的原始数据, 防止有些用户需要一些特殊的数据
     */
    private final String data;

    /**
     * 时间戳
     */
    private final Long time;

    /**
     * 订单id
     */
    private final String orderId;

    /**
     * 成交明细id
     */
    private final String detailId;

    /**
     * 成交价格
     */
    private final BigDecimal price;

    /**
     * 成交数量
     */
    private final BigDecimal amount;

    /**
     * 手续费
     */
    private final BigDecimal fee;

    /**
     * 手续费货币
     */
    private final String feeCurrency;

    /**
     * 买卖方向
     */
    private final Side side;

    public OrderDetail(String data, Long time, String orderId, String detailId,
                       BigDecimal price, BigDecimal amount, BigDecimal fee, String feeCurrency) {
        this.data = data;
        this.time = time;
        this.orderId = orderId;
        this.detailId = detailId;
        this.price = Util.strip(price);
        this.amount = Util.strip(amount);
        this.fee = Util.strip(fee);
        this.feeCurrency = feeCurrency;
        this.side = null;
    }

    public OrderDetail(String data, Long time, String orderId, String detailId,
                       BigDecimal price, BigDecimal amount, BigDecimal fee, String feeCurrency, Side side) {
        this.data = data;
        this.time = time;
        this.orderId = orderId;
        this.detailId = detailId;
        this.price = Util.strip(price);
        this.amount = Util.strip(amount);
        this.fee = Util.strip(fee);
        this.feeCurrency = feeCurrency;
        this.side = side;
    }

    @Override
    public String to() {
        return '[' +
                Util.encode(this.data) +
                "," + Util.to(this.time) +
                "," + Util.to(this.orderId) +
                "," + Util.to(this.detailId) +
                "," + Util.to(this.price) +
                "," + Util.to(this.amount) +
                "," + Util.to(this.fee) +
                "," + Util.to(this.feeCurrency) +
                "," + Util.to(this.side) +
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

    public double fee() {
        Objects.requireNonNull(this.fee);
        return this.fee.doubleValue();
    }

    public static OrderDetail of(String data, Logger log) {
        return Util.of(data, OrderDetail::of, log);
    }

    static OrderDetail of(JSONArray r) {
        return new OrderDetail
                (Util.decode(r.getString(0)),
                        r.getLong(1),
                        r.getString(2),
                        r.getString(3),
                        r.getBigDecimal(4),
                        r.getBigDecimal(5),
                        r.getBigDecimal(6),
                        r.getString(7),
                        Util.of(r.getString(8), Side::valueOf));
    }
}
