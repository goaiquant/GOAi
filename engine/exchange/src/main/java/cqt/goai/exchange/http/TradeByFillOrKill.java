package cqt.goai.exchange.http;

import cqt.goai.exchange.ExchangeInfo;
import cqt.goai.exchange.ExchangeName;
import dive.http.common.MimeHttp;
import dive.http.common.MimeRequest;
import org.slf4j.Logger;

import java.util.List;

/**
 * fok交易
 *
 * @author GOAi
 */
public interface TradeByFillOrKill {

    /**
     * 获取请求工具
     * @return 请求工具
     */
    MimeHttp getHttp();

    /**
     * 获取日志输出
     * @return 日志
     */
    Logger getLog();

    /**
     * 获取交易所名称
     * @return 交易所名称
     */
    ExchangeName getExchangeName();

    /**
     * 下fok买单
     * @param access access
     * @param secret secret
     * @param symbol 币对
     * @param price 价格
     * @param amount 数量
     * @return 订单id
     */
    default String buyFillOrKill(String symbol, String access, String secret, String price, String amount) {
        List<MimeRequest> mimeRequests = this.buyFillOrKillRequests(symbol, access, secret, price, amount, 0);
        List<String> results = HttpExchange.results(mimeRequests, this.getHttp(), this.getLog());
        return this.parseBuyFillOrKill(results, symbol, access, secret, price, amount);
    }

    /**
     * 获取fok买单请求
     * @param symbol 币对
     * @param access access
     * @param secret secret
     * @param price 价格
     * @param amount 数量
     * @param delay 延时
     * @return 请求
     */
    List<MimeRequest> buyFillOrKillRequests(String symbol, String access, String secret, String price, String amount, long delay);

    /**
     * 解析fok订单
     * @param results 请结果
     * @param symbol 币对
     * @param access access
     * @param secret secret
     * @param price 价格
     * @param amount 数量
     * @return 订单id
     */
    default String parseBuyFillOrKill(List<String> results, String symbol, String access, String secret, String price, String amount) {
        return HttpExchange.analyze(results, ExchangeInfo.buyLimit(symbol, access, secret, price, amount), this::transformBuyFillOrKill, this.getExchangeName(), this.getLog());
    }

    /**
     * 解析订单id
     * @param results 请结果
     * @param info 请求信息
     * @return 订单id
     */
    String transformBuyFillOrKill(List<String> results, ExchangeInfo info);


    /**
     * 下fok卖单
     * @param access access
     * @param secret secret
     * @param symbol 币对
     * @param price 价格
     * @param amount 数量
     * @return 订单id
     */
    default String sellFillOrKill(String symbol, String access, String secret, String price, String amount) {
        List<MimeRequest> mimeRequests = this.sellFillOrKillRequests(symbol, access, secret, price, amount, 0);
        List<String> results = HttpExchange.results(mimeRequests, this.getHttp(), this.getLog());
        return this.parseSellFillOrKill(results, symbol, access, secret, price, amount);
    }

    /**
     * 获取fok卖单请求
     * @param symbol 币对
     * @param access access
     * @param secret secret
     * @param price 价格
     * @param amount 数量
     * @param delay 延时
     * @return 请求
     */
    List<MimeRequest> sellFillOrKillRequests(String symbol, String access, String secret, String price, String amount, long delay);

    /**
     * 解析订单id
     * @param results 请结果
     * @param symbol 币对
     * @param access access
     * @param secret secret
     * @param price 价格
     * @param amount 数量
     * @return 订单id
     */
    default String parseSellFillOrKill(List<String> results, String symbol, String access, String secret, String price, String amount) {
        return HttpExchange.analyze(results, ExchangeInfo.buyLimit(symbol, access, secret, price, amount), this::transformSellFillOrKill, this.getExchangeName(), this.getLog());
    }

    /**
     * 解析订单id
     * @param results 请结果
     * @param info 请求信息
     * @return 订单id
     */
    String transformSellFillOrKill(List<String> results, ExchangeInfo info);
}
