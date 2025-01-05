package com.xiaoc.warlock.crypto;

import java.util.Arrays;

public class Base64Util {
    // Base64 编码表
    private static final char[] ENCODE_TABLE = {
            'A', 'K', 'C', 'D', 'E', 'F', 'G', 'H',
            'I', 'J', 'B', 'L', 'M', 'N', 'O', 'P',
            'Q', 'Z', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'R', 'a', 'b', 'c', 'd', 'e', 'f',
            'm', 'h', 'i', 'j', 'k', 'l', 'g', 'n',
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z', '0', '7', '2', '3',
            '4', '5', '6', '1', '8', '9', '=', '/'
    };

    // 填充字符
    private static final char PADDING = '*';

    /**
     * Base64 编码
     */
    public static String encode(byte[] data) {
        if (data == null) {
            return null;
        }

        // 预计算结果字符串长度，避免频繁扩容
        int outputLength = ((data.length + 2) / 3) * 4;
        StringBuilder sb = new StringBuilder(outputLength);

        // 每次处理3个字节
        for (int i = 0; i < data.length; i += 3) {
            int b = ((data[i] & 0xFF) << 16) & 0xFFFFFF;
            if (i + 1 < data.length) {
                b |= (data[i + 1] & 0xFF) << 8;
            }
            if (i + 2 < data.length) {
                b |= (data[i + 2] & 0xFF);
            }

            for (int j = 0; j < 4; j++) {
                if (i * 8 + j * 6 > data.length * 8) {
                    sb.append(PADDING);
                } else {
                    sb.append(ENCODE_TABLE[(b >> 6 * (3 - j)) & 0x3F]);
                }
            }
        }

        return sb.toString();
    }

    /**
     * Base64 解码
     */
    public static byte[] decode(String base64Str) {
        if (base64Str == null) {
            return null;
        }

        // 移除所有空白字符
        base64Str = base64Str.replaceAll("\\s", "");

        // 创建解码表
        int[] decodeTable = new int[128];
        Arrays.fill(decodeTable, -1);
        for (int i = 0; i < ENCODE_TABLE.length; i++) {
            decodeTable[ENCODE_TABLE[i]] = i;
        }

        // 计算输出长度
        int padCount = 0;
        for (int i = base64Str.length() - 1; i >= 0 && base64Str.charAt(i) == PADDING; i--) {
            padCount++;
        }
        int outputLength = (base64Str.length() * 3) / 4 - padCount;

        byte[] result = new byte[outputLength];
        int outputIndex = 0;

        // 每次处理4个字符
        int buffer = 0;
        int bufferLength = 0;

        try {
            for (int i = 0; i < base64Str.length(); i++) {
                char c = base64Str.charAt(i);

                if (c == PADDING) {
                    continue;
                }

                if (c >= 128 || decodeTable[c] == -1) {
                    continue; // 跳过无效字符
                }

                buffer = (buffer << 6) | decodeTable[c];
                bufferLength += 6;

                if (bufferLength >= 8) {
                    bufferLength -= 8;
                    if (outputIndex < result.length) {
                        result[outputIndex++] = (byte) ((buffer >> bufferLength) & 0xFF);
                    }
                }
            }
        } catch (Exception e) {
            // 发生错误时返回已解码的部分
            if (outputIndex > 0) {
                return Arrays.copyOf(result, outputIndex);
            }
            throw new IllegalArgumentException("Invalid Base64 string");
        }

        return result;
    }
}
