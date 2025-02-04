/**
 * 魔改版AES实现
 * 主要魔改点：
 * 1. 自定义S-box和逆S-box
 * 2. 使用前12个素数作为轮常量
 * 3. 增强的密钥扩展函数
 */

#include "../inc/crypto/crypto.h"
#include <cstring>
#include <android/log.h>

#define LOG_TAG "AES_Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// S-box查找表 - 经过魔改的替换表
// 原始AES的第一个字节是0x63，这里改为0x37
// 其他位置也进行了相应的修改，但保持了S-box的数学特性
static const uint8_t SBOX[256] = {
        0x37, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0xf5, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
        0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
        0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
        0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
        0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
        0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
        0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
        0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0x6f, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
        0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
        0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
        0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0x68, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
        0xe7, 0xc8, 0x63, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
        0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
        0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
        0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
        0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0xc2, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
};

// 逆S-box查找表 - 根据魔改后的S-box重新生成
// 确保对任意字节x，都有：INV_SBOX[SBOX[x]] = x
static const uint8_t INV_SBOX[256] = {
        0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb,
        0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb,
        0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e,
        0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0x00, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25,
        0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92,
        0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84,
        0x90, 0xd8, 0xab, 0xb2, 0x8c, 0xbc, 0xd3, 0x0a, 0xa8, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x77,
        0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b,
        0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73,
        0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e,
        0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b,
        0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4,
        0x1f, 0xdd, 0xf7, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f,
        0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef,
        0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61,
        0x17, 0x2b, 0x04, 0x7e, 0xba, 0x06, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d
};

// 轮常量 - 使用前12个素数（标准AES使用10个2的幂次）
// 原始：0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36
// 魔改：使用素数序列增加复杂度
static const uint8_t RCON[12] = {
        0x02, 0x03, 0x05, 0x07, 0x11, 0x13, 0x17, 0x1d, 0x1f, 0x25, 0x29, 0x2b
};

/**
 * 有限域GF(2^8)上的乘法运算
 * 使用标准的不可约多项式：x^8 + x^4 + x^3 + x + 1 (0x11b)
 * @param a 第一个操作数
 * @param b 第二个操作数
 * @return 乘法结果
 */
static uint8_t gmul(uint8_t a, uint8_t b) {
    uint8_t p = 0;
    uint8_t hi_bit_set;
    for(int i = 0; i < 8; i++) {
        if((b & 1) == 1)
            p ^= a;
        hi_bit_set = (a & 0x80);
        a <<= 1;
        if(hi_bit_set == 0x80)
            a ^= 0x1b;
        b >>= 1;
    }
    return p;
}

/**
 * 魔改的密钥扩展函数
 * 在标准AES基础上增加了两个额外的变换：
 * 1. 每隔两个字进行部分S-box替换
 * 2. 每隔三个字进行循环右移
 * @param key 初始密钥
 * @param round_key 扩展后的轮密钥
 */
static void key_expansion(const uint8_t *key, uint32_t *round_key) {
    uint32_t temp;
    int i = 0;

    // 复制初始密钥
    for(i = 0; i < 4; i++) {
        round_key[i] = (key[4*i] << 24) | (key[4*i+1] << 16) |
                       (key[4*i+2] << 8) | key[4*i+3];
    }

    // 生成轮密钥
    for(; i < 4 * (AES_ROUNDS + 1); i++) {
        temp = round_key[i-1];
        if(i % 4 == 0) {
            // 字循环
            temp = ((temp << 8) | (temp >> 24)) & 0xFFFFFFFF;
            // S-box 替换
            temp = (SBOX[(temp >> 24) & 0xFF] << 24) |
                   (SBOX[(temp >> 16) & 0xFF] << 16) |
                   (SBOX[(temp >> 8) & 0xFF] << 8) |
                   SBOX[temp & 0xFF];
            // 轮常量异或
            temp ^= RCON[(i/4)-1] << 24;
        }
            // 额外的变换：每隔两个字进行一次额外的S-box替换
        else if(i % 2 == 0) {
            temp = (SBOX[(temp >> 24) & 0xFF] << 24) |
                   (SBOX[(temp >> 16) & 0xFF] << 16) |
                   (temp & 0x0000FFFF);
        }
            // 额外的变换：每隔三个字进行一次循环右移
        else if(i % 3 == 0) {
            temp = ((temp >> 8) | (temp << 24)) & 0xFFFFFFFF;
        }
        round_key[i] = round_key[i-4] ^ temp;
    }
}

