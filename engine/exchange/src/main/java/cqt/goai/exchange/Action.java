package cqt.goai.exchange;

/**
 * 请求行为动作
 * @author GOAi

 */
public enum Action {
    // 市场接口
    TICKER,
    KLINES,
    DEPTH,
    TRADES,

    // 账户接口
    BALANCES,
    ACCOUNT,

    // 交易接口
    PRECISIONS,
    PRECISION,
    BUY_LIMIT,
    SELL_LIMIT,
    BUY_MARKET,
    SELL_MARKET,
    MULTI_BUY,
    MULTI_SELL,
    CANCEL_ORDER,
    CANCEL_ORDERS,

    // 订单接口
    ORDERS,
    HISTORY_ORDERS,
    ORDER,
    ORDER_DETAILS,

    // 推送接口
    ON_TICKER,
    ON_KLINES,
    ON_DEPTH,
    ON_TRADES,
    ON_ACCOUNT,
    ON_ORDERS
}
