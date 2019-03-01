package cqt.goai.exchange.web.socket.okexv3;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.*;
import cqt.goai.exchange.util.CommonUtil;
import cqt.goai.exchange.util.OkhttpWebSocket;
import cqt.goai.exchange.util.RateLimit;
import cqt.goai.exchange.util.Seal;
import cqt.goai.exchange.util.okexv3.Okexv3Util;
import cqt.goai.exchange.web.socket.BaseWebSocketClient;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.*;
import cqt.goai.model.trade.Account;
import cqt.goai.model.trade.Balance;
import cqt.goai.model.trade.Orders;
import dive.common.crypto.Base64Util;
import dive.common.crypto.HmacUtil;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import static dive.common.util.Util.exist;

/**
 * OKExV3的websocket连接
 * @author GOAi
 */
public class Okexv3WebSocketClient extends BaseWebSocketClient {

    private static final String URL = "wss://real.okex.com:10442/ws/v3";

    /**
     * 推送回调
     */
    private OkhttpWebSocket client;

    /**
     * 是否需要登录
     */
    private final boolean doLogin;

    /**
     * 是否已经连接
     */
    private boolean connected = false;

    /**
     * 标记是否登录
     */
    private boolean login = false;

    /**
     * 标记是否主动关闭
     */
    private boolean dead = false;

    /**
     * 普通命令
     */
    private CopyOnWriteArrayList<String> commands = new CopyOnWriteArrayList<>();

    /**
     * 需要登录的命令
     */
    private CopyOnWriteArrayList<String> loginCommands = new CopyOnWriteArrayList<>();

    private ConcurrentHashMap<Period, ConcurrentLinkedDeque<Kline>> klines = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Period, Lock> klinesLocks = new ConcurrentHashMap<>();

    private ConcurrentHashMap<BigDecimal, Row> asks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigDecimal, Row> bids = new ConcurrentHashMap<>();
    private Lock depthLock = new ReentrantLock();
    private RateLimit depthLimit = RateLimit.second10();

    private Balance base;
    private Balance count;

    Okexv3WebSocketClient(String symbol, Logger log) {
        super(symbol, null, null, RateLimit.second13(),  log);
        this.client = new OkhttpWebSocket(URL, Okexv3Util::uncompress,
                this::open, this::transform, this::closed, this.log);
        this.doLogin = false;
    }

    Okexv3WebSocketClient(String symbol, String access, String secret, Logger log) {
        super(symbol, access, secret, RateLimit.second13(),  log);
        this.client = new OkhttpWebSocket(URL, Okexv3Util::uncompress,
                this::open, this::transform, this::closed, this.log);
        this.doLogin = true;
    }

    @Override
    public void open() {
        this.connected = true;
        if (this.doLogin) {
            this.login();
        }
        this.commands.forEach(this::send);
    }

    /**
     * 登录
     */
    private void login() {
        /*
         * {"op":"login","args":["985d5b66-57ce-40fb-b714-afc0b9787083","xxx","1538054050.975",
         * "xxx"]}
         */
        String access = super.access;
        String[] split = CommonUtil.split(super.secret);
        String secret = split[0];
        String passphrase = split[1];
        long timestamp = System.currentTimeMillis() / 1000;
        String sign = null;
        try {
            sign = Base64Util.base64EncodeToString(HmacUtil.hmac((timestamp + "GET/users/self/verify").getBytes(),
                    secret, HmacUtil.HMAC_SHA256));
        } catch (NoSuchAlgorithmException | InvalidKeyException ignored) { }
        super.commandLog(String.format("{\"op\":\"login\",\"args\":[\"%s\",\"%s\",\"%s\",\"%s\"]}",
                Seal.seal(access), Seal.seal(passphrase), timestamp, sign),
                Okexv3WebSocketClient.URL);
        String message = String.format("{\"op\":\"login\",\"args\":[\"%s\",\"%s\",\"%s\",\"%s\"]}",
                access, passphrase, timestamp, sign);
        this.client.send(message);
    }

