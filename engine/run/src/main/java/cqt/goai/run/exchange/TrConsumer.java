package cqt.goai.run.exchange;

/**
 * @author GOAi
 */
@FunctionalInterface
public interface TrConsumer<T, U, K> {
    /**
     * 接受3个参数
     * @param t t
     * @param u u
     * @param k k
     */
    void accept(T t, U u, K k);
}
