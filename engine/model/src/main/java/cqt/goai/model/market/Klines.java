package cqt.goai.model.market;

import cqt.goai.model.BaseModelList;
import cqt.goai.model.Util;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;

/**
 * 多根K线，按时间顺序由近到远 get(0)是最近的K线
 * @author GOAi
 */
public class Klines extends BaseModelList<Kline> implements Serializable {

    private static final long serialVersionUID = 3954815101761999805L;

    public Klines(List<Kline> list) {
        super(list);
    }

    public static Klines of(String data, Logger log) {
        return Util.of(data, Klines::new, Kline::of, log);
    }

    @Override
    public String toString() {
        return "Klines" + super.toString();
    }
}
