package cqt.goai.run.main;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.OptionHelper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.ExchangeException;
import cqt.goai.exchange.ExchangeUtil;
import cqt.goai.model.other.RunInfo;
import cqt.goai.model.other.RunState;
import cqt.goai.run.Application;
import cqt.goai.run.annotation.Scheduled;
import cqt.goai.run.annotation.ScheduledScope;
import dive.common.crypto.AESUtil;
import dive.common.crypto.DHUtil;
import dive.http.common.MimeRequest;
import dive.http.common.model.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static cqt.goai.run.main.Const.DEFAULT_SCHEDULED_METHOD;
import static cqt.goai.run.main.Const.LEFT_SQUARE_BRACKETS;
import static dive.common.util.Util.exist;
import static dive.common.util.Util.useful;

/**
 * 管理类
 *
 * @author GOAi

 */
@Slf4j
public class Minister {

    /**
     * 单例
     * Singleton Pattern 单例模式
     * 只需要一个对象管理所有的配置和运行实例即可
     */
    private static Minister instance;

    /**
     * 所有程序运行相关信息
     */
    private final Secretary secretary = new Secretary();

    /**
     * 所有配置信息
     * @author GOAi
     */
    private class Secretary {
        /**
         * 标识系统状态，初始正在启动
         */
        RunInfo runInfo = new RunInfo(System.currentTimeMillis(), RunState.STARTING);

        /**
         * 策略名称
         */
        String strategyName = "";

        /**
         * 运行类名
         */
        String className = null;

        /**
         * 是否以debug模式启动
         */
        Boolean debug = false;

        /**
         * 多个实例配置
         */
        Map<Integer, JSONObject> configs = new HashMap<>();


        /**
         * 运行类
         */
        Class<?> taskClass = null;

        /**
         * 运行类中需要定时任务的方法
         */
        List<MethodInfo> methods = new LinkedList<>();

        /**
         * 多个运行实例
         */
        ConcurrentHashMap<Integer, TaskManager> managers = new ConcurrentHashMap<>();

        /**
         * 是否初始化完毕
         */
        boolean ready = false;

    }

    private Minister() {} // 私有构造器

    /**
     * 获取单例
     */
    static Minister getInstance() {
        if (null == instance) {
            synchronized (Minister.class) {
                if (null == instance) {
                    instance = new Minister();
                }
            }
        }
        return instance;
    }

    boolean isDebug() {
        return this.secretary.debug;
    }

