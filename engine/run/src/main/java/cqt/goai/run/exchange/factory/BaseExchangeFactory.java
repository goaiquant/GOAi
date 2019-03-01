package cqt.goai.run.exchange.factory;

import com.alibaba.fastjson.JSONObject;
import cqt.goai.run.exchange.Exchange;
import org.slf4j.Logger;

import java.util.function.Supplier;

/**
 * @author GOAi
 */
public abstract class BaseExchangeFactory {

    /**
     * Exchange工厂
     * Factory Method Pattern 工厂方法模式
     * 有多种Exchange类型，不同的工厂产生的Exchange底层实现不一样
     * @param proxy 是否包装代理
     * @param log 日志
     * @param config 配置信息
     * @param ready 是否准备好接收推送
     * @return Exchange
     */
    public abstract Exchange getExchange(boolean proxy, Logger log, JSONObject config, Supplier<Boolean> ready);

}
