package cqt.goai.run.exchange;

/**
 * @author GOAi
 */
@FunctionalInterface
public interface FourFunction<T, U, K, L, R> {
    /**
     * 接受6个参数
     * @param t t
     * @param u u
     * @param k k
     * @param l l
     */
    R apply(T t, U u, K k, L l);
}
