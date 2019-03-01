package test.market;

import cqt.goai.model.market.Row;
import cqt.goai.model.market.Rows;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class RowsTest {

    @Test
    public void test() {
        Rows rows = new Rows(Arrays.asList(new Row(
                "{123,\"123\"}",
                new BigDecimal("3"),
                new BigDecimal("0")
        ),new Row(
                null,
                new BigDecimal("3"),
                new BigDecimal("0")
        )));

        String data = rows.to();
        Assert.assertEquals("[[\"ezEyMywiMTIzIn0=\",3,0],[null,3,0]]", data);

        Rows rs = Rows.of(data, null);
        Assert.assertEquals(rows, rs);
    }

}