/**
 * SubBytes变换 - 使用魔改的S-box进行字节替换
 * 对状态矩阵中的每个字节进行非线性变换
 * @param state 4x4状态矩阵
 */
static void sub_bytes(uint8_t state[4][4]) {
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            state[i][j] = SBOX[state[i][j]];
        }
    }
}

/**
 * 逆SubBytes变换 - 使用魔改的逆S-box进行字节替换
 * 是SubBytes变换的逆操作
 * @param state 4x4状态矩阵
 */
static void inv_sub_bytes(uint8_t state[4][4]) {
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            state[i][j] = INV_SBOX[state[i][j]];
        }
    }
}

/**
 * ShiftRows变换 - 对状态矩阵的行进行循环左移
 * 第i行左移i个字节（i=0,1,2,3）
 * 保持标准AES的移位方式
 * @param state 4x4状态矩阵
 */
static void shift_rows(uint8_t state[4][4]) {
    uint8_t temp;

    // 第二行左移1位
    temp = state[1][0];
    state[1][0] = state[1][1];
    state[1][1] = state[1][2];
    state[1][2] = state[1][3];
    state[1][3] = temp;

    // 第三行左移2位
    temp = state[2][0];
    state[2][0] = state[2][2];
    state[2][2] = temp;
    temp = state[2][1];
    state[2][1] = state[2][3];
    state[2][3] = temp;

    // 第四行左移3位
    temp = state[3][3];
    state[3][3] = state[3][2];
    state[3][2] = state[3][1];
    state[3][1] = state[3][0];
    state[3][0] = temp;
}

/**
 * 逆ShiftRows变换 - 对状态矩阵的行进行循环右移
 * 是ShiftRows变换的逆操作
 * @param state 4x4状态矩阵
 */
static void inv_shift_rows(uint8_t state[4][4]) {
    uint8_t temp;

    // 第二行右移1位
    temp = state[1][3];
    state[1][3] = state[1][2];
    state[1][2] = state[1][1];
    state[1][1] = state[1][0];
    state[1][0] = temp;

    // 第三行右移2位
    temp = state[2][0];
    state[2][0] = state[2][2];
    state[2][2] = temp;
    temp = state[2][1];
    state[2][1] = state[2][3];
    state[2][3] = temp;

    // 第四行右移3位
    temp = state[3][0];
    state[3][0] = state[3][1];
    state[3][1] = state[3][2];
    state[3][2] = state[3][3];
    state[3][3] = temp;
}

/**
 * MixColumns变换 - 列混合变换
 * 使用标准AES的系数：{02, 03, 01, 01}
 * @param state 4x4状态矩阵
 */
static void mix_columns(uint8_t state[4][4]) {
    uint8_t temp[4];
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            temp[j] = state[j][i];
        }
        // 使用标准系数：2, 3, 1, 1
        state[0][i] = gmul(0x02, temp[0]) ^ gmul(0x03, temp[1]) ^ temp[2] ^ temp[3];
        state[1][i] = temp[0] ^ gmul(0x02, temp[1]) ^ gmul(0x03, temp[2]) ^ temp[3];
        state[2][i] = temp[0] ^ temp[1] ^ gmul(0x02, temp[2]) ^ gmul(0x03, temp[3]);
        state[3][i] = gmul(0x03, temp[0]) ^ temp[1] ^ temp[2] ^ gmul(0x02, temp[3]);
    }
}

/**
 * 逆MixColumns变换
 * 使用标准AES的逆变换系数：{0E, 0B, 0D, 09}
 * @param state 4x4状态矩阵
 */
