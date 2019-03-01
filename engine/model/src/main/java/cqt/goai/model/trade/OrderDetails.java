package cqt.goai.model.trade;

import cqt.goai.model.BaseModelList;
import cqt.goai.model.Util;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;

/**
 * 多个订单成交明细记录
 * @author GOAi
 */
public class OrderDetails extends BaseModelList<OrderDetail> implements Serializable {

    private static final long serialVersionUID = -5883032867443051561L;

    public OrderDetails(List<OrderDetail> list) {
        super(list);
    }

    public static OrderDetails of(String data, Logger log) {
        return Util.of(data, OrderDetails::new, OrderDetail::of, log);
    }

    @Override
    public String toString() {
        return "OrderDetails" + super.toString();
    }

}