    /**
     * 运行入口
     * @param args 启动参数
     */
    public static void run(String[] args) {
        Minister.pid();
        Minister minister = getInstance();
        // 处理配置
        minister.config(args);
        try {
            minister.run(); // 运行
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());

            minister.secretary.runInfo.setRunState(RunState.ERROR);
            minister.secretary.runInfo.setEndTime(System.currentTimeMillis());
            minister.updateRunInfo(minister.secretary.runInfo);

            System.exit(-1);
        }
    }

    /**
     * 获取配置信息 2种获取配置的方式
     *  1. args中的url参数
     *      例: --url=http://localhost:7758/config?token=token
     *      若配置--url表明从node请求获取配置信息
     *  2. 指定配置文件
     *      例: --name=/data/my_config.yml --> 全路径
     *          --name=my_config.yml --> 相对路径 运行jar同目录下的my_config.yml文件
     *                                  若jar同目录下无该文件，则从resources获取
     *      默认文件名为config.yml
     * @param args 启动参数
     */
    private void config(String[] args) {
        this.updateRunInfo(this.secretary.runInfo);

        // 尝试从网络获取配置
        JSONObject config = this.getConfigByUrl(args);

        if (!exist(config)) {
            // 从本地获取配置
            config = this.getConfigByLocal(args);
        }

        if (null != config) {
            this.config(config);
        } else {
            String message = "config can not be null";
            log.error(message);
            throw new ExchangeException(message);
        }
    }

    /**
     * 尝试从网络获取配置
     */
    private JSONObject getConfigByUrl(String[] args) {
        // 若存在--url参数，从node处获取config
        for (String arg : args) {
            if (arg.startsWith(Const.START_URL)) {
                String url = arg.substring(6);
                log.info("config url --> {}", url);

                String[] keys = DHUtil.dhKeyToBase64();
                String pk = keys[0];
                log.info("response config pk --> {}", pk);
                String response = MimeRequest.builder()
                        .url(url)
                        .post()
                        .body(Parameter.build("pk", pk).json(JSON::toJSONString))
                        .execute(ExchangeUtil.OKHTTP);

                if (!exist(response)) {
                    log.info("response data empty");
                    return new JSONObject();
                }
                JSONObject json = JSONObject.parseObject(response);
                if (json.containsKey("run_mode") && "debug".equals(json.getString("run_mode"))) {
                    log.debug(response);
                }
                if(json.getInteger("code") != 200){
                    log.info("response data error ---> {}", response);
                    return new JSONObject();
                }

                String pubKey = Util.getParamByUrl(url, "key");
                pubKey = new String(Base64.getDecoder().decode(pubKey));
                String configs = AESUtil.aesDecryptByBase64(json.getString("data"),
                        DHUtil.aesKeyToBase64(pubKey, keys[1]));

                return JSON.parseObject(configs);
            }
        }
        return null;
    }

    /**
     * 从本地文件获取配置
     */
    private JSONObject getConfigByLocal(String[] args) {
        // 默认配置文件名
        String name = "config.yml";
        // 若存在--name参数，更新配置文件名
        for (String arg : args) {
            if (arg.startsWith("--name=")) {
                name = arg.substring(7);
                log.info("config name change to: {}", name);
                break;
            }
        }

        log.info("load config from local");

        // 查找同目录下config.yml文件
        File file = new File(name);
        if (file.isAbsolute()) {
            // 若是绝对路径直接用
            log.info("load file by --name: {}", name);
        } else {
            // 获取运行路径
            String path = Application.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();
            final String split = "/";
            if (!path.endsWith(split)) {
                path = path.substring(0, path.lastIndexOf(split)) + split;
            }
            if (new File(path + name).exists()) {
                log.info("load file {} : {}", name, path + name);
                // 全路径
                name = path + name;
                file = new File(name);
            } else {
                // 调用resource下config文件
                log.info("load file {} from resources", name);
            }
        }
        Map map;
        try {
            if (file.isAbsolute()) {
                map = new Yaml().load(new FileInputStream(file));
            } else {
                map = new Yaml().load(
                        Application.class.getClassLoader().getResourceAsStream(name));
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            String message = String.format("load file %s failed.", name);
            throw new RuntimeException(message);
        }
        return JSON.parseObject(JSON.toJSONString(map));
    }

    /**
     * 处理配置文件
     * @param config 配置文件
     */
    private void config(JSONObject config) {
        // 统一处理配置文件

        // 获取简单的配置
        this.configSimple(config);

        // 获取每个实例的运行配置
        this.configs(config);
    }

    /**
     * 获取并设置简单的配置
     */
    private void configSimple(JSONObject config) {
        String strategyName = config.getString(Const.CONFIG_STRATEGY_NAME);
        if (useful(strategyName)) {
            this.secretary.strategyName = strategyName;
            log.info("strategy name --> {}", strategyName);
        } else {
            throw new ExchangeException("config '" + Const.CONFIG_STRATEGY_NAME + "' can not be null");
        }

        String className = config.getString(Const.CONFIG_CLASS_NAME);
        if (useful(className)) {
            this.secretary.className = className;
            log.info("task class name --> {}", className);
        } else {
            throw new ExchangeException("config '" + Const.CONFIG_CLASS_NAME + "' can not be null");
        }

        if (config.containsKey(Const.CONFIG_RUN_MODE) &&
                "debug".equalsIgnoreCase(config.getString(Const.CONFIG_RUN_MODE))) {
            this.secretary.debug = true;
            log.info("config run mode --> {}", config.getString(Const.CONFIG_RUN_MODE));
        }

    }

    /**
     * 获取并配置每个配置实例
     */
    private void configs(JSONObject config) {
        String tempConfigs = config.getString(Const.CONFIG_CONFIGS);
        if (tempConfigs.startsWith(LEFT_SQUARE_BRACKETS)) {
            JSONArray configs = config.getJSONArray(Const.CONFIG_CONFIGS);
            if (null == configs) {
                throw new ExchangeException("configs in config can not be null");
            }
            for (int i = 0; i < configs.size(); i++) {
                JSONObject c = configs.getJSONObject(i);
                Integer id = c.getInteger("id");
                if (null == id) {
                    log.error("can not mapping config, there is no id: {}", c);
                    continue;
                }
                if (id < 1) {
                    log.error("can not mapping config, id can not less then 1", c);
                    continue;
                }
                this.secretary.configs.put(id, c);
            }
        } else {
            Integer id = 1;
            this.secretary.configs.put(id, config.getJSONObject(Const.CONFIG_CONFIGS));
        }
        log.info("config init configs size --> {}", this.secretary.configs.size());
    }


    /**
     * 运行程序
     */
    private void run() {
        // 获取任务类信息
        Class<?> taskClass;
        try {
            taskClass = Class.forName(this.secretary.className);
        } catch (Exception e) {
            throw new ExchangeException("can not find(load) class: " + this.secretary.className);
        }

        // 测试实例化对象和是否为RunTask子类
        try {
            Object test = taskClass.newInstance();
            if (!(test instanceof RunTask)) {
                String message = "task class must extends RunTask: " + this.secretary.className;
                throw new ExchangeException(message);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ExchangeException(
                    "class must has public non-parameter constructor: " + this.secretary.className);
        }

        this.secretary.taskClass = taskClass;

        // 检查定时任务
        this.checkSchedules(taskClass);
        this.checkSchedules(RunTask.class);
        this.checkLoop();

        // 启动任务
        this.secretary.configs.forEach(this::checkTask);

        log.info("start successful. instance size: {}", this.secretary.managers.size());

        this.updateRunInfo(this.secretary.runInfo.setRunState(RunState.STARTED));

        this.secretary.ready = true;

        ScheduledExecutorService running = new ScheduledThreadPoolExecutor(1,
                new ThreadPoolExecutor.DiscardPolicy());
        //这个定时任务，让程序不停止，否则，没有运行实例定时任务，程序就会结束
        running.scheduleAtFixedRate(this::checkManagers, 0, 1, TimeUnit.MINUTES);

        // 关闭程序回调
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> {
                    // 修改状态 -> 停止中
                    this.updateRunInfo(this.secretary.runInfo.setRunState(RunState.STOPPING));
                    // 调用正在运行任务的停止方法
                    this.secretary.managers.values()
                            .parallelStream().forEach(TaskManager::destroy);
                    // 修改状态 -> 已停止
                    this.updateRunInfo(this.secretary.runInfo.setRunState(RunState.STOPPED));
                    running.shutdown();
                }));
    }

    /**
     * 把定时任务检索出来
     */
    private void checkSchedules(Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method m : methods) {
            if (m.getName().equals(DEFAULT_SCHEDULED_METHOD)) {
                continue;
            }
            Scheduled[] schedules = m.getAnnotationsByType(Scheduled.class);
            if (exist(schedules) && 0 < schedules.length) {
                m.setAccessible(true);
                List<ScheduledInfo> sis = new ArrayList<>(schedules.length);
                for (Scheduled s : schedules) {
                    sis.add(new ScheduledInfo(s.cron(), s.fixedRate(), s.delay()));
                }
                if (!sis.isEmpty()) {
                    ScheduledScope scheduledScope = m.getAnnotation(ScheduledScope.class);
                    this.secretary.methods.add(new MethodInfo(m, null == scheduledScope ?
                            MethodScope.INSTANCE : scheduledScope.value(), sis));
                }
            }
        }
    }

    /**
     * 检查默认循环任务
     */
    private void checkLoop() {
        if (this.secretary.configs.isEmpty()) {
            return;
        }
        try {
            Method method = this.secretary.taskClass
                    .getDeclaredMethod(DEFAULT_SCHEDULED_METHOD);
            ScheduledScope scheduledScope = method.getAnnotation(ScheduledScope.class);
            method.setAccessible(true);
            this.secretary.methods.add(new MethodInfo(method, null == scheduledScope ?
                    MethodScope.INSTANCE : scheduledScope.value(),
                    Collections.singletonList(new ScheduledInfo(null, 1000, 1000))));
        } catch (NoSuchMethodException ignored) {
        }
    }

    /**
     * 启动一个实例
     * @param id 配置id
     * @param config 配置
     */
    private void checkTask(Integer id, JSONObject config) {
        ConcurrentHashMap<Integer, TaskManager> managers = this.secretary.managers;
        if (id < 0) {
            id = -id;
            // 停止指定任务
            if (managers.containsKey(id)) {
                managers.get(id).destroy();
                managers.remove(id);
            }
            return;
        }
        TaskManager manager = managers.get(id);
        if (exist(manager)) {
            // 如果已经有这个实例了，就关闭
            manager.destroy();
        }
        try {
            RunTask runTask = (RunTask) this.secretary.taskClass.newInstance();
            // 统一的配置从这里设置
            Logger log = this.initLog(id);
            runTask.init(this.secretary.strategyName, id, config, log);
            manager = new TaskManager(id, runTask, log,
                    this.secretary.methods.stream().map(MethodInfo::copy).collect(Collectors.toList()),
                    this.secretary.debug);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        if (null != manager) {
            managers.put(id, manager);
        } else {
            managers.remove(id);
        }
    }

    /**
     * 初始化日志
     */
    private Logger initLog(Integer id) {
        // 实例化log，每个实例存放地方不一致
        ch.qos.logback.classic.Logger logger = ((ch.qos.logback.classic.Logger)
                LoggerFactory.getLogger(this.secretary.strategyName + "-" + id));

        LoggerContext context = logger.getLoggerContext();

        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setFileNamePattern(OptionHelper.substVars(
                "logs/past/" + id + "/%d{yyyy-MM-dd}.log.gz", context));
        policy.setMaxHistory(31);
        policy.setContext(context);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern("%d{yyyy-MM-dd HH:mm:ss.SSS} %5level - [%thread] %logger : %msg%n");

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName(this.secretary.strategyName + "-" + id);
        appender.setFile(OptionHelper.substVars("logs/" + id + "/log.log", context));
        appender.setAppend(true);
        // 同一文件多输入完整检查
        appender.setPrudent(false);
        appender.setRollingPolicy(policy);
        appender.setEncoder(encoder);
        policy.setParent(appender);

        policy.start();
        encoder.start();
        appender.start();

        logger.setLevel(Level.INFO);
        // 终端输出
        logger.setAdditive(true);
        logger.addAppender(appender);

        return logger;
    }

    /**
     * 核对检查运行状态，未运行的要运行，停止的要关闭
     */
    private void checkManagers() {
        Set<Integer> need = this.secretary.configs.keySet();
        Set<Integer> running = this.secretary.configs.keySet();
        Set<Integer> start = new HashSet<>();
        Set<Integer> stop = new HashSet<>();
        for (Integer id : need) {
            if (!running.contains(id)) {
                start.add(id);
            }
        }
        for (Integer id : running) {
            if (!need.contains(id)) {
                stop.add(id);
            }
        }
        start.forEach(id -> this.checkTask(id, this.secretary.configs.get(id)));
        stop.forEach(id -> this.checkTask(-id, this.secretary.configs.get(id)));
    }

    /**
     * 获取TaskManager
     */
    Map<Integer, TaskManager> getManagers() {
        return this.secretary.managers;
    }

    boolean isReady() {
        return this.secretary.ready;
    }

    /**
     * 更新系统状态
     */
    private void updateRunInfo(RunInfo info) {
        if (info.getRunState() == RunState.STOPPED) {
            info.setEndTime(System.currentTimeMillis());
        }
        File file = new File(".run_info");
        if (Util.checkFile(file)) {
            Util.writeFile(file, JSON.toJSONString(info));
        }
    }

    /**
     * 是否正在停止
     * @return 是否正在停止
     */
    boolean isStopping() {
        return this.secretary.runInfo.getRunState() == RunState.STOPPING;
    }

    // ==================== tools ====================

    /**
     * 输出程序启动的pid
     */
    private static void pid() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        String name = runtime.getName();
        log.info("process mark: {}", name);
        int index = name.indexOf("@");
        if (index != -1) {
            int pid = Integer.parseInt(name.substring(0, index));
            getInstance().secretary.runInfo.setPid(pid);
            log.info("process id: {}", pid);
            File file = new File("PID");
            if (Util.checkFile(file)) {
                Util.writeFile(file, String.valueOf(pid));
            }
        }
    }

}
