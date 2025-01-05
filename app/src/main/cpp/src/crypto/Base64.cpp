#include "../../inc/crypto/crypto.h"


// Base64编码表
static const char base64_table[] =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

// Base64解码表
static const unsigned char base64_reverse_table[256] = {
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 62, 64, 64, 64, 63,
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 64, 64, 64, 64, 64, 64,
        64,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 64, 64, 64, 64, 64,
        64, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
        41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64,
        64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64, 64
};

// 计算Base64编码后的长度
size_t BASE64_encode_len(size_t input_length) {
    return ((input_length + 2) / 3) * 4;
}

// Base64编码
void BASE64_encode(const uint8_t *input, size_t input_length, char *output) {
    size_t i, j;
    uint32_t triple;

    for (i = 0, j = 0; i < input_length;) {
        triple = (i < input_length ? input[i++] : 0) << 16;
        triple |= (i < input_length ? input[i++] : 0) << 8;
        triple |= (i < input_length ? input[i++] : 0);

        output[j++] = base64_table[(triple >> 18) & 0x3F];
        output[j++] = base64_table[(triple >> 12) & 0x3F];
        output[j++] = (i > input_length + 1) ? '=' : base64_table[(triple >> 6) & 0x3F];
        output[j++] = (i > input_length) ? '=' : base64_table[triple & 0x3F];
    }
    output[j] = '\0';
}

// 计算Base64解码后的长度
size_t BASE64_decode_len(const char *input) {
    size_t len = strlen(input);
    size_t padding = 0;

    if (len > 0 && input[len-1] == '=' && input[len-2] == '=')
        padding = 2;
    else if (len > 0 && input[len-1] == '=')
        padding = 1;

    return (len * 3) / 4 - padding;
}

// Base64解码
int BASE64_decode(const char *input, size_t input_length, uint8_t *output, size_t *output_length) {
    size_t i, j;
    uint32_t quad;

    for (i = 0, j = 0; i < input_length; i += 4) {
        quad = (base64_reverse_table[input[i]] << 18);
        quad += (base64_reverse_table[input[i+1]] << 12);
        quad += (base64_reverse_table[input[i+2]] << 6);
        quad += base64_reverse_table[input[i+3]];

        if (j < *output_length) output[j++] = (quad >> 16) & 0xFF;
        if (input[i+2] != '=' && j < *output_length) output[j++] = (quad >> 8) & 0xFF;
        if (input[i+3] != '=' && j < *output_length) output[j++] = quad & 0xFF;
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