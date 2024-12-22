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
        getSignatureX509(); //a47
        getSignatureMD5(); //a48
    }
    private void getSignatureX509(){
        try {
            // 方法1: 常规PackageManager方式
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            String normalSignature = packageInfo.signatures[0].toCharsString();

            // 方法2: Binder方式
            String binderSignature = getSignatureViaBinder();

            Map<String, Object> signatureInfo = new LinkedHashMap<>();
            if (normalSignature != null && binderSignature != null) {
                if (normalSignature.equals(binderSignature)) {
                    putInfo("a47", normalSignature);
                } else {
                    signatureInfo.put("pm", normalSignature);
                    signatureInfo.put("binder", binderSignature);
                    putInfo("a47", signatureInfo);
                }
            }
        } catch (Exception e) {
            putFailedInfo("a47");

            XLog.e(TAG, "Failed to collect signature info", e);
        }
    }
    private void getSignatureMD5(){
        try {
            // 方法1: 常规PackageManager方式获取签名
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(),
                    PackageManager.GET_SIGNATURES
            );
            String normalMD5 = getSignatureMD5(packageInfo.signatures[0]);

            // 方法2: Binder方式获取签名
            String binderMD5 = getSignatureMD5ViaBinder();

            Map<String, Object> fingerprintInfo = new LinkedHashMap<>();

            if (normalMD5 != null && binderMD5 != null) {
                if (normalMD5.equals(binderMD5)) {
                    putInfo("a48", normalMD5);
                } else {
                    fingerprintInfo.put("pm", normalMD5);
                    fingerprintInfo.put("binder", binderMD5);
                    putInfo("a48", fingerprintInfo);
                }
            }
        } catch (Exception e) {
            putFailedInfo("a48");

            XLog.e(TAG, "Failed to collect signature fingerprint", e);
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
     * 通过Binder获取签名MD5指纹
     */
    private String getSignatureMD5ViaBinder() {
        Parcel data = null;
        Parcel reply = null;

        try {
            // 获取PackageManager对象
            PackageManager pm = context.getPackageManager();

            // 获取IPackageManager的Binder对象
            Class<?> pmClass = Class.forName("android.app.ApplicationPackageManager");
            Field mPmField = pmClass.getDeclaredField("mPM");
            mPmField.setAccessible(true);
            Object mPM = mPmField.get(pm);

            Field mRemoteField = mPM.getClass().getDeclaredField("mRemote");
            mRemoteField.setAccessible(true);
            IBinder binder = (IBinder) mRemoteField.get(mPM);

            if (binder == null) return null;

            // 准备Parcel数据
            data = Parcel.obtain();
            reply = Parcel.obtain();

            // 写入接口标识
            data.writeInterfaceToken("android.content.pm.IPackageManager");

            // 写入参数
            data.writeString(context.getPackageName());
            data.writeInt(PackageManager.GET_SIGNATURES);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                data.writeInt(context.getAttributionTag() != null ? 1 : 0);
                if (context.getAttributionTag() != null) {
                    data.writeString(context.getAttributionTag());
                }
            }

            data.writeInt(Process.myUid() / 100000);

            // 获取TRANSACTION_getPackageInfo值
            Class<?> stubClass = Class.forName("android.content.pm.IPackageManager$Stub");
            Field transactionField = stubClass.getDeclaredField("TRANSACTION_getPackageInfo");
            transactionField.setAccessible(true);
            int transaction = transactionField.getInt(null);

            // 执行Binder调用
            binder.transact(transaction, data, reply, 0);
            reply.readException();

            // 读取返回的PackageInfo对象
            PackageInfo packageInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                packageInfo = reply.readTypedObject(PackageInfo.CREATOR);
            } else {
                packageInfo = PackageInfo.CREATOR.createFromParcel(reply);
            }

            if (packageInfo != null && packageInfo.signatures != null &&
                    packageInfo.signatures.length > 0) {
                return getSignatureMD5(packageInfo.signatures[0]);
            }

            return null;
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get signature MD5 via binder", e);
            return null;
        } finally {
            if (data != null) data.recycle();
            if (reply != null) reply.recycle();
        }
    }
    /**
     * 通过Binder获取签名信息
     */
    private String getSignatureViaBinder() {
        Parcel data = null;
        Parcel reply = null;

        try {
            // 获取PackageManager对象
            PackageManager pm = context.getPackageManager();

            // 获取IPackageManager的Binder对象
            Class<?> pmClass = Class.forName("android.app.ApplicationPackageManager");
            Field mPmField = pmClass.getDeclaredField("mPM");
            mPmField.setAccessible(true);
            Object mPM = mPmField.get(pm);

            Field mRemoteField = mPM.getClass().getDeclaredField("mRemote");
            mRemoteField.setAccessible(true);
            IBinder binder = (IBinder) mRemoteField.get(mPM);

            if (binder == null) return null;

            // 准备Parcel数据
            data = Parcel.obtain();
            reply = Parcel.obtain();

            // 写入接口标识
            data.writeInterfaceToken("android.content.pm.IPackageManager");

            // 写入参数
            data.writeString(context.getPackageName());
            data.writeInt(PackageManager.GET_SIGNATURES);  // 修改为writeInt

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                data.writeInt(context.getAttributionTag() != null ? 1 : 0);
                if (context.getAttributionTag() != null) {
                    data.writeString(context.getAttributionTag());
                }
            }

            data.writeInt(Process.myUid() / 100000);  // userId

            // 获取TRANSACTION_getPackageInfo值
            Class<?> stubClass = Class.forName("android.content.pm.IPackageManager$Stub");
            Field transactionField = stubClass.getDeclaredField("TRANSACTION_getPackageInfo");
            transactionField.setAccessible(true);
            int transaction = transactionField.getInt(null);

            // 执行Binder调用
            binder.transact(transaction, data, reply, 0);
            reply.readException();

            // 读取返回的PackageInfo对象
            PackageInfo packageInfo;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                packageInfo = reply.readTypedObject(PackageInfo.CREATOR);
            } else {
                packageInfo = PackageInfo.CREATOR.createFromParcel(reply);
            }

            if (packageInfo != null && packageInfo.signatures != null &&
                    packageInfo.signatures.length > 0) {
                return packageInfo.signatures[0].toCharsString();
            }

            return null;
        } catch (Exception e) {
            XLog.e(TAG, "Failed to get signature via binder", e);
            return null;
        } finally {
            if (data != null) data.recycle();
            if (reply != null) reply.recycle();
        }
    }




}
