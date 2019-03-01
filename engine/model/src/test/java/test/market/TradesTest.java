package test.market;

import cqt.goai.model.enums.Side;
import cqt.goai.model.market.Trade;
import cqt.goai.model.market.Trades;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;

public class TradesTest {

    @Test
    public void test() {
        Trades trades = new Trades(Arrays.asList(new Trade(
                "asd{89as\"",
                123L,
                "123",
                Side.BUY,
                new BigDecimal("1"),
                new BigDecimal("2")
        ),new Trade(
                null,
                123L,
                "123",
                Side.SELL,
                new BigDecimal("1"),
                new BigDecimal("2")
        )));

        String data = trades.to();
        Assert.assertEquals("[[\"YXNkezg5YXMi\",123,\"123\",\"BUY\",1,2],[null,123,\"123\",\"SELL\",1,2]]", data);
        Trades ts = Trades.of(data, null);
        Assert.assertEquals(trades, ts);
    }

}
