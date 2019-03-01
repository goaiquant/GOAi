package cqt.goai.run.exchange.model;

import cqt.goai.exchange.util.RateLimit;
import cqt.goai.model.market.Trade;
import cqt.goai.model.market.Trades;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author GOAi
 */
public class TradesObserver extends ModelObserver<Trades> {
    /**
     * 最多缓存个数
     */
    private int max;
    /**
     * 对于有频率限制的推送，不能就此丢弃Trades
     * 缓存起来，等到能推送的时候一起推
     */
    private ConcurrentLinkedDeque<Trade> trades;

    public TradesObserver(Consumer<Trades> consumer, RateLimit limit, String id) {
        this(consumer, limit, id, 200);
    }

    public TradesObserver(Consumer<Trades> consumer, RateLimit limit, String id, int max) {
        super(consumer, limit, id);
        if (limit.isLimit()) {
            trades = new ConcurrentLinkedDeque<>();
            this.max = max;
        }
    }

    @Override
    void on(Trades model) {
        if (super.limit.timeout()) {
            if (!this.trades.isEmpty()) {
                this.addFirst(model);
                model = new Trades(new ArrayList<>(this.trades));
            }
            this.trades.clear();
            super.consumer.accept(model);
        } else {
            this.addFirst(model);
        }
    }

    /**
     * 从前面加入
     */
    private void addFirst(Trades trades) {
        List<Trade> ts = trades.getList();
        for (int i = ts.size() - 1; 0 <= i; i--) {
            this.trades.addFirst(ts.get(i));
        }
        // 最多保存200个
        while (this.max < this.trades.size()) {
            this.trades.removeLast();
        }
    }

}
