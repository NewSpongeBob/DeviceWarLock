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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
     * 写入字符串到应用私有目录下的文件
     * @param context 上下文
     * @param fileName 文件名
     * @param content 要写入的内容
     * @param append 是否追加模式
     * @return 是否写入成功
     */
    public static boolean writePrivateFile(Context context, String fileName, String content, boolean append) {
        try {
            // 获取应用私有目录
            File dir = context.getFilesDir();  // /data/data/包名/files/
            // 或者使用 context.getDir("custom_dir", Context.MODE_PRIVATE) 创建自定义子目录

            File file = new File(dir, fileName);

            // 确保父目录存在
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) return false;
            }

            // 写入文件
            try (FileWriter writer = new FileWriter(file, append)) {
                writer.write(content);
                writer.flush();
                return true;
            }

        } catch (Exception e) {
            XLog.e("FileUtils", "Failed to write private file: " + e.getMessage());
            return false;
        }
    }

    /**
     * 写入字符串到应用私有缓存目录下的文件
     * @param context 上下文
     * @param fileName 文件名
     * @param content 要写入的内容
     * @param append 是否追加模式
     * @return 是否写入成功
     */
    public static boolean writePrivateCacheFile(Context context, String fileName, String content, boolean append) {
        try {
            // 获取应用私有缓存目录
            File dir = context.getCacheDir();  // /data/data/包名/cache/
            File file = new File(dir, fileName);

            // 确保父目录存在
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                if (!parent.mkdirs()) return false;
            }

            // 写入文件
            try (FileWriter writer = new FileWriter(file, append)) {
                writer.write(content);
                writer.flush();
                return true;
            }

        } catch (Exception e) {
            XLog.e("FileUtils", "Failed to write cache file: " + e.getMessage());
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
     * 读取整个文件内容为字节数组
     *
     * @param file 要读取的文件
     * @return 文件内容对应的 byte[]
     * @throws IOException 读取失败时抛出
     */
    public static byte[] readFileToByteArray(File file) throws IOException {
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            throw new IOException("File is too large to fit in a byte array: " + file.getName());
        }

        byte[] bytes = new byte[(int) length];
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            int offset = 0;
            int numRead;
            while (offset < bytes.length && (numRead = fis.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                throw new IOException("Could not completely read file " + file.getName());
            }
            return bytes;
        } finally {
            if (fis != null) {
                fis.close();
            }
        }
    }
    /**
     * 列出指定目录及其子目录下所有指定后缀名的文件
     *
     * @param dir 起始目录
     * @param extensions 文件扩展名数组，例如 {"ttf"}
     * @param recursive 是否递归子目录
     * @return 文件集合
     */
    public static Collection<File> listFiles(File dir, String[] extensions, boolean recursive) {
        List<File> result = new ArrayList<>();
        if (dir != null && dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory() && recursive) {
                        result.addAll(listFiles(file, extensions, true));
                    } else {
                        for (String ext : extensions) {
                            if (file.getName().toLowerCase().endsWith("." + ext.toLowerCase())) {
                                result.add(file);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }
}