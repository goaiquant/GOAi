package cqt.goai.run.exchange.model;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.Action;
import cqt.goai.model.market.Row;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author GOAi
 */
@Data
public class LogAction {

    private String name;
    private String symbol;
    private String access;
    private Action action;

    private BigDecimal price;
    private BigDecimal amount;

    private BigDecimal quote;
    private BigDecimal base;

    private List<Row> rows;

    private String id;
    private List<String> ids;

    private LogAction(String name, String symbol, String access, Action action) {
        this.name = name;
        this.symbol = symbol;
        this.access = access;
        this.action = action;
    }

    private LogAction(String name, String symbol, String access, Action action,
                      BigDecimal price, BigDecimal amount) {
        this(name, symbol, access, action);
        this.price = price;
        this.amount = amount;
    }

    public static LogAction buyLimit(String name, String symbol, String access,
                                     BigDecimal price, BigDecimal amount) {
        return new LogAction(name, symbol, access, Action.BUY_LIMIT, price, amount);
    }

    public static LogAction sellLimit(String name, String symbol, String access,
                                     BigDecimal price, BigDecimal amount) {
        return new LogAction(name, symbol, access, Action.SELL_LIMIT, price, amount);
    }

    public static LogAction buyMarket(String name, String symbol, String access, BigDecimal quote) {
        LogAction la = new LogAction(name, symbol, access, Action.BUY_MARKET);
        la.quote = quote;
        return la;
    }

    public static LogAction sellMarket(String name, String symbol, String access, BigDecimal base) {
        LogAction la = new LogAction(name, symbol, access, Action.BUY_MARKET);
        la.base = base;
        return la;
    }

    public static LogAction multiBuy(String name, String symbol, String access, List<Row> rows) {
        LogAction la = new LogAction(name, symbol, access, Action.MULTI_BUY);
        la.rows = rows;
        return la;
    }

    public static LogAction multiSell(String name, String symbol, String access, List<Row> rows) {
        LogAction la = new LogAction(name, symbol, access, Action.MULTI_SELL);
        la.rows = rows;
        return la;
    }

    private static final String NAME = "name";
    private static final String SYMBOL = "symbol";
    private static final String ACCESS = "access";
    private static final String ACTION = "action";
    private static final String PRICE = "price";
    private static final String AMOUNT = "amount";
    private static final String QUOTE = "quote";
    private static final String BASE = "base";
    private static final String ROWS = "rows";
    private static final String ID = "id";
    private static final String IDS = "ids";

    public static LogAction of(String json) {
        JSONObject r = JSON.parseObject(json);
        String name = null;
        String symbol = null;
        String access = null;
        Action action = null;
        if (r.containsKey(NAME)) {
            name = r.getString(NAME);
        }
        if (r.containsKey(SYMBOL)) {
            symbol = r.getString(SYMBOL);
        }
        if (r.containsKey(ACCESS)) {
            access = r.getString(ACCESS);
        }
        if (r.containsKey(ACTION)) {
            action = Action.valueOf(r.getString(ACTION));
        }
        LogAction la = new LogAction(name, symbol, access, action);
        if (r.containsKey(PRICE)) {
            la.price = r.getBigDecimal(PRICE);
        }
        if (r.containsKey(AMOUNT)) {
            la.amount = r.getBigDecimal(AMOUNT);
        }
        if (r.containsKey(QUOTE)) {
            la.quote = r.getBigDecimal(QUOTE);
        }
        if (r.containsKey(BASE)) {
            la.base = r.getBigDecimal(BASE);
        }
        if (r.containsKey(ROWS)) {
            JSONArray a = r.getJSONArray(ROWS);
            la.rows = new ArrayList<>(a.size());
            for (int i = 0; i < a.size(); i++) {
                la.rows.add(Row.row(a.getJSONObject(i).getBigDecimal("price"),
                        a.getJSONObject(i).getBigDecimal("amount")));
            }
        }
        if (r.containsKey(ID)) {
            la.id = r.getString(ID);
        }
        if (r.containsKey(IDS)) {
            JSONArray array = r.getJSONArray(IDS);
            la.ids = new ArrayList<>(array.size());
            for (int i = 0; i < array.size(); i++) {
                la.ids.add(array.getString(i));
            }
        }

        return la;
    }

}
