package com.xiaoc.warlock;

import android.app.Application;
import android.os.Build;

import com.xiaoc.warlock.Util.AppChecker;
import me.weishu.reflection.Reflection;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppChecker.checkStackTrace(new Exception());

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Reflection.unseal(this);

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
