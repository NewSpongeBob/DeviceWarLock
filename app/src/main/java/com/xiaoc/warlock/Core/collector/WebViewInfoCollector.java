package com.xiaoc.warlock.Core.collector;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XLog;
import com.xiaoc.warlock.crypto.MD5Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebView信息收集器
 * 用于收集WebView相关的指纹信息，包括：
 * - 用户代理(User Agent)
 * - 屏幕分辨率
 * - 像素比例
 * - 时区
 * - GPU信息
 * - 字体列表
 * - WebView版本
 * - 系统语言
 */
public class WebViewInfoCollector extends BaseCollector {
    private static final String TAG = "WebViewInfoCollector";
    private WebView webView;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // 键名定义 - 使用a81-a89的键名
    private static final String WEB_FINGERPRINT = "a81";  // 复合指纹（MD5值）
    private static final String WEB_USER_AGENT = "a82";   // 仅存储User Agent，不参与复合指纹
    private static final String WEB_WEBVIEW_VERSION = "a88";
    private static final String WEB_SYSTEM_LANGUAGE = "a89";

    public WebViewInfoCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        // 先采集不需要WebView的数据
        collectSystemLanguage();
        collectWebViewVersion();
        
        // 使用WebView收集其他数据
        collectWebViewInfo();
    }

    /**
     * 收集系统语言信息
     */
    private void collectSystemLanguage() {
        try {
            Locale locale = Locale.getDefault();
            String language = locale.toString(); // 例如：zh_CN
            putInfo(WEB_SYSTEM_LANGUAGE, language);
            XLog.d(TAG, "Collected system language: " + language);
        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect system language: " + e.getMessage());
            putFailedInfo(WEB_SYSTEM_LANGUAGE);
        }
    }

    /**
     * 收集WebView版本信息
     */
    private void collectWebViewVersion() {
        try {
            // 获取WebView版本（可以通过User-Agent提取或直接获取）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PackageInfo webViewPackage = WebView.getCurrentWebViewPackage();
                if (webViewPackage != null) {
                    String version = webViewPackage.versionName;
                    putInfo(WEB_WEBVIEW_VERSION, version);
                    XLog.d(TAG, "Collected WebView version: " + version);
                    return;
                }
            }
            
            // 如果无法直接获取，通过创建临时WebView获取
            final AtomicReference<String> userAgent = new AtomicReference<>();
            final CountDownLatch latch = new CountDownLatch(1);
            
            mainHandler.post(() -> {
                try {
                    WebView tempWebView = new WebView(context);
                    userAgent.set(tempWebView.getSettings().getUserAgentString());
                    tempWebView.destroy();
                } catch (Exception e) {
                    XLog.e(TAG, "Error getting WebView UA: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            
            if (latch.await(2, TimeUnit.SECONDS)) {
                String ua = userAgent.get();
                if (ua != null) {
                    // 从UA中提取Chrome版本号作为WebView版本
                    String[] parts = ua.split(" ");
                    for (String part : parts) {
                        if (part.startsWith("Chrome/")) {
                            String version = part.substring(7);
                            putInfo(WEB_WEBVIEW_VERSION, version);
                            XLog.d(TAG, "Extracted WebView version from UA: " + version);
                            return;
                        }
                    }
                }
            }
            
            putFailedInfo(WEB_WEBVIEW_VERSION);
        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect WebView version: " + e.getMessage());
            putFailedInfo(WEB_WEBVIEW_VERSION);
        }
    }

    /**
     * 通过WebView收集各种Web相关信息
     * 使用简化的JavaScript指纹收集方法
     */
    private void collectWebViewInfo() {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<JSONObject> resultDataRef = new AtomicReference<>();
        
        mainHandler.post(() -> {
            try {
                // 创建WebView实例
                webView = new WebView(context);
                
                // 配置WebView
                WebSettings settings = webView.getSettings();
                settings.setJavaScriptEnabled(true);
                settings.setDomStorageEnabled(true);
                
                // 设置WebViewClient处理页面加载完成事件
                webView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        XLog.d(TAG, "WebView page loaded, injecting fingerprint JS");
                        injectFingerprintJS(latch, resultDataRef);
                    }
                });
                
                // 设置WebChromeClient以记录JavaScript控制台消息
                webView.setWebChromeClient(new android.webkit.WebChromeClient() {
                    @Override
                    public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                        XLog.d(TAG, "WebView console: " + consoleMessage.message());
                        return true;
                    }
                });
                
                // 加载空白页面以触发WebView初始化
                webView.loadData("<html><body></body></html>", "text/html", "UTF-8");
                
                // 设置超时处理
                mainHandler.postDelayed(() -> {
                    if (latch.getCount() > 0) {
                        XLog.w(TAG, "WebView fingerprint collection timed out");
                        try {
                            // 尝试再次执行JavaScript
                            injectFingerprintJS(latch, resultDataRef);
                            // 额外等待3秒
                            mainHandler.postDelayed(() -> {
                                if (latch.getCount() > 0) {
                                    XLog.e(TAG, "Final timeout, cleaning up WebView");
                                    cleanupWebView();
                                    latch.countDown();
                                }
                            }, 3000);
                        } catch (Exception e) {
                            XLog.e(TAG, "Error in timeout handler: " + e.getMessage());
                            cleanupWebView();
                            latch.countDown();
                        }
                    }
                }, 5000); // 5秒后超时
                
            } catch (Exception e) {
                XLog.e(TAG, "Error setting up WebView: " + e.getMessage());
                cleanupWebView();
                latch.countDown();
            }
        });
        
        try {
            // 等待收集完成
            if (latch.await(10, TimeUnit.SECONDS)) {
                // 处理收集的数据
                JSONObject data = resultDataRef.get();
                if (data != null) {
                    processCollectedData(data);
                } else {
                    XLog.e(TAG, "No WebView data was collected");
                    markWebViewFieldsAsFailed();
                }
            } else {
                XLog.e(TAG, "Timeout waiting for WebView data collection");
                markWebViewFieldsAsFailed();
            }
        } catch (Exception e) {
            XLog.e(TAG, "Error in WebView collection: " + e.getMessage());
            markWebViewFieldsAsFailed();
        }
    }
    
    /**
     * 注入JavaScript代码采集指纹
     */
    private void injectFingerprintJS(CountDownLatch latch, AtomicReference<JSONObject> resultDataRef) {
        if (webView == null) {
            XLog.e(TAG, "WebView is null, cannot inject JS");
            return;
        }
        
        String jsCode = "(function() {\n" +
            "    // 1. 基础信息\n" +
            "    const data = {\n" +
            "        userAgent: navigator.userAgent,\n" +
            "        screen: window.screen.width + 'x' + window.screen.height,\n" +
            "        pixelRatio: window.devicePixelRatio,\n" +
            "        timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,\n" +
            "        hardwareConcurrency: navigator.hardwareConcurrency || 'unknown',\n" +
            "        touchSupport: 'ontouchstart' in window,\n" +
            "        language: navigator.language,\n" +
            "        platform: navigator.platform\n" +
            "    };\n" +
            "\n" +
            "    // 2. WebGL指纹\n" +
            "    try {\n" +
            "        const canvas = document.createElement('canvas');\n" +
            "        const gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');\n" +
            "        if (gl) {\n" +
            "            const debugInfo = gl.getExtension('WEBGL_debug_renderer_info');\n" +
            "            if (debugInfo) {\n" +
            "                data.gpuVendor = gl.getParameter(debugInfo.UNMASKED_VENDOR_WEBGL);\n" +
            "                data.gpuRenderer = gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL);\n" +
            "            }\n" +
            "            // 收集更多WebGL参数\n" +
            "            data.maxTextureSize = gl.getParameter(gl.MAX_TEXTURE_SIZE);\n" +
            "            data.maxViewportDims = gl.getParameter(gl.MAX_VIEWPORT_DIMS);\n" +
            "            data.shadings = gl.getParameter(gl.SHADING_LANGUAGE_VERSION);\n" +
            "            data.extensions = gl.getSupportedExtensions().join(',').substring(0, 500);\n" +
            "        }\n" +
            "    } catch (e) {\n" +
            "        data.webglError = e.message;\n" +
            "    }\n" +
            "\n" +
            "    // 3. 字体列表（简化版）\n" +
            "    data.fonts = [];\n" +
            "    const testFonts = ['Arial', 'Courier New', 'Times New Roman', 'Roboto', 'Noto Sans', 'Helvetica', 'Verdana', 'Tahoma', 'Georgia'];\n" +
            "    for (const font of testFonts) {\n" +
            "        data.fonts.push(font);\n" +
            "    }\n" +
            "\n" +
            "    // 4. Canvas指纹\n" +
            "    try {\n" +
            "        const canvas = document.createElement('canvas');\n" +
            "        canvas.width = 200;\n" +
            "        canvas.height = 20;\n" +
            "        const ctx = canvas.getContext('2d');\n" +
            "        ctx.textBaseline = 'top';\n" +
            "        ctx.font = '14px Arial';\n" +
            "        ctx.fillStyle = '#f60';\n" +
            "        ctx.fillRect(125, 1, 62, 20);\n" +
            "        ctx.fillStyle = '#069';\n" +
            "        ctx.fillText('WebViewFP,za2', 2, 15);\n" +
            "        const base64 = canvas.toDataURL().substring(0, 100);\n" +
            "        data.canvasHash = base64;\n" +
            "    } catch (e) {\n" +
            "        data.canvasError = e.message;\n" +
            "    }\n" +
            "\n" +
            "    return JSON.stringify(data);\n" +
            "})();";
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                webView.evaluateJavascript(jsCode, value -> {
                    try {
                        // 处理JS返回的JSON数据
                        XLog.d(TAG, "Received WebView data: " + value);
                        
                        // 移除多余的引号和转义字符
                        String jsonStr = value;
                        if (jsonStr.startsWith("\"") && jsonStr.endsWith("\"")) {
                            jsonStr = jsonStr.substring(1, jsonStr.length() - 1)
                                    .replace("\\\"", "\"")
                                    .replace("\\\\", "\\");
                        }
                        
                        JSONObject data = new JSONObject(jsonStr);
                        XLog.d(TAG, "Parsed JSON data successfully");
                        
                        // 存储结果并解除锁定
                        resultDataRef.set(data);
                        cleanupWebView();
                        latch.countDown();
                        
                    } catch (JSONException e) {
                        XLog.e(TAG, "Error parsing WebView data: " + e.getMessage());
                        cleanupWebView();
                        latch.countDown();
                    }
                });
            } else {
                // 为KitKat以下设备提供后备方案
                XLog.e(TAG, "Device API level too low for evaluateJavascript");
                cleanupWebView();
                latch.countDown();
            }
        } catch (Exception e) {
            XLog.e(TAG, "Error injecting fingerprint JS: " + e.getMessage());
            cleanupWebView();
            latch.countDown();
        }
    }
    
    /**
     * 处理收集到的数据并存储，生成指纹
     */
    private void processCollectedData(JSONObject data) {
        try {
            // 仅保存User Agent
            if (data.has("userAgent")) {
                String userAgent = data.getString("userAgent");
                putInfo(WEB_USER_AGENT, userAgent);
                XLog.d(TAG, "Saved user agent: " + userAgent);
            }

            // 构建复合指纹
            StringBuilder fingerprintBuilder = new StringBuilder();
            
            // 添加各项属性到复合指纹，按照固定顺序添加
            addToFingerprint(fingerprintBuilder, data, "screen");
            addToFingerprint(fingerprintBuilder, data, "pixelRatio");
            addToFingerprint(fingerprintBuilder, data, "timezone");
            addToFingerprint(fingerprintBuilder, data, "gpuVendor");
            addToFingerprint(fingerprintBuilder, data, "gpuRenderer");
            addToFingerprint(fingerprintBuilder, data, "platform");
            addToFingerprint(fingerprintBuilder, data, "language");
            addToFingerprint(fingerprintBuilder, data, "hardwareConcurrency");
            addToFingerprint(fingerprintBuilder, data, "touchSupport");
            addToFingerprint(fingerprintBuilder, data, "maxTextureSize");
            addToFingerprint(fingerprintBuilder, data, "maxViewportDims");
            addToFingerprint(fingerprintBuilder, data, "shadings");
            addToFingerprint(fingerprintBuilder, data, "canvasHash");

            // 添加字体信息
            if (data.has("fonts")) {
                JSONArray fontsArray = data.getJSONArray("fonts");
                StringBuilder fontsList = new StringBuilder();
                for (int i = 0; i < fontsArray.length(); i++) {
                    if (i > 0) fontsList.append(",");
                    fontsList.append(fontsArray.getString(i));
                }
                fingerprintBuilder.append(fontsList);
            }
            
            // 添加系统语言和WebView版本，这些是在Java端收集的
            String systemLanguage = Locale.getDefault().toString();
            fingerprintBuilder.append(systemLanguage);

            String webViewVersion = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    PackageInfo webViewPackage = WebView.getCurrentWebViewPackage();
                    if (webViewPackage != null) {
                        webViewVersion = webViewPackage.versionName;
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }
            fingerprintBuilder.append(webViewVersion);
            
            // 计算复合指纹的MD5哈希值
            String combinedData = fingerprintBuilder.toString();
            String fingerprint = MD5Util.md5(combinedData);
            
            // 存储生成的指纹
            putInfo(WEB_FINGERPRINT, fingerprint);
            XLog.d(TAG, "Generated WebView fingerprint (MD5): " + fingerprint);
            XLog.d(TAG, "Fingerprint source data length: " + combinedData.length());
            
        } catch (Exception e) {
            XLog.e(TAG, "Error processing collected data: " + e.getMessage(), e);
            markWebViewFieldsAsFailed();
        }
    }
    
    /**
     * 将指定键的值添加到指纹字符串构建器
     */
    private void addToFingerprint(StringBuilder builder, JSONObject data, String key) {
        try {
            if (data.has(key)) {
                Object value = data.get(key);
                builder.append(value.toString());
            } else {
                builder.append("NA_").append(key);
            }
        } catch (JSONException e) {
            builder.append("ERR_").append(key);
        }
    }
    
    /**
     * 标记所有WebView相关字段为失败
     */
    private void markWebViewFieldsAsFailed() {
        putFailedInfo(WEB_FINGERPRINT);
        putFailedInfo(WEB_USER_AGENT);
    }
    
    /**
     * 清理WebView资源
     */
    private void cleanupWebView() {
        mainHandler.post(() -> {
            if (webView != null) {
                webView.destroy();
                webView = null;
                XLog.d(TAG, "WebView destroyed");
            }
        });
    }
} 