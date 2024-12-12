package com.xiaoc.warlock.Core.collector;

import android.content.Context;
import android.system.Os;

import com.xiaoc.warlock.BuildConfig;
import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XCommandUtil;
import com.xiaoc.warlock.Util.XLog;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatInfoCollector extends BaseCollector {
    private String TAG = "StatInfoCollector";


    public StatInfoCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        collectorStatFIle();
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute("stat " + BuildConfig.SERIAL_BLACKLIST_FILE);
            XLog.d("xiaoc666", result.getSuccessMsg());

            if (result.isSuccess()) {
                String output = result.getSuccessMsg();
                String[] lines = output.split("\n");

                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("Modify:") && line.contains("-")) {
                        putInfo("a30", extractTimestamp(line));
                    } else if (line.startsWith("Change:") && line.contains("-")) {
                        putInfo("a31", extractTimestamp(line));
                    }else if (line.startsWith("Access:") && line.contains("-")){
                        putInfo("a29", extractTimestamp(line));
                    }
                }
            } else {
                putFailedInfo("a29");
                putFailedInfo("a30");
                putFailedInfo("a31");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect stats: " + e.getMessage());
            putFailedInfo("a30");
            putFailedInfo("a31");
        }
        try {
            // 获取 pubkey_blacklist.txt 的纳秒级时间戳
            XCommandUtil.CommandResult result = XCommandUtil.execute("stat " + BuildConfig.PUBKEY_BLACKLIST_FILE);
            XLog.d("xiaoc666", result.getSuccessMsg());

            if (result.isSuccess()) {
                String output = result.getSuccessMsg();
                String[] lines = output.split("\n");
                XLog.d("xiaoc666", result.getSuccessMsg());
                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("Access:") && line.contains("-")) {  // Access time
                        putInfo("a33", extractTimestamp(line));
                    } else if (line.startsWith("Modify:") && line.contains("-")) {  // Modify time
                        putInfo("a32", extractTimestamp(line));
                    } else if (line.startsWith("Change:") && line.contains("-")) {  // Change time
                        putInfo("a34", extractTimestamp(line));
                    }
                }
            } else {
                putFailedInfo("a32");
                putFailedInfo("a33");
                putFailedInfo("a34");
            }

            // 获取 keychain 目录的访问时间

                result = XCommandUtil.execute("stat " + BuildConfig.KEYCHAIN_DIR);
                XLog.d("xiaoc666", result.getSuccessMsg());

                if (result.isSuccess()) {
                    String output = result.getSuccessMsg();
                    String[] lines = output.split("\n");

                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("Access:") && line.contains("-")) {  // Access time
                            putInfo("a35", extractTimestamp(line));
                        }  else if (line.startsWith("Change:") && line.contains("-")) {  // Change time
                            putInfo("a36", extractTimestamp(line));
                        }
                    }
                } else {
                    putFailedInfo("a35");
                    putFailedInfo("a36");
                }

        } catch (Exception e) {
            putFailedInfo("a32");
            putFailedInfo("a33");
            putFailedInfo("a34");
            putFailedInfo("a35");
            putFailedInfo("a36");
            XLog.e(TAG, "Failed to collect keychain stats: " + e.getMessage());
        }
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute("stat " + BuildConfig.APK_PATH);
            XLog.d("xiaoc666", result.getSuccessMsg() + " | " + result.getErrorMsg());

            // 检查错误输出是否包含 "No such file or directory"
            if (result.getErrorMsg() != null &&
                    result.getErrorMsg().contains("No such file or directory")) {
                putInfo("a40", "false");
            } else {
                putInfo("a40", "true");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to check Apppackage: " + e.getMessage());
            putFailedInfo("a40");
        }
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute("stat " + BuildConfig.SDCARD_DOWNLOAD_PATH);
            XLog.d("xiaoc666", result.getSuccessMsg());

            if (result.isSuccess()) {
                Map<String, String> timeMap = new LinkedHashMap<>();
                String output = result.getSuccessMsg();
                String[] lines = output.split("\n");

                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("Access:") && line.contains("-")) {
                        timeMap.put("access", extractTimestamp(line));
                    } else if (line.startsWith("Change:") && line.contains("-")) {
                        timeMap.put("change", extractTimestamp(line));
                    }
                }

                // 检查是否都获取到了时间戳
                if (timeMap.containsKey("access") && timeMap.containsKey("change")) {
                    putInfo("a37", timeMap);
                } else {
                    putFailedInfo("a37");
                }
            } else {
                putFailedInfo("a37");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect download path stats: " + e.getMessage());
            putFailedInfo("a37");
        }
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute("stat " + BuildConfig.SDCARD_ANDROID_PATH);
            XLog.d("xiaoc666", result.getSuccessMsg());

            if (result.isSuccess()) {
                Map<String, String> timeMap = new LinkedHashMap<>();
                String output = result.getSuccessMsg();
                String[] lines = output.split("\n");

                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("Access:") && line.contains("-")) {
                        timeMap.put("access", extractTimestamp(line));
                    } else if (line.startsWith("Change:") && line.contains("-")) {
                        timeMap.put("change", extractTimestamp(line));
                    }
                }

                // 检查是否都获取到了时间戳
                if (timeMap.containsKey("access") && timeMap.containsKey("change")) {
                    putInfo("a38", timeMap);
                } else {
                    putFailedInfo("a38");
                }
            } else {
                putFailedInfo("a38");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect sdcard android path stats: " + e.getMessage());
            putFailedInfo("a38");
        }
        try {
            XCommandUtil.CommandResult result = XCommandUtil.execute("stat " + BuildConfig.DATA_LOCAL_TMP_PATH);
            XLog.d("xiaoc666", result.getSuccessMsg());

            if (result.isSuccess()) {
                Map<String, String> timeMap = new LinkedHashMap<>();
                String output = result.getSuccessMsg();
                String[] lines = output.split("\n");

                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("Access:") && line.contains("-")) {
                        timeMap.put("access", extractTimestamp(line));
                    } else if (line.startsWith("Change:") && line.contains("-")) {
                        timeMap.put("change", extractTimestamp(line));
                    }
                }

                // 检查是否都获取到了时间戳
                if (timeMap.containsKey("access") && timeMap.containsKey("change")) {
                    putInfo("a39", timeMap);
                } else {
                    putFailedInfo("a39");
                }
            } else {
                putFailedInfo("a39");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect /data/local/tmp path stats: " + e.getMessage());
            putFailedInfo("a39");
        }
        /**
         * 这个检测还不确定，先留着，这个检测是利用stat的bug，如果stat返回的是权限被拒绝就代表存在这个文件，如果返回的是没有这个文件则代表有这个文件。
         */
