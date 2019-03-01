package cqt.goai.run.main;

/**
 * 常量
 * @author GOAi
 */
class Const {

    /**
     * 获取配置的url
     */
    static final String START_URL = "--url=";

    /**
     * 配置文件名称，可以相对路径或绝对路径
     */
    static final String START_NAME = "--name=";


    /**
     * 策略名称
     */
    static final String CONFIG_STRATEGY_NAME = "strategy_name";
    /**
     * 启动类名，java的run需要
     */
    static final String CONFIG_CLASS_NAME = "class_name";
    /**
     * 每个实例的配置
     */
    static final String CONFIG_CONFIGS = "configs";
    /**
     * 运行模式 如果是debug，不使用ProxyExchange
     */
    static final String CONFIG_RUN_MODE = "run_mode";
//    static final String CONFIG_RUN_MODE_DEBUG = "DEBUG";

    /**
     * 默认的定时方法
     */
    static final String DEFAULT_SCHEDULED_METHOD = "loop";


}
