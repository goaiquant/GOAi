package cqt.goai.run.exchange.model;

import cqt.goai.exchange.util.RateLimit;

import java.util.function.Consumer;

/**
 * 某个具体订阅
 * @author GOAi
 */
public class ModelObserver<T> {
    /**
     * 消费函数
     */
    final Consumer<T> consumer;
    /**
     * 频率限制
     */
    final RateLimit limit;

    /**
     * 推送id
     */
    private final String id;

    public ModelObserver(Consumer<T> consumer, RateLimit limit, String id) {
        this.consumer = consumer;
        this.limit = limit;
        this.id = id;
    }

    /**
     * 接收model
     * @param model 新的对象
     */
    void on(T model) {
        if (this.limit.timeout()) {
            this.consumer.accept(model);
        }
    }

    public String getId() {
        return id;
    }
}
