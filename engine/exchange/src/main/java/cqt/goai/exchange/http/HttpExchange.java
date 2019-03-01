package cqt.goai.exchange.http;

import cqt.goai.exchange.*;
import cqt.goai.exchange.util.CommonUtil;
import cqt.goai.model.enums.Period;
import cqt.goai.model.market.Klines;
import cqt.goai.model.market.Depth;
import cqt.goai.model.market.Ticker;
import cqt.goai.model.market.Trades;
import cqt.goai.model.trade.*;
import dive.http.common.MimeHttp;
import dive.http.common.MimeRequest;
import org.slf4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dive.common.util.Util.exist;

/**
 * Http请求，每个交易所的Http请求方式都要继承该类
 * Abstract Factory Pattern 抽象工厂模式
 * 任何一个交易所必须有这些接口，实现这些功能，提供这些Model，每个交易所实现方式不一样
 *
 * @author GOAi
 */
public class HttpExchange extends BaseExchange {

    /**
     * http请求工具
     */
    protected final MimeHttp http;

    private HttpExchange(ExchangeName name, MimeHttp http, Logger log) {
        super(name, log);
        this.http = http;
    }

    public HttpExchange(ExchangeName name, Logger log) {
        this(name, ExchangeUtil.OKHTTP, log);
    }

    // =============== post request ================

    protected void postRequest(ExchangeInfo info) { }

    // =============== change symbol ===============

    /**
     * 每个交易所使用的币对不一致，该方法针对需要的不同币对方式进行转换
     *
     * @param info 请求信息
     * @return 转换后的币对形式
     */
    public String symbol(ExchangeInfo info) {
        return info.getSymbol();
    }


    // =============== tick ===============

    /**
     * 获取Ticker
     *
     * @param info 请求信息
     * @return Ticker
     */
    public final Ticker getTicker(ExchangeInfo info) {
        return this.get(info, this::tickerRequests, this::parseTicker);
    }

