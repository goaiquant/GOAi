package test.market;

import cqt.goai.model.market.Kline;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class KlineTest {

    @Test
    public void test() {
        Kline kline = new Kline(
                "[123,123]",
        1546419093195L,
        new BigDecimal(1),
        new BigDecimal(2),
        null,
        new BigDecimal(4),
        new BigDecimal(5)
        );
        String data = kline.to();
        Assert.assertEquals("[\"WzEyMywxMjNd\",1546419093195,1,2,null,4,5]", data);
        Kline c = Kline.of(data, null);
        Assert.assertEquals(kline, c);
    }

}
