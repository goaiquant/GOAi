package cqt.goai.model.enums;

/**
 * 订单状态
 * @author GOAi
 */
public enum State {
    // 已提交
    SUBMIT,
    // 已成交
    FILLED,
    // 已取消
    CANCEL,
    // 部分成交
    PARTIAL,
    // 部分成交后取消
    UNDONE,

}