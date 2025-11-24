package com.future.music.util;

import com.future.music.domain.dto.LyricLineDTO;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricParseUtil {
    // LRC格式正则：[00:03.00] 窗外的麻雀
    private static final Pattern LRC_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{1,2})\\](.*)");

    /**
     * 解析歌词内容为结构化syncLyric
     * @param lyricContent 歌词原始内容
     * @param lyricType 歌词格式（lrc/txt/krc）
     * @return 结构化歌词列表
     */
    public static List<LyricLineDTO> parse(String lyricContent, String lyricType) {
        List<LyricLineDTO> syncLyric = new ArrayList<>();

        // 空内容直接返回空列表
        if (!StringUtils.hasText(lyricContent)) {
            return syncLyric;
        }

        // 按格式解析
        switch (lyricType.toLowerCase()) {
            case "lrc":
                syncLyric = parseLrc(lyricContent);
                break;
            case "txt":
                syncLyric = parseTxt(lyricContent);
                break;
            case "krc":
                // KRC（逐字歌词）解析逻辑可自行扩展，此处先返回基础文本
                syncLyric = parseLrc(lyricContent); // 兼容LRC格式
                break;
            default:
                syncLyric = parseTxt(lyricContent);
                break;
        }

        return syncLyric;
    }

    /**
     * 解析LRC格式歌词
     */
    private static List<LyricLineDTO> parseLrc(String lyricContent) {
        List<LyricLineDTO> syncLyric = new ArrayList<>();
        String[] lines = lyricContent.split("\\r?\\n");

        for (String line : lines) {
            if (!StringUtils.hasText(line)) {
                continue;
            }

            Matcher matcher = LRC_PATTERN.matcher(line.trim());
            if (matcher.find()) {
                // 解析时间戳：分→秒→毫秒
                int minute = Integer.parseInt(matcher.group(1));
                int second = Integer.parseInt(matcher.group(2));
                int millisecond = Integer.parseInt(matcher.group(3));
                // 补全毫秒（如3→30ms，50→50ms）
                millisecond = millisecond < 10 ? millisecond * 100 : millisecond * 10;
                long totalTime = (minute * 60 + second) * 1000L + millisecond;

                // 解析歌词文本
                String text = matcher.group(4).trim();
                if (StringUtils.hasText(text)) {
                    syncLyric.add(new LyricLineDTO(totalTime, text));
                }
            }
        }

        return syncLyric;
    }

    /**
     * 解析纯文本歌词（无时间戳，按行均分时长）
     * @param lyricContent 纯文本歌词
     * @return 结构化歌词（时间戳均分）
     */
    private static List<LyricLineDTO> parseTxt(String lyricContent) {
        List<LyricLineDTO> syncLyric = new ArrayList<>();
        String[] lines = lyricContent.split("\\r?\\n");
        int lineCount = 0;
        // 统计有效行数
        for (String line : lines) {
            if (StringUtils.hasText(line.trim())) {
                lineCount++;
            }
        }
        if (lineCount == 0) {
            return syncLyric;
        }

        // 纯文本默认按总时长均分（此处暂用60秒示例，实际可传歌曲时长动态计算）
        long baseTime = 0;
        long interval = 60000L / lineCount; // 每行歌词间隔（毫秒）

        for (String line : lines) {
            String text = line.trim();
            if (!StringUtils.hasText(text)) {
                continue;
            }
            syncLyric.add(new LyricLineDTO(baseTime, text));
            baseTime += interval;
        }

        return syncLyric;
    }
}