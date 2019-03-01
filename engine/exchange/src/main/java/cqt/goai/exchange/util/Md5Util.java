package cqt.goai.exchange.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author GOAi
 * @description
 */
public class Md5Util {

    private static final char[] HEX_DIGITS = { '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    public static String md5(String str) {
        try {
            if (str == null || str.trim().length() == 0) {
                return "";
            }
            byte[] bytes = str.getBytes();
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(bytes);
            bytes = messageDigest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(HEX_DIGITS[(b & 0xf0) >> 4]).append(HEX_DIGITS[b & 0xf]);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

//    public static String md5(String str) {
//        return md5(str).toLowerCase();
//    }
}
