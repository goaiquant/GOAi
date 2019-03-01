package cqt.goai.exchange.web.socket;

import cqt.goai.exchange.Action;
import cqt.goai.model.enums.Period;
import lombok.Data;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * WebSocket推送封装的信息
 * @author GOAi
 */
@Data
public class WebSocketInfo<T> {

    /**
     * 推送id
     */
    private String id;

    /**
     * 推送行为类型
     */
    private Action action;

    /**
     * 推送币对
     */
    private String symbol;

    /**
     * 推送用户
     */
    private String access;

    /**
     * 消费函数
     */
    private Consumer<T> consumer;

    /**
     * 周期
     */
    private Period period;

    public WebSocketInfo(String id, Action action, String symbol, String access, Consumer<T> consumer) {
        this.id = id;
        this.action = action;
        this.symbol = symbol;
        this.access = access;
        this.consumer = consumer;
    }

    public WebSocketInfo(String id, Action action, String symbol, String access, Consumer<T> consumer, Period period) {
        this.id = id;
        this.action = action;
        this.symbol = symbol;
        this.access = access;
        this.consumer = consumer;
        this.period = period;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WebSocketInfo<?> onInfo = (WebSocketInfo<?>) o;
        return Objects.equals(id, onInfo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
