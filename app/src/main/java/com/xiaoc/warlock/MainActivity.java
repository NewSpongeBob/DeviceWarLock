package com.xiaoc.warlock;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.xiaoc.warlock.Core.Warlock;
import com.xiaoc.warlock.Util.NativeEngine;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.Xson;

public class MainActivity extends AppCompatActivity {
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化日志系统
        XLog.init(this, XLog.DEBUG, true);

        //开始获取指纹信息

        Warlock warlock = Warlock.getInstance(this);
        warlock.collectFingerprint();
        // 获取收集到的信息
        // 延迟3秒后执行
        String jsonResult = Xson.getMapString(true);
        XLog.d(jsonResult);
//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                String jsonResult = Xson.getMapString(true);
//                XLog.d(jsonResult);
//            }
//        }, 3000); // 3000毫秒 = 3秒
    }
}