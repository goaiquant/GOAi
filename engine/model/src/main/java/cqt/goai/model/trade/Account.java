package cqt.goai.model.trade;

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
 * 某个币对账户信息
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class Account implements To, Serializable {

    private static final long serialVersionUID = -81334812598889084L;

    /**
     * 时间戳
     */
    private final Long time;

    /**
     * 目标货币 BTC_USD 中的 BTC, 用USD买卖BTC
     */
    private final Balance base;

    /**
     * 计价货币 BTC_USD 中的 USD
     */
    private final Balance quote;

    public Account(Long time, Balance base, Balance quote) {
        this.time = time;
        this.base = base;
        this.quote = quote;
    }

    public BigDecimal getBaseFree() {
        Objects.requireNonNull(base);
        return this.base.getFree();
    }

    public BigDecimal getBaseUsed() {
        Objects.requireNonNull(base);
        return this.base.getUsed();
    }

    public BigDecimal getQuoteFree() {
        Objects.requireNonNull(quote);
        return this.quote.getFree();
    }

    public BigDecimal getQuoteUsed() {
        Objects.requireNonNull(quote);
        return this.quote.getUsed();
    }

    public double baseFree() {
        Objects.requireNonNull(base);
        return this.base.free();
    }

    public double baseUsed() {
        Objects.requireNonNull(base);
        return this.base.used();
    }

    public double quoteFree() {
        Objects.requireNonNull(quote);
        return this.quote.free();
    }

    public double quoteUsed() {
        Objects.requireNonNull(quote);
        return this.quote.used();
    }

    @Override
    public String to() {
        return '{' +
                "\"time\":" + Util.to(this.time) +
                ",\"base\":" + Util.to(this.base) +
                ",\"quote\":" + Util.to(this.quote) +
                '}';
    }

    public static Account of(String data, Logger log) {
        try {
            JSONObject r = JSON.parseObject(data);
            return new Account(
                    r.getLong("time"),
                    Balance.of(r.getJSONArray("base")),
                    Balance.of(r.getJSONArray("quote")));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            log.error("of error -> {}", data);
        }
        return null;
    }

}