    /**
     * 获取Ticker的Http请求信息，每个交易所不一样
     *
     * @param info  请求信息
     * @param delay 延迟，毫秒，若通过其他节点请求则延时一段时间，封装的加密信息不至于过期失效
     * @return Http请求信息
     */
    public List<MimeRequest> tickerRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    /**
     * 获取Ticker
     *
     * @param results 交易所返回的Ticker信息
     * @param info    请求信息
     * @return 处理后的Ticker
     */
    public final Ticker parseTicker(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformTicker, this.name, this.log);
    }

    /**
     * 解析交易所返回的Ticker信息，每个交易所不一样
     *
     * @param results 交易所返回的Ticker信息
     * @param info    请求信息
     * @return 处理后的Ticker
     */
    protected Ticker transformTicker(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformTicker is not supported");
    }

    // =============== klines ===============

    public final Klines getKlines(ExchangeInfo info) {
        return this.get(info, this::klinesRequests, this::parseKlines);
    }

    public List<MimeRequest> klinesRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final Klines parseKlines(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformKlines, this.name, this.log);
    }

    protected Klines transformKlines(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformKlines is not supported");
    }


    // =============== depth ===============

    public final Depth getDepth(ExchangeInfo info) {
        return this.get(info, this::depthRequests, this::parseDepth);
    }

    public List<MimeRequest> depthRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final Depth parseDepth(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformDepth, this.name, this.log);
    }

    protected Depth transformDepth(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformKlines is not supported");
    }


    // =============== trades ===============

    public final Trades getTrades(ExchangeInfo info) {
        return this.get(info, this::tradesRequests, this::parseTrades);
    }

    public List<MimeRequest> tradesRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final Trades parseTrades(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformTrades, this.name, this.log);
    }

    protected Trades transformTrades(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformTrades is not supported");
    }


    // =============== balances ===============

    public final Balances getBalances(ExchangeInfo info) {
        return this.get(info, this::balancesRequests, this::parseBalances);
    }

    public List<MimeRequest> balancesRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final Balances parseBalances(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformBalances, this.name, this.log);
    }

    protected Balances transformBalances(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformBalances is not supported");
    }

    // =============== account ===============

    public final Account getAccount(ExchangeInfo info) {
        return this.get(info, this::accountRequests, this::parseAccount);
    }

    public List<MimeRequest> accountRequests(ExchangeInfo info, long delay) {
        return this.balancesRequests(info, delay);
    }

    public final Account parseAccount(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformAccount, this.name, this.log);
    }

    protected Account transformAccount(List<String> results, ExchangeInfo info) {
        Balances balances = this.transformBalances(results, info);
        String[] symbols = CommonUtil.split(info.getSymbol());
        Balance base = new Balance(null, symbols[0], BigDecimal.ZERO, BigDecimal.ZERO);
        Balance count = new Balance(null, symbols[1], BigDecimal.ZERO, BigDecimal.ZERO);
        for (Balance b : balances) {
            if (b.getCurrency().equals(symbols[0])) {
                base = b;
            } else if (b.getCurrency().equals(symbols[1])) {
                count = b;
            }
        }
        return new Account(System.currentTimeMillis(), base, count);
    }

    // =============== precisions ===============

    public final Precisions getPrecisions(ExchangeInfo info) {
        return this.get(info, this::precisionsRequests, this::parsePrecisions);
    }

    public List<MimeRequest> precisionsRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final Precisions parsePrecisions(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformPrecisions, this.name, this.log);
    }

    protected Precisions transformPrecisions(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformPrecisions is not supported");
    }

    // =============== precision ===============

    public final Precision getPrecision(ExchangeInfo info) {
        return this.get(info, this::precisionRequests, this::parsePrecision);
    }

    public List<MimeRequest> precisionRequests(ExchangeInfo info, long delay) {
        List<MimeRequest> requests = this.precisionsRequests(info, delay);
        if (requests.isEmpty()) {
            return this.depthRequests(info, delay);
        }
        return requests;
    }

    public final Precision parsePrecision(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformPrecision, this.name, this.log);
    }

    protected Precision transformPrecision(List<String> results, ExchangeInfo info) {
        List<MimeRequest> requests = this.precisionsRequests(info, 0);
        if (requests.isEmpty()) {
            // 走转换深度路线
            Depth depth = this.transformDepth(results, info);
            return CommonUtil.parsePrecisionByDepth(depth, info.getSymbol());
        }
        Precisions precisions = this.transformPrecisions(results, info);
        return precisions.stream()
                .filter(p -> info.getSymbol().equals(p.getSymbol()))
                .findAny()
                .orElse(null);
    }

    // =============== buy limit ===============

    public final String buyLimit(ExchangeInfo info) {
        return this.get(info, this::buyLimitRequests, this::parseBuyLimit);
    }

    public List<MimeRequest> buyLimitRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final String parseBuyLimit(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformBuyLimit, this.name, this.log);
    }

    public String transformBuyLimit(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformBuyLimit is not supported");
    }


    // =============== sell limit ===============

    public final String sellLimit(ExchangeInfo info) {
        return this.get(info, this::sellLimitRequests, this::parseSellLimit);
    }

    public List<MimeRequest> sellLimitRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final String parseSellLimit(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformSellLimit, this.name, this.log);
    }

    public String transformSellLimit(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformSellLimit is not supported");
    }


    // =============== buy market ===============

    public final String buyMarket(ExchangeInfo info) {
        return this.get(info, this::buyMarketRequests, this::parseBuyMarket);
    }

    public List<MimeRequest> buyMarketRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final String parseBuyMarket(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformBuyMarket, this.name, this.log);
    }

    public String transformBuyMarket(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformBuyMarket is not supported");
    }

    // =============== sell market ===============

    public final String sellMarket(ExchangeInfo info) {
        return this.get(info, this::sellMarketRequests, this::parseSellMarket);
    }

    public List<MimeRequest> sellMarketRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final String parseSellMarket(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformSellMarket, this.name, this.log);
    }

    public String transformSellMarket(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformSellMarket is not supported");
    }

    // =============== multi buy market ===============

    public final List<String> multiBuy(ExchangeInfo info) {
        return this.get(info, this::multiBuyRequests, this::parseMultiBuy);
    }

    public List<MimeRequest> multiBuyRequests(ExchangeInfo info, long delay) {
        return Stream.iterate(0, i -> i + 1)
                .limit(info.getRows().size())
                .map(i -> this.buyLimitRequests(info.clone()
                        .setPrice(info.getRows().get(i).getPrice().toPlainString())
                        .setAmount(info.getRows().get(i).getAmount().toPlainString()), delay + i))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public final List<String> parseMultiBuy(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformMultiBuy, this.name, this.log);
    }

    public List<String> transformMultiBuy(List<String> results, ExchangeInfo info) {
        return Stream.iterate(0, i -> i + 1)
                .limit(results.size())
                .map(i -> this.transformBuyLimit(results.subList(i, i + 1),
                        info.clone().setPrice(info.getRows().get(i).getPrice().toPlainString())
                        .setAmount(info.getRows().get(i).getAmount().toPlainString())))
                .collect(Collectors.toList());
    }

    // =============== multi sell market ===============

    public final List<String> multiSell(ExchangeInfo info) {
        return this.get(info, this::multiSellRequests, this::parseMultiSell);
    }

    public List<MimeRequest> multiSellRequests(ExchangeInfo info, long delay) {
        return Stream.iterate(0, i -> i + 1)
                .limit(info.getRows().size())
                .map(i -> this.sellLimitRequests(info.clone()
                        .setPrice(info.getRows().get(i).getPrice().toPlainString())
                        .setAmount(info.getRows().get(i).getAmount().toPlainString()), delay + i))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public final List<String> parseMultiSell(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformMultiSell, this.name, this.log);
    }

    public List<String> transformMultiSell(List<String> results, ExchangeInfo info) {
        return Stream.iterate(0, i -> i + 1)
                .limit(results.size())
                .map(i -> this.transformSellLimit(results.subList(i, i + 1), info.clone()
                        .setPrice(info.getRows().get(i).getPrice().toPlainString())
                        .setAmount(info.getRows().get(i).getAmount().toPlainString())))
                .collect(Collectors.toList());
    }

    // =============== cancel order ===============

    public final Boolean cancelOrder(ExchangeInfo info) {
        return this.get(info, this::cancelOrderRequests, this::parseCancelOrder);
    }

    public List<MimeRequest> cancelOrderRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final Boolean parseCancelOrder(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformCancelOrder, this.name, this.log);
    }

    protected Boolean transformCancelOrder(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformCancelOrder is not supported");
    }

    // =============== cancel orders ===============

    public final List<String> cancelOrders(ExchangeInfo info) {
        return this.get(info, this::cancelOrdersRequests, this::parseCancelOrders);
    }

    public List<MimeRequest> cancelOrdersRequests(ExchangeInfo info, long delay) {
        return Stream.iterate(0, i -> i + 1)
                .limit(info.getCancelIds().size())
                .map(i -> this.cancelOrderRequests(info.clone().setCancelId(info.getCancelIds().get(i)),
                        delay + i))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public final List<String> parseCancelOrders(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformCancelOrders, this.name, this.log);
    }

    public List<String> transformCancelOrders(List<String> results, ExchangeInfo info) {
        return Stream.iterate(0, i -> i + 1)
                .limit(results.size())
                .filter(i -> {
                    Boolean result = this.transformCancelOrder(results.subList(i, i + 1), info.clone()
                            .setCancelId(info.getCancelIds().get(i)));
                    return null != result && result;
                })
                .map(i -> info.getCancelIds().get(i))
                .collect(Collectors.toList());
    }


    // =============== orders ===============

    public final Orders getOrders(ExchangeInfo info) {
        return this.get(info, this::ordersRequests, this::transformOrders);
    }

    public List<MimeRequest> ordersRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final Orders parseOrders(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformOrders, this.name, this.log);
    }

    protected Orders transformOrders(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformOrders is not supported");
    }


    // =============== historyOrders ===============

    public final Orders getHistoryOrders(ExchangeInfo info) {
        return this.get(info, this::historyOrdersRequests, this::parseHistoryOrders);
    }

    public List<MimeRequest> historyOrdersRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final Orders parseHistoryOrders(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformHistoryOrders, this.name, this.log);
    }

    protected Orders transformHistoryOrders(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformHistoryOrders is not supported");
    }

    // =============== order ===============

    public final Order getOrder(ExchangeInfo info) throws ExchangeException {
        return this.get(info, this::orderRequests, this::parseOrder);
    }

    public List<MimeRequest> orderRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final Order parseOrder(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformOrder, this.name, this.log);
    }

    protected Order transformOrder(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformOrder is not supported");
    }

    // =============== details ===============

    public final OrderDetails getOrderDetails(ExchangeInfo info) {
        return this.get(info, this::orderDetailsRequests, this::parseOrderDetails);
    }

    public List<MimeRequest> orderDetailsRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final OrderDetails parseOrderDetails(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformOrderDetails, this.name, this.log);
    }

    protected OrderDetails transformOrderDetails(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformOrderDetails is not supported");
    }

    public final OrderDetails getOrderDetailAll(ExchangeInfo info) {
        return this.get(info, this::orderDetailAllRequests, this::parseOrderDetailAll);
    }

    public List<MimeRequest> orderDetailAllRequests(ExchangeInfo info, long delay) {
        return Collections.emptyList();
    }

    public final OrderDetails parseOrderDetailAll(List<String> results, ExchangeInfo info) {
        return HttpExchange.analyze(results, info, this::transformOrderDetailAll, this.name, this.log);
    }

    public OrderDetails transformOrderDetailAll(List<String> results, ExchangeInfo info) {
        throw new ExchangeException("transformOrderDetailAll is not supported");
    }

    // =============== tools ===============

    /**
     * 统一转换币对方式，主要是报错
     *
     * @param info   请求信息
     * @param change 转换函数
     * @return 转换结果
     */
    protected String symbol(ExchangeInfo info, Function<String, String> change) {
        String symbol = info.getSymbol();
        Exception exception;
        try {
            return change.apply(symbol);
        } catch (Exception e) {
            this.log.error("{} {} exception is {}", super.name, info.tip(), e.getMessage());
            exception = e;
        }
        String message = String.format("%s %s can not identify symbol: %s",
                super.name, info.tip(), symbol);
        this.log.error(message);
        if (exist(exception)) {
            throw new ExchangeException(ExchangeError.SYMBOL, message, exception);
        }
        throw new ExchangeException(ExchangeError.SYMBOL, message);
    }

    /**
     * 执行Http请求信息
     *
     * @param requests Http请求信息
     * @return 执行结果
     */
    protected static List<String> results(List<MimeRequest> requests, MimeHttp http, Logger log) {
        if (null == requests) {
            return null;
        }
        return requests.stream()
//                .map(r -> r.execute(http))
                .map(r -> r.execute(http, (url, method, request, body, code, response, result) -> {
                    if (null != result) {
                        return;
                    }
                    log.info("url: " + url);
                    log.info("method: " + method);
                    log.info("requestHeader: " + request);
                    log.info("body: " + body);
                    log.info("code: " + code);
                    log.info("responseHeader: " + response);
                    //noinspection ConstantConditions
                    log.info("result: " + result);
                }))
                .collect(Collectors.toList());
    }

    /**
     * 统一请求
     *
     * @param info     请求信息
     * @param requests 生成Http请求信息
     * @param analyze  解析请求结果
     * @param <R>      请求类型
     * @return 请求结果
     */
    private <R> R get(ExchangeInfo info,
                      BiFunction<ExchangeInfo, Long, List<MimeRequest>> requests,
                      BiFunction<List<String>, ExchangeInfo, R> analyze) {
        List<MimeRequest> mimeRequests = requests.apply(info, 0L);
        List<String> results;
        try {
            results = HttpExchange.results(mimeRequests, this.http, this.log);
        } catch (Exception e){
            this.postRequest(info);
            throw e;
        }
        return analyze.apply(results, info);
    }

    /**
     * 解析请求结果
     *
     * @param results 请求结果
     * @param info    请求信息
     * @param analyze 解析函数
     * @param <R>     结果类型
     * @return 结果
     */
    static <R> R analyze(List<String> results, ExchangeInfo info,
                         BiFunction<List<String>, ExchangeInfo, R> analyze, ExchangeName name, Logger log) {
        Exception exception = null;
        try {
            R r = analyze.apply(results, info);
            if (exist(r)) {
                return r;
            }
        } catch (Exception e) {
            log.error("{} {} exception is {}", name.getName(), info.tip(), e);
            exception = e;
        }
        String message = String.format("%s %s failed. results is %s",
                name.getName(), info.tip(), "[" + String.join(",", results) + "]");
        log.error(message);
        if (exist(exception)) {
            throw new ExchangeException(message, exception);
        }
        throw new ExchangeException(message);
    }

    /**
     * K线周期转换
     *
     * @param info     请求信息
     * @param function 转换函数
     * @param <R>      结果类型
     * @return 结果
     */
    protected <R> R period(ExchangeInfo info, Function<Period, R> function) {
        R r = function.apply(info.getPeriod());
        if (exist(r)) {
            return r;
        }
        String message = String.format("%s %s period is nonsupport: %s",
                name, info.tip(), info.getPeriod());
        log.error(message);
        throw new ExchangeException(ExchangeError.PERIOD, message);
    }

    protected static String hmacSha256(String message, String secret) {
        try {
            final String hmacSHA256 = "HmacSHA256";
            Mac sha256hMac = Mac.getInstance(hmacSHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), hmacSHA256);
            sha256hMac.init(secretKeySpec);
            return new String(encodeHex(sha256hMac.doFinal(message.getBytes())));
        } catch (Exception e) {
            throw new RuntimeException("Unable to sign message.", e);
        }
    }

    private static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        int i = 0;

        for (int var5 = 0; i < l; ++i) {
            out[var5++] = DIGITS_LOWER[(240 & data[i]) >>> 4];
            out[var5++] = DIGITS_LOWER[15 & data[i]];
        }

        return out;
    }

    private static final char[] DIGITS_LOWER =
            new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};


}
