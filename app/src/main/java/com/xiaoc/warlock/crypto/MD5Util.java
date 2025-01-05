package com.xiaoc.warlock.crypto;

public class MD5Util {
    /**
     * String text = "Hello, World!";
     * String hash = MD5Util.md5(text);
     * System.out.println(hash);
     */
    private static final int[] SHIFT = {
            7, 12, 17, 22,
            5,  9, 14, 20,
            4, 11, 16, 23,
            6, 10, 15, 21
    };

    private static final int[] TABLE = new int[64];

    static {
        for (int i = 0; i < 64; i++) {
            TABLE[i] = (int)(long)((1L << 32) * Math.abs(Math.sin(i + 1)));
        }
    }

    public static String md5(String input) {
        byte[] message = input.getBytes();

        // 初始化变量
        int a0 = 0x67452301;
        int b0 = 0xefcdab89;
        int c0 = 0x98badcfe;
        int d0 = 0x10325476;

        // 处理消息
        int paddedLength = ((message.length + 8) / 64 + 1) * 64;
        byte[] padded = new byte[paddedLength];
        System.arraycopy(message, 0, padded, 0, message.length);
        padded[message.length] = (byte)0x80;

        long messageLengthBits = (long)message.length * 8;
        for (int i = 0; i < 8; i++) {
            padded[padded.length - 8 + i] = (byte)messageLengthBits;
            messageLengthBits >>>= 8;
        }

        // 主循环
        int[] words = new int[16];
        for (int i = 0; i < padded.length; i += 64) {
            for (int j = 0; j < 16; j++) {
                words[j] = ((padded[i + j * 4] & 0xff)) |
                        ((padded[i + j * 4 + 1] & 0xff) << 8) |
                        ((padded[i + j * 4 + 2] & 0xff) << 16) |
                        ((padded[i + j * 4 + 3] & 0xff) << 24);
            }

            int A = a0;
            int B = b0;
            int C = c0;
            int D = d0;

            // 64轮运算
            for (int j = 0; j < 64; j++) {
                int F = 0;
                int g = 0;

                if (j < 16) {
                    F = (B & C) | (~B & D);
                    g = j;
                } else if (j < 32) {
                    F = (D & B) | (~D & C);
                    g = (5 * j + 1) % 16;
                } else if (j < 48) {
                    F = B ^ C ^ D;
                    g = (3 * j + 5) % 16;
                } else {
                    F = C ^ (B | ~D);
                    g = (7 * j) % 16;
                }

                int temp = D;
                D = C;
                C = B;
                B = B + Integer.rotateLeft(A + F + TABLE[j] + words[g], SHIFT[j % 4 + (j / 16) * 4]);
                A = temp;
            }

            a0 += A;
            b0 += B;
            c0 += C;
            d0 += D;
        }

        // 转换为十六进制字符串
        byte[] digest = new byte[16];
        int[] ints = {a0, b0, c0, d0};
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                digest[i * 4 + j] = (byte)(ints[i] >>> (j * 8));
            }
        }

        StringBuilder result = new StringBuilder();
        for (byte b : digest) {
            result.append(String.format("%02x", b & 0xff));
        }
        return result.toString();
    }
}