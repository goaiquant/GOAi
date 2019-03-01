package test;

import cqt.goai.exchange.util.RateLimit;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author GOAi
 */
public class RateLimitTest {

    @Test
    public void test() throws InterruptedException {
        RateLimit NO = RateLimit.NO;
        for (int i = 0; i < 10000; i++) {
            // 次次需要更新
            Assert.assertTrue(NO.timeout(false));
        }

        RateLimit limit = RateLimit.limit(300);
        Assert.assertFalse(limit.timeout()); // 300毫秒之内，不需要推送
        Thread.sleep(200);
        Assert.assertFalse(limit.timeout()); // 300毫秒之内，不需要推送
        Thread.sleep(200);
        Assert.assertTrue(limit.timeout()); // 300毫秒之外，需要推送
        Assert.assertFalse(limit.timeout()); // 300毫秒之内，不需要推送

    }

}
