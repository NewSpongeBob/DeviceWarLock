package com.xiaoc.warlock;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.xiaoc.warlock.Core.Warlock;
import com.xiaoc.warlock.Util.NativeEngine;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.Util.Xson;
import com.xiaoc.warlock.ui.MainUI;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private MainUI mainUI;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化UI
        mainUI = new MainUI(this);
        // 初始化日志系统
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions("");
        }
        XLog.init(this, XLog.DEBUG, true);

        //开始获取指纹信息

        Warlock warlock = Warlock.getInstance(this);
        warlock.collectFingerprint();
        // 获取收集到的信息
        // 延迟3秒后执行
        String jsonResult = Xson.getMapString(true);
        XLog.d(jsonResult);
        boolean result = XFile.writeExternalFile(this,"log.txt",jsonResult,true);
        XLog.d(String.valueOf(result));
        if (!result){
            XFile.writePrivateFile(this,"log.txt",jsonResult,true);
        }
//        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                String jsonResult = Xson.getMapString(true);
//                XLog.d(jsonResult);
//            }
//        }, 3000); // 3000毫秒 = 3秒
    }
}