//        try {
//            XCommandUtil.CommandResult result = XCommandUtil.execute("stat " + BuildConfig.AP_PACKAGE_PATH);
//            XLog.d("xiaoc666", result.getSuccessMsg() + " | " + result.getErrorMsg());
//
//            // 检查错误输出是否包含 "No such file or directory"
//            if (result.getErrorMsg() != null &&
//                    result.getErrorMsg().contains("No such file or directory")) {
//                putInfo("a41", "false");
//            } else {
//                putInfo("a41", "true");
//            }
//
//        } catch (Exception e) {
//            XLog.e("StatInfoCollector", "Failed to check Ap_Package_Path: " + e.getMessage());
//            putFailedInfo("a40");
//        }
    }

    /**
     * 将秒级时间戳转换为纳秒级时间戳
     */
    private String extractTimestamp(String line) {
        try {
            // 提取时间部分，格式如：2024-10-25 15:21:34.828000001
            Pattern pattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+");
            Matcher matcher = pattern.matcher(line);

            if (matcher.find()) {
                String timeStr = matcher.group();
                // 解析时间字符串
                String[] parts = timeStr.split("\\.");
                String datePart = parts[0]; // 2024-10-25 15:21:34
                String nanoPart = parts[1]; // 828000001

                // 转换为时间戳
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("GMT+8")); // 设置时区为+8
                long milliseconds = sdf.parse(datePart).getTime();

                // 转换为纳秒级时间戳
                long nanoSeconds = milliseconds * 1_000_000L + Long.parseLong(nanoPart);
                return String.valueOf(nanoSeconds);
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to extract timestamp: " + e.getMessage());
        }
        return "-1";
    }
    private  void collectorStatFIle(){
        try {
            Map<String, Map<String, Object>> result = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : BuildConfig.PATH_MAPPINGS.entrySet()) {
                String path = entry.getKey();
                String mapping = entry.getValue();

                try {

                    // getFileStatsByReflection是通过反射去stat的，这个获取不到纳米级的时间所以使用命令行的stat方法
                   //Map<String, Object> stats = getFileStatsByReflection(new File(path));


                    Map<String, Object>   stats = getFileStatsByCommand(path);


                    if (!stats.isEmpty()) {
                        result.put(mapping, stats);
                    }
                } catch (Exception e) {
                    XLog.e(TAG, "Failed to get stats for " + path + ": " + e.getMessage());
                }
            }

            if (!result.isEmpty()) {
                putInfo("a53", result);
            } else {
                putFailedInfo("a53");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect file stats: " + e.getMessage());
            putFailedInfo("a53");
        }
    }
    private Map<String, Object> getFileStatsByReflection(File file) {
        Map<String, Object> stats = new LinkedHashMap<>();
        try {
            Class<?> statClass = Class.forName("android.system.StructStat");
            Object stat = Os.stat(file.getAbsolutePath());

            // 获取秒级时间戳
            long atimeSecs = ((Long) statClass.getField("st_atime").get(stat));
            long mtimeSecs = ((Long) statClass.getField("st_mtime").get(stat));
            long ctimeSecs = ((Long) statClass.getField("st_ctime").get(stat));

            // 获取纳秒部分
            long atimeNanos = 0;
            long mtimeNanos = 0;
            long ctimeNanos = 0;

            try {
                // Android 7.0 (API 24) 及以上版本支持纳秒字段
                atimeNanos = ((Long) statClass.getField("st_atime_nsec").get(stat));
                mtimeNanos = ((Long) statClass.getField("st_mtime_nsec").get(stat));
                ctimeNanos = ((Long) statClass.getField("st_ctime_nsec").get(stat));
            } catch (NoSuchFieldException e) {
                // 旧版本 Android 不支持纳秒字段
                XLog.d(TAG, "Nanosecond fields not available");
            }

            // 转换每个时间戳
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

            // Access time
            Date atimeDate = new Date(atimeSecs * 1000);
            String atimeDateStr = sdf.format(atimeDate);
            String atimeNanoStr = String.format("%-9s", atimeNanos).replace(' ', '0');
            long atimeTimestamp = (atimeSecs) * 1_000_000_000L + atimeNanos;

            // Modify time
            Date mtimeDate = new Date(mtimeSecs * 1000);
            String mtimeDateStr = sdf.format(mtimeDate);
            String mtimeNanoStr = String.format("%-9s", mtimeNanos).replace(' ', '0');
            long mtimeTimestamp = (mtimeSecs) * 1_000_000_000L + mtimeNanos;

            // Change time
            Date ctimeDate = new Date(ctimeSecs * 1000);
            String ctimeDateStr = sdf.format(ctimeDate);
            String ctimeNanoStr = String.format("%-9s", ctimeNanos).replace(' ', '0');
            long ctimeTimestamp = (ctimeSecs) * 1_000_000_000L + ctimeNanos;

            // 获取inode
            long inode = (Long) statClass.getField("st_ino").get(stat);

            // 存储结果
            stats.put("A", atimeTimestamp);
            stats.put("M", mtimeTimestamp);
            stats.put("C", ctimeTimestamp);
            stats.put("I", inode);

            // 调试输出
            XLog.d(TAG, String.format("Access: dateStr=%s, nanoStr=%s, timestamp=%d",
                    atimeDateStr, atimeNanoStr, atimeTimestamp));
            XLog.d(TAG, String.format("Modify: dateStr=%s, nanoStr=%s, timestamp=%d",
                    mtimeDateStr, mtimeNanoStr, mtimeTimestamp));
            XLog.d(TAG, String.format("Change: dateStr=%s, nanoStr=%s, timestamp=%d",
                    ctimeDateStr, ctimeNanoStr, ctimeTimestamp));

        } catch (Exception e) {
            XLog.e(TAG, "Reflection failed: " + e.getMessage());
        }
        return stats;
    }
    private Map<String, Object> getFileStatsByCommand(String path) {
        Map<String, Object> stats = new LinkedHashMap<>();

        String command = String.format("stat '%s'", path);
        XCommandUtil.CommandResult result = XCommandUtil.execute(command);

        if (result.isSuccess() && !result.getSuccessMsg().isEmpty()) {
            try {
                // 匹配时间行和Inode行
                Pattern timePattern = Pattern.compile("(Access|Modify|Change): (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\.(\\d+)");
                Pattern inodePattern = Pattern.compile("Inode: (\\d+)");

                Matcher timeMatcher = timePattern.matcher(result.getSuccessMsg());
                while (timeMatcher.find()) {
                    String type = timeMatcher.group(1);
                    String dateStr = timeMatcher.group(2);
                    String nanoStr = timeMatcher.group(3);

                    // 解析日期时间部分到毫秒
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    sdf.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
                    long millis = sdf.parse(dateStr).getTime();

                    // 处理纳秒部分（保持原始精度）
                    nanoStr = String.format("%-9s", nanoStr).replace(' ', '0');
                    long nanos = Long.parseLong(nanoStr);

                    // 组合为纳秒时间戳
                    long timestamp = (millis / 1000) * 1_000_000_000L + nanos;

                    // 调试输出
                    XLog.d(TAG, String.format("%s: dateStr=%s, nanoStr=%s, timestamp=%d",
                            type, dateStr, nanoStr, timestamp));

                    // 根据类型存储
                    switch (type) {
                        case "Access":
                            stats.put("A", timestamp);
                            break;
                        case "Modify":
                            stats.put("M", timestamp);
                            break;
                        case "Change":
                            stats.put("C", timestamp);
                            break;
                    }
                }

                // 获取Inode
                Matcher inodeMatcher = inodePattern.matcher(result.getSuccessMsg());
                if (inodeMatcher.find()) {
                    stats.put("I", Long.parseLong(inodeMatcher.group(1)));
                }

            } catch (Exception e) {
                XLog.e(TAG, "Failed to parse stat output: " + e.getMessage());
            }
        }

        return stats;
    }


}
