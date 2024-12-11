package com.xiaoc.warlock.Util;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class XLog {
    private static final String TAG = "XLog";
    private static final String LOG_FOLDER = "logs"; // 日志文件夹名称

    // 日志级别
    public static final int VERBOSE = 1;
    public static final int DEBUG = 2;
    public static final int INFO = 3;
    public static final int WARN = 4;
    public static final int ERROR = 5;
    public static final int NONE = 6;

    private static int currentLevel = DEBUG;
    private static boolean writeToFile = false;
    private static String logDir;
    private static final SimpleDateFormat fileFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 初始化日志系统
     * @param context 上下文
     * @param level 日志级别
     * @param writeFile 是否写入文件
     */
    public static void init(Context context, int level, boolean writeFile) {
        currentLevel = level;
        writeToFile = writeFile;

        if (writeFile && context != null) {
            // 使用应用私有目录
            File dir = new File(context.getFilesDir(), LOG_FOLDER);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            logDir = dir.getAbsolutePath();
        }
    }

    public static void v(String msg) {
        v(TAG, msg);
    }

    public static void d(String msg) {
        d(TAG, msg);
    }

    public static void i(String msg) {
        i(TAG, msg);
    }

    public static void w(String msg) {
        w(TAG, msg);
    }

    public static void e(String msg) {
        e(TAG, msg);
    }

    public static void v(String tag, String msg) {
        if (currentLevel <= VERBOSE) {
            Log.v(tag, msg);
            writeLog('V', tag, msg);
        }
    }

    public static void d(String tag, String msg) {
        if (currentLevel <= DEBUG) {
            Log.d(tag, msg);
            writeLog('D', tag, msg);
        }
    }

    public static void i(String tag, String msg) {
        if (currentLevel <= INFO) {
            Log.i(tag, msg);
            writeLog('I', tag, msg);
        }
    }

    public static void w(String tag, String msg) {
        if (currentLevel <= WARN) {
            Log.w(tag, msg);
            writeLog('W', tag, msg);
        }
    }

    public static void e(String tag, String msg) {
        if (currentLevel <= ERROR) {
            Log.e(tag, msg);
            writeLog('E', tag, msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (currentLevel <= ERROR) {
            Log.e(tag, msg, tr);
            writeLog('E', tag, msg + '\n' + Log.getStackTraceString(tr));
        }
    }

    private static void writeLog(char level, String tag, String msg) {
        if (!writeToFile || logDir == null) return;

        executor.execute(() -> {
            String time = timeFormat.format(new Date());
            String fileName = fileFormat.format(new Date()) + ".log";
            File logFile = new File(logDir, fileName);

            String log = String.format("%s %c/%s: %s\n", time, level, tag, msg);

            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.append(log);
            } catch (IOException e) {
                Log.e(TAG, "Write log file failed", e);
            }
        });
    }

    /**
     * 获取日志文件列表
     */
    public static File[] getLogFiles() {
        if (!writeToFile || logDir == null) return new File[0];
        File dir = new File(logDir);
        return dir.listFiles((dir1, name) -> name.endsWith(".log"));
    }

    /**
     * 删除指定天数之前的日志
     */
    public static void deleteOldLogs(int days) {
        if (!writeToFile || logDir == null) return;

        executor.execute(() -> {
            File dir = new File(logDir);
            File[] files = dir.listFiles((dir1, name) -> name.endsWith(".log"));
            if (files == null) return;

            long now = System.currentTimeMillis();
            long daysInMillis = days * 24 * 60 * 60 * 1000L;

            for (File file : files) {
                if (now - file.lastModified() > daysInMillis) {
                    file.delete();
                }
            }
        });
    }

    /**
     * 获取日志文件大小
     */
    public static long getLogSize() {
        if (!writeToFile || logDir == null) return 0;

        File dir = new File(logDir);
        File[] files = dir.listFiles((dir1, name) -> name.endsWith(".log"));
        if (files == null) return 0;

        long size = 0;
        for (File file : files) {
            size += file.length();
        }
        return size;
    }
}