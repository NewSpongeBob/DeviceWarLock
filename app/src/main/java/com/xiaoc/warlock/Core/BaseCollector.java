package com.xiaoc.warlock.Core;

import android.content.Context;

import com.xiaoc.warlock.Util.InfoValue;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.Xson;

import java.util.List;
import java.util.Map;

public abstract class BaseCollector  implements CollectCallback {
    protected Context context;

    public BaseCollector(Context context) {
        this.context = context;
    }

    public abstract void collect();

    /**
     * 存储字符串信息
     */
    protected void putInfo(String key, String value) {
        try {
            Xson.put(key, new InfoValue(0, value));
        } catch (Exception e) {
            Xson.put(key, InfoValue.fail());
            XLog.e("BaseCollector", "Failed to collect " + key + ": " + e.getMessage());
        }
    }
    /**
     * 存储布尔值信息
     */
    protected void putInfo(String key, Boolean value) {
        try {
            Xson.put(key, new InfoValue(0, value));
        } catch (Exception e) {
            Xson.put(key, InfoValue.fail());
            XLog.e("BaseCollector", "Failed to collect " + key + ": " + e.getMessage());
        }
    }
    /**
     * 存储长整型的信息
     */
    protected void putInfo(String key, long value) {
        try {
            Xson.put(key, new InfoValue(0, String.valueOf(value)));
        } catch (Exception e) {
            Xson.put(key, InfoValue.fail());
            XLog.e("BaseCollector", "Failed to collect " + key + ": " + e.getMessage());
        }
    }
    /**
     * 存储列表信息
     */
    protected void putInfo(String key, List<?> value) {
        try {
            Xson.put(key, new InfoValue(0, value));
        } catch (Exception e) {
            Xson.put(key, InfoValue.fail());
            XLog.e("BaseCollector", "Failed to collect " + key + ": " + e.getMessage());
        }
    }

    /**
     * 存储Map信息
     */
    protected void putInfo(String key, Map<?, ?> value) {
        try {
            Xson.put(key, new InfoValue(0, value));
        } catch (Exception e) {
            Xson.put(key, InfoValue.fail());
            XLog.e("BaseCollector", "Failed to collect " + key + ": " + e.getMessage());
        }
    }
    protected void putFailedInfo(String key) {
        Xson.put(key, InfoValue.fail());
    }

    protected void putNotCollectedInfo(String key) {
        Xson.put(key, InfoValue.notCollected());
    }
    @Override
    public void onNativeCollectComplete() {
        // 处理 Native 层收集完成的逻辑
    }
}