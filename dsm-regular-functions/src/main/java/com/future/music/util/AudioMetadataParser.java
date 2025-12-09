package com.future.music.util;

import lombok.Data;
import lombok.ToString;
import org.apache.commons.io.FilenameUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.Tag;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * 音频元数据解析工具
 */
@Component
public class AudioMetadataParser {

    /**
     * 解析音频核心元数据（采样率/位深/格式/码率/声道）
     */
    public AudioMetadata parse(MultipartFile audioFile) throws Exception {
        // 1. 临时文件转换
        File tempFile = File.createTempFile("audio_", "." + FilenameUtils.getExtension(audioFile.getOriginalFilename()));
        audioFile.transferTo(tempFile);

        // 2. 读取基础音频头信息
        AudioFile audio = AudioFileIO.read(tempFile);
        AudioHeader header = audio.getAudioHeader();

        // 3. 封装基础元数据
        AudioMetadata metadata = new AudioMetadata();
        metadata.setSampleRate(header.getSampleRateAsNumber());
        metadata.setFormat(header.getFormat());
        // 码率：兼容long/int类型
        metadata.setBitRate(header.getBitRateAsNumber());
        // 关键修改：将声道描述字符串转为数字
        metadata.setChannels(convertChannelsToNumber(header.getChannels()));
        // 位深：通用解析逻辑
        metadata.setBitDepth(getBitDepthUniversal(tempFile, header));

        // 4. 清理临时文件
        tempFile.delete();
        return metadata;
    }
    /**
     * 将JAudiotagger返回的声道描述字符串转为数字
     * @param channelsStr 如 "Stereo"、"Mono"、"5.1"
     * @return 声道数（整数）
     */
    private int convertChannelsToNumber(String channelsStr) {
        if (channelsStr == null || channelsStr.trim().isEmpty()) {
            return 0; // 未知声道数，可根据业务调整默认值
        }
        switch (channelsStr.trim().toLowerCase()) {
            case "mono":
                return 1;
            case "stereo":
                return 2;
            case "5.1":
                return 6;
            case "7.1":
                return 8;
            default:
                // 兼容其他格式（如直接返回数字的情况，或自定义解析）
                try {
                    // 处理如 "3"、"4" 等数字字符串
                    return Integer.parseInt(channelsStr.trim());
                } catch (NumberFormatException e) {
                    // 无法解析时返回0或抛出异常，根据业务需求调整
                    System.err.println("不支持的声道类型：" + channelsStr);
                    return 0;
                }
        }
    }

    /**
     * 通用位深解析逻辑（修复 FLAC/WAV 解析错误，兼容标准格式）
     */
    private int getBitDepthUniversal(File audioFile, AudioHeader header) {
        String format = header.getFormat().toUpperCase().trim();

        // 针对WAV/FLAC格式，优先通过文件字节精准解析
        try {
            if ("WAV".equals(format)) {
                int wavBitDepth = getWavBitDepth(audioFile);
                return wavBitDepth > 0 ? wavBitDepth : 16; // 兜底默认16bit
            } else if ("FLAC".equals(format)) {
                int flacBitDepth = getFlacBitDepth(audioFile);
                return flacBitDepth > 0 ? flacBitDepth : 24; // 兜底默认24bit
            }
        } catch (Exception e) {
            // 字节解析失败，走格式兜底逻辑
            System.err.println(e.getMessage());
        }

        // 格式兜底（无损格式默认24bit，有损默认16bit）
        if ("FLAC".equals(format) || "WAV".equals(format)) {
            return 24;
        } else {
            return 16; // MP3/AAC/OGG等有损格式默认位深
        }
    }

