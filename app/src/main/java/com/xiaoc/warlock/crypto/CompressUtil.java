package com.xiaoc.warlock.crypto;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

public class CompressUtil {
    // 魔数和版本号
    private static final byte[] MAGIC = "CZIP".getBytes();
    private static final int VERSION = 1;

    // 头部大小：魔数(4) + 版本(4) + 原始长度(8) + CRC32(8)
    private static final int HEADER_SIZE = 24;

    /**
     * 压缩数据
     */
    public byte[] compress(byte[] data) throws Exception {
        // 1. 计算CRC32校验和
        CRC32 crc = new CRC32();
        crc.update(data);
        long checksum = crc.getValue();

        // 2. 压缩数据
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();

        // 使用ByteArrayOutputStream动态增长
        ByteArrayOutputStream compressed = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1024];

        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            compressed.write(buffer, 0, count);
        }
        deflater.end();

        byte[] compressedData = compressed.toByteArray();

        // 3. 构建头部和压缩数据
        ByteBuffer result = ByteBuffer.allocate(HEADER_SIZE + compressedData.length);
        result.put(MAGIC);                    // 魔数
        result.putInt(VERSION);               // 版本号
        result.putLong(data.length);          // 原始数据长度
        result.putLong(checksum);             // CRC32校验和
        result.put(compressedData);           // 压缩后的数据

        return result.array();
    }

}
