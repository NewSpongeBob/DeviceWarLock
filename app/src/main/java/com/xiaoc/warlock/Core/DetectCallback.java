package com.xiaoc.warlock.Core;

import android.content.Context;

public interface DetectCallback {
    Context getContext();  // 添加获取Context的方法
    void onDetectWarning(String type, String level, String detail);
}