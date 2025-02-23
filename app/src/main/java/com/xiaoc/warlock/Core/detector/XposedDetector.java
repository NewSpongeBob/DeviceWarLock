package com.xiaoc.warlock.Core.detector;

import android.content.Context;

import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.Util.XFile;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.ui.adapter.InfoItem;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class XposedDetector extends BaseDetector {
    private String TAG = "XposedDetector";
    public XposedDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
    }

    @Override
    public void detect() {
        checkXposed();
    }
    private void checkXposed() {
        try {

            List<String> foundFiles = new ArrayList<>();
            for (String path : BuildConfig.XPOSED_PATHS) {
                if (XFile.exists(path)) {
                    foundFiles.add(path);
                }
            }

            if (!foundFiles.isEmpty()) {
                StringBuilder details = new StringBuilder();
                for (String file : foundFiles) {
                    details.append(file).append("\n");
                }

                InfoItem warning = new WarningBuilder("checkXposedFile", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "medium")
                        .build();
                reportAbnormal(warning);
            }
        } catch (Exception e) {
        }
    }
}
