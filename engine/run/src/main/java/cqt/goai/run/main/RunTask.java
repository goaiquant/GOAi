package cqt.goai.run.main;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.model.market.Klines;
import cqt.goai.model.market.Depth;
import cqt.goai.model.market.Ticker;
import cqt.goai.model.market.Trades;
import cqt.goai.model.trade.Account;
import cqt.goai.model.trade.Orders;
import cqt.goai.run.exchange.Exchange;
import dive.cache.mime.PersistCache;
import dive.common.util.DateUtil;
import dive.common.util.TryUtil;
import org.slf4j.Logger;

import java.io.*;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 抽象任务类
 *
 * @author GOAi
 */
public class RunTask {
    /**
     * 是否准备好推送，init方法结束后设置为true
     */
    private Boolean ready = false;

    /**
     * 准备好了才推送
     */
    final Supplier<Boolean> isReady = () -> this.ready;

    /**
     * 原始日志工具
     */
    private Logger originLog;

    /**
     * 用户日志
     */
    protected Logger log;

    /**
     * 通知对象
     */
    protected Notice notice;

    /**
     * 配置名称
     */
    protected String strategyName;

    /**
     * 本实例id
     */
    protected Integer id;

    /**
     * 本实例配置
     */
    protected JSONObject config;

    /**
     * 默认e
     */
    protected Exchange e;

    /**
     * 获取exchange 参数为json字符串
     *      {"name":"xxx","symbol":"BTC_USD","access":"xxx","secret":"xxx"}
     */
    protected Function<String, Exchange> getExchange;

    public RunTask() {}

    /**
     * 初始化设置
     * @param strategyName 配置名称
     * @param id 实例id
     * @param config 实例配置
     */
    void init(String strategyName, Integer id, JSONObject config, Logger log) {
        this.originLog = log;
        this.log = new UserLogger(log);
        this.notice = new Notice(log, config.getString("telegramGroup"), config.getString("telegramToken"));
        this.strategyName = strategyName;
        this.id = id;
        this.config = config;
    }

    /**
     * 初始化方法
     */
    protected void init() {}

    /**
     * 默认循环
     */
    protected void loop() {}

    /**
     * 退出方法
     */
    protected void destroy() {}


    /**
     * 重写该方法，可自动对e进行主动推送Ticker
     * @param ticker 主动推送的ticker
     */
    protected void onTicker(Ticker ticker) { }

    /**
     * 重写该方法，可自动对e进行主动推送Klines
     * K线是1分钟K线，100个
     * @param klines K线
     */
    protected void onKlines(Klines klines) { }

    /**
     * 重写该方法，可自动对e进行主动推送Depth
     * @param depth 盘口深度
     */
    protected void onDepth(Depth depth) { }

    /**
     * 重写该方法，可自动对e进行主动推送Trades
     * @param trades 交易信息
     */
    protected void onTrades(Trades trades) { }

    /**
     * 重写该方法，可自动对e进行主动推送币对余额
     * @param account 币对余额
     */
    protected void onAccount(Account account) { }

    /**
     * 重写该方法，可自动对e进行主动推送Orders
     * @param orders 订单信息
     */
    protected void onOrders(Orders orders) { }


    void setGetExchange(Function<String, Exchange> getExchange) {
        this.getExchange = getExchange;
    }

    /**
     * 睡眠一段时间
     * @param millis 毫秒
     */
    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 重试获取
     * @param supplier 获取函数
     * @param times 次数
     * @param millis 失败暂停时间
     * @param <T> 结果对象
     * @return 结果
     */
    protected <T> T retry(Supplier<T> supplier, int times, long millis) {
        return TryUtil.retry(supplier, times, () -> this.sleep(millis));
    }

    /**
     * 重试获取
     * @param supplier 获取函数
     * @param times 次数
     * @param <T> 结果对象
     * @return 结果
     */
    protected <T> T retry(Supplier<T> supplier, int times) {
        return retry(supplier, times, 3000);
    }

    /**
     * 重试获取
     * @param supplier 获取函数
     * @param <T> 结果对象
     * @return 结果
     */
    protected <T> T retry(Supplier<T> supplier) {
        return retry(supplier, 5, 1000);
    }


    // ============================

    /**
     * 全局持久化
     */
    private static final PersistCache<String, Serializable> PERSIST_CACHE = new PersistCache<>(".global");

    /**
     * 全局保存
     * @param key 键
     * @param value 值
     */
    protected void global(String key, Serializable value) {
        PERSIST_CACHE.set(key, value);
    }

    /**
     * 读取值
     * @param key 键
     * @param <T> 值类型
     * @return 读取的值
     */
    @SuppressWarnings("unchecked")
    protected <T> T global(String key) {
        return (T) PERSIST_CACHE.get(key);
    }

    /**
     * 持久化缓存
     */
    private PersistCache<String, Serializable> persistCache;

    /**
     * 初始化缓存
     */
    private void initPersistCache() {
        if (null == this.persistCache) {
            synchronized (this) {
                if (null == this.persistCache) {
                    this.persistCache = new PersistCache<>(".global_" + id);
                }
            }
        }
    }

    /**
     * 实例内保存
     * @param key 键
     * @param value 值
     */
    protected void store(String key, Serializable value) {
        this.initPersistCache();
        this.persistCache.set(key, value);
    }

    /**
     * 读取值
     * @param key 键
     * @param <T> 值类型
     * @return 读取的值
     */
    @SuppressWarnings("unchecked")
    protected <T> T store(String key) {
        this.initPersistCache();
        return (T) this.persistCache.get(key);
    }

    /**
     * 实时信息保存路径
     */
    private static final String PATH_REALTIME = ".realtime";
    static {
        File file = new File(PATH_REALTIME);
        if (!file.exists()) {
            boolean result = file.mkdir();
            if (!result) {
                throw new RuntimeException("can not create dir: " + PATH_REALTIME);
            }
        }
    }

    /**
     * 存储实时信息
     * @param key 键
     * @param value 值
     */
    protected void realtime(String key, Object value) {
        String message = JSON.toJSONString(value);
        File file = new File(PATH_REALTIME + "/" + key);
        if (!file.exists()) {
            try {
                boolean result = file.createNewFile();
                if (!result) {
                    throw new RuntimeException("can not create file: " + file.getPath());
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file, false);
            os.write(message.getBytes());
        } catch (IOException e1) {
            e1.printStackTrace();
        } finally {
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    /**
     * 输出盈利的时间序列数据
     * @param profit 盈利
     */
    protected void profit(double profit) {
        String date = DateUtil.formatISO8601(new Date());
        String p = String.format("% 8.8f", profit);
        String message = date + " " + p;
        this.originLog.info("PROFIT {}", message);
        File file = new File(",profit");
        try (FileWriter fw = new FileWriter(file, true)){
            fw.write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
