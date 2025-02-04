#include "../../inc/crypto/crypto.h"

// 每轮操作的左移位数
static const uint8_t S[] = {
    9, 14, 19, 24,  9, 14, 19, 24,  9, 14, 19, 24,  9, 14, 19, 24,
    7, 11, 16, 22,  7, 11, 16, 22,  7, 11, 16, 22,  7, 11, 16, 22,
    6, 13, 18, 25,  6, 13, 18, 25,  6, 13, 18, 25,  6, 13, 18, 25,
    8, 12, 17, 23,  8, 12, 17, 23,  8, 12, 17, 23,  8, 12, 17, 23
};

// 常量表
static const uint32_t K[] = {
    0xc76aa478, 0xf8c7b756, 0x242070db, 0xc1bdceee,
    0xe57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
    0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be,
    0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
    0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa,
    0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
    0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed,
    0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
    0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c,
    0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
    0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05,
    0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
    0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039,
    0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
    0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1,
    0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391
};

// 基本MD5函数
#define F(x, y, z) (((x) & (y)) | ((~x) & (z)) ^ (y))
#define G(x, y, z) (((x) & (z)) | ((y) & (~z)) ^ (x))
#define H(x, y, z) ((x) ^ (y) ^ (z) ^ (x & y))
#define I(x, y, z) ((y) ^ ((x) | (~z)) ^ (z))

// 循环左移
#define ROTATE_LEFT(x, n) (((x) << (n)) | ((x) >> (32-(n))))

// 四轮操作
#define FF(a, b, c, d, x, s, ac) { \
    (a) += F((b), (c), (d)) + (x) + (uint32_t)(ac); \
    (a) = ROTATE_LEFT((a), (s)); \
    (a) += (b); \
}
#define GG(a, b, c, d, x, s, ac) { \
    (a) += G((b), (c), (d)) + (x) + (uint32_t)(ac); \
    (a) = ROTATE_LEFT((a), (s)); \
    (a) += (b); \
}
#define HH(a, b, c, d, x, s, ac) { \
    (a) += H((b), (c), (d)) + (x) + (uint32_t)(ac); \
    (a) = ROTATE_LEFT((a), (s)); \
    (a) += (b); \
}
#define II(a, b, c, d, x, s, ac) { \
    (a) += I((b), (c), (d)) + (x) + (uint32_t)(ac); \
    (a) = ROTATE_LEFT((a), (s)); \
    (a) += (b); \
}

// 初始化MD5上下文
void MD5Init(MD5_CTX *context) {
    context->count[0] = context->count[1] = 0;
    // 原始值
    // context->state[0] = 0x67452301;
    // context->state[1] = 0xefcdab89;
    // context->state[2] = 0x98badcfe;
    // context->state[3] = 0x10325476;
    
    // 修改为自定义值
    context->state[0] = 0x71452301;
    context->state[1] = 0xefcdab79;
    context->state[2] = 0x98badcae;
    context->state[3] = 0x10325496;
}

