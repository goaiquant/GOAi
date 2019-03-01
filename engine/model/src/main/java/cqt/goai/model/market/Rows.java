package cqt.goai.model.market;

import com.alibaba.fastjson.JSONArray;
import cqt.goai.model.BaseModelList;
import cqt.goai.model.Util;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;

/**
 * 多个档位
 * @author GOAi
 */
public class Rows extends BaseModelList<Row> implements Serializable {

    private static final long serialVersionUID = -7432880663696816431L;

    public Rows(List<Row> list) {
        super(list);
    }

    public static Rows of(String data, Logger log) {
        return Util.of(data, Rows::new, Row::of, log);
    }

    static Rows of(JSONArray r, Logger log) {
        return Util.of(r, Rows::new, Row::of, log);
    }

    @Override
    public String toString() {
        return "Rows" + super.toString();
    }

}
