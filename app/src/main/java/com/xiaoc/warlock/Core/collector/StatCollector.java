package com.xiaoc.warlock.Core.collector;

import android.content.Context;

import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XCommandUtil;
import com.xiaoc.warlock.Util.XLog;

public class StatCollector extends BaseCollector {

    private static final String KEYCHAIN_DIR = "/data/misc/keychain";
    private static final String BLACKLIST_FILE = "/data/misc/keychain/pubkey_blacklist.txt";

    public StatCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        try {
            // 获取 pubkey_blacklist.txt 的纳秒级时间戳
            XCommandUtil.CommandResult result = XCommandUtil.execute("stat "+BLACKLIST_FILE);
            XLog.d("xiaoc666",result.getSuccessMsg());
            if (result.isSuccess()) {
                String[] times = result.getSuccessMsg().split("\n");
                if (times.length >= 3) {
                    // 转换为纳秒级时间戳
                    putInfo("a33", convertToNanoTime(times[0])); // Access
                    putInfo("a32", convertToNanoTime(times[1])); // Modify
                    putInfo("a34", convertToNanoTime(times[2])); // Change
                }
            } else {
                putFailedInfo("a32");
                putFailedInfo("a33");
                putFailedInfo("a34");
            }

            // 获取 keychain 目录的访问时间
            result = XCommandUtil.execute("stat "+KEYCHAIN_DIR);
            XLog.d("xiaoc666",result.getSuccessMsg());
            if (result.isSuccess()) {
                putInfo("a35", convertToNanoTime(result.getSuccessMsg().trim()));
            } else {
                putFailedInfo("a35");
            }

        } catch (Exception e) {
            putFailedInfo("a32");
            putFailedInfo("a33");
            putFailedInfo("a34");
            putFailedInfo("a35");
            XLog.e("KeychainStatCollector", "Failed to collect keychain stats: " + e.getMessage());
        }
    }

    /**
     * 将秒级时间戳转换为纳秒级时间戳
     */
    private String convertToNanoTime(String seconds) {
        try {
            long secs = Long.parseLong(seconds.trim());
            return String.valueOf(secs * 1_000_000_000L);
        } catch (NumberFormatException e) {
            XLog.e("KeychainStatCollector", "Failed to convert time: " + e.getMessage());
            return "0";
        }
    }
}