    @Override
    public void closed() {
        this.connected = false;
        if (!this.dead) {
            this.client.connect();
        }
    }

    @Override
    protected void send(String message) {
        super.commandLog(message, Okexv3WebSocketClient.URL);
        this.client.send(message);
    }

    @Override
    public void ping() {
        // okhttp 30s 未收消息会关闭连接
        // 所以定时发个命令，也算是接收消息了
        if (this.limit.timeout(true)) {
            this.client.send(ALIVE);
        }
    }

    @Override
    protected void transform(String message) {
        JSONObject r = JSON.parseObject(message);
        if (r.containsKey(EVENT)) {
            switch (r.getString(EVENT)) {
                case "subscribe":
                    this.log.info("{} subscribe success: {}", super.symbol, message);
                    return;
                case "unsubscribe":
                    this.log.info("{} unsubscribe success: {}", super.symbol, message);
                    return;
                case "login":
                    if (r.containsKey(SUCCESS) && r.getBoolean(SUCCESS)) {
                        this.log.info("{} {} login success: {}", Seal.seal(super.access), super.symbol, message);
                        Account account = ExchangeManager.getHttpExchange(ExchangeName.OKEXV3, this.log)
                                .getAccount(ExchangeInfo.ticker(super.symbol, super.access, super.secret));
                        if (null != account) {
                            this.base = account.getBase();
                            this.count = account.getQuote();
                        }
                        this.login = true;
                        this.loginCommands.forEach(this::send);
                    }
                    return;
                case "error":
                    if (r.containsKey(ERROR_CODE)) {
                        switch (r.getInteger(ERROR_CODE)) {
                            case ERROR_CODE_LOGGED_OUT:
                                this.login = false;
                                this.login();
                                break;
                            case ERROR_CODE_LOGGED_IN:
                                this.login = true;
                                return;
                            case ERROR_CODE_ERROR_COMMAND:
                                if (r.getString(MESSAGE).endsWith(ALIVE)) {
                                    this.limit.update();
                                    return;
                                }
                                default:
                        }
                    }
                default:
            }
        }
        if (r.containsKey(TABLE)) {
            String table = r.getString(TABLE);
            switch (table) {
                case "spot/ticker":
                    // 解析Ticker
                    this.transformTicker(r);
                    return;
                case "spot/depth":
                    // 解析Depth
                    if (this.transformDepth(r)) {
                        return;
                    }
                    super.limit.update();
                    break;
                case "spot/trade":
                    // 解析Trades
                    this.transformTrades(r);
                    return;
                case "spot/account":
                    // 解析account
                    log.info("account: {}", message);
                    this.transformAccount(r);
                    return;
                case "spot/order":
                    // 解析Trades
                    this.transformOrders(r);
                    return;
                default:
            }
            if (table.startsWith(KLINE_START)) {
                this.transformKlines(table, r);
                return;
            }
        }
        this.log.error("can not transform: {}", message);
    }

    private void transformOrders(JSONObject r) {
        Orders orders = Okexv3Util.parseOrders(r.getJSONArray("data"));
        if (exist(orders)) {
            super.onOrders(orders);
        }
        super.limit.update();
    }

    private void transformAccount(JSONObject r) {
        Balance balance = Okexv3Util.parseBalance(r.getJSONArray("data").getString(0),
                r.getJSONArray("data").getJSONObject(0));
        if (exist(balance)) {
            boolean update = false;
            if (super.symbol.startsWith(balance.getCurrency())) {
                this.base = balance;
                update = true;
            } else if (super.symbol.endsWith(balance.getCurrency())) {
                this.count = balance;
                update = true;
            }
            if (update) {
                super.onAccount(new Account(System.currentTimeMillis(), this.base, this.count));
            }
        }
        super.limit.update();
    }

    private void transformTrades(JSONObject r) {
        Trades trades = Okexv3Util.parseTrades(r.getJSONArray("data"));
        if (exist(trades)) {
            super.onTrades(trades);
        }
        super.limit.update();
    }

    private boolean transformDepth(JSONObject r) {
        String action = r.getString("action");
        if (PARTIAL.equals(action) || UPDATE.equals(action)) {
            try {
                if (this.depthLock.tryLock(TRY_TIME, TimeUnit.MILLISECONDS)) {
                    try {
                        Depth depth = null;
                        r = r.getJSONArray("data").getJSONObject(0);
                        if (PARTIAL.equals(action)) {
                            // 全更新
                            depth = Okexv3Util.parseDepth(r);
                            this.depthLimit.update();
                            this.updateLocalRows(depth.getAsks().getList(), depth.getBids().getList());
                        } else {
                            // 部分更新
                            Long time = r.getDate("timestamp").getTime();
                            // 把更新的替换
                            List<Row> asks = CommonUtil.parseRowsByIndex(r.getJSONArray("asks"));
                            List<Row> bids = CommonUtil.parseRowsByIndex(r.getJSONArray("bids"));
                            asks.forEach(row -> this.asks.put(row.getPrice(), row));
                            bids.forEach(row -> this.bids.put(row.getPrice(), row));
                            // 排序过滤
                            asks = this.asks.values().stream()
                                    .filter(row -> 0 < row.getAmount().compareTo(BigDecimal.ZERO))
                                    .sorted(Comparator.comparing(Row::getPrice))
                                    .limit(MAX)
                                    .collect(Collectors.toList());
                            bids = this.bids.values().stream()
                                    .filter(row -> 0 < row.getAmount().compareTo(BigDecimal.ZERO))
                                    .sorted((r1, r2) -> r2.getPrice().compareTo(r1.getPrice()))
                                    .limit(MAX)
                                    .collect(Collectors.toList());
                            boolean check = this.depthCheckSum(asks, bids, r);
                            if (check) {
                                depth = new Depth(time, new Rows(asks), new Rows(bids));
                            }
                            this.updateLocalRows(asks, bids);
                        }
                        if (exist(depth)) {
                            super.onDepth(depth);
                        }
                        super.limit.update();
                        return true;
                    } finally {
                        this.depthLock.unlock();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean depthCheckSum(List<Row> asks, List<Row> bids, JSONObject r) {
        // 检查
        // 校验个数
        int size = 25;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            if (i < bids.size()) {
                // COLON ':'
                sb.append(bids.get(i).getPrice().toPlainString())
                        .append(COLON)
                        .append(bids.get(i).getAmount().toPlainString())
                        .append(COLON);
            }
            if (i < asks.size()) {
                // COLON ':'
                sb.append(asks.get(i).getPrice().toPlainString())
                        .append(COLON)
                        .append(asks.get(i).getAmount().toPlainString())
                        .append(COLON);
            }
        }
        if (0 < sb.length()
                && COLON == sb.charAt(sb.length() - 1)
                && r.containsKey(CHECK_SUM)) {
            sb.deleteCharAt(sb.length() - 1);
            long check = Okexv3WebSocketClient.crc32(sb.toString().getBytes());
            Long checkSum = r.getLong(CHECK_SUM);
            if (check != checkSum) {
                // 校验不通过
                if (this.depthLimit.timeout(true)) {
                    // 如果10s都不是完全正确的，则强制注册推送完整版
                    this.send("subscribe", "spot/depth", false);
                    return false;
                }
            }
        }
        this.depthLimit.update();
        return true;
    }

    private static long crc32(byte[] data) {
        CRC32 crc32 = new CRC32();
        crc32.update(data);
        return crc32.getValue();
    }

    private void updateLocalRows(List<Row> asks, List<Row> bids) {
        this.asks.clear();
        asks.forEach(row -> this.asks.put(row.getPrice(), row));
        this.bids.clear();
        bids.forEach(row -> this.bids.put(row.getPrice(), row));
    }

    private void transformKlines(String table, JSONObject r) {
        String type = table.substring(11);
        type = type.substring(0, type.length() - 1);
        Period period = Period.parse(Integer.valueOf(type));
        String result = r.getJSONArray("data").getJSONObject(0).getString("candle");
        Kline kline = Okexv3Util.parseKline(result,
                r.getJSONArray("data").getJSONObject(0).getJSONArray("candle"));
        if (exist(kline)) {
            Klines klines = this.onKline(kline, period);
            if (null != klines) {
                super.onKlines(klines, period);
            }
        }
        super.limit.update();
    }

    private void transformTicker(JSONObject r) {
        String result = r.getJSONArray("data").getString(0);
        Ticker ticker = Okexv3Util.parseTicker(result,
                r.getJSONArray("data").getJSONObject(0));
        if (exist(ticker)) {
            super.onTicker(ticker);
        }
        super.limit.update();
    }

    /**
     * 尝试时间
     */
    private static final long TRY_TIME = 3000;
    /**
     * k线保留最大长度
     */
    private static final int MAX = 200;

    /**
     * 构造Klines
     * @param kline 最新的k线
     * @param period 周期
     * @return 100根k线
     */
    private Klines onKline(Kline kline, Period period) {
        Lock lock = this.klinesLocks.computeIfAbsent(period, p -> new ReentrantLock());
        try {
            if (lock.tryLock(TRY_TIME, TimeUnit.MILLISECONDS)) {
                try {
                    ConcurrentLinkedDeque<Kline> klines =
                            this.klines.computeIfAbsent(period, p -> new ConcurrentLinkedDeque<>());
                    if (klines.isEmpty()) {
                        // 如果没有k线，则通过http方式获取
                        try {
                            Klines cs = ExchangeManager.getHttpExchange(ExchangeName.OKEXV3, this.log)
                                    .getKlines(ExchangeInfo.klines(super.symbol, "", "", period));
                            klines.addAll(cs.getList());
                        } catch (Exception e) {
                            e.printStackTrace();
                            this.log.error(e.getMessage());
                        }
                    }
                    if (klines.isEmpty()) {
                        klines.add(kline);
                    } else {
                        // 取出第1根，比较时间，相同则替换，不相同则插入，然后判断长度，
                        Kline c = klines.peek();
                        if (c.getTime().equals(kline.getTime())) {
                            // 相同
                            klines.poll();
                            klines.addFirst(kline);
                        } else {
                            klines.addFirst(kline);
                            while (MAX < klines.size()) {
                                klines.pollLast();
                            }
                        }
                    }
                    return new Klines(new ArrayList<>(klines));
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            this.log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void close(int code, String reason) {
        this.dead = true;
        this.client.close(code, reason);
    }

    /**
     * 统一发送信息
     * @param command 命令 subscribe unsubscribe
     * @param channel 订阅类型 spot/ticker spot/candle60s 等
     */
    private void send(String command, String channel, boolean record) {
        String symbol = super.symbol.replace("_", "-");
        String message = String.format("{\"op\":\"%s\",\"args\":[\"%s:%s\"]}", command, channel, symbol);
        super.commandLog(message, Okexv3WebSocketClient.URL);
        if (record) {
            this.commands.add(message);
        }
        if (this.connected) {
            this.send(message);
        }
    }

    @Override
    protected void askTicker() {
        this.send("subscribe", "spot/ticker", true);
    }

    @Override
    public void noTicker() {
        this.send("unsubscribe", "spot/ticker", true);
    }

    @Override
    protected void askKlines(Period period) {
        String channel = Okexv3WebSocketClient.getChannel(period);
        this.send("subscribe", channel, true);
    }

    /**
     * 获取K线channel
     * @param period 周期
     * @return channel
     */
    private static String getChannel(Period period) {
        /*
         * swap/candle60s // 1分钟k线数据频道
         * swap/candle180s // 3分钟k线数据频道
         * swap/candle300s // 5分钟k线数据频道
         * swap/candle900s // 15分钟k线数据频道
         * swap/candle1800s // 30分钟k线数据频道
         * swap/candle3600s // 1小时k线数据频道
         * swap/candle7200s // 2小时k线数据频道
         * swap/candle14400s // 4小时k线数据频道
         * swap/candle21600 // 6小时k线数据频道
         * swap/candle43200s // 12小时k线数据频道
         * swap/candle86400s // 1day k线数据频道
         * swap/candle604800s // 1week k线数据频道
         */
        Integer granularity = Okexv3Util.getPeriod(period);
        if (null == granularity) {
            throw new ExchangeException(ExchangeError.PERIOD,
                    "OKEx is not supported for period: " + period.name());
        }
        return  "spot/candle" + granularity + "s";
    }

    @Override
    public void noKlines(Period period) {
        String channel = Okexv3WebSocketClient.getChannel(period);
        this.send("unsubscribe", channel, true);
    }

    @Override
    protected void askDepth() {
        this.send("subscribe", "spot/depth", true);
    }

    @Override
    public void noDepth() {
        this.send("unsubscribe", "spot/depth", true);
    }

    @Override
    protected void askTrades() {
        this.send("subscribe", "spot/trade", true);
    }

    @Override
    public void noTrades() {
        this.send("unsubscribe", "spot/trade", true);
    }

    @Override
    protected void askAccount() {
//        this.askTicker();
        String[] split = CommonUtil.split(super.symbol);
        String message1 = "{\"op\": \"subscribe\", \"args\": [\"spot/account:" + split[0] + "\"]}";
        String message2 = "{\"op\": \"subscribe\", \"args\": [\"spot/account:" + split[1] + "\"]}";
        this.loginCommand(message1);
        this.loginCommand(message2);
    }

    private void loginCommand(String command) {
        this.loginCommands.add(command);
        if (this.login) {
            super.commandLog(command, Okexv3WebSocketClient.URL);
            this.send(command);
        }
    }

    @Override
    public void noAccount() {
        String[] split = CommonUtil.split(super.symbol);
        String message1 = "{\"op\": \"unsubscribe\", \"args\": [\"spot/account:" + split[0] + "\"]}";
        String message2 = "{\"op\": \"unsubscribe\", \"args\": [\"spot/account:" + split[1] + "\"]}";
        this.loginCommand(message1);
        this.loginCommand(message2);
    }

    @Override
    protected void askOrders() {
        String message = "{\"op\":\"subscribe\",\"args\":[\"spot/order:"
                + super.symbol.replace("_", "-") + "\"]}";
        this.loginCommands.add(message);
        this.send(message);
    }

    @Override
    public void noOrders() {
        String message = "{\"op\":\"unsubscribe\",\"args\":[\"spot/order:"
                + super.symbol.replace("_", "-") + "\"]}";
        this.loginCommands.add(message);
        this.send(message);
    }

    // ================= tools =================

    private static final String EVENT = "event";
    private static final String TABLE = "table";
    private static final String MESSAGE = "message";
    private static final String ALIVE = "alive";
    private static final String KLINE_START = "spot/candle";
    private static final String PARTIAL = "partial";
    private static final String UPDATE = "update";
    private static final char COLON = ':';
    private static final String CHECK_SUM = "checksum";
    private static final String SUCCESS = "success";
    private static final String ERROR_CODE = "errorCode";
    private static final int ERROR_CODE_LOGGED_OUT = 30041;
    private static final int ERROR_CODE_LOGGED_IN = 30042;
    private static final int ERROR_CODE_ERROR_COMMAND = 30039;


}
