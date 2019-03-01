package cqt.goai.model.trade;

import cqt.goai.model.BaseModelList;
import cqt.goai.model.Util;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;

/**
 * 多笔订单
 * @author GOAi
 */
public class Orders extends BaseModelList<Order> implements Serializable {

    private static final long serialVersionUID = 3062332528451320335L;

    public Orders(List<Order> list) {
        super(list);
    }

    public static Orders of(String data, Logger log) {
        return Util.of(data, Orders::new, Order::of, log);
    }

    @Override
    public String toString() {
        return "Orders" + super.toString();
    }

}
