import cqt.goai.model.enums.Period;
import cqt.goai.model.market.Ticker;
import cqt.goai.run.exchange.Exchange;
import cqt.goai.run.main.RunTask;
import static dive.common.util.Util.exist;

/**
 * 演示类 演示HTTP调用的基础使用方法
 * @author GOAi
 */
public class Demo extends RunTask {

    /**
     * 以下配置对应 config.yml 中的配置名 如需使用请先定义好 引擎会自动赋值
     */
    private String myString;

    /**
     * 在这里可以完成策略初始化的工作
     */
    @Override
    protected void init() {
        log.info("myString --> {}",myString);
    }

    /**
     * 默认一秒执行一次该函数 可在配置表中配置loop字段 指定时间
     */
    @Override
    protected void loop() {
        Ticker ticker = e.getTicker(true);
        log.info("exchange name --> {} ticker last: {}", e.getName(), exist(ticker) ? ticker.getLast() : null);
    }

    /**
     * 程序关闭的时候调用
     */
    @Override
    protected void destroy() {
        log.info(id + " destroy");
    }
}
