package cqt.goai.exchange.util;

import cqt.goai.exchange.Action;

import java.util.*;
import java.util.stream.Stream;

import static cqt.goai.exchange.Action.*;

/**
 * 便捷标识交易所接口的完成情况
 * @author GOAi
 */
public class CompleteList {

    public static Action[] ALL;
    public static Action[] MARKET;
    public static Action[] ACCOUNT;
    public static Action[] TRADE;
    public static Action[] ORDER;
    public static Action[] PUSH;

    static {
        MARKET = new Action[]{TICKER, KLINES, DEPTH, TRADES};
        ACCOUNT = new Action[]{BALANCES, Action.ACCOUNT};
        TRADE = new Action[]{PRECISIONS, PRECISION, BUY_LIMIT, SELL_LIMIT,
                BUY_MARKET, SELL_MARKET, MULTI_BUY, MULTI_SELL, CANCEL_ORDER, CANCEL_ORDERS};
        ORDER = new Action[]{ORDERS, HISTORY_ORDERS, Action.ORDER, ORDER_DETAILS};
        PUSH = new Action[]{ON_TICKER, ON_KLINES, ON_DEPTH, ON_TRADES, ON_ACCOUNT, ON_ORDERS};
        Set<Action> list = new HashSet<>();
        list.addAll(Arrays.asList(MARKET));
        list.addAll(Arrays.asList(ACCOUNT));
        list.addAll(Arrays.asList(TRADE));
        list.addAll(Arrays.asList(ORDER));
        list.addAll(Arrays.asList(PUSH));
        ALL = list.toArray(new Action[]{});
    }


    public static Action[] include(Object... actions) {
        Set<Action> set = new HashSet<>();
        for (Object o : actions) {
            if (o instanceof Action) {
                set.add((Action) o);
            } else if ("Action[]".equals(o.getClass().getSimpleName())
                    && o.getClass().getComponentType() == Action.class) {
                set.addAll(Arrays.asList((Action[]) o));
            }
        }
        return set.toArray(new Action[]{});
    }

    public static Action[] exclude(Action[] exclude) {
        Set<Action> all = new HashSet<>(Arrays.asList(ALL));
        Stream.of(exclude).forEach(all::remove);
        return all.toArray(new Action[]{});
    }

    public static Action[] exclude(Object... actions) {
        Action[] exclude = CompleteList.include(actions);
        Set<Action> all = new HashSet<>(Arrays.asList(ALL));
        Stream.of(exclude).forEach(all::remove);
        return all.toArray(new Action[]{});
    }

}
