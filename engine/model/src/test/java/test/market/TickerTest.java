package test.market;

import cqt.goai.model.market.Ticker;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class TickerTest {

    @Test
    public void test() {
        Ticker ticker = new Ticker(
                "{123,\"123\"}",
                1546419093195L,
                new BigDecimal(1),
                new BigDecimal(2),
                null,
                new BigDecimal(4),
                new BigDecimal(5)
                );
        String data = ticker.to();
        Assert.assertEquals("{\"data\":\"ezEyMywiMTIzIn0=\",\"time\":1546419093195,\"open\":1,\"high\":2,\"low\":null,\"last\":4,\"volume\":5}", data);
        Ticker t = Ticker.of(data, null);
        Assert.assertEquals(ticker, t);
    }

}
