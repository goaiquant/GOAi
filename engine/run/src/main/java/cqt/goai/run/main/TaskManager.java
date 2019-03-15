package cqt.goai.run.main;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import cqt.goai.exchange.ExchangeException;
import cqt.goai.exchange.util.CommonUtil;
import cqt.goai.model.market.Klines;
import cqt.goai.model.market.Depth;
import cqt.goai.model.market.Ticker;
import cqt.goai.model.market.Trades;
import cqt.goai.model.trade.Account;
import cqt.goai.model.trade.Orders;
import cqt.goai.run.exchange.Exchange;
import cqt.goai.run.exchange.factory.BaseExchangeFactory;
import cqt.goai.run.exchange.factory.LocalExchangeFactory;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.slf4j.Logger;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import static dive.common.util.Util.exist;


/**
 * 单个实例管理类，主要是初始化和定时任务
 * @author GOAi
 */
class TaskManager {

    /**
     * 生成LocalExchange
     */
    private static final BaseExchangeFactory LOCAL_FACTORY = new LocalExchangeFactory();

    /**
     * 日志
     */
    Logger log;

    /**
     * 是否debug模式，不用proxy exchange代理，不忽略异常
     */
    private boolean debug;

    /**
     * 实例id
     */
    private Integer id;

    /**
     * 实例对象
     */
    private RunTask runTask;

    /**
     * 实例类
     */
    private Class<?> taskClass;

    /**
     * 配置
     */
    private JSONObject config;

    /**
     * 定时任务
     */
    private Scheduler scheduler;

    /**
     * 定时方法
     */
    private List<ScheduledMethod> methodTasks;

    /**
     * 交易所列表，删除时需要
     */
    private List<Exchange> exchangeList = new LinkedList<>();

    /**
     * 单个主动推送，name + type(type)
     */
    private ConcurrentHashMap<String, Exchange> pushSingle = new ConcurrentHashMap<>();

    /**
     * 多个主动推送, name + type(e, type)
     */
    private ConcurrentHashMap<String, List<Exchange>> pushCollections = new ConcurrentHashMap<>();

    /**
     * 构造器
     * @param id 实例id
     * @param runTask 实例
     * @param log 日志
     * @param methods 模板定时任务
     * @throws Exception 异常
     */
    TaskManager(Integer id, RunTask runTask, Logger log, List<MethodInfo> methods, boolean debug)
            throws Exception {
        this.log = log;
        this.debug = debug;

        this.id = id;
        this.runTask = runTask;
        this.taskClass = runTask.getClass();
        this.config = runTask.config;

        try {
            this.runTask.setGetExchange(this::getExchange);
            // 子类参数未知，只能反射注入，检测设置主动推送函数
            this.inject();
            this.pushSingle.forEach((filed, e) -> this.checkPush(e, filed, true));
            if (this.pushSingle.containsKey(E)) {
                this.checkPush(this.pushSingle.get(E), "", true);
            }
            this.pushCollections.forEach((filed, es) ->
                    es.forEach(e -> this.checkPush(e, filed, false)));

            // 检查定时任务并加入任务队列
            this.scheduler(methods);

            // 执行init
            this.runTask.init();
            log.info("init {} successful.", this.id);

            // 准备完成
            Field field = RunTask.class.getDeclaredField("ready");
            field.setAccessible(true);
            field.set(this.runTask, true);
            field.setAccessible(false);

            if (null != this.scheduler) {
                this.scheduler.start();
            }
        } catch (Exception e) {
            this.destroy();
            /*
             * Chain of Responsibility 责任链模式
             * 出错层层抛出直到被处理，异常的封装抛出是典型的责任链模式
             */
            throw e;
        }
    }

    /**
     * 简单类型的注入
     */
    private static LinkedHashMap<Function<Class<?>, Boolean>, Function<String, ?>> injectMap;
    static {
        injectMap = new LinkedHashMap<>();
        injectMap.put((t) -> t == Double.class, Double::valueOf);
        injectMap.put((t) -> "double".equals(t.getName()), Double::valueOf);
        injectMap.put((t) -> t == Integer.class, Integer::valueOf);
        injectMap.put((t) -> "int".equals(t.getName()), c -> new BigDecimal(c).intValue());
        injectMap.put((t) -> t == Long.class, Long::valueOf);
        injectMap.put((t) -> "long".equals(t.getName()), c -> new BigDecimal(c).longValue());
        injectMap.put((t) -> t == String.class, s -> s);
        injectMap.put((t) -> t == JSONObject.class, JSON::parseObject);
        injectMap.put((t) -> t == JSONArray.class, JSON::parseArray);
        injectMap.put((t) -> t == BigDecimal.class, BigDecimal::new);
        injectMap.put((t) -> t == Boolean.class, Boolean::valueOf);
        injectMap.put((t) -> "boolean".equals(t.getName()), Boolean::valueOf);
    }

