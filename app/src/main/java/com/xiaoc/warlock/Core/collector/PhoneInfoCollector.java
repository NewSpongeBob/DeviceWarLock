package com.xiaoc.warlock.Core.collector;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XLog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhoneInfoCollector extends BaseCollector {

    public PhoneInfoCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                putFailedInfo("a17");
                return;
            }

            Map<String, String> phoneInfo = new LinkedHashMap<>();
            boolean hasValidValue = false;  // 标记是否有非空值

            // 获取 IMEI
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    String imei = tm.getImei();
                    if (imei != null && !imei.isEmpty()) {
                        hasValidValue = true;
                    }
                    phoneInfo.put("imei", imei != null ? imei : "");
                } else {
                    @SuppressWarnings("deprecation")
                    String imei = tm.getDeviceId();
                    if (imei != null && !imei.isEmpty()) {
                        hasValidValue = true;
                    }
                    phoneInfo.put("imei", imei != null ? imei : "");
                }
            } catch (SecurityException e) {
                phoneInfo.put("imei", "");
            }

            // 获取 IMSI
            try {
                String imsi = tm.getSubscriberId();
                if (imsi != null && !imsi.isEmpty()) {
                    hasValidValue = true;
                }
                phoneInfo.put("imsi", imsi != null ? imsi : "");
            } catch (SecurityException e) {
                phoneInfo.put("imsi", "");
            }

            // 获取 ICCID
            try {
                String iccid = tm.getSimSerialNumber();
                if (iccid != null && !iccid.isEmpty()) {
                    hasValidValue = true;
                }
                phoneInfo.put("iccid", iccid != null ? iccid : "");
            } catch (SecurityException e) {
                phoneInfo.put("iccid", "");
            }

            // 获取 Line1Number (手机号)
            try {
                String line1Number = tm.getLine1Number();
                if (line1Number != null && !line1Number.isEmpty()) {
                    hasValidValue = true;
                }
                phoneInfo.put("line1_number", line1Number != null ? line1Number : "");
            } catch (SecurityException e) {
                phoneInfo.put("line1_number", "");
            }

            // 如果是双卡设备，尝试获取第二个 SIM 卡的信息
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (subscriptionManager != null) {
                    List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
                    if (subscriptionInfos != null) {
                        for (SubscriptionInfo info : subscriptionInfos) {
                            int slotIndex = info.getSimSlotIndex();
                            if (slotIndex >= 0) {
                                TelephonyManager subTm = tm.createForSubscriptionId(info.getSubscriptionId());

                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        String imei = tm.getImei(slotIndex);
                                        if (imei != null && !imei.isEmpty()) {
                                            hasValidValue = true;
                                        }
                                        phoneInfo.put("imei_" + slotIndex, imei != null ? imei : "");
                                    }
                                } catch (SecurityException e) {
                                    phoneInfo.put("imei_" + slotIndex, "");
                                }

                                try {
                                    String imsi = subTm.getSubscriberId();
                                    if (imsi != null && !imsi.isEmpty()) {
                                        hasValidValue = true;
                                    }
                                    phoneInfo.put("imsi_" + slotIndex, imsi != null ? imsi : "");
                                } catch (SecurityException e) {
                                    phoneInfo.put("imsi_" + slotIndex, "");
                                }

                                try {
                                    String iccid = subTm.getSimSerialNumber();
                                    if (iccid != null && !iccid.isEmpty()) {
                                        hasValidValue = true;
                                    }
                                    phoneInfo.put("iccid_" + slotIndex, iccid != null ? iccid : "");
                                } catch (SecurityException e) {
                                    phoneInfo.put("iccid_" + slotIndex, "");
                                }

                                try {
                                    String line1Number = subTm.getLine1Number();
                                    if (line1Number != null && !line1Number.isEmpty()) {
                                        hasValidValue = true;
                                    }
                                    phoneInfo.put("line1_number_" + slotIndex, line1Number != null ? line1Number : "");
                                } catch (SecurityException e) {
                                    phoneInfo.put("line1_number_" + slotIndex, "");
                                }
                            }
                        }
                    }
                }
            }

            // 检查是否有任何有效值
            if (hasValidValue) {
                putInfo("a17", phoneInfo);
            } else {
                putFailedInfo("a17");
            }

        } catch (Exception e) {
            putFailedInfo("a17");
            XLog.e("PhoneInfoCollector", "Failed to collect phone info: " + e.getMessage());
        }
    }
}