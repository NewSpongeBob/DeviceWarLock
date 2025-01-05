package com.xiaoc.warlock.crypto;

import com.xiaoc.warlock.Util.XLog;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class EncryptUtil {
    private String plaintext = "";
    private String base64Result;
    private String uuidMd5;
    private String uuid;
    private String timeStamp;
    private final SecureRandom secureRandom;
    public String result;

    public EncryptUtil(String plaintext){
        this.plaintext = plaintext;
        this.secureRandom = new SecureRandom();
        doEncrypt();
    }
    private void doEncrypt() {
        // 1. 先生成所需的基础数据
        generateBaseData();

        // 2. 生成key和iv
        byte[] key = generateKey();
        byte[] iv = generateIV();
        XLog.d("UUID:"+uuid);
        XLog.d("Key (hex): " + bytesToHex(key));
        XLog.d("Key (string): " + new String(key, StandardCharsets.UTF_8));
        XLog.d("IV (hex): " + bytesToHex(iv));
        XLog.d("IV (string): " + new String(iv, StandardCharsets.UTF_8));
        // 3. 使用AES加密
        byte[] encrypted = encryptData(key, iv);

        // 4. 重排结果
        result = rearrangeResult(Base64Util.encode(encrypted));
        XLog.d("Final result: " + result);
    }

    private void generateBaseData() {
        // Base64编码原文
        base64Result = Base64Util.encode(plaintext.getBytes(StandardCharsets.UTF_8));

        // 生成32位UUID
        uuid = generateHex(32);

        // 计算UUID的MD5
        uuidMd5 = MD5Util.md5(uuid);

        // 获取10位时间戳
        timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
    }

    public String generateHex(int length) {
        byte[] bytes = new byte[length / 2];
        secureRandom.nextBytes(bytes);
        return bytesToHex(bytes);
    }
    private byte[] generateKey() {
        // 1. 将UUID间隔取值，得到两组数据
        char[] first8 = new char[8];
        char[] second8 = new char[8];
        int firstIndex = 0, secondIndex = 0;

        for(int i = 0; i < uuid.length(); i++) {
            if(i % 2 == 0 && firstIndex < 8) {
                first8[firstIndex++] = uuid.charAt(i);
            } else if(secondIndex < 8) {
                second8[secondIndex++] = uuid.charAt(i);
            }
        }

        // 2. 生成16字节的key
        byte[] key = new byte[16];

        // 前8位直接使用first8
        for(int i = 0; i < 8; i++) {
            key[i] = (byte) first8[i];
        }

        // 后8位使用异或结果
        for(int i = 0; i < 8; i++) {
            key[i + 8] = (byte) (first8[i] ^ second8[i]);
        }

        return key;
    }

    private byte[] generateIV() {
        String combined = uuidMd5 + timeStamp; // 42位

        StringBuilder frontPart = new StringBuilder();  // 存储前10位
        StringBuilder backPart = new StringBuilder();   // 存储后11位

        // 2. 从前往后每隔一位取10位
        int frontCount = 0;
        for(int i = 0; frontCount < 10 && i < combined.length(); i += 2) {
            frontPart.append(combined.charAt(i));
            frontCount++;
        }

        // 3. 从后往前每隔一位取11位
        int backCount = 0;
        for(int i = combined.length() - 1; backCount < 11 && i >= 0; i -= 2) {
            backPart.append(combined.charAt(i));
            backCount++;
        }

        // 4. 位移操作
        byte[] iv = new byte[16];
        Arrays.fill(iv, (byte)0);

        // 处理前10位（左移1位）
        String frontStr = frontPart.toString();
        for(int i = 0; i < 8; i++) {
            if(i < frontStr.length()) {
                iv[i] = (byte)((frontStr.charAt(i) << 1) & 0xFF);
            }
        }

        // 处理后11位（右移2位）
        String backStr = backPart.toString();
        for(int i = 0; i < 8; i++) {
            if(i < backStr.length()) {
                iv[i + 8] = (byte)((backStr.charAt(i) >> 2) & 0xFF);
            }
        }

        return iv;
    }

    private byte[] encryptData(byte[] key, byte[] iv) {
        AESUtil aes = new AESUtil();
        return aes.encrypt(base64Result.getBytes(), key,iv);
    }

    private String rearrangeResult(String encryptedBase64) {
        // 1. 拼接MD5、时间戳和UUID
        String combined = uuidMd5 + timeStamp + uuid;
        XLog.d("combined:" + combined);

        // 2. 计算首尾位和
        int sum = Character.getNumericValue(combined.charAt(0)) +
                Character.getNumericValue(combined.charAt(combined.length() - 1));
        XLog.d("sum:" + sum);

        // 3. 将组合字符串按sum值插入到加密结果中
        StringBuilder result = new StringBuilder(encryptedBase64);
        int currentPos = 0;

        // 遍历组合字符串的每个字符
        for(int i = 0; i < combined.length(); i++) {
            currentPos = (currentPos + sum) % result.length();
            // 在当前位置插入字符
            result.insert(currentPos, combined.charAt(i));
        }

        return result.toString();
    }
    private  String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
}
