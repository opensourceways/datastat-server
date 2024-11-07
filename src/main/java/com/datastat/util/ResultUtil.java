package com.datastat.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ResultUtil {
    
    public static String resultJsonStr(int code, String item, Object data, String msg) {
        String updateAt = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date());
        return "{\"code\":" + code + ",\"data\":{\"" + item + "\":" + data + "},\"msg\":\"" + msg + "\",\"update_at\":\"" + updateAt + "\"}";
    }

    public static String resultJsonStr(int code, Object data, String msg) {
        String updateAt = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date());
        return "{\"code\":" + code + ",\"data\":" + data + ",\"msg\":\"" + msg + "\",\"update_at\":\"" + updateAt + "\"}";
    }

    public static String resultJsonStr(int code, Object data, String msg, Map<String, Object> map) {
        String updateAt = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")).format(new Date());
        HashMap<String, Object> resMap = new HashMap<>();
        resMap.put("code", code);
        resMap.put("data", data);
        resMap.put("msg", msg);
        resMap.put("update_at", updateAt);
        resMap.putAll(map);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.valueToTree(resMap).toString();
    }
}
