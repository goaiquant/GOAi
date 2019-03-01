package test.market;

import cqt.goai.model.market.Depth;
import cqt.goai.model.market.Row;
import cqt.goai.model.market.Rows;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class DepthTest {

    @Test
    public void test() {
        Depth depth = new Depth(
                1546426483495L,
                null,
                new Rows( Arrays.asList(new Row("231",new BigDecimal("1"), new BigDecimal("2") ),
                        new Row(null, new BigDecimal("0.500"), new BigDecimal("1"))))
                );
        String data = depth.to();
        Assert.assertEquals("{\"time\":1546426483495,\"asks\":null,\"bids\":[[\"MjMx\",1,2],[null,0.5,1]]}", data);
        Depth t = Depth.of(data, null);
        Assert.assertEquals(depth, t);
    }

}