    /**
     * 解析WAV文件位深（精准定位fmt块，处理字节序，兼容标准WAV）
     * WAV格式规范：https://docs.fileformat.com/audio/wav/
     */
    private int getWavBitDepth(File wavFile) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(wavFile, "r")) {
            // 1. 验证WAV文件头（RIFF + WAVE）
            byte[] riffHeader = new byte[12];
            raf.readFully(riffHeader);
            String riff = new String(riffHeader, 0, 4);
            String wave = new String(riffHeader, 8, 4);
            if (!"RIFF".equals(riff) || !"WAVE".equals(wave)) {
                throw new IOException("不是标准WAV文件");
            }

            // 2. 定位fmt 块（WAV的核心格式块）
            byte[] chunkHeader = new byte[8];
            while (raf.read(chunkHeader) == 8) {
                String chunkId = new String(chunkHeader, 0, 4);
                long chunkSize = bytesToLittleEndianLong(chunkHeader, 4, 4); // 小端序解析块大小

                // 找到fmt 块，解析位深
                if ("fmt ".equals(chunkId)) {
                    // fmt块结构：PCM格式下，位深在fmt块的第14-15字节（偏移16）
                    raf.seek(raf.getFilePointer() + 14); // 跳过前14字节（格式类型、声道数、采样率等）
                    short bitsPerSample = raf.readShort(); // 位深（小端序，readShort已处理）
                    return bitsPerSample & 0xFFFF; // 转为无符号整数
                }

                // 跳过非fmt块的内容
                raf.seek(raf.getFilePointer() + chunkSize);
            }

            throw new IOException("WAV文件中未找到fmt块");
        }
    }

    /**
     * 解析FLAC文件位深（精准定位STREAMINFO块，兼容FLAC标准）
     * FLAC格式规范：https://xiph.org/flac/format.html#streaminfo
     */
    private int getFlacBitDepth(File flacFile) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(flacFile, "r")) {
            // 1. 验证FLAC文件头（fLaC）
            byte[] flacHeader = new byte[4];
            raf.readFully(flacHeader);
            if (!(flacHeader[0] == 0x66 && flacHeader[1] == 0x4C &&
                    flacHeader[2] == 0x61 && flacHeader[3] == 0x43)) { // "fLaC"的ASCII码
                throw new IOException("不是标准FLAC文件");
            }

            // 2. 读取第一个元数据块（必须是STREAMINFO块）
            byte[] metaHeader = new byte[4];
            raf.readFully(metaHeader);
            // 元数据块头部：第1位=是否最后块，接下来7位=块类型，后24位=块长度
            int metaType = (metaHeader[0] & 0x7F); // 提取块类型（0=STREAMINFO）
            if (metaType != 0) {
                throw new IOException("FLAC第一个元数据块不是STREAMINFO");
            }

            // 3. 解析STREAMINFO块中的位深（偏移17字节，即raf当前位置+10字节）
            raf.seek(raf.getFilePointer() + 10); // 跳过STREAMINFO前10字节（最小/最大帧大小、采样率等）
            byte bitDepthByte = raf.readByte(); // 第18字节（偏移17）：位深+声道数编码
            // FLAC位深编码规则：高4位 = 位深-1，取值范围1-16（对应位深2-17）
            int bitDepth = ((bitDepthByte >> 4) & 0x0F) + 1;
            // 验证位深有效性（FLAC支持4-32bit，常见16/24bit）
            return (bitDepth >= 4 && bitDepth <= 32) ? bitDepth : 0;
        }
    }

    /**
     * 小端序字节转长整型（处理WAV块大小解析）
     * @param bytes 字节数组
     * @param offset 起始偏移
     * @param length 字节长度（最多4字节）
     */
    private long bytesToLittleEndianLong(byte[] bytes, int offset, int length) {
        long result = 0;
        for (int i = 0; i < length; i++) {
            result |= ((long) (bytes[offset + i] & 0xFF)) << (8 * i);
        }
        return result;
    }

    /**
     * 音频元数据实体（兼容码率long/int）
     */
    @Data
    @ToString
    public static class AudioMetadata {
        private int sampleRate;    // 采样率（Hz）
        private int bitDepth;      // 位深（bit）
        private String format;     // 编码格式（MP3/FLAC/WAV等）
        private long bitRate;       // 码率（kbps）
        private int channels;      // 声道数
    }
}