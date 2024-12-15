package com.xiaoc.warlock.Core.detector;

import android.content.Context;

import com.xiaoc.warlock.App;
import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.AppChecker;
import com.xiaoc.warlock.Util.WarningBuilder;
import com.xiaoc.warlock.ui.adapter.InfoItem;

public class MiscDetector extends BaseDetector {


    public MiscDetector(Context context, EnvironmentCallback callback) {
        super(context, callback);
    }

    /**
     * 检测当前设备是否能够正常调用反射
     */
    private void checkReflectionAvailable(){
        try {
            boolean supported =AppChecker.isReflectionSupported();
                if (supported){{
                    InfoItem warning = new WarningBuilder("checkHideApi", null)
                            .addDetail("check", String.valueOf(supported))
                            .addDetail("level", "medium")
                            .build();

                    reportAbnormal(warning);
                }}

        } catch (Exception e) {
        }
    }
    /**
     * 检测App启动时是否会有异常堆栈
     */
    private void checkException(){
        try {
            boolean StackTraceBbnormal =AppChecker.isStackTraceBbnormal();
            if (StackTraceBbnormal){{
                StringBuilder details = new StringBuilder();
                for (String line : AppChecker.getStackTraceBbnormal()) {
                    details.append(line).append("\n");
                }
                InfoItem warning = new WarningBuilder("checkStackTrace", null)
                        .addDetail("check", details.toString().trim())
                        .addDetail("level", "high")
                        .build();

                reportAbnormal(warning);
            }}

        } catch (Exception e) {
        }
    }
    @Override
    public void detect() {
        checkReflectionAvailable();
        checkException();
    }
}
