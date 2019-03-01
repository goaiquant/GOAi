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
 * 某交易币对的精度
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class Precision implements To, Serializable {

    private static final long serialVersionUID = -5822155062030969112L;

    /**
     * 交易所返回的原始数据, 防止有些用户需要一些特殊的数据
     */
    private final String data;

    /**
     * 币对
     */
    private final String symbol;

    /**
     * 交易时币精度，amount
     */
    private final Integer base;

    /**
     * 计价货币精度，price BTC/USD 1btc->6000usd  所以数数数的是usd，也就是写price的位置的精度
     */
    private final Integer quote;

    /**
     * 最小币步长
     */
    private final BigDecimal baseStep;

    /**
     * 最小价格步长
     */
    private final BigDecimal quoteStep;

    /**
     * 最小买卖数量
     */
    private final BigDecimal minBase;

    /**
     * 最小买价格
     */
    private final BigDecimal minQuote;

    /**
     * 最大买卖数量
     */
    private final BigDecimal maxBase;

    /**
     * 最大买价格
     */
    private final BigDecimal maxQuote;

    public Precision(String data, String symbol,
                     Integer base, Integer quote,
                     BigDecimal baseStep, BigDecimal quoteStep,
                     BigDecimal minBase, BigDecimal minQuote,
                     BigDecimal maxBase, BigDecimal maxQuote) {
        this.data = data;
        this.symbol = symbol;
        this.base = base;
        this.quote = quote;
        this.baseStep = Util.strip(baseStep);
        this.quoteStep = Util.strip(quoteStep);
        this.minBase = Util.strip(minBase);
        this.minQuote = Util.strip(minQuote);
        this.maxBase = Util.strip(maxBase);
        this.maxQuote = Util.strip(maxQuote);
    }

    @Override
    public String to() {
        return '[' +
                Util.encode(this.data) +
                "," + Util.to(this.symbol) +
                "," + Util.to(this.base) +
                "," + Util.to(this.quote) +
                "," + Util.to(this.baseStep) +
                "," + Util.to(this.quoteStep) +
                "," + Util.to(this.minBase) +
                "," + Util.to(this.minQuote) +
                "," + Util.to(this.maxBase) +
                "," + Util.to(this.maxQuote) +
                ']';
    }

    public double baseStep() {
        Objects.requireNonNull(this.baseStep);
        return this.baseStep.doubleValue();
    }

    public double quoteStep() {
        Objects.requireNonNull(this.quoteStep);
        return this.quoteStep.doubleValue();
    }

    public double minBase() {
        Objects.requireNonNull(this.minBase);
        return this.minBase.doubleValue();
    }

    public double minQuote() {
        Objects.requireNonNull(this.minQuote);
        return this.minQuote.doubleValue();
    }

    public double maxBase() {
        Objects.requireNonNull(this.maxBase);
        return this.maxBase.doubleValue();
    }

    public double maxQuote() {
        Objects.requireNonNull(this.maxQuote);
        return this.maxQuote.doubleValue();
    }

    public static Precision of(String data, Logger log) {
        return Util.of(data, Precision::of, log);
    }

    static Precision of(JSONArray r) {
        return new Precision(
                Util.decode(r.getString(0)),
                r.getString(1),
                r.getInteger(2),
                r.getInteger(3),
                r.getBigDecimal(4),
                r.getBigDecimal(5),
                r.getBigDecimal(6),
                r.getBigDecimal(7),
                r.getBigDecimal(8),
                r.getBigDecimal(9));
    }

}
