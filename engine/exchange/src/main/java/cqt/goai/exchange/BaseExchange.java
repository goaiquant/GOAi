package cqt.goai.exchange;

import org.slf4j.Logger;

/**
 * 交易所基本信息
 * @author GOAi
 */
public class BaseExchange {

    /**
     * 日志
     */
    protected final Logger log;

    /**
     * 交易所名称
     */
    protected final ExchangeName name;

    public BaseExchange(ExchangeName name, Logger log) {
        this.name = name;
        this.log = log;
    }

}
