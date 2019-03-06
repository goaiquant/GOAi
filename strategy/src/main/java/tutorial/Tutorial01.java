package tutorial;

import cqt.goai.run.main.RunTask;

/**
 * 教程1：   本教程你将了解 1、你的策略类需继承的父类。2、策略基础的三个方法
 *           1、GOAi引擎会封装好 运行策略的相关工作。你只需新建好你的策略类 并继承RunRask即可。
 *           2、有三个基础函数可能需要重写 init()、loop()、destroy() 。
 *           init()顾名思义负责做初始化的工作。如果你的策略没有需要做的准备工作则无需重写该函数。
 *           loop()你的策略逻辑在该函数中实现。函数默认1秒引擎会调用一次，如需更改请在配置中指定loop的值。
 *           destroy() 策略关闭前会调用一次，如你的策略无需做收尾工作则不用重写该函数。
 */

public class Tutorial01 extends RunTask {

    /**
     * 在这里可以完成策略初始化的工作
     */
    @Override
    protected void init() {
        log.info("Hello GOAi! 在这里可以完成你的策略的准备工作。");
    }

    /**
     * 默认一秒执行一次该函数 可在配置表中配置loop字段 指定时间 例如0.5 为每半秒执行一次。
     */
    @Override
    protected void loop() {
        log.info("该函数固定时间会调用一次。在这里写你的策略逻辑吧 就是这么简单！");
    }

    /**
     * 程序关闭的时候调用
     */
    @Override
    protected void destroy() {
        log.info("在这里处理你策略关闭的时候需要做的收尾工作，仅执行一次。");
    }
}
