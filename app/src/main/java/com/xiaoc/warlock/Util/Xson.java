package com.xiaoc.warlock.Util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Xson {

    private static final Gson gson = new Gson();
    private static final Map<String, InfoValue> dataMap = new HashMap<>();
    private Xson() {
        // 私有构造方法,防止实例化
    }

    /**
     * 将对象转换为JSON字符串
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }

    /**
     * 将JSON字符串转换为对象
     */
    public static <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }

    /**
     * 将JSON字符串转换为对象列表
     */
    public static <T> List<T> fromJsonList(String json, Class<T> classOfT) {
        Type type = TypeToken.getParameterized(List.class, classOfT).getType();
        return gson.fromJson(json, type);
    }

    /**
     * 将JSON字符串转换为Map
     */
    public static <K, V> Map<K, V> fromJsonMap(String json, Class<K> keyClass, Class<V> valueClass) {
        Type type = TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
        return gson.fromJson(json, type);
    }
    /**
     * 存储键值对
     * @param key 键
     * @param value 值
     */
    public static void put(String key, InfoValue value) {
        if (key == null) return;
        dataMap.put(key, value);
    }

    /**
     * 将存储的键值对转换为JSON字符串
     * @return JSON字符串
     */
    public static String getMapString(boolean isMatted) {
        if (isMatted){
            return getFormattedJson();
        }
        return gson.toJson(dataMap);
    }
    private static String getFormattedJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(dataMap);
    }
    /**
     * 清空存储的键值对
     */
    public static void clear() {
        dataMap.clear();
    }
}