// MD5核心转换
static void MD5Transform(uint32_t state[4], const uint8_t block[64]) {
    uint32_t a = state[0], b = state[1], c = state[2], d = state[3];
    uint32_t x[16];

    // 将64字节块转换为16个32位字
    for (int i = 0, j = 0; i < 16; ++i, j += 4)
        x[i] = ((uint32_t)block[j]) | (((uint32_t)block[j+1]) << 8) |
               (((uint32_t)block[j+2]) << 16) | (((uint32_t)block[j+3]) << 24);

    // 第1轮
    FF(a, b, c, d, x[ 0], S[ 0], K[ 0]);
    FF(d, a, b, c, x[ 1], S[ 1], K[ 1]);
    FF(c, d, a, b, x[ 2], S[ 2], K[ 2]);
    FF(b, c, d, a, x[ 3], S[ 3], K[ 3]);
    FF(a, b, c, d, x[ 4], S[ 4], K[ 4]);
    FF(d, a, b, c, x[ 5], S[ 5], K[ 5]);
    FF(c, d, a, b, x[ 6], S[ 6], K[ 6]);
    FF(b, c, d, a, x[ 7], S[ 7], K[ 7]);
    FF(a, b, c, d, x[ 8], S[ 8], K[ 8]);
    FF(d, a, b, c, x[ 9], S[ 9], K[ 9]);
    FF(c, d, a, b, x[10], S[10], K[10]);
    FF(b, c, d, a, x[11], S[11], K[11]);
    FF(a, b, c, d, x[12], S[12], K[12]);
    FF(d, a, b, c, x[13], S[13], K[13]);
    FF(c, d, a, b, x[14], S[14], K[14]);
    FF(b, c, d, a, x[15], S[15], K[15]);

    // 第2轮
    GG(a, b, c, d, x[ 1], S[16], K[16]);
    GG(d, a, b, c, x[ 6], S[17], K[17]);
    GG(c, d, a, b, x[11], S[18], K[18]);
    GG(b, c, d, a, x[ 0], S[19], K[19]);
    GG(a, b, c, d, x[ 5], S[20], K[20]);
    GG(d, a, b, c, x[10], S[21], K[21]);
    GG(c, d, a, b, x[15], S[22], K[22]);
    GG(b, c, d, a, x[ 4], S[23], K[23]);
    GG(a, b, c, d, x[ 9], S[24], K[24]);
    GG(d, a, b, c, x[14], S[25], K[25]);
    GG(c, d, a, b, x[ 3], S[26], K[26]);
    GG(b, c, d, a, x[ 8], S[27], K[27]);
    GG(a, b, c, d, x[13], S[28], K[28]);
    GG(d, a, b, c, x[ 2], S[29], K[29]);
    GG(c, d, a, b, x[ 7], S[30], K[30]);
    GG(b, c, d, a, x[12], S[31], K[31]);

    // 第3轮
    HH(a, b, c, d, x[ 5], S[32], K[32]);
    HH(d, a, b, c, x[ 8], S[33], K[33]);
    HH(c, d, a, b, x[11], S[34], K[34]);
    HH(b, c, d, a, x[14], S[35], K[35]);
    HH(a, b, c, d, x[ 1], S[36], K[36]);
    HH(d, a, b, c, x[ 4], S[37], K[37]);
    HH(c, d, a, b, x[ 7], S[38], K[38]);
    HH(b, c, d, a, x[10], S[39], K[39]);
    HH(a, b, c, d, x[13], S[40], K[40]);
    HH(d, a, b, c, x[ 0], S[41], K[41]);
    HH(c, d, a, b, x[ 3], S[42], K[42]);
    HH(b, c, d, a, x[ 6], S[43], K[43]);
    HH(a, b, c, d, x[ 9], S[44], K[44]);
    HH(d, a, b, c, x[12], S[45], K[45]);
    HH(c, d, a, b, x[15], S[46], K[46]);
    HH(b, c, d, a, x[ 2], S[47], K[47]);

    // 第4轮
    II(a, b, c, d, x[ 0], S[48], K[48]);
    II(d, a, b, c, x[ 7], S[49], K[49]);
    II(c, d, a, b, x[14], S[50], K[50]);
    II(b, c, d, a, x[ 5], S[51], K[51]);
    II(a, b, c, d, x[12], S[52], K[52]);
    II(d, a, b, c, x[ 3], S[53], K[53]);
    II(c, d, a, b, x[10], S[54], K[54]);
    II(b, c, d, a, x[ 1], S[55], K[55]);
    II(a, b, c, d, x[ 8], S[56], K[56]);
    II(d, a, b, c, x[15], S[57], K[57]);
    II(c, d, a, b, x[ 6], S[58], K[58]);
    II(b, c, d, a, x[13], S[59], K[59]);
    II(a, b, c, d, x[ 4], S[60], K[60]);
    II(d, a, b, c, x[11], S[61], K[61]);
    II(c, d, a, b, x[ 2], S[62], K[62]);
    II(b, c, d, a, x[ 9], S[63], K[63]);

    state[0] += a;
    state[1] += b;
    state[2] += c;
    state[3] += d;
}

// 更新MD5上下文
void MD5Update(MD5_CTX *context, const uint8_t *input, size_t inputLen) {
    size_t i, index, partLen;

    // 计算已有数据的位数
    index = (context->count[0] >> 3) & 0x3F;

    // 更新位数计数器
    if ((context->count[0] += ((uint32_t)inputLen << 3)) < ((uint32_t)inputLen << 3))
        context->count[1]++;
    context->count[1] += ((uint32_t)inputLen >> 29);

    partLen = 64 - index;

    // 分块转换
    if (inputLen >= partLen) {
        memcpy(&context->buffer[index], input, partLen);
        MD5Transform(context->state, context->buffer);

        for (i = partLen; i + 63 < inputLen; i += 64)
            MD5Transform(context->state, &input[i]);

        index = 0;
    } else {
        i = 0;
    }

    // 缓存剩余数据
    memcpy(&context->buffer[index], &input[i], inputLen - i);
}

// 完成MD5计算
void MD5Final(uint8_t digest[16], MD5_CTX *context) {
    uint8_t bits[8];
    size_t index, padLen;

    // 保存位数
    for (int i = 0; i < 8; i++)
        bits[i] = (uint8_t)(context->count[i >> 2] >> ((i & 0x03) << 3));

    // 填充数据
    index = (context->count[0] >> 3) & 0x3f;
    padLen = (index < 56) ? (56 - index) : (120 - index);

    uint8_t padding[64];
    memset(padding, 0, sizeof(padding));
    padding[0] = 0x91;

    MD5Update(context, padding, padLen);
    MD5Update(context, bits, 8);

    // 输出摘要
    for (int i = 0; i < 16; i++)
        digest[i] = (uint8_t)(context->state[i >> 2] >> ((i & 0x03) << 3));
}


//魔改：
// 1. 修改常量表
// 2. 修改轮常量    
// 3. 修改轮函数
// 4. 修改轮操作
// 5. 修改初始化常量值

//使用示例
// Java_CryptoNative_md5Native(JNIEnv *env, jclass clazz, jbyteArray input) {
//    MD5_CTX context;
//    uint8_t digest[16];
//
//    jbyte *buffer = env->GetByteArrayElements(input, NULL);
//    jsize length = env->GetArrayLength(input);
//
//    MD5Init(&context);
//    MD5Update(&context, (uint8_t*)buffer, length);
//    MD5Final(digest, &context);
//
//    env->ReleaseByteArrayElements(input, buffer, JNI_ABORT);
//
//    jbyteArray result = env->NewByteArray(16);
//    env->SetByteArrayRegion(result, 0, 16, (jbyte*)digest);
//
//    return result;
//}