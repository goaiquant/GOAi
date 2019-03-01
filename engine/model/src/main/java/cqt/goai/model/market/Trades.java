package cqt.goai.model.market;

import cqt.goai.model.BaseModelList;
import cqt.goai.model.Util;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;

/**
 * 多个成交信息
 * @author GOAi
 */
public class Trades extends BaseModelList<Trade> implements Serializable {

    private static final long serialVersionUID = -5998165404968198947L;

    public Trades(List<Trade> list) {
        super(list);
    }

    public static Trades of(String data, Logger log) {
        return Util.of(data, Trades::new, Trade::of, log);
    }

    @Override
    public String toString() {
        return "Trades" + super.toString();
    }

}
