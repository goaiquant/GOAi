package cqt.goai.exchange.util;

/**
 * 频率限制
 * 2种限制
 * 1. 主动推送限制频率，limit毫秒内只有一次
 * 2. 超时限制，推送则更新，30s未推送需要重连
 *
 * @author GOAi
 */
public class RateLimit {

    /**
     * 无限制
     */
    public static final RateLimit NO = new RateLimit(0);

    /**
     * 上次true时间
     * 这里判断时间应当非常快没有阻塞的，就不考虑并发问题了，反正限制的频率都是主动推送，不必那么精确的
     * AtomicLong 和 Lock 都可以实现并发安全，但是得不偿失，因为即使多推送一次，无伤大雅
     */
    private long last;

    /**
     * 频率限制，若为5000，则在下个5000毫秒内，不会返回true
     */
    private final long limit;

    private RateLimit(long limit) {
        this.limit = limit;
        this.last = 0;
    }

    /**
     * 获取实例
     * @param milliseconds 毫秒
     * @return 实例
     */
    public static RateLimit limit(long milliseconds) {
        return new RateLimit(milliseconds);
    }

    public static RateLimit second() { return new RateLimit(1000); }
    public static RateLimit second5() { return new RateLimit(1000 * 5); }
    public static RateLimit second10() { return new RateLimit(1000 * 10); }
    public static RateLimit second15() { return new RateLimit(1000 * 15); }
    public static RateLimit second20() { return new RateLimit(1000 * 20); }
    public static RateLimit second30() { return new RateLimit(1000 * 30); }
    public static RateLimit second3() { return new RateLimit(1000 * 3); }
    public static RateLimit second13() { return new RateLimit(1000 * 13); }
    public static RateLimit second23() { return new RateLimit(1000 * 23); }

    /**
     * limit内不会再次返回true
     * @param update 是否更新
     * @return 是否到了下一次
     */
    public boolean timeout(boolean update) {
        if (limit <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        boolean result = last + limit < now;
        if (update && result) {
            this.last = now;
        }
        return result;
    }

    /**
     * limit内不会再次返回true
     * @return 是否到了下一次
     */
    public boolean timeout() {
        return this.timeout(true);
    }

    /**
     * 更新时间, 收到消息更新一下
     */
    public void update() {
        this.last = System.currentTimeMillis();
    }

    /**
     * 是否有限制
     * @return 是否有限制
     */
    public boolean isLimit() {
        return 0 < this.limit;
    }

    @Override
    public String toString() {
        return "RateLimit{" +
                "limit=" + limit +
                '}';
    }
}
