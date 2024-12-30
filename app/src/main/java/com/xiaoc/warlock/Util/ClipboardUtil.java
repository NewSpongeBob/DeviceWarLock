package com.xiaoc.warlock.Util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import com.xiaoc.warlock.ui.adapter.InfoItem;

public class ClipboardUtil {
    public static void copyInfoItemToClipboard(Context context, InfoItem item, boolean isEnvironmentInfo) {
        StringBuilder content = new StringBuilder();

        if (isEnvironmentInfo) {
            // 环境检测信息的复制格式
            content.append("【").append(item.getTitle()).append("】\n");
            for (InfoItem.DetailItem detail : item.getDetails()) {
                content.append("- ").append(detail.getKey())
                        .append(": ")
                        .append(detail.getValue())
                        .append("\n");
            }
        } else {
            // 设备指纹信息的复制格式
            content.append(item.getTitle()).append("\n");
            for (InfoItem.DetailItem detail : item.getDetails()) {
                content.append(detail.getKey())
                        .append("：")
                        .append(detail.getValue())
                        .append("\n");
            }
        }

        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(
                isEnvironmentInfo ? "环境检测信息" : "设备信息",
                content.toString()
        );

        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            String toastMsg = isEnvironmentInfo ? "已复制检测信息" : "已复制设备信息";
            Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show();
        }
    }
}