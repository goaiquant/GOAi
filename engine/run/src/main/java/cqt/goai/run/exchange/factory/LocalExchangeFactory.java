package cqt.goai.run.exchange.factory;

import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.run.exchange.Exchange;
import cqt.goai.run.exchange.LocalExchange;
import cqt.goai.run.exchange.ProxyExchange;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * 本地实现Exchange
 * @author GOAi
 */
public class LocalExchangeFactory extends BaseExchangeFactory {

    @Override
    public Exchange getExchange(boolean proxy, Logger log, JSONObject config, Supplier<Boolean> ready) {
        Exchange exchange = new LocalExchange(log,
                ExchangeName.getByName(config.getString("name")),
                config.getString("symbol"),
                config.getString("access"),
                config.getString("secret"),
                ready);
        if (proxy) {
            exchange = new ProxyExchange(exchange);
        }
        return exchange;
    }
}
