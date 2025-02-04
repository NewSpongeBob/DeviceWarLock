package com.xiaoc.warlock.Core.collector;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.os.IBinder;
import android.os.Parcel;

import com.xiaoc.warlock.Core.BaseCollector;
import com.xiaoc.warlock.Core.BaseDetector;
import com.xiaoc.warlock.Util.XLog;
import android.os.Process;
import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

public class SignatureCollector extends BaseCollector {
    private static final String TAG = "SignatureDetector";
    private static int TRANSACTION_getPackageInfo = -1;

    public SignatureCollector(Context context) {
        super(context);
    }

    @Override
    public void collect() {
        getSignatureX509(); //a47、a48
    }
    private void getSignatureX509(){
        try {
            // 方法1: 常规PackageManager方式
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            Signature normalSignature = packageInfo.signatures[0];

            // 方法2: Binder方式
            Signature binderSignature = getAppSignatureForBinder(context);

            Map<String, Object> signatureInfo = new LinkedHashMap<>();
            Map<String, Object> md5signatureInfo = new LinkedHashMap<>();
                if (normalSignature.equals(binderSignature)) {
                    putInfo("a47", normalSignature.toCharsString());

                    putInfo("a48", getSignatureMD5(normalSignature));

                } else {
                    signatureInfo.put("pm",  normalSignature != null ? normalSignature.toCharsString() : "-1");
                    signatureInfo.put("binder", binderSignature != null ? binderSignature.toCharsString() : "-1");
                    putInfo("a47", signatureInfo);
                    md5signatureInfo.put("pm", normalSignature != null ? getSignatureMD5(normalSignature) : "-1");
                    md5signatureInfo.put("binder", binderSignature != null ? getSignatureMD5(binderSignature) : "-1");
                
                    putInfo("a48", md5signatureInfo);
                }
            
        } catch (Exception e) {
            putFailedInfo("a47");
            putFailedInfo("a48");

            XLog.e(TAG, "Failed to collect signature info", e);
        }
    }
    /**
     * 获取签名的MD5指纹
     */
    private String getSignatureMD5(Signature signature) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] signatureBytes = signature.toByteArray();
            md.update(signatureBytes);
            byte[] digest = md.digest();

            // 转换为16进制字符串
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02X", b));
            }
            return sb.toString();
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get signature MD5", e);
            return null;
        }
    }


    /**
     * 通过Binder获取签名信息
     */
    public static Signature getAppSignatureForBinder(Context context) {
        Signature signature = null;
        try {
            PackageManager packageManager = context.getPackageManager();
            // 通过反射获取 mPM 字段
            Field mPmField = packageManager.getClass().getDeclaredField("mPM");
            mPmField.setAccessible(true);
            Object iPackageManager = mPmField.get(packageManager);

            // 获取 mRemote
            Field mRemoteField = iPackageManager.getClass().getDeclaredField("mRemote");
            mRemoteField.setAccessible(true);
            IBinder binder = (IBinder) mRemoteField.get(iPackageManager);

            // 准备Parcel数据
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();

            try {
                // 写入接口标识
                data.writeInterfaceToken("android.content.pm.IPackageManager");
                // 写入包名
                data.writeString(context.getPackageName());
                // 写入获取签名的flag
                data.writeLong(PackageManager.GET_SIGNATURES);
                // 写入当前进程uid
                data.writeInt(Process.myUid());

                // 执行transact调用
                binder.transact(getTransactionId(), data, reply, 0);
                // 读取异常信息(如果有)
                reply.readException();
                // 读取返回的PackageInfo对象
                PackageInfo packageInfo = reply.readTypedObject(PackageInfo.CREATOR);

                if (packageInfo != null && packageInfo.signatures != null && packageInfo.signatures.length > 0) {
                    signature = packageInfo.signatures[0];
                }
            } finally {
                // 回收Parcel
                data.recycle();
                reply.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return signature;
    }

    // 获取TRANSACTION_getPackageInfo的值
    private static int getTransactionId() {
        try {
            Class<?> stubClass = Class.forName("android.content.pm.IPackageManager$Stub");
            Field field = stubClass.getDeclaredField("TRANSACTION_getPackageInfo");
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }



}
