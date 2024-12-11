package com.xiaoc.warlock.Core;

import android.content.Context;

import com.xiaoc.warlock.Core.collector.BasicInfoCollector;
import com.xiaoc.warlock.Core.collector.StatCollector;
import com.xiaoc.warlock.Util.Xson;

import java.util.Arrays;
import java.util.List;

public class Warlock {
    private static volatile Warlock instance;
    private final Context context;
    private final List<BaseCollector> collectors;

    private Warlock(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context.getApplicationContext();
        this.collectors = initCollectors();
    }

    public static Warlock getInstance(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        if (instance == null) {
            synchronized (Warlock.class) {
                if (instance == null) {
                    instance = new Warlock(context);
                }
            }
        }
        return instance;
    }

    private List<BaseCollector> initCollectors() {
        return Arrays.asList(
                new BasicInfoCollector(context),
                new StatCollector(context)
                // 添加更多收集器...
        );
    }

    /**
     * 收集所有信息
     */
    public void collectFingerprint() {
        // 清除旧数据
        Xson.clear();

        // 收集新数据
        for (BaseCollector collector : collectors) {
            collector.collect();
        }
    }
}
