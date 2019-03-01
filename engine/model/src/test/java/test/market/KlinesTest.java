package test.market;

import cqt.goai.model.market.Kline;
import cqt.goai.model.market.Klines;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class KlinesTest {

    @Test
    public void test() {
        Klines klines = new Klines(Arrays.asList(new Kline(
                "{123,\"123\"}",
                1546422552967L,
                null,
                new BigDecimal("1"),
                new BigDecimal("2"),
                new BigDecimal("3"),
                new BigDecimal("0")
        ),new Kline(
                null,
                1546422552968L,
                null,
                new BigDecimal("2"),
                new BigDecimal("3"),
                new BigDecimal("4"),
                new BigDecimal("5")
        )));

        String data = klines.to();
        Assert.assertEquals("[[\"ezEyMywiMTIzIn0=\",1546422552967,null,1,2,3,0],[null,1546422552968,null,2,3,4,5]]", data);

        Klines c = Klines.of(data, null);
        Assert.assertEquals(klines, c);
    }

}
