package com.xiaoc.warlock.Util;

import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class XCommandUtil {
    private static final int BUFFER_SIZE = 16384;// 增加缓冲区大小

    /**
     * 执行shell命令，默认返回命令执行结果
     * @param command 命令
     * @return 命令执行结果
     */
    public static CommandResult execute(String command) {
        return execute(command, true);
    }

    /**
     * 执行shell命令
     * @param command 命令
     * @param needResult 是否需要返回结果
     */
    public static CommandResult execute(String command, boolean needResult) {
        Process process = null;
        try {
            // 使用ProcessBuilder替代Runtime.exec
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("sh", "-c", command);
            process = processBuilder.start();

            if (needResult) {
                // 使用独立线程读取输出流和错误流
                StreamReader outputReader = new StreamReader(process.getInputStream());
                StreamReader errorReader = new StreamReader(process.getErrorStream());

                // 启动读取线程
                Thread outputThread = new Thread(outputReader);
                Thread errorThread = new Thread(errorReader);
                outputThread.start();
                errorThread.start();

                // 等待进程执行完成
                int exitValue = process.waitFor();

                // 等待读取线程完成
                outputThread.join(5000);  // 5秒超时
                errorThread.join(5000);   // 5秒超时

                return new CommandResult(
                        exitValue,
                        outputReader.getOutput(),
                        errorReader.getOutput()
                );
            }

            process.waitFor();
            return new CommandResult(process.exitValue(), "", "");

        } catch (Exception e) {
            e.printStackTrace();
            return new CommandResult(-1, "", e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
    private static class StreamReader implements Runnable {
        private final InputStream inputStream;
        private final StringBuilder output = new StringBuilder();

        StreamReader(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream), BUFFER_SIZE)) {
                char[] buffer = new char[BUFFER_SIZE];
                int len;
                while ((len = reader.read(buffer)) > 0) {
                    output.append(buffer, 0, len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String getOutput() {
            return output.toString().trim();
        }
    }
    /**
     * 异步执行shell命令
     * @param command 命令
     * @param callback 回调
     */
    public static void executeAsync(String command, CommandCallback callback) {
        new Thread(() -> {
            CommandResult result = execute(command);
            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    callback.onComplete(result);
                });
            }
        }).start();
    }

    /**
     * 使用root权限执行命令
     * @param command 命令
     * @return 命令执行结果
     */
    public static CommandResult executeAsRoot(String command) {
        return execute("su -c " + command);
    }

    /**
     * 检查是否有root权限
     * @return 是否有root权限
     */
    public static boolean hasRootPermission() {
        CommandResult result = execute("su -c ls /data");
        return result.isSuccess();
    }

    /**
     * 获取系统属性
     * @param key 属性key
     * @return 属性值
     */
    public static String getSystemProperty(String key) {
        CommandResult result = execute("getprop " + key);
        return result.isSuccess() ? result.successMsg : "";
    }

    /**
     * 安全关闭流
     */
    private static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 命令执行结果
     */
    public static class CommandResult {
        private int exitValue;
        private String successMsg;
        private String errorMsg;

        public CommandResult(int exitValue, String successMsg, String errorMsg) {
            this.exitValue = exitValue;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }

        public boolean isSuccess() {
            return exitValue == 0;
        }

        public int getExitValue() {
            return exitValue;
        }

        public String getSuccessMsg() {
            return successMsg;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        @Override
        public String toString() {
            return "CommandResult{" +
                    "exitValue=" + exitValue +
                    ", successMsg='" + successMsg + '\'' +
                    ", errorMsg='" + errorMsg + '\'' +
                    '}';
        }
    }

    /**
     * 命令执行回调
     */
    public interface CommandCallback {
        void onComplete(CommandResult result);
    }
}
