package cqt.goai.exchange;


import dive.common.model.Message;

/**
 * 交易所信息处理异常
 * @author GOAi
 */
public class ExchangeException extends RuntimeException {

    private static final long serialVersionUID = -8633608065854161508L;

    private Integer code = Message.CODE_FAILED;

    public ExchangeException(String message) {
        super(message);
    }

    public ExchangeException(ExchangeError error, String message) {
        super(message);
        this.code = error.getCode();
    }

    public ExchangeException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ExchangeException(ExchangeError error, String message, Throwable throwable) {
        super(message, throwable);
        this.code = error.getCode();
    }


    public Integer getCode() {
        return code;
    }
}
