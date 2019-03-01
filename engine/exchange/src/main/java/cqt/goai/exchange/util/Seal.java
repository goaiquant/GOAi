package cqt.goai.exchange.util;


import static dive.common.util.Util.useful;

/**
 * 隐藏字符串
 * @author GOAi
 */
public class Seal {
    /**
     * 隐藏字符串
     */
    public static String seal(String secret) {
        if (!useful(secret)) {
            return "";
        }
        int length = secret.length();
        switch (length) {
            case 1: return "*";
            case 2: return "**";
            default:
        }
        int margin = 5;
        int div = length / 3;
        if (margin <= div) {
            return secret.substring(0, margin)
                    + getStar(length - margin * 2)
                    + secret.substring(length - margin);
        } else {
            return secret.substring(0, div)
                    + getStar(length - div * 2)
                    + secret.substring(length - div);
        }
    }

    /**
     * 获取指定长度的*
     */
    private static String getStar(int length) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length) {
            sb.append("*");
        }
        return sb.toString();
    }

}
