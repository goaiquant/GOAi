package cqt.goai.exchange;

/**
 * 交易所相关错误码
 *
 * @author GOAi
 */
public enum ExchangeError {
    // 交易所不支持
    EXCHANGE(1001),
    // 币对不支持
    SYMBOL(1002),
    // K线周期不支持
    PERIOD(1003);
//    public static final Integer ERROR_TOKEN = 1001;     // 授权错误

    private int code;

    ExchangeError(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}
