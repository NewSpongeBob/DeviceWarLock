package com.xiaoc.warlock.Util;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class XFile {

    /**
     * 读取文件内容为字符串
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return content.toString();
    }

    /**
     * 写入字符串到文件
     * @param filePath 文件路径
     * @param content 要写入的内容
     * @param append 是否追加模式
     * @return 是否写入成功
     */
    public static boolean writeFile(String filePath, String content, boolean append) {
        try (FileWriter writer = new FileWriter(filePath, append)) {
            writer.write(content);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * 写入字符串到私有目录下的文件
     * @param context 上下文
     * @param fileName 文件路径
     * @param content 要写入的内容
     * @param append 是否追加模式
     * @return 是否写入成功
     */
    public static  boolean writeExternalFile(Context context,String fileName, String content, boolean append) {
        try {
            File file;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10 及以上
                File dir = context.getExternalFilesDir(null);
                if (dir == null) return false;
                file = new File(dir, fileName);
            } else {
                // Android 10 以下
                File dir = new File(Environment.getExternalStorageDirectory(),
                        "Android/data/" + context.getPackageName());
                file = new File(dir, fileName);
            }

            // 确保目录存在
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) return false;
            }

            // 写入文件
            try (FileWriter writer = new FileWriter(file, append)) {
                writer.write(content);
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    /**
     * 复制文件
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @return 是否复制成功
     */
    public static boolean copyFile(String sourcePath, String targetPath) {
        try (FileInputStream fis = new FileInputStream(sourcePath);
             FileOutputStream fos = new FileOutputStream(targetPath)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除文件或目录
     * @param path 文件或目录路径
     * @return 是否删除成功
     */
    public static boolean delete(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    delete(child.getPath());
                }
            }
        }
        return file.delete();
    }

    /**
     * 创建目录
     * @param dirPath 目录路径
     * @return 是否创建成功
     */
    public static boolean createDir(String dirPath) {
        File dir = new File(dirPath);
        return dir.exists() || dir.mkdirs();
    }

    /**
     * 检查文件是否存在
     * @param path 文件路径
     * @return 是否存在
     */
    public static boolean exists(String path) {
        return new File(path).exists();
    }

    /**
     * 获取文件大小
     * @param path 文件路径
     * @return 文件大小（字节）
     */
    public static long getFileSize(String path) {
        File file = new File(path);
        return file.exists() && file.isFile() ? file.length() : -1;
    }

    /**
     * 读取文件为字节数组
     * @param filePath 文件路径
     * @return 字节数组
     */
    public static byte[] readFileToBytes(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) return null;

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}