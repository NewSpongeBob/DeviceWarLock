#include "../inc/crypto/crypto.h"

// 魔改的 Base64 编码表
static const char base64_table[64] = {
        'A', 'B', 'P', 'D', 'E', 'F', 'T', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'C', 'Q', 'R', 'S', 'G', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 's', 'g', 'h', 'i', 'j', 'k', 'y', 'm',
        'n', 'o', 'p', 'q', 'r', 'f', 't', 'u', 'v', 'w', 'x', 'l', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '/', '='
};

// 生成解码表
static unsigned char base64_reverse_table[256];
static bool base64_reverse_table_initialized = false;

// 初始化解码表
static void init_base64_reverse_table() {
    if (base64_reverse_table_initialized) return;

    // 初始化所有值为无效值
    memset(base64_reverse_table, 0xFF, 256);

    // 填充有效值
    for (int i = 0; i < 64; i++) {
        base64_reverse_table[(unsigned char)base64_table[i]] = i;
    }

    base64_reverse_table_initialized = true;
}

// 计算Base64编码后的长度
size_t BASE64_encode_len(size_t input_length) {
    return ((input_length + 2) / 3) * 4;
}

// Base64编码
void BASE64_encode(const uint8_t *input, size_t input_length, char *output) {
    size_t i = 0;
    size_t j = 0;

    // 每次处理3个字节
    while (i + 2 < input_length) {
        uint32_t b = (input[i] << 16) | (input[i + 1] << 8) | input[i + 2];
        output[j] = base64_table[(b >> 18) & 0x3F];
        output[j + 1] = base64_table[(b >> 12) & 0x3F];
        output[j + 2] = base64_table[(b >> 6) & 0x3F];
        output[j + 3] = base64_table[b & 0x3F];
        i += 3;
        j += 4;
    }

    // 处理剩余字节
    if (i < input_length) {
        uint32_t b = input[i] << 16;
        if (i + 1 < input_length) {
            b |= input[i + 1] << 8;
        }

        output[j] = base64_table[(b >> 18) & 0x3F];
        output[j + 1] = base64_table[(b >> 12) & 0x3F];

        if (i + 1 < input_length) {
            output[j + 2] = base64_table[(b >> 6) & 0x3F];
            output[j + 3] = '*';  // 一个填充字符
        } else {
            output[j + 2] = '*';  // 两个填充字符
            output[j + 3] = '*';
        }
        j += 4;
    }

    output[j] = '\0';
}

// 计算Base64解码后的长度
size_t BASE64_decode_len(const char *input) {
    size_t len = strlen(input);
    size_t padding = 0;

    if (len >= 2) {
        if (input[len - 1] == '*') padding++;
        if (input[len - 2] == '*') padding++;
    }

    return (len / 4) * 3 - padding;
}

// Base64解码
int BASE64_decode(const char *input, size_t input_length, uint8_t *output, size_t *output_length) {
    if (!base64_reverse_table_initialized) {
        init_base64_reverse_table();
    }

    size_t i = 0;
    size_t j = 0;
    uint32_t b = 0;

    while (i + 3 < input_length) {
        unsigned char b1 = base64_reverse_table[(unsigned char)input[i]];
        unsigned char b2 = base64_reverse_table[(unsigned char)input[i + 1]];
        unsigned char b3 = base64_reverse_table[(unsigned char)input[i + 2]];
        unsigned char b4 = base64_reverse_table[(unsigned char)input[i + 3]];

        // 检查无效字符
        if (b1 == 0xFF || b2 == 0xFF ||
            (input[i + 2] != '*' && b3 == 0xFF) ||
            (input[i + 3] != '*' && b4 == 0xFF)) {
            return -1;
        }

        b = (b1 << 18) | (b2 << 12);
        if (input[i + 2] != '*') {
            b |= b3 << 6;
        }
        if (input[i + 3] != '*') {
            b |= b4;
        }

        output[j] = (b >> 16) & 0xFF;
        if (input[i + 2] != '*') {
            output[j + 1] = (b >> 8) & 0xFF;
        }
        if (input[i + 3] != '*') {
            output[j + 2] = b & 0xFF;
        }

        i += 4;
        j += 3;
        if (input[i - 1] == '*') j--;
        if (input[i - 2] == '*') j--;
    }

    *output_length = j;
    return 0;
}

// 带缓冲区分配的编码函数
char* BASE64_encode_alloc(const uint8_t *input, size_t input_length, size_t *output_length) {
    *output_length = BASE64_encode_len(input_length);
    char *output = new char[*output_length + 1];  // +1 for null terminator
    if (output) {
        BASE64_encode(input, input_length, output);
    }
    return output;
}

// 带缓冲区分配的解码函数
uint8_t* BASE64_decode_alloc(const char *input, size_t input_length, size_t *output_length) {
    *output_length = BASE64_decode_len(input);
    uint8_t *output = new uint8_t[*output_length];
    if (output) {
        if (BASE64_decode(input, input_length, output, output_length) != 0) {
            delete[] output;
            return nullptr;
        }
    }
    return output;
}
//void example() {
//    // 编码示例
//    const char* message = "Hello, World!";
//    size_t input_len = strlen(message);
//    size_t output_len;
//
//    // 使用自动分配内存的版本
//    char* encoded = BASE64_encode_alloc((const uint8_t*)message, input_len, &output_len);
//    if (encoded) {
//        printf("Encoded: %s\n", encoded);
//
//        // 解码
//        size_t decoded_len;
//        uint8_t* decoded = BASE64_decode_alloc(encoded, output_len, &decoded_len);
//        if (decoded) {
//            printf("Decoded: %.*s\n", (int)decoded_len, decoded);
//            delete[] decoded;
//        }
//
//        delete[] encoded;
//    }
//
//    // 或者使用预分配缓冲区的版本
//    size_t encode_buf_size = BASE64_encode_len(input_len);
//    char* encode_buf = new char[encode_buf_size + 1];  // +1 for null terminator
//
//    BASE64_encode((const uint8_t*)message, input_len, encode_buf);
//    printf("Encoded: %s\n", encode_buf);
//
//    size_t decode_buf_size = BASE64_decode_len(encode_buf);
//    uint8_t* decode_buf = new uint8_t[decode_buf_size];
//
//    BASE64_decode(encode_buf, strlen(encode_buf), decode_buf, &decode_buf_size);
//    printf("Decoded: %.*s\n", (int)decode_buf_size, decode_buf);
//
//    delete[] encode_buf;
//    delete[] decode_buf;
//}