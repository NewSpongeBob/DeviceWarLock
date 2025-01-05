package com.xiaoc.warlock.crypto;


import java.util.Arrays;

public class AESUtil {
    // AES S-box
    private static final byte[] SBOX = {
            (byte)0x63, (byte)0x7c, (byte)0x77, (byte)0x7b, (byte)0xf2, (byte)0x6b, (byte)0x6f, (byte)0xc5,
            (byte)0x30, (byte)0x01, (byte)0x67, (byte)0x2b, (byte)0xfe, (byte)0xd7, (byte)0xab, (byte)0x76,
            (byte)0xca, (byte)0x82, (byte)0xc9, (byte)0x7d, (byte)0xfa, (byte)0x59, (byte)0x47, (byte)0xf0,
            (byte)0xad, (byte)0xd4, (byte)0xa2, (byte)0xaf, (byte)0x9c, (byte)0xa4, (byte)0x72, (byte)0xc0,
            (byte)0xb7, (byte)0xfd, (byte)0x93, (byte)0x26, (byte)0x36, (byte)0x3f, (byte)0xf7, (byte)0xcc,
            (byte)0x34, (byte)0xa5, (byte)0xe5, (byte)0xf1, (byte)0x71, (byte)0xd8, (byte)0x31, (byte)0x15,
            (byte)0x04, (byte)0xc7, (byte)0x23, (byte)0xc3, (byte)0x18, (byte)0x96, (byte)0x05, (byte)0x9a,
            (byte)0x07, (byte)0x12, (byte)0x80, (byte)0xe2, (byte)0xeb, (byte)0x27, (byte)0xb2, (byte)0x75,
            (byte)0x09, (byte)0x83, (byte)0x2c, (byte)0x1a, (byte)0x1b, (byte)0x6e, (byte)0x5a, (byte)0xa0,
            (byte)0x52, (byte)0x3b, (byte)0xd6, (byte)0xb3, (byte)0x29, (byte)0xe3, (byte)0x2f, (byte)0x84,
            (byte)0x53, (byte)0xd1, (byte)0x00, (byte)0xed, (byte)0x20, (byte)0xfc, (byte)0xb1, (byte)0x5b,
            (byte)0x6a, (byte)0xcb, (byte)0xbe, (byte)0x39, (byte)0x4a, (byte)0x4c, (byte)0x58, (byte)0xcf,
            (byte)0xd0, (byte)0xef, (byte)0xaa, (byte)0xfb, (byte)0x43, (byte)0x4d, (byte)0x33, (byte)0x85,
            (byte)0x45, (byte)0xf9, (byte)0x02, (byte)0x7f, (byte)0x50, (byte)0x3c, (byte)0x9f, (byte)0xa8,
            (byte)0x51, (byte)0xa3, (byte)0x40, (byte)0x8f, (byte)0x92, (byte)0x9d, (byte)0x38, (byte)0xf5,
            (byte)0xbc, (byte)0xb6, (byte)0xda, (byte)0x21, (byte)0x10, (byte)0xff, (byte)0xf3, (byte)0xd2,
            (byte)0xcd, (byte)0x0c, (byte)0x13, (byte)0xec, (byte)0x5f, (byte)0x97, (byte)0x44, (byte)0x17,
            (byte)0xc4, (byte)0xa7, (byte)0x7e, (byte)0x3d, (byte)0x64, (byte)0x5d, (byte)0x19, (byte)0x73,
            (byte)0x60, (byte)0x81, (byte)0x4f, (byte)0xdc, (byte)0x22, (byte)0x2a, (byte)0x90, (byte)0x88,
            (byte)0x46, (byte)0xee, (byte)0xb8, (byte)0x14, (byte)0xde, (byte)0x5e, (byte)0x0b, (byte)0xdb,
            (byte)0xe0, (byte)0x32, (byte)0x3a, (byte)0x0a, (byte)0x49, (byte)0x06, (byte)0x24, (byte)0x5c,
            (byte)0xc2, (byte)0xd3, (byte)0xac, (byte)0x62, (byte)0x91, (byte)0x95, (byte)0xe4, (byte)0x79,
            (byte)0xe7, (byte)0xc8, (byte)0x37, (byte)0x6d, (byte)0x8d, (byte)0xd5, (byte)0x4e, (byte)0xa9,
            (byte)0x6c, (byte)0x56, (byte)0xf4, (byte)0xea, (byte)0x65, (byte)0x7a, (byte)0xae, (byte)0x08,
            (byte)0xba, (byte)0x78, (byte)0x25, (byte)0x2e, (byte)0x1c, (byte)0xa6, (byte)0xb4, (byte)0xc6,
            (byte)0xe8, (byte)0xdd, (byte)0x74, (byte)0x1f, (byte)0x4b, (byte)0xbd, (byte)0x8b, (byte)0x8a,
            (byte)0x70, (byte)0x3e, (byte)0xb5, (byte)0x66, (byte)0x48, (byte)0x03, (byte)0xf6, (byte)0x0e,
            (byte)0x61, (byte)0x35, (byte)0x57, (byte)0xb9, (byte)0x86, (byte)0xc1, (byte)0x1d, (byte)0x9e,
            (byte)0xe1, (byte)0xf8, (byte)0x98, (byte)0x11, (byte)0x69, (byte)0xd9, (byte)0x8e, (byte)0x94,
            (byte)0x9b, (byte)0x1e, (byte)0x87, (byte)0xe9, (byte)0xce, (byte)0x55, (byte)0x28, (byte)0xdf,
            (byte)0x8c, (byte)0xa1, (byte)0x89, (byte)0x0d, (byte)0xbf, (byte)0xe6, (byte)0x42, (byte)0x68,
            (byte)0x41, (byte)0x99, (byte)0x2d, (byte)0x0f, (byte)0xb0, (byte)0x54, (byte)0xbb, (byte)0x16
    };
    private static final byte[] INV_SBOX = {
            (byte)0x52, (byte)0x09, (byte)0x6A, (byte)0xD5, (byte)0x30, (byte)0x36, (byte)0xA5, (byte)0x38,
            (byte)0xBF, (byte)0x40, (byte)0xA3, (byte)0x9E, (byte)0x81, (byte)0xF3, (byte)0xD7, (byte)0xFB,
            (byte)0x7C, (byte)0xE3, (byte)0x39, (byte)0x82, (byte)0x9B, (byte)0x2F, (byte)0xFF, (byte)0x87,
            (byte)0x34, (byte)0x8E, (byte)0x43, (byte)0x44, (byte)0xC4, (byte)0xDE, (byte)0xE9, (byte)0xCB,
            (byte)0x54, (byte)0x7B, (byte)0x94, (byte)0x32, (byte)0xA6, (byte)0xC2, (byte)0x23, (byte)0x3D,
            (byte)0xEE, (byte)0x4C, (byte)0x95, (byte)0x0B, (byte)0x42, (byte)0xFA, (byte)0xC3, (byte)0x4E,
            (byte)0x08, (byte)0x2E, (byte)0xA1, (byte)0x66, (byte)0x28, (byte)0xD9, (byte)0x24, (byte)0xB2,
            (byte)0x76, (byte)0x5B, (byte)0xA2, (byte)0x49, (byte)0x6D, (byte)0x8B, (byte)0xD1, (byte)0x25,
            (byte)0x72, (byte)0xF8, (byte)0xF6, (byte)0x64, (byte)0x86, (byte)0x68, (byte)0x98, (byte)0x16,
            (byte)0xD4, (byte)0xA4, (byte)0x5C, (byte)0xCC, (byte)0x5D, (byte)0x65, (byte)0xB6, (byte)0x92,
            (byte)0x6C, (byte)0x70, (byte)0x48, (byte)0x50, (byte)0xFD, (byte)0xED, (byte)0xB9, (byte)0xDA,
            (byte)0x5E, (byte)0x15, (byte)0x46, (byte)0x57, (byte)0xA7, (byte)0x8D, (byte)0x9D, (byte)0x84,
            (byte)0x90, (byte)0xD8, (byte)0xAB, (byte)0x00, (byte)0x8C, (byte)0xBC, (byte)0xD3, (byte)0x0A,
            (byte)0xF7, (byte)0xE4, (byte)0x58, (byte)0x05, (byte)0xB8, (byte)0xB3, (byte)0x45, (byte)0x06,
            (byte)0xD0, (byte)0x2C, (byte)0x1E, (byte)0x8F, (byte)0xCA, (byte)0x3F, (byte)0x0F, (byte)0x02,
            (byte)0xC1, (byte)0xAF, (byte)0xBD, (byte)0x03, (byte)0x01, (byte)0x13, (byte)0x8A, (byte)0x6B,
            (byte)0x3A, (byte)0x91, (byte)0x11, (byte)0x41, (byte)0x4F, (byte)0x67, (byte)0xDC, (byte)0xEA,
            (byte)0x97, (byte)0xF2, (byte)0xCF, (byte)0xCE, (byte)0xF0, (byte)0xB4, (byte)0xE6, (byte)0x73,
            (byte)0x96, (byte)0xAC, (byte)0x74, (byte)0x22, (byte)0xE7, (byte)0xAD, (byte)0x35, (byte)0x85,
            (byte)0xE2, (byte)0xF9, (byte)0x37, (byte)0xE8, (byte)0x1C, (byte)0x75, (byte)0xDF, (byte)0x6E,
            (byte)0x47, (byte)0xF1, (byte)0x1A, (byte)0x71, (byte)0x1D, (byte)0x29, (byte)0xC5, (byte)0x89,
            (byte)0x6F, (byte)0xB7, (byte)0x62, (byte)0x0E, (byte)0xAA, (byte)0x18, (byte)0xBE, (byte)0x1B,
            (byte)0xFC, (byte)0x56, (byte)0x3E, (byte)0x4B, (byte)0xC6, (byte)0xD2, (byte)0x79, (byte)0x20,
            (byte)0x9A, (byte)0xDB, (byte)0xC0, (byte)0xFE, (byte)0x78, (byte)0xCD, (byte)0x5A, (byte)0xF4,
            (byte)0x1F, (byte)0xDD, (byte)0xA8, (byte)0x33, (byte)0x88, (byte)0x07, (byte)0xC7, (byte)0x31,
            (byte)0xB1, (byte)0x12, (byte)0x10, (byte)0x59, (byte)0x27, (byte)0x80, (byte)0xEC, (byte)0x5F,
            (byte)0x60, (byte)0x51, (byte)0x7F, (byte)0xA9, (byte)0x19, (byte)0xB5, (byte)0x4A, (byte)0x0D,
            (byte)0x2D, (byte)0xE5, (byte)0x7A, (byte)0x9F, (byte)0x93, (byte)0xC9, (byte)0x9C, (byte)0xEF,
            (byte)0xA0, (byte)0xE0, (byte)0x3B, (byte)0x4D, (byte)0xAE, (byte)0x2A, (byte)0xF5, (byte)0xB0,
            (byte)0xC8, (byte)0xEB, (byte)0xBB, (byte)0x3C, (byte)0x83, (byte)0x53, (byte)0x99, (byte)0x61,
            (byte)0x17, (byte)0x2B, (byte)0x04, (byte)0x7E, (byte)0xBA, (byte)0x77, (byte)0xD6, (byte)0x26,
            (byte)0xE1, (byte)0x69, (byte)0x14, (byte)0x63, (byte)0x55, (byte)0x21, (byte)0x0C, (byte)0x7D
    };
    // 轮常量
    private static final int[] RCON = {
            0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36
    };

