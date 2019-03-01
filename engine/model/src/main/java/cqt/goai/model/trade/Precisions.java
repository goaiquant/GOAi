package cqt.goai.model.trade;

import cqt.goai.model.BaseModelList;
import cqt.goai.model.Util;
import org.slf4j.Logger;

import java.io.Serializable;
import java.util.List;

/**
 * @author GOAi
 */
public class Precisions extends BaseModelList<Precision> implements Serializable {

    private static final long serialVersionUID = 6672027458716855352L;

    public Precisions(List<Precision> list) {
        super(list);
    }

    public static Precisions of(String data, Logger log) {
        return Util.of(data, Precisions::new, Precision::of, log);
    }

    @Override
    public String toString() {
        return "Precisions" + super.toString();
    }

}
