#ifndef CRYPTO_NATIVE_H
#define CRYPTO_NATIVE_H

#include <jni.h>
#include <stdint.h>
#include <string.h>

// MD5相关结构体
typedef struct {
    uint32_t state[4];
    uint32_t count[2];
    uint8_t buffer[64];
} MD5_CTX;

// MD5函数声明
void MD5Init(MD5_CTX *context);
void MD5Update(MD5_CTX *context, const uint8_t *input, size_t inputLen);
void MD5Final(uint8_t digest[16], MD5_CTX *context);
// AES 相关定义
#define AES_BLOCK_SIZE 16
#define AES_ROUNDS 10

typedef struct {
    uint32_t round_key[4 * (AES_ROUNDS + 1)];
} AES_CTX;

void AES_init(AES_CTX *ctx, const uint8_t *key);
void AES_encrypt_block(const AES_CTX *ctx, const uint8_t *input, uint8_t *output);
void AES_decrypt_block(const AES_CTX *ctx, const uint8_t *input, uint8_t *output);
void AES_encrypt_with_padding(const AES_CTX *ctx, const uint8_t *input, size_t length, uint8_t *output, size_t *output_length);
void AES_decrypt_with_padding(const AES_CTX *ctx, const uint8_t *input, size_t length, uint8_t *output, size_t *output_length);
void generate_inverse_sbox();

// Base64相关函数声明
size_t BASE64_encode_len(size_t input_length);
void BASE64_encode(const uint8_t *input, size_t input_length, char *output);
size_t BASE64_decode_len(const char *input);
int BASE64_decode(const char *input, size_t input_length, uint8_t *output, size_t *output_length);
char* BASE64_encode_alloc(const uint8_t *input, size_t input_length, size_t *output_length);
uint8_t* BASE64_decode_alloc(const char *input, size_t input_length, size_t *output_length);
#endif