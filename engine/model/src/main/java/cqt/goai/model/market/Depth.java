package cqt.goai.model.market;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.model.To;
import cqt.goai.model.Util;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.slf4j.Logger;

import java.io.Serializable;

/**
 * 盘口深度
 * @author GOAi
 */
@Getter
@ToString
@EqualsAndHashCode
public class Depth implements To, Serializable {

    private static final long serialVersionUID = -8133481259875889084L;

    /**
     * 时间戳
     */
    private final Long time;

    /**
     * 卖盘数据，由低到高
     */
    private final Rows asks;

    /**
     * 买盘数据，由高到低
     */
    private final Rows bids;

    public Depth(Long time, Rows asks, Rows bids) {
        this.time = time;
        this.asks = asks;
        this.bids = bids;
    }

    @Override
    public String to() {
        return '{' +
                "\"time\":" + Util.to(this.time) +
                ",\"asks\":" + Util.to(this.asks) +
                ",\"bids\":" + Util.to(this.bids) +
                '}';
    }

    public static Depth of(String data, Logger log) {
        try {
            JSONObject r = JSON.parseObject(data);
            return new Depth(
                    r.getLong("time"),
                    Rows.of(r.getJSONArray("asks"), log),
                    Rows.of(r.getJSONArray("bids"), log));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            log.error("of error -> {}", data);
        }
        return null;
    }

}
