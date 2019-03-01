package cqt.goai.run.main;

import cqt.goai.run.exchange.Exchange;

import java.util.function.Function;

/**
 * 自定义类型实现该接口，可实现自动注入
 * @author GOAi
 */
public interface Injectable {

    /**
     * 注入
     *
     * @param config 配置内容
     * @param getExchange 获取exchange 参数为json字符串
     *           {"name":"xxx","symbol":"BTC_USD","access":"xxx","secret":"xxx"}
     */
    void inject(String config, Function<String, Exchange> getExchange);



}
