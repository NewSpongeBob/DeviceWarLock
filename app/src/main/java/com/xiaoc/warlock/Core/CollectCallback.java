package com.xiaoc.warlock.Core;


/**
 * Native层指纹收集完成的回调接口
 */
public interface CollectCallback {
    /**
     * Native层收集完成时调用
     */
    void onNativeCollectComplete();
}