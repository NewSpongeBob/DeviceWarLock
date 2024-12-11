package com.xiaoc.warlock.Util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class XNetwork {
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build();
    /**
     * 检查网络是否可用
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * 判断是否是WiFi连接
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;

        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI;
    }
    /**
     * GET请求
     * @param url 请求地址
     * @param callback 回调接口
     */
    public static void get(String url, NetworkCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        executeRequest(request, callback);
    }

    /**
     * POST请求（JSON数据）
     * @param url 请求地址
     * @param jsonBody JSON字符串
     * @param callback 回调接口
     */
    public static void postJson(String url, String jsonBody, NetworkCallback callback) {
        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        executeRequest(request, callback);
    }

    /**
     * POST请求（表单数据）
     * @param url 请求地址
     * @param params 参数Map
     * @param callback 回调接口
     */
    public static void postForm(String url, Map<String, String> params, NetworkCallback callback) {
        FormBody.Builder formBuilder = new FormBody.Builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formBuilder.add(entry.getKey(), entry.getValue());
        }

        Request request = new Request.Builder()
                .url(url)
                .post(formBuilder.build())
                .build();

        executeRequest(request, callback);
    }

    /**
     * 上传文件
     * @param url 请求地址
     * @param file 文件
     * @param callback 回调接口
     */
    public static void uploadFile(String url, File file, NetworkCallback callback) {
        RequestBody requestBody = MultipartBody.create(file, MediaType.parse("application/octet-stream"));
        MultipartBody.Builder multipartBuilder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), requestBody);

        Request request = new Request.Builder()
                .url(url)
                .post(multipartBuilder.build())
                .build();

        executeRequest(request, callback);
    }

    /**
     * 下载文件
     * @param url 文件URL
     * @param destFile 目标文件
     * @param callback 下载回调
     */
    public static void downloadFile(String url, File destFile, DownloadCallback callback) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onFailure(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onFailure("下载失败: " + response.code());
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    callback.onFailure("响应体为空");
                    return;
                }

                long totalBytes = body.contentLength();
                long downloadedBytes = 0;

                try (InputStream is = body.byteStream();
                     FileOutputStream fos = new FileOutputStream(destFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, len);
                        downloadedBytes += len;
                        callback.onProgress((int) (downloadedBytes * 100 / totalBytes));
                    }
                    callback.onSuccess(destFile);
                } catch (Exception e) {
                    callback.onFailure(e.getMessage());
                }
            }
        });
    }

    private static void executeRequest(Request request, NetworkCallback callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> callback.onFailure(e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String result = "";
                if (response.body() != null) {
                    result = response.body().string();
                }
                String finalResult = result;
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> callback.onSuccess(finalResult));
            }
        });
    }

    /**
     * 网络请求回调接口
     */
    public interface NetworkCallback {
        void onSuccess(String response);
        void onFailure(String error);
    }

    /**
     * 下载回调接口
     */
    public interface DownloadCallback {
        void onProgress(int progress);
        void onSuccess(File file);
        void onFailure(String error);
    }
}
