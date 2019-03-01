package cqt.goai.exchange.http;

import cqt.goai.exchange.ExchangeInfo;
import cqt.goai.exchange.ExchangeName;
import cqt.goai.model.trade.OrderDetails;
import dive.http.common.MimeHttp;
import dive.http.common.MimeRequest;
import org.slf4j.Logger;

import java.util.List;

/**
 * 获取历史订单明细
 *
 * @author GOAi
 */
public interface HistoryOrdersDetailsByTimestamp {

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
     * 获取历史订单明细
     * @param symbol 币对
     * @param access access
     * @param secret secret
     * @param start 开始时间
     * @param end 结束时间
     * @return 订单明细
     */
    default OrderDetails getHistoryOrdersDetails(String symbol, String access, String secret, Long start, Long end) {
        List<MimeRequest> mimeRequests = this.historyOrdersDetailsRequests(symbol, access, secret, start, end, 0);
        List<String> results = HttpExchange.results(mimeRequests, this.getHttp(), this.getLog());
        return this.parseHistoryOrdersDetails(results, symbol, access, secret);

    }

    /**
     * 获取历史订单明细请求
     * @param symbol 币对
     * @param access access
     * @param secret secret
     * @param start 开始时间戳
     * @param end 截止时间戳
     * @param delay 延时
     * @return 请求
     */
    List<MimeRequest> historyOrdersDetailsRequests(String symbol, String access, String secret,
                                            Long start, Long end, long delay);

    /**
     * 解析历史订单明细
     * @param results 请求结果
     * @param symbol 币对
     * @param access access
     * @param secret secret
     * @return 订单明细
     */
    default OrderDetails parseHistoryOrdersDetails(List<String> results, String symbol, String access, String secret) {
        return HttpExchange.analyze(results, ExchangeInfo.historyOrders(symbol, access, secret), this::transformHistoryOrdersDetails, this.getExchangeName(), this.getLog());
    }

    /**
     * 解析历史订单明细
     * @param results 请求结果
     * @param info 请求信息
     * @return 结果
     */
    OrderDetails transformHistoryOrdersDetails(List<String> results, ExchangeInfo info);

}
