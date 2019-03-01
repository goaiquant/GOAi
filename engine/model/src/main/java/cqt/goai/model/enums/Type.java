package cqt.goai.model.enums;

/**
 * 订单类型
 * @author GOAi
 */
public enum Type {
    // 限价单
    LIMIT,
    // 市价单
    MARKET,
    // FOK
    FILL_OR_KILL,
    // IOC
    IMMEDIATE_OR_CANCEL,
}
