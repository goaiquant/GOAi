package cqt.goai.model.trade;

import cqt.goai.model.BaseModelList;
import cqt.goai.model.Util;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;

/**
 * 账户下所有币余额信息
 * @author GOAi
 */
public class Balances extends BaseModelList<Balance> implements Serializable {

    private static final long serialVersionUID = -58830328674451561L;

    public Balances(List<Balance> list) {
        super(list);
    }

    public static Balances of(String data, Logger log) {
        return Util.of(data, Balances::new, Balance::of, log);
    }

    @Override
    public String toString() {
        return "Balances" + super.toString();
    }

}
