package cqt.goai.run.exchange.model;

import cqt.goai.exchange.util.RateLimit;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理某一类型对象
 * @author GOAi
 */
public class ModelManager<T> {
    /**
     * 缓存对象
     */
    private T model;

    /**
     * model是否超时
     */
    private RateLimit limit;

    /**
     * 管理所有需要推送的函数
     */
    private ConcurrentHashMap<String, ModelObserver<T>> observers = new ConcurrentHashMap<>();

    public ModelManager(RateLimit limit) {
        this.limit = limit;
    }

    /**
     * 推送model
     * @param ready 是否准备好接收
     * @param model model
     * @param id 推送id
     */
    public void on(boolean ready, T model, String id) {
        this.update(model);
        if (ready && this.on() && this.observers.containsKey(id)) {
            this.observers.get(id).on(model);
        }
    }

    /**
     * 是否正在推送
     */
    public boolean on() {
        return !observers.isEmpty();
    }

    /**
     * 注册推送
     * @param observer 注册者
     */
    public void observe(ModelObserver<T> observer) {
        this.observers.put(observer.getId(), observer);
    }

    /**
     * http方式不用推送
     * @param model model
     */
    public void update(T model) {
        this.model = model;
        this.limit.update();
    }

    public T getModel() {
        return model;
    }

    public RateLimit getLimit() {
        return limit;
    }

    public ConcurrentHashMap<String, ModelObserver<T>> getObservers() {
        return this.observers;
    }
}
