package cqt.goai.run.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static dive.common.util.Util.exist;


/**
 * 工具类
 * @author GOAi
 */
class Util {

    /**
     * 检查文件是否存在
     */
    static boolean checkFile(File file) {
        if (null == file) {
            return false;
        }
        if (file.isDirectory()) {
            return false;
        }
        if (!file.exists()) {
            try {
                boolean result = file.createNewFile();
                if (!result) {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /**
     * 写文件
     * @param file 文件
     * @param message 写的内容
     */
    static void writeFile(File file, String message) {
        try (FileOutputStream os = new FileOutputStream(file, false)){
            os.write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取url参数
     * @param url url
     */
    static Map<String, String> getParamsByUrl(String url){
        String params = url.substring(url.indexOf("?") + 1);
        if(!exist(params)){
            return null;
        }

        String[] paramsArr = params.split("&");
        Map<String, String> map = new HashMap<>();
        for(String param : paramsArr){
            String[] keyValue = param.split("=");
            map.put(keyValue[0], keyValue[1]);
        }

        return map;
    }

    /**
     * 获取url指定参数
     * @param url url
     * @param name 参数名
     */
    static String getParamByUrl(String url, String name){
        Map<String, String> map = getParamsByUrl(url);

        if(map.isEmpty() || !map.containsKey(name)){
            return null;
        }

        return map.get(name);
    }
}
