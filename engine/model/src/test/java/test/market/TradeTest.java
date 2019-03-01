package test.market;

import cqt.goai.model.enums.Side;
import cqt.goai.model.market.Trade;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

public class TradeTest {

    @Test
    public void test() {
        Trade trade = new Trade(
                "[123,123]",
                1546419093195L,
                "123",
                Side.BUY,
                new BigDecimal(1),
                new BigDecimal(2)
                );
        String data = trade.to();
        Assert.assertEquals("[\"WzEyMywxMjNd\",1546419093195,\"123\",\"BUY\",1,2]", data);
        Trade t = Trade.of(data, null);
        Assert.assertEquals(trade, t);
    }

}