    // 分组大小(16字节)
    private static final int BLOCK_SIZE = 16;

    // 密钥扩展
    private int[] expandKey(byte[] key) {
        int Nk = key.length / 4;
        int Nr = Nk + 6;
        int[] w = new int[4 * (Nr + 1)];

        int temp;
        int i = 0;

        while (i < Nk) {
            w[i] = ((key[4*i] & 0xff) << 24) |
                    ((key[4*i+1] & 0xff) << 16) |
                    ((key[4*i+2] & 0xff) << 8) |
                    (key[4*i+3] & 0xff);
            i++;
        }

        i = Nk;
        while (i < w.length) {
            temp = w[i-1];
            if (i % Nk == 0) {
                temp = subWord(rotWord(temp)) ^ RCON[i/Nk - 1];
            }
            w[i] = w[i-Nk] ^ temp;
            i++;
        }

        return w;
    }

    // SubBytes 变换
    private void subBytes(byte[][] state) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                state[i][j] = (byte) SBOX[state[i][j] & 0xff];
            }
        }
    }

    // ShiftRows 变换
    private void shiftRows(byte[][] state) {
        byte temp;

        // 第二行左移一位
        temp = state[1][0];
        state[1][0] = state[1][1];
        state[1][1] = state[1][2];
        state[1][2] = state[1][3];
        state[1][3] = temp;

        // 第三行左移两位
        temp = state[2][0];
        state[2][0] = state[2][2];
        state[2][2] = temp;
        temp = state[2][1];
        state[2][1] = state[2][3];
        state[2][3] = temp;

        // 第四行左移三位
        temp = state[3][3];
        state[3][3] = state[3][2];
        state[3][2] = state[3][1];
        state[3][1] = state[3][0];
        state[3][0] = temp;
    }

    // MixColumns 变换
    private void mixColumns(byte[][] state) {
        byte[] temp = new byte[4];

        for (int c = 0; c < 4; c++) {
            for (int i = 0; i < 4; i++) {
                temp[i] = state[i][c];
            }

            state[0][c] = (byte)(gmul(0x02, temp[0]) ^ gmul(0x03, temp[1]) ^ temp[2] ^ temp[3]);
            state[1][c] = (byte)(temp[0] ^ gmul(0x02, temp[1]) ^ gmul(0x03, temp[2]) ^ temp[3]);
            state[2][c] = (byte)(temp[0] ^ temp[1] ^ gmul(0x02, temp[2]) ^ gmul(0x03, temp[3]));
            state[3][c] = (byte)(gmul(0x03, temp[0]) ^ temp[1] ^ temp[2] ^ gmul(0x02, temp[3]));
        }
    }

    // GF(2^8)上的乘法
    private byte gmul(int a, byte b) {
        int p = 0;
        int counter;
        int hi_bit_set;

        for (counter = 0; counter < 8; counter++) {
            if ((b & 1) != 0) {
                p ^= a;
            }
            hi_bit_set = (a & 0x80);
            a <<= 1;
            if (hi_bit_set != 0) {
                a ^= 0x1b;
            }
            b >>= 1;
        }
        return (byte) (p & 0xff);
    }

    /**
     * 加密方法 - 包含自动填充
     * @param data 原始数据
     * @param key 密钥 (16, 24 或 32 字节)
     * @param iv 初始化向量 (16字节)
     * @return 加密后的数据
     */
    public byte[] encrypt(byte[] data, byte[] key, byte[] iv) {
        validateKeyAndIV(key, iv);

        // 进行PKCS7填充
        byte[] paddedData = pkcs7Pad(data);

        byte[] encrypted = new byte[paddedData.length];
        byte[] previousBlock = iv.clone();

        int[] expandedKey = expandKey(key);

        // 按块进行加密
        for (int i = 0; i < paddedData.length; i += BLOCK_SIZE) {
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(paddedData, i, block, 0, BLOCK_SIZE);

            // CBC模式：与前一个块进行XOR
            for (int j = 0; j < BLOCK_SIZE; j++) {
                block[j] ^= previousBlock[j];
            }

            byte[] encryptedBlock = encryptBlock(block, expandedKey);
            System.arraycopy(encryptedBlock, 0, encrypted, i, BLOCK_SIZE);
            previousBlock = encryptedBlock;
        }

        return encrypted;
    }
    /**
     * 解密方法 - 包含自动去除填充
     * @param encrypted 加密数据
     * @param key 密钥 (16, 24 或 32 字节)
     * @param iv 初始化向量 (16字节)
     * @return 解密后的原始数据
     */
    public byte[] decrypt(byte[] encrypted, byte[] key, byte[] iv) {
        validateKeyAndIV(key, iv);

        if (encrypted.length % BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("加密数据长度必须是16的倍数");
        }

        byte[] decrypted = new byte[encrypted.length];
        byte[] previousBlock = iv.clone();

        int[] expandedKey = expandKey(key);

        // 按块进行解密
        for (int i = 0; i < encrypted.length; i += BLOCK_SIZE) {
            byte[] block = new byte[BLOCK_SIZE];
            System.arraycopy(encrypted, i, block, 0, BLOCK_SIZE);

            byte[] decryptedBlock = decryptBlock(block.clone(), expandedKey);

            // CBC模式：与前一个块进行XOR
            for (int j = 0; j < BLOCK_SIZE; j++) {
                decryptedBlock[j] ^= previousBlock[j];
            }

            System.arraycopy(decryptedBlock, 0, decrypted, i, BLOCK_SIZE);
            previousBlock = block;
        }

        // 移除填充
        try {
            return removePadding(decrypted);
        } catch (Exception e) {
            // 如果去除填充失败，返回原始解密数据
            return decrypted;
        }
    }
    // 加密单个块
    private byte[] encryptBlock(byte[] block, int[] expandedKey) {
        byte[][] state = new byte[4][4];

        // 将输入块转换为state数组
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                state[j][i] = block[i * 4 + j];
            }
        }

        // 初始轮密钥加
        addRoundKey(state, expandedKey, 0);

        // 9个标准轮
        for (int round = 1; round < 10; round++) {
            subBytes(state);
            shiftRows(state);
            mixColumns(state);
            addRoundKey(state, expandedKey, round);
        }

        // 最后一轮(没有mixColumns)
        subBytes(state);
        shiftRows(state);
        addRoundKey(state, expandedKey, 10);

        // 将state数组转换回字节数组
        byte[] output = new byte[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                output[i * 4 + j] = state[j][i];
            }
        }

        return output;
    }

    // 轮密钥加
    private void addRoundKey(byte[][] state, int[] expandedKey, int round) {
        for (int i = 0; i < 4; i++) {
            int rk = expandedKey[round * 4 + i];
            for (int j = 0; j < 4; j++) {
                state[j][i] ^= (byte) ((rk >> (24 - j * 8)) & 0xff);
            }
        }
    }

    private int subWord(int word) {
        return (SBOX[(word >> 24) & 0xff] << 24) |
                (SBOX[(word >> 16) & 0xff] << 16) |
                (SBOX[(word >> 8) & 0xff] << 8) |
                SBOX[word & 0xff];
    }

    private int rotWord(int word) {
        return ((word << 8) | ((word >> 24) & 0xff));
    }


    // 解密单个块
    private byte[] decryptBlock(byte[] block, int[] expandedKey) {
        byte[][] state = new byte[4][4];

        // 将输入块转换为state数组
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                state[j][i] = block[i * 4 + j];
            }
        }

        // 初始轮密钥加
        addRoundKey(state, expandedKey, 10);

        // 9个标准轮
        for (int round = 9; round > 0; round--) {
            invShiftRows(state);
            invSubBytes(state);
            addRoundKey(state, expandedKey, round);
            invMixColumns(state);
        }

        // 最后一轮
        invShiftRows(state);
        invSubBytes(state);
        addRoundKey(state, expandedKey, 0);

        // 将state数组转换回字节数组
        byte[] output = new byte[16];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                output[i * 4 + j] = state[j][i];
            }
        }

        return output;
    }

    // 逆SubBytes变换
    private void invSubBytes(byte[][] state) {
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                state[i][j] = (byte) INV_SBOX[state[i][j] & 0xff];
            }
        }
    }

    // 逆ShiftRows变换
    private void invShiftRows(byte[][] state) {
        byte temp;

        // 第二行右移一位
        temp = state[1][3];
        state[1][3] = state[1][2];
        state[1][2] = state[1][1];
        state[1][1] = state[1][0];
        state[1][0] = temp;

        // 第三行右移两位
        temp = state[2][0];
        state[2][0] = state[2][2];
        state[2][2] = temp;
        temp = state[2][1];
        state[2][1] = state[2][3];
        state[2][3] = temp;

        // 第四行右移三位
        temp = state[3][0];
        state[3][0] = state[3][1];
        state[3][1] = state[3][2];
        state[3][2] = state[3][3];
        state[3][3] = temp;
    }

    // 逆MixColumns变换
    private void invMixColumns(byte[][] state) {
        byte[] temp = new byte[4];

        for (int c = 0; c < 4; c++) {
            for (int i = 0; i < 4; i++) {
                temp[i] = state[i][c];
            }

            state[0][c] = (byte)(gmul(0x0e, temp[0]) ^ gmul(0x0b, temp[1]) ^
                    gmul(0x0d, temp[2]) ^ gmul(0x09, temp[3]));
            state[1][c] = (byte)(gmul(0x09, temp[0]) ^ gmul(0x0e, temp[1]) ^
                    gmul(0x0b, temp[2]) ^ gmul(0x0d, temp[3]));
            state[2][c] = (byte)(gmul(0x0d, temp[0]) ^ gmul(0x09, temp[1]) ^
                    gmul(0x0e, temp[2]) ^ gmul(0x0b, temp[3]));
            state[3][c] = (byte)(gmul(0x0b, temp[0]) ^ gmul(0x0d, temp[1]) ^
                    gmul(0x09, temp[2]) ^ gmul(0x0e, temp[3]));
        }
    }
    /**
     * PKCS7 填充
     */
    private byte[] pkcs7Pad(byte[] data) {
        int padding = BLOCK_SIZE - (data.length % BLOCK_SIZE);
        byte[] paddedData = new byte[data.length + padding];
        System.arraycopy(data, 0, paddedData, 0, data.length);
        for (int i = data.length; i < paddedData.length; i++) {
            paddedData[i] = (byte) padding;
        }
        return paddedData;
    }

    /**
     * 移除PKCS7填充，增加容错处理
     */
    private byte[] removePadding(byte[] paddedData) {
        if (paddedData == null || paddedData.length == 0) {
            return paddedData;
        }

        int padding = paddedData[paddedData.length - 1] & 0xFF;

        // 如果填充值不合理，返回原始数据
        if (padding < 1 || padding > BLOCK_SIZE || padding > paddedData.length) {
            return paddedData;
        }

        // 验证填充
        for (int i = paddedData.length - padding; i < paddedData.length; i++) {
            if ((paddedData[i] & 0xFF) != padding) {
                // 如果填充不一致，返回原始数据
                return paddedData;
            }
        }

        // 移除填充
        return Arrays.copyOfRange(paddedData, 0, paddedData.length - padding);
    }

    /**
     * 验证密钥和IV
     */
    private void validateKeyAndIV(byte[] key, byte[] iv) {
        if (key == null || (key.length != 16 && key.length != 24 && key.length != 32)) {
            throw new IllegalArgumentException("密钥长度必须是16、24或32字节");
        }

        if (iv == null || iv.length != BLOCK_SIZE) {
            throw new IllegalArgumentException("IV必须是16字节");
        }
    }
}

