package com.xiaoc.warlock.ui.dialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.xiaoc.warlock.R;

import java.io.File;

public class DialogManager {
    private final FragmentActivity activity;

    public DialogManager(FragmentActivity activity) {
        this.activity = activity;
    }

    public void showAboutDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle("关于 WarLock")
                .setMessage(
                        "WarLock 是一个设备指纹和环境检测工具，同时具备黑灰产设备感知以及唯一设备的能力。\n\n" +
                                "主要功能：\n" +
                                "• 收集设备信息\n" +
                                "• 检测运行环境安全性\n" +
                                "• 生成设备唯一标识\n" +
                                "• 分析环境特征\n\n" +
                                "开发者：xiaoc\n\n" +
                                "项目开源计划：作者计划在2025年进行改机项目的研发，同时将本项目及其后端完整开源。\n\n"

                )
                .setPositiveButton("确定", null)
                .setNeutralButton("访问项目主页", (dialog, which) -> {
                    // 打开项目主页
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse("https://github.com/imxiaoc996/DeviceWarLock"));
                        activity.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(activity, "无法打开浏览器", Toast.LENGTH_SHORT).show();
                    }
                });

        // 创建并显示对话框
        AlertDialog dialog = builder.create();

        // 设置消息文本的对齐方式（可选）
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }

        dialog.show();
    }
    public void showexplainDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle("WarLock 使用说明")
                .setMessage(
                        "隐私保护承诺：\n\n" +
                                "• 所有上传的设备信息及指纹数据将在5天后自动删除\n" +
                                "• 应用列表(AppList)仅用于本地环境检测，绝不上传服务器\n\n" +
                                "核心功能说明：\n\n" +
                                "1. 设备指纹服务\n" +
                                "   • 客户端采集设备特征信息\n" +
                                "   • 服务端通过多重算法生成四重防伪指纹\n\n" +
                                "2. 环境检测服务\n" +
                                "   • 90%的检测逻辑在本地执行\n" +
                                "   • 服务端协助分析设备指纹，提供额外10%的风险识别\n" +
                                "   • 双重校验确保更准确的环境安全评估"
                )
                .setPositiveButton("确定", null);

        // 创建并显示对话框
        AlertDialog dialog = builder.create();

        // 设置消息文本的对齐方式（可选）
        TextView messageView = dialog.findViewById(android.R.id.message);
        if (messageView != null) {
            messageView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        }

        dialog.show();
    }
    public void showFeedbackDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle("反馈与支持")
                .setMessage(
                        "您可以通过以下方式反馈问题或获取帮助：\n\n" +
                                "• GitHub Issues：提交问题或建议\n" +
                                "• 微信：xiaoc_engine\n\n" +
                                "如果您觉得这个项目对您有帮助，欢迎：\n\n" +
                                "• 在 GitHub 上点个 Star\n" +
                                "• 推荐给其他开发者\n" +
                                "• 参与项目开发"
                )
                .setPositiveButton("GitHub", (dialog, which) -> {
                    openGitHub();
                })
                .setNegativeButton("关闭", null);

        builder.create().show();
    }
    private void openGitHub() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/imxiaoc996/DeviceWarLock/issues"));
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity, "无法打开浏览器", Toast.LENGTH_SHORT).show();
        }
    }
    public void showSettingsDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle("设置")
                .setView(createSettingsView());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private View createSettingsView() {
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_settings, null);
        initSettingsControls(view);
        return view;
    }

    private void initSettingsControls(View view) {
        SwitchMaterial switchNoReport = view.findViewById(R.id.switch_no_report);
        switchNoReport.setChecked(getNoReportSetting());
        switchNoReport.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 当用户尝试开启"不上报"时，显示确认对话框
                new MaterialAlertDialogBuilder(activity)
                        .setTitle("功能说明")
                        .setMessage("开启\"设备信息不上报服务器\"后：\n\n" +
                                "• 将不会生成唯一设备指纹信息\n" +
                                "• 环境检测服务会减少服务端侧的识别逻辑\n" +
                                "• 所有检测仅在本地进行\n\n" +
                                "确定要开启此功能吗？")
                        .setPositiveButton("确定", (dialog, which) -> {
                            saveNoReportSetting(true);
                            Toast.makeText(activity, "设置已保存", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("取消", (dialog, which) -> {
                            // 如果用户取消，将开关状态改回
                            switchNoReport.setChecked(false);
                        })
                        .show();
            } else {
                // 当用户关闭"不上报"时，直接保存设置
                saveNoReportSetting(false);
                Toast.makeText(activity, "设置已保存", Toast.LENGTH_SHORT).show();
            }
        });

        // 重启应用按钮
        MaterialButton btnRestartApp = view.findViewById(R.id.btn_restart_app);
        btnRestartApp.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("重启应用")
                    .setMessage("确定要重启应用吗？")
                    .setPositiveButton("确定", (dialog, which) -> restartApp())
                    .setNegativeButton("取消", null)
                    .show();
        });

        // 清除数据按钮
        MaterialButton btnClearData = view.findViewById(R.id.btn_clear_data);
        btnClearData.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(activity)
                    .setTitle("清除数据")
                    .setMessage("此操作将清除所有已收集的数据，确定继续吗？")
                    .setPositiveButton("确定", (dialog, which) -> clearAllData())
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    // 保存不上报设置
    private void saveNoReportSetting(boolean noReport) {
        SharedPreferences prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("no_report", noReport).apply();
    }

    // 获取不上报设置
    private boolean getNoReportSetting() {
        SharedPreferences prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getBoolean("no_report", false);
    }

    // 重启应用
    private void restartApp() {
        Intent intent = activity.getPackageManager()
                .getLaunchIntentForPackage(activity.getPackageName());
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            activity.finish();
            Runtime.getRuntime().exit(0);
        }
    }

    // 清除数据
    private void clearAllData() {
        // 清除SharedPreferences
        activity.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .edit().clear().apply();

        // 清除文件
        File dir = activity.getFilesDir();
        deleteRecursive(dir);

        // 清除缓存
        activity.getCacheDir().delete();

        Toast.makeText(activity, "数据已清除", Toast.LENGTH_SHORT).show();

        // 延迟1秒后重启应用
        new Handler().postDelayed(this::restartApp, 1000);
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }
}
