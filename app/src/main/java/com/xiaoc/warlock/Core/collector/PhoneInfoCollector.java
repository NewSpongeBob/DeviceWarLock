package com.xiaoc.warlock.Core.collector;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Util.XLog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhoneInfoCollector extends BaseCollector {
    private static final String TAG = "PhoneInfoCollector";

    public PhoneInfoCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        collectPhoneInfo();     // a17
        collectDeviceIds();     // a52
    }

    /**
     * 收集手机相关信息，包括IMEI、IMSI、ICCID和手机号
     * 对于双卡设备，会分别收集每个卡槽的信息
     */
    private void collectPhoneInfo() {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm == null) {
                putFailedInfo("a17");
                return;
            }

            Map<String, String> phoneInfo = new LinkedHashMap<>();
            boolean hasValidValue = false;

            // 收集主卡信息
            hasValidValue |= collectMainSimInfo(tm, phoneInfo);
            
            // 收集双卡信息（如果支持）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                hasValidValue |= collectDualSimInfo(tm, phoneInfo);
            }

            // 检查是否有任何有效值
            if (hasValidValue) {
                putInfo("a17", phoneInfo);
            } else {
                putFailedInfo("a17");
            }

        } catch (Exception e) {
            putFailedInfo("a17");
            XLog.e(TAG, "Failed to collect phone info: " + e.getMessage());
        }
    }

    /**
     * 收集主卡信息
     */
    private boolean collectMainSimInfo(TelephonyManager tm, Map<String, String> phoneInfo) {
        boolean hasValidValue = false;

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

        return hasValidValue;
    }

    /**
     * 收集双卡信息
     */
    private boolean collectDualSimInfo(TelephonyManager tm, Map<String, String> phoneInfo) {
        boolean hasValidValue = false;

        try {
            SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            if (subscriptionManager != null) {
                List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
                if (subscriptionInfos != null) {
                    for (SubscriptionInfo info : subscriptionInfos) {
                        int slotIndex = info.getSimSlotIndex();
                        if (slotIndex >= 0) {
                            TelephonyManager subTm = tm.createForSubscriptionId(info.getSubscriptionId());
                            hasValidValue |= collectSimSlotInfo(tm, subTm, slotIndex, phoneInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect dual sim info: " + e.getMessage());
        }

        return hasValidValue;
    }

    /**
     * 收集指定卡槽的信息
     */
    private boolean collectSimSlotInfo(TelephonyManager tm, TelephonyManager subTm, int slotIndex, Map<String, String> phoneInfo) {
        boolean hasValidValue = false;

        // 获取 IMEI
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

        // 获取 IMSI
        try {
            String imsi = subTm.getSubscriberId();
            if (imsi != null && !imsi.isEmpty()) {
                hasValidValue = true;
            }
            phoneInfo.put("imsi_" + slotIndex, imsi != null ? imsi : "");
        } catch (SecurityException e) {
            phoneInfo.put("imsi_" + slotIndex, "");
        }

        // 获取 ICCID
        try {
            String iccid = subTm.getSimSerialNumber();
            if (iccid != null && !iccid.isEmpty()) {
                hasValidValue = true;
            }
            phoneInfo.put("iccid_" + slotIndex, iccid != null ? iccid : "");
        } catch (SecurityException e) {
            phoneInfo.put("iccid_" + slotIndex, "");
        }

        // 获取 Line1Number (手机号)
        try {
            String line1Number = subTm.getLine1Number();
            if (line1Number != null && !line1Number.isEmpty()) {
                hasValidValue = true;
            }
            phoneInfo.put("line1_number_" + slotIndex, line1Number != null ? line1Number : "");
        } catch (SecurityException e) {
            phoneInfo.put("line1_number_" + slotIndex, "");
        }

        return hasValidValue;
    }

    /**
     * 收集设备标识信息
     * u: uuid
     * a: ad_aaid
     * r: ReaperAssignedDeviceId
     * i: IMEI
     * m: mdm_uuid
     * p: ps_imei
     * o: op_security_uuid
     * s: ai_stored_imei
     * d: device_serial
     */
    private void collectDeviceIds() {
        try {
            Map<String, String> deviceIds = new LinkedHashMap<>();
            boolean hasValidValue = false;

            // 收集 UUID
            String uuid = Settings.System.getString(context.getContentResolver(), "uuid");
            deviceIds.put("u", uuid != null && !uuid.isEmpty() ? uuid : "-1");
            hasValidValue |= (uuid != null && !uuid.isEmpty());

            // 收集 ad_aaid (Advertising ID)
            String adId = getAdvertisingId();
            deviceIds.put("a", adId != null && !adId.isEmpty() ? adId : "-1");
            hasValidValue |= (adId != null && !adId.isEmpty());

            // 收集 ReaperAssignedDeviceId
            String reaperId = Settings.System.getString(context.getContentResolver(), "ReaperAssignedDeviceId");
            deviceIds.put("r", reaperId != null && !reaperId.isEmpty() ? reaperId : "-1");
            hasValidValue |= (reaperId != null && !reaperId.isEmpty());

            // 收集 IMEI
            String imei = getDeviceImei();
            deviceIds.put("i", imei != null && !imei.isEmpty() ? imei : "-1");
            hasValidValue |= (imei != null && !imei.isEmpty());

            // 收集 mdm_uuid
            String mdmUuid = Settings.System.getString(context.getContentResolver(), "mdm_uuid");
            deviceIds.put("m", mdmUuid != null && !mdmUuid.isEmpty() ? mdmUuid : "-1");
            hasValidValue |= (mdmUuid != null && !mdmUuid.isEmpty());

            // 收集 ps_imei
            String psImei = Settings.System.getString(context.getContentResolver(), "ps_imei");
            deviceIds.put("p", psImei != null && !psImei.isEmpty() ? psImei : "-1");
            hasValidValue |= (psImei != null && !psImei.isEmpty());

            // 收集 op_security_uuid
            String opUuid = Settings.System.getString(context.getContentResolver(), "op_security_uuid");
            deviceIds.put("o", opUuid != null && !opUuid.isEmpty() ? opUuid : "-1");
            hasValidValue |= (opUuid != null && !opUuid.isEmpty());

            // 收集 ai_stored_imei
            String aiImei = Settings.System.getString(context.getContentResolver(), "ai_stored_imei");
            deviceIds.put("s", aiImei != null && !aiImei.isEmpty() ? aiImei : "-1");
            hasValidValue |= (aiImei != null && !aiImei.isEmpty());

            // 收集 device_serial
            String deviceSerial = getDeviceSerial();
            deviceIds.put("d", deviceSerial != null && !deviceSerial.isEmpty() ? deviceSerial : "-1");
            hasValidValue |= (deviceSerial != null && !deviceSerial.isEmpty());

            // 如果所有值都为空，则返回-1
            if (hasValidValue) {
                putInfo("a52", deviceIds);
            } else {
                putInfo("a52", "-1");
            }

        } catch (Exception e) {
            XLog.e(TAG, "Failed to collect device IDs: " + e.getMessage());
            putFailedInfo("a52");
        }
    }

    /**
     * 获取设备IMEI
     */
    private String getDeviceImei() {
        try {
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    return tm.getImei();
                } else {
                    @SuppressWarnings("deprecation")
                    String imei = tm.getDeviceId();
                    return imei;
                }
            }
        } catch (SecurityException e) {
            XLog.e(TAG, "Failed to get IMEI: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取设备序列号
     */
    private String getDeviceSerial() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return Build.getSerial();
            } else {
                return Build.SERIAL;
            }
        } catch (SecurityException e) {
            XLog.e(TAG, "Failed to get device serial: " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取广告ID
     */
    private String getAdvertisingId() {
        try {
            AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(context);
            if (adInfo != null) {
                return adInfo.getId();
            }
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get advertising ID: " + e.getMessage());
        }
        return null;
    }
}