    /**
     * 注入属性
     */
    private void inject() {
        if (!exist(this.config)) {
            return;
        }
        NEXT:
        for (String key : this.config.keySet()) {
            try {
                Field field = this.taskClass.getDeclaredField(key);
                String content = this.config.getString(key);

                boolean flag = field.isAccessible();
                field.setAccessible(true);
                content = TaskManager.trim(content);
                Class<?> type = field.getType();

                // 简单类型的注入
                for (Function<Class<?>, Boolean> condition : TaskManager.injectMap.keySet()) {
                    Function<String, ?> change = TaskManager.injectMap.get(condition);
                    if (condition.apply(type)) {
                        field.set(this.runTask, change.apply(content));
                        this.log.info("inject field {} : {}", key, content);
                        continue NEXT;
                    }
                }

                // 复杂类型的注入
                Object value = this.inject(type, field, content, key);
                if (null != value) {
                    field.set(this.runTask, value);
                    this.log.info("inject field {} : {}", key, value);
                } else {
                    throw new Exception(key + " can not recognize config: " + content);
                }
                field.setAccessible(flag);
            } catch (NoSuchFieldException ignored) {
            } catch (Exception e) { e.printStackTrace(); this.log.error(e.getMessage()); }
        }
        this.configE();
    }

    /**
     * 进行特殊注入
     * @param type 类型
     * @param field 属性
     * @param content 内容
     * @param key 属性名
     * @return 注入值
     * @throws Exception 异常
     */
    private Object inject(Class<?> type, Field field, String content, String key) throws Exception {
        Object value = null;
        if (type == Exchange.class) {
            Exchange exchange = this.getExchange(content);
            this.pushSingle.put(key, exchange);
            value = exchange;
        } else if (type.isEnum()) {
            Enum[] objects = (Enum[]) field.getType().getEnumConstants();
            for (Enum o : objects) {
                if (o.name().equals(content)) {
                    value = o;
                    break;
                }
            }
            if (null == value) {
                throw new Exception(key + " can not recognize config: " + content);
            }
        } else if (Arrays.asList(type.getInterfaces()).contains(Injectable.class)) {
            Injectable inject = this.getInjectable(type);
            inject.inject(content, this::getExchange);
            value = inject;
        }else if (type == List.class && field.getGenericType() instanceof ParameterizedType) {
            Class<?> genericClass = Class.forName(((ParameterizedType) field.getGenericType())
                    .getActualTypeArguments()[0].getTypeName());
            // 处理集合类型
            for (Function<Class<?>, Boolean> condition : TaskManager.injectMap.keySet()) {
                Function<String, ?> change = TaskManager.injectMap.get(condition);
                if (condition.apply(genericClass)) {
                    JSONArray array = JSON.parseArray(content);
                    List<Object> list = new ArrayList<>(array.size());
                    for (int i = 0; i < array.size(); i++) {
                        list.add(change.apply(array.getString(i)));
                    }
                    return list;
                }
            }
            if (genericClass == Exchange.class) {
                JSONArray array = JSON.parseArray(content);
                List<Exchange> list = new ArrayList<>(array.size());
                for (int i = 0; i < array.size(); i++) {
                    list.add(this.getExchange(array.getString(i)));
                }
                this.pushCollections.put(key, new ArrayList<>(list));
                value = list;
            } else if (Arrays.asList(genericClass.getInterfaces()).contains(Injectable.class)) {
                JSONArray array = JSON.parseArray(content);
                List<Injectable> list = new ArrayList<>(array.size());
                for (int i = 0; i < array.size(); i++) {
                    Injectable inject = this.getInjectable(genericClass);
                    inject.inject(array.getString(i), this::getExchange);
                    list.add(inject);
                }
                value = list;
            }
        }
        return value;
    }

    /**
     * 获取一个injectable实例
     * @param type 类型
     * @return 实例
     * @throws Exception 异常
     */
    private Injectable getInjectable(Class<?> type) throws Exception {
        Constructor c = type.getDeclaredConstructors()[0];
        boolean flag = c.isAccessible();
        c.setAccessible(true);
        Injectable inject;
        int length = c.getParameterCount();
        if (0 == length) {
            inject = (Injectable) c.newInstance();
        } else if (1 == length && c.getParameterTypes()[0] == this.taskClass) {
            inject = (Injectable) c.newInstance(this.runTask);
        } else {
            throw new RuntimeException("can not get instance: " + type.getName());
        }
        c.setAccessible(flag);
        return inject;
    }

