package cqt.goai.exchange;

import cqt.goai.exchange.http.HttpExchange;
import cqt.goai.exchange.http.binance.BinanceExchange;
import cqt.goai.exchange.http.bitfinex.BitfinexExchange;
import cqt.goai.exchange.http.huobi.pro.HuobiProExchange;
import cqt.goai.exchange.http.okexv3.Okexv3Exchange;
import cqt.goai.exchange.web.socket.WebSocketExchange;
import cqt.goai.exchange.web.socket.okexv3.Okexv3WebSocketExchange;
import org.slf4j.Logger;

/**
 * 统一获取Exchange对象
 * @author GOAi
 */
public class ExchangeManager {

    /**
     * 获取HttpExchange
     * Simple Factory Pattern 简单工厂模式
     * 获取实例对象即可，没有太多的拓展性
     * @param name 交易所名
     * @param log 日志
     */
    public static HttpExchange getHttpExchange(ExchangeName name, Logger log) {
        if (null != name) {
            switch (name) {
                case OKEXV3: return new Okexv3Exchange(log);
                case BITFINEX: return new BitfinexExchange(log);
                case HUOBIPRO: return new HuobiProExchange(log);
                case BINANCE: return new BinanceExchange(log);
                default:
            }
        }
        throw new ExchangeException(ExchangeError.EXCHANGE, "exchange name is not supported");
    }

    /**
     * 获取WSExchange
     * 简单工厂模式
     * @param name 交易所名
     * @param log 日志
     */
    public static WebSocketExchange getWebSocketExchange(ExchangeName name, Logger log) {
        if (null != name) {
            switch (name) {
                case OKEXV3: return new Okexv3WebSocketExchange(log);
                default:
            }
        }
        throw new ExchangeException(ExchangeError.EXCHANGE, "exchange name is not supported");
    }

}
