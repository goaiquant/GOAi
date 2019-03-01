package test.trade;

import cqt.goai.model.trade.Precision;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class PrecisionTest {

    @Test
    public void test() {
        Precision precision = new Precision(
                "asda\"",
                "symbol",
                2,
                3,
                new BigDecimal("3"),
                new BigDecimal("5"),
                new BigDecimal("0.01"),
                new BigDecimal("0.001"),
                new BigDecimal("50").stripTrailingZeros(),
                new BigDecimal("0.01")
                );
        String data = precision.to();
        Assert.assertEquals("[\"YXNkYSI=\",\"symbol\",2,3,3,5,0.01,0.001,50,0.01]", data);
        Assert.assertEquals(precision, Precision.of(data, null));
    }

}