    /**
     * 根据配置获取Exchange
     * @param content 配置
     * @return Exchange
     */
    private Exchange getExchange(String content) {
        JSONObject c = JSON.parseObject(content);
        if (!c.containsKey(NAME) || !c.containsKey(SYMBOL)) {
            String message = "can not load Exchange by config: " + content;
            this.log.error(message);
            throw new ExchangeException(message);
        }
        if (!c.containsKey(ACCESS)) {
            c.put(ACCESS, "");
        }
        if (!c.containsKey(SECRET)) {
            c.put(SECRET, "");
        }


        // FIXME 这里应该判断加载：
        //  1. 本地Exchange
        //  2. 指定NodeExchange（本地构造签名）
        //  3. 指定NodeExchange（Node构造签名）

        //  1. 本地Exchange
        // FIXME Test
        Exchange exchange = LOCAL_FACTORY.getExchange(!this.debug, this.log, c, this.runTask.isReady);

        // 保存，等销毁时要取消订阅的
        this.exchangeList.add(exchange);

        // FIXME 应该返回代理对象
        return exchange;
    }

    /**
     * 配置e
     */
    private void configE() {
        if (this.config.containsKey(E)) {
            try {
                Field field = null;
                try {
                    field = this.taskClass.getDeclaredField(E);
                } catch (NoSuchFieldException | SecurityException ignored) { }
                if (null != field) {
                    // 有设置e，无需通过父类设置
                    return;
                }
                field = RunTask.class.getDeclaredField(E);
                boolean flag = field.isAccessible();
                field.setAccessible(true);
                Exchange exchange = this.getExchange(TaskManager.trim(this.config.getString(E)));
                if (exist(exchange)) {
                    field.set(this.runTask, exchange);
                    this.pushSingle.put(E, exchange);
                    this.checkPush(exchange, "", false);
                    this.log.info("inject field e : {}", exchange);
                }
                field.setAccessible(flag);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 检查主动推送
     * @param exchange 推送交易所
     * @param prefix 前缀
     * @param single 是否单个推送
     */
    private void checkPush(Exchange exchange, String prefix, boolean single) {
        // FIXME 检查是否需要主动推送
        checkPush(exchange, exchange::setOnTicker, prefix, "onTicker", single, Ticker.class);
        checkPush(exchange, exchange::setOnKlines, prefix, "onKlines", single, Klines.class);
        checkPush(exchange, exchange::setOnDepth, prefix, "onDepth", single, Depth.class);
        checkPush(exchange, exchange::setOnTrades, prefix, "onTrades", single, Trades.class);
        checkPush(exchange, exchange::setOnAccount, prefix, "onAccount", single, Account.class);
        checkPush(exchange, exchange::setOnOrders, prefix, "onOrders", single, Orders.class);
    }

    /**
     * 检查是否需要主动推送
     * @param set 交易所推送设置方法
     * @param prefix 属性名
     * @param type 推送类型
     * @param exchange 被推送的交易所
     * @param parameterType 参数列表
     */
    private void checkPush(Exchange exchange, Consumer<Consumer> set, String prefix, String type,
                           boolean single, Class<?> parameterType) {
        try {
            String methodName;
            if ("".equals(prefix)) {
                methodName = type;
            } else {
                methodName = prefix + type.substring(0, 1).toUpperCase() + type.substring(1);
            }
            Method method;
            if (single) {
                method = this.taskClass.getDeclaredMethod(methodName, parameterType);
            } else {
                method = this.taskClass.getDeclaredMethod(methodName, Exchange.class, parameterType);
            }
            if (exist(method)) {
                method.setAccessible(true);
                this.log.info("config push: field: {} name:{} symbol:{} methodName: {}",
                        "".equals(prefix) ? E : prefix, exchange.getName(), exchange.getSymbol(), methodName);
                if (single) {
                    set.accept(ticker -> {
                        try {
                            method.invoke(this.runTask, ticker);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                            this.log.error(e.getMessage());
                        }
                    });
                } else {
                    set.accept(ticker -> {
                        try {
                            method.invoke(this.runTask, exchange, ticker);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            e.printStackTrace();
                            this.log.error(e.getMessage());
                        }
                    });
                }
            }
        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
        }
    }


    /**
     * 设置定时任务
     * @param methods 定时任务
     * @throws SchedulerException E
     */
    private void scheduler(List<MethodInfo> methods) throws SchedulerException {
        if (0 < methods.size()) {
            this.methodTasks = new LinkedList<>();
            for (MethodInfo mi : methods) {
                List<ScheduledInfo> schedules = mi.getSchedules();

                // 尝试从配置中找该方法的配置信息
                String methodName = mi.getMethod().getName();
                List<String> cs = new LinkedList<>();
                if (exist(this.config)) {
                    for (String key : this.config.keySet()) {
                        if (key.startsWith(methodName)) {
                            cs.add(key);
                        }
                    }
                }
                if (0 < cs.size()) {
                    // 该方法若有配置
                    schedules = new ArrayList<>(cs.size());
                    for (String key : cs) {
                        String value = this.config.getString(key);
                        if (value.contains("*")) {
                            schedules.add(new ScheduledInfo(value, 0, 0));
                        } else {
                            try {
                                long f = new BigDecimal(value)
                                        .multiply(CommonUtil.THOUSAND).longValue();
                                schedules.add(new ScheduledInfo(null, f, 0));
                            } catch (Exception e) {
                                this.log.error("can not format " + value + " to number.");
                            }
                        }
                    }
                }

                for (ScheduledInfo si : schedules) {
                    this.methodTasks.add(new ScheduledMethod(this.log, this.id, this.runTask,
                            mi.getMethod(), mi.getLock(), si.copy(), mi.getMethodScope()));
                }

            }

            List<ScheduleJob> list = new LinkedList<>();
            for (int i = 0; i < this.methodTasks.size(); i++) {
                ScheduledMethod mt = this.methodTasks.get(i);
                if (null != mt.getScheduledInfo().getTrigger()) {
                    list.add(this.checkJob(mt, i));
                }
            }
            this.log.info("{} tasks size: {}", id, list.size());

            Properties properties = new Properties();
            properties.put("org.quartz.scheduler.instanceName", this.taskClass.getSimpleName() + ":" + this.id);
            // 最大个数
            properties.put("org.quartz.threadPool.threadCount", String.valueOf((int)(list.size() * 1.5 + 0.5)));
            SchedulerFactory schedulerFactory = new StdSchedulerFactory(properties);
            this.scheduler = schedulerFactory.getScheduler();
            for (ScheduleJob sj : list) {
                this.scheduler.scheduleJob(sj.jobDetail, sj.trigger);
            }
        }
    }

    /**
     * 添加任务
     */
    private ScheduleJob checkJob(ScheduledMethod mt, int number) {
        Method method = mt.getMethod();
        Trigger trigger = mt.getScheduledInfo().getTrigger();
        String description = this.id + "-" + method.getName() + "-" + number;
        this.log.info("{} {}", description, mt.getScheduledInfo().getTip());
        JobDetail jobDetail = JobBuilder.newJob(ScheduledJob.class)
                .withDescription(description)
                .withIdentity(this.taskClass.getSimpleName(), description)
                .build();
        if (trigger instanceof CronTriggerImpl) {
            ((CronTriggerImpl) trigger).setName(description);
        }
        if (trigger instanceof SimpleTriggerImpl) {
            ((SimpleTriggerImpl) trigger).setName(description);
        }
        return new ScheduleJob(jobDetail, trigger);
    }

    /**
     * 定时任务
     */
    private class ScheduleJob {
        private JobDetail jobDetail;
        private Trigger trigger;

        ScheduleJob(JobDetail jobDetail, Trigger trigger) {
            this.jobDetail = jobDetail;
            this.trigger = trigger;
        }
    }


    /**
     * 清除所有任务，准备销毁
     */
    void destroy() {
        try {
            if (null != this.scheduler && !this.scheduler.isShutdown()) {
                this.scheduler.shutdown();
            }
            this.exchangeList.forEach(Exchange::destroy);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
        this.runTask.destroy();
        log.info("destroy {} successful.", this.id);
    }

    List<ScheduledMethod> getMethodTasks() {
        return this.methodTasks;
    }

    RunTask getRunTask() {
        return this.runTask;
    }

    // ===================== tools =====================

    private static final String E = "e";
    private static final String N = "\n";
    private static final String R = "\r";
    private static final String T = "\t";
    private static final String Z = "\0";
    private static final String NAME = "name";
    private static final String SYMBOL = "symbol";
    private static final String ACCESS = "access";
    private static final String SECRET = "secret";

    /**
     * 去除字符串首尾无效字符
     * @param v 字符串
     */
    private static String trim(String v) {
        v = v.trim();
        while (v.startsWith(N) || v.startsWith(R) || v.startsWith(T) || v.startsWith(Z)) {
            v = v.substring(1);
        }
        while (v.endsWith(N) || v.endsWith(R) || v.endsWith(T) || v.endsWith(Z)) {
            v = v.substring(1);
        }
        return v;
    }

}
