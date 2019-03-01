package cqt.goai.model.trade;

import com.alibaba.fastjson.JSONArray;
import cqt.goai.model.To;
import cqt.goai.model.Util;
import cqt.goai.model.enums.Side;
import cqt.goai.model.enums.State;
import cqt.goai.model.enums.Type;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static java.math.BigDecimal.ZERO;

/**
 * 订单
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class Order implements To, Serializable {

    private static final long serialVersionUID = -298726083025069628L;

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
    private final String id;

    /**
     * 买卖方向
     */
    private final Side side;

    /**
     * 订单类型
     */
    private final Type type;

    /**
     * 订单状态
     */
    private final State state;

    /**
     * 下单价格
     */
    private final BigDecimal price;

    /**
     * 下单数量
     */
    private final BigDecimal amount;

    /**
     * 成交数量
     */
    private final BigDecimal deal;

    /**
     * 成交均价
     */
    private BigDecimal average;

    public Order(String data, Long time, String id, Side side, Type type, State state,
                 BigDecimal price, BigDecimal amount, BigDecimal deal, BigDecimal average) {
        this.data = data;
        this.time = time;
        this.id = id;
        this.side = side;
        this.type = type;
        this.state = state;
        this.price = Util.strip(price);
        this.amount = Util.strip(amount);
        this.deal = Util.strip(deal);
        this.average = Util.strip(average);
    }

    @Override
    public String to() {
        return '[' +
                Util.encode(data) +
                ',' + Util.to(time) +
                ',' + Util.to(id) +
                ',' + Util.to(side) +
                ',' + Util.to(type) +
                ',' + Util.to(state) +
                ',' + Util.to(price) +
                ',' + Util.to(amount) +
                ',' + Util.to(deal) +
                ',' + Util.to(average) +
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

    public double deal() {
        Objects.requireNonNull(this.deal);
        return this.deal.doubleValue();
    }

    public double average() {
        Objects.requireNonNull(this.average);
        return this.average.doubleValue();
    }

    public static Order of(String data, Logger log) {
        return Util.of(data, Order::of, log);
    }

    static Order of(JSONArray r) {
        return new Order(
                Util.decode(r.getString(0)),
                r.getLong(1),
                r.getString(2),
                Util.of(r.getString(3), Side::valueOf),
                Util.of(r.getString(4), Type::valueOf),
                Util.of(r.getString(5), State::valueOf),
                r.getBigDecimal(6),
                r.getBigDecimal(7),
                r.getBigDecimal(8),
                r.getBigDecimal(9));
    }

    /**
     * 是否活跃订单
     * @return 是否活跃订单
     */
    public boolean alive() {
        return this.state == State.SUBMIT || this.state == State.PARTIAL;
    }

    /**
     * 是否有成交
     * @return 是否有成交
     */
    public boolean isDeal() {
        return this.state == State.FILLED || this.state == State.UNDONE;
    }

    /**
     * 成交总价
     * @return 成交总价
     */
    public BigDecimal dealVolume() {
        return null == this.average ? ZERO : this.deal.multiply(this.average);
    }

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public String simple() {
        return "Order{" +
                "time='" + LocalDateTime
                .ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
                .format(DTF) + '\'' +
                ", id='" + id + '\'' +
                ", side=" + side +
                ", type=" + type +
                ", state=" + state +
                ", price=" + price +
                ", amount=" + amount +
                ", deal=" + deal +
                ", average=" + average +
                '}';
    }

    public boolean isSell() {
        return this.side == Side.SELL;
    }

    public boolean isBuy() {
        return this.side == Side.BUY;
    }

}