static void inv_mix_columns(uint8_t state[4][4]) {
    uint8_t temp[4];
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            temp[j] = state[j][i];
        }
        // 使用标准的逆系数：0x0e, 0x0b, 0x0d, 0x09
        state[0][i] = gmul(0x0e, temp[0]) ^ gmul(0x0b, temp[1]) ^ gmul(0x0d, temp[2]) ^ gmul(0x09, temp[3]);
        state[1][i] = gmul(0x09, temp[0]) ^ gmul(0x0e, temp[1]) ^ gmul(0x0b, temp[2]) ^ gmul(0x0d, temp[3]);
        state[2][i] = gmul(0x0d, temp[0]) ^ gmul(0x09, temp[1]) ^ gmul(0x0e, temp[2]) ^ gmul(0x0b, temp[3]);
        state[3][i] = gmul(0x0b, temp[0]) ^ gmul(0x0d, temp[1]) ^ gmul(0x09, temp[2]) ^ gmul(0x0e, temp[3]);
    }
}

/**
 * 轮密钥加变换 - 将轮密钥与状态矩阵进行异或
 * @param state 4x4状态矩阵
 * @param round_key 轮密钥数组
 * @param round 当前轮数
 */
static void add_round_key(uint8_t state[4][4], const uint32_t *round_key, int round) {
    for(int i = 0; i < 4; i++) {
        uint32_t rk = round_key[round * 4 + i];
        state[0][i] ^= (rk >> 24) & 0xFF;
        state[1][i] ^= (rk >> 16) & 0xFF;
        state[2][i] ^= (rk >> 8) & 0xFF;
        state[3][i] ^= rk & 0xFF;
    }
}

/**
 * 初始化AES上下文
 * 生成所有轮密钥
 * @param ctx AES上下文
 * @param key 初始密钥
 */
void AES_init(AES_CTX *ctx, const uint8_t *key) {
    key_expansion(key, ctx->round_key);
}

/**
 * 加密单个数据块（16字节）
 * @param ctx AES上下文
 * @param input 输入数据
 * @param output 输出数据
 */
void AES_encrypt_block(const AES_CTX *ctx, const uint8_t *input, uint8_t *output) {
    uint8_t state[4][4];

    // 输入数据转换为状态矩阵
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            state[j][i] = input[i * 4 + j];
        }
    }

    // 初始轮密钥加
    add_round_key(state, ctx->round_key, 0);

    // 9 个标准轮
    for(int round = 1; round < AES_ROUNDS; round++) {
        sub_bytes(state);
        shift_rows(state);
        mix_columns(state);
        add_round_key(state, ctx->round_key, round);
    }

    // 最后一轮（没有 MixColumns）
    sub_bytes(state);
    shift_rows(state);
    add_round_key(state, ctx->round_key, AES_ROUNDS);

    // 状态矩阵转换为输出数据
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            output[i * 4 + j] = state[j][i];
        }
    }
}

/**
 * 解密单个数据块（16字节）
 * @param ctx AES上下文
 * @param input 输入数据
 * @param output 输出数据
 */
void AES_decrypt_block(const AES_CTX *ctx, const uint8_t *input, uint8_t *output) {
    uint8_t state[4][4];

    // 输入数据转换为状态矩阵
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            state[j][i] = input[i * 4 + j];
        }
    }

    // 初始轮密钥加
    add_round_key(state, ctx->round_key, AES_ROUNDS);

    // 9 个标准轮
    for(int round = AES_ROUNDS - 1; round > 0; round--) {
        inv_shift_rows(state);
        inv_sub_bytes(state);
        add_round_key(state, ctx->round_key, round);
        inv_mix_columns(state);
    }

    // 最后一轮（没有 InvMixColumns）
    inv_shift_rows(state);
    inv_sub_bytes(state);
    add_round_key(state, ctx->round_key, 0);

    // 状态矩阵转换为输出数据
    for(int i = 0; i < 4; i++) {
        for(int j = 0; j < 4; j++) {
            output[i * 4 + j] = state[j][i];
        }
    }
}

/**
 * 带PKCS7填充的加密函数
 * @param ctx AES上下文
 * @param input 输入数据
 * @param length 输入数据长度
 * @param output 输出缓冲区
 * @param output_length 输出数据长度
 */
