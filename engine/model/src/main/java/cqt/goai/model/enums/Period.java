package cqt.goai.model.enums;

/**
 * 周期，K线需要使用
 * @author GOAi
 */
public enum Period {
    // 1分钟
    MIN1    (60),
    // 3分钟
    MIN3    (60 * 3),
    // 5分钟
    MIN5    (60 * 5),
    // 15分钟
    MIN15   (60 * 15),
    // 30分钟
    MIN30   (60 * 30),
    // 1小时
    HOUR1   (60 * 60),
    // 2小时
    HOUR2   (60 * 60 * 2),
    // 3小时
    HOUR3   (60 * 60 * 3),
    // 4小时
    HOUR4   (60 * 60 * 4),
    // 6小时
    HOUR6   (60 * 60 * 6),
    // 12小时
    HOUR12  (60 * 60 * 12),
    // 1天
    DAY1    (60 * 60 * 24),
    // 3天
    DAY3    (60 * 60 * 24 * 3),
    // 1周
    WEEK1   (60 * 60 * 24 * 7),
    // 2周
    WEEK2   (60 * 60 * 24 * 14),
    // 1月
    MONTH1  (60 * 60 * 24 * 30);

    private int value;

    Period(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * 根据值解析周期
     * @param value 秒
     * @return 周期
     */
    public static Period parse(int value) {
        switch (value) {
            case 60:
                return MIN1;
            case 60 * 3:
                return MIN3;
            case 60 * 5:
                return MIN5;
            case 60 * 15:
                return MIN15;
            case 60 * 30:
                return MIN30;
            case 60 * 60:
                return HOUR1;
            case 60 * 60 * 2:
                return HOUR2;
            case 60 * 60 * 3:
                return HOUR3;
            case 60 * 60 * 4:
                return HOUR4;
            case 60 * 60 * 6:
                return HOUR6;
            case 60 * 60 * 12:
                return HOUR12;
            case 60 * 60 * 24:
                return DAY1;
            case 60 * 60 * 24 * 3:
                return DAY3;
            case 60 * 60 * 24 * 7:
                return WEEK1;
            case 60 * 60 * 24 * 14:
                return WEEK2;
            case 60 * 60 * 24 * 30:
                return MONTH1;
            default:
                return null;
        }
    }
}
