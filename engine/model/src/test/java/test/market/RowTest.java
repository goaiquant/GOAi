package test.market;

import cqt.goai.model.market.Row;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class RowTest {

    @Test
    public void test() {
        Row row = new Row("12321,qwr42w3r238987c",new BigDecimal("1.12"),new BigDecimal("1.12"));
        String data = row.to();
        Assert.assertEquals("[\"MTIzMjEscXdyNDJ3M3IyMzg5ODdj\",1.12,1.12]", data);
        Row r = Row.of(data, null);
        Assert.assertEquals(row, r);
    }

}
