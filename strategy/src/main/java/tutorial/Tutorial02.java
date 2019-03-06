package tutorial;

import cqt.goai.model.market.Ticker;
import cqt.goai.run.main.RunTask;

import static dive.common.util.Util.exist;

/**
 * 教程2：本教程你将了解 如何进行策略的配置。
 *        配置策略参数的两种方式：1、会优先在线获取配置：通过GOAi后台配置策略对应参数的值，并在策略中定义该变量名
 *        引擎启动后会自动把线上参数的值赋值给同名变量。
 *        2、如果线上配置获取不成功或本地调试的时候会从本地的resources目录下的 config.yml 中获取参数值并赋值给策略中
 *        定义好的变量。
 *        config.yml 配置信息中
 *        strategy_name：是该策略的名称 可以任意取。
 *        class_name：是你策略的全类名。例如该教程 class_name: 'tutorial.Tutorial02'
 *        configs: configs下定义好策略配置需要的参数。除 loop、e、telegramToken、telegramGroup
 *                  外的变量名须在策略中定义好同名变量引擎才会自动赋值。
 *                  例如：loop: 0.5  指定了loop 函数0.5秒执行一次
 *                  交易所的配置方式可以参照yml 文件。因okexv3交易所密钥多了一个字段 需在公钥后加 下划线指定。
 */

public class Tutorial02 extends RunTask {

    /**
     * 以下配置对应 config.yml 中的配置名 如需使用请先定义好 引擎会自动赋值
     */
    private String myString;

    @Override
    protected void init() {
        log.info("打印了config.yml 中配置的变量 myString：{}",myString);
        log.info("loop函数获取了交易所的Ticker 如网络无法连接 等待几秒后会有错误信息 connect timed out");
    }

    @Override
    protected void loop() {
        Ticker ticker = e.getTicker();
        log.info("exchange name --> {} ticker last: {}", e.getName(), exist(ticker) ? ticker.getLast() : null);
    }

}
