package cqt.goai.exchange;

import cqt.goai.exchange.util.CompleteList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static cqt.goai.exchange.Action.*;
import static cqt.goai.exchange.Action.ORDER;
import static cqt.goai.exchange.util.CompleteList.*;

/**
 * 交易所名称
 *
 * @author GOAi
 */
public enum ExchangeName {

    /**
     * OKEX V3 版本, ALL
     * https://www.okex.com/docs/zh/
     * CompleteList.ALL
     */
    OKEXV3("okexv3", CompleteList.ALL),

    /**
     * bitfinex
     * https://docs.bitfinex.com/v1/docs/rest-general
     * https://docs.bitfinex.com/v2/docs/rest-auth
     * CompleteList.MARKET
     * CompleteList.ACCOUNT
     * CompleteList.TRADE
     * CompleteList.ORDER
     */
    BITFINEX("bitfinex", CompleteList.exclude(PUSH)),


    /**
     * huobipro
     * https://github.com/huobiapi/API_Docs
     */
    HUOBIPRO("huobipro", CompleteList.exclude(PUSH)),



    /**
     * binance
     * https://github.com/binance-exchange/binance-official-api-docs/blob/master/rest-api.md
     */
    BINANCE("binance", CompleteList.exclude(PUSH));


    /**
     * 交易所名称，唯一识别
     */
    private String name;

    /**
     * 已完成的功能
     */
    private List<Action> functions;

    ExchangeName(String name, Action... actions) {
        this.name = name;
        this.functions = null == actions ? new ArrayList<>() : Arrays.asList(actions);
    }

    public String getName() {
        return this.name;
    }

    public List<Action> getFunctions() {
        return this.functions;
    }

    /**
     * 根据名称获取
     */
    public static ExchangeName getByName(String name) {
        for (ExchangeName en : ExchangeName.values()) {
            if (en.name.equals(name)) {
                return en;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.name;
    }

}