void AES_encrypt_with_padding(const AES_CTX *ctx, const uint8_t *input, size_t length, uint8_t *output, size_t *output_length) {
    // 计算填充后的长度
    size_t padded_len = ((length + AES_BLOCK_SIZE - 1) / AES_BLOCK_SIZE) * AES_BLOCK_SIZE;
    *output_length = padded_len;

    // 创建临时缓冲区
    uint8_t* padded_input = new uint8_t[padded_len];

    // 复制原始数据
    memcpy(padded_input, input, length);

    // PKCS7 填充
    uint8_t pad_value = padded_len - length;
    for (size_t i = length; i < padded_len; i++) {
        padded_input[i] = pad_value;
    }

    // 逐块加密
    for (size_t i = 0; i < padded_len; i += AES_BLOCK_SIZE) {
        AES_encrypt_block(ctx, padded_input + i, output + i);
    }

    delete[] padded_input;
}

/**
 * 带PKCS7填充的解密函数
 * 包含填充验证和错误处理
 * @param ctx AES上下文
 * @param input 输入数据
 * @param length 输入数据长度
 * @param output 输出缓冲区
 * @param output_length 输出数据长度
 */
void AES_decrypt_with_padding(const AES_CTX *ctx, const uint8_t *input, size_t length, uint8_t *output, size_t *output_length) {
    if (length == 0 || length % AES_BLOCK_SIZE != 0) {
        *output_length = 0;
        return;
    }

    // 创建临时缓冲区
    uint8_t* decrypted = new uint8_t[length];

    // 逐块解密
    for (size_t i = 0; i < length; i += AES_BLOCK_SIZE) {
        AES_decrypt_block(ctx, input + i, decrypted + i);
    }

    // 获取填充长度
    uint8_t padding_len = decrypted[length - 1];

    // 验证填充长度
    if (padding_len == 0 || padding_len > AES_BLOCK_SIZE || padding_len > length) {
        // 如果填充无效，返回原始解密数据
        memcpy(output, decrypted, length);
        *output_length = length;
        delete[] decrypted;
        return;
    }

    // 验证所有填充字节
    bool padding_valid = true;
    for (size_t i = length - padding_len; i < length; i++) {
        if (decrypted[i] != padding_len) {
            padding_valid = false;
            break;
        }
    }

    if (!padding_valid) {
        // 如果填充验证失败，返回原始解密数据
        memcpy(output, decrypted, length);
        *output_length = length;
    } else {
        // 填充验证成功，移除填充
        *output_length = length - padding_len;
        memcpy(output, decrypted, *output_length);
    }

    delete[] decrypted;
}

/**
 * 生成逆S-box的辅助函数
 * 用于验证S-box和逆S-box的对应关系
 */
void generate_inverse_sbox() {
    uint8_t inv_sbox[256];
    for (int i = 0; i < 256; i++) {
        uint8_t value = SBOX[i];
        inv_sbox[value] = i;
    }

    // 使用Android日志输出
    LOGI("static const uint8_t INV_SBOX[256] = {");
    for (int i = 0; i < 256; i++) {
        if (i % 16 == 0) {
            LOGI("    ");
        }
        LOGI("0x%02x%s", inv_sbox[i], (i == 255 ? "" : ", "));
        if ((i + 1) % 16 == 0 && i != 255) {
            LOGI("\n");
        }
    }
    LOGI("\n};");
}
//
//// 示例函数也改用Android日志
//void example() {
//    // 1. 准备密钥
//    uint8_t key[16] = {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16};
//
//    // 2. 初始化上下文
//    AES_CTX ctx;
//    AES_init(&ctx, key);
//
//    // 3. 准备要加密的数据
//    const char* message = "Hello, World!"; // 13字节
//    size_t message_len = strlen(message);
//
//    // 4. 计算输出缓冲区大小
//    size_t output_len;
//    size_t padded_len = ((message_len + AES_BLOCK_SIZE - 1) / AES_BLOCK_SIZE) * AES_BLOCK_SIZE;
//    uint8_t* encrypted = new uint8_t[padded_len];
//
//    // 5. 加密
//    AES_encrypt_with_padding(&ctx, (const uint8_t*)message, message_len,
//                             encrypted, &output_len);
//
//    // 6. 使用加密后的数据
//    char hex_output[output_len * 3 + 1];
//    char* p = hex_output;
//    for (size_t i = 0; i < output_len; i++) {
//        p += sprintf(p, "%02x ", encrypted[i]);
//    }
//    LOGI("Encrypted data: %s", hex_output);
//
//    delete[] encrypted;
//}
