package com.future.music.util;

import com.future.music.domain.dto.LyricLineDTO;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LyricParseUtil {
    // ========== 常量定义 ==========
    // LRC基础时间戳正则（兼容1-3位毫秒、多时间戳）
    private static final Pattern LRC_TIME_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{1,3})\\]");
    // LRC注释行正则（[ti:][ar:][al:][by:][offset:]等）
    private static final Pattern LRC_COMMENT_PATTERN = Pattern.compile("^\\[(ti|ar|al|by|offset|re|ve):.*\\]$");
    // 默认歌词均分时长（秒）- 可配置化
    private static final long DEFAULT_TXT_TOTAL_DURATION = 60 * 1000L;

    /**
     * 解析歌词内容为结构化syncLyric
     * @param lyricContent 歌词原始内容
     * @param lyricType 歌词格式（lrc/txt/krc）
     * @return 结构化歌词列表
     */
    public static List<LyricLineDTO> parse(String lyricContent, String lyricType) {
        List<LyricLineDTO> syncLyric = new ArrayList<>();
        if (!StringUtils.hasText(lyricContent) || !StringUtils.hasText(lyricType)) {
            return syncLyric;
        }

        switch (lyricType.toLowerCase()) {
            case "lrc":
                syncLyric = parseLrc(lyricContent);
                break;
            case "txt":
                // TXT类型直接返回空列表（不生成同步歌词）
                break;
            default:
                syncLyric = new ArrayList<>();
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

        for (String s : lines) {
            String line = s.trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }

            // 跳过LRC注释行
            if (LRC_COMMENT_PATTERN.matcher(line).matches()) {
                continue;
            }

            // 提取所有时间戳
            Matcher timeMatcher = LRC_TIME_PATTERN.matcher(line);
            List<Long> timeList = new ArrayList<>();
            while (timeMatcher.find()) {
                try {
                    int minute = Integer.parseInt(timeMatcher.group(1));
                    int second = Integer.parseInt(timeMatcher.group(2));
                    String millisStr = timeMatcher.group(3);
                    // 统一转换为3位毫秒（补零/截断）
                    int millisecond = Integer.parseInt(String.format("%-3s", millisStr).replace(' ', '0').substring(0, 3));
                    long totalTime = (minute * 60L + second) * 1000L + millisecond;
                    timeList.add(totalTime);
                } catch (NumberFormatException e) {
                    continue;
                }
            }

            // 提取歌词文本（移除所有时间戳后内容）
            String text = LRC_TIME_PATTERN.matcher(line).replaceAll("").trim();
            if (timeList.isEmpty() || !StringUtils.hasText(text)) {
                continue;
            }

            // 多时间戳对应同一歌词
            for (Long time : timeList) {
                syncLyric.add(new LyricLineDTO(time, text));
            }
        }

        // 按时间戳排序（避免乱序）
        syncLyric.sort(Comparator.comparingLong(LyricLineDTO::getTime));
        return syncLyric;
    }

    /**
     * 解析TXT歌词为纯文本（保留换行，过滤无效空行）
     */
    public static String parseTxtToRawText(String lyricContent) {
        if (!StringUtils.hasText(lyricContent)) {
            return "";
        }
        String[] lines = lyricContent.split("\\r?\\n");
        StringBuilder rawText = new StringBuilder();
        boolean lastLineEmpty = false; // 标记上一行是否为空行
        for (String line : lines) {
            String trimLine = line.trim();
            if (StringUtils.hasText(trimLine)) {
                // 有内容的行：直接追加，重置空行标记
                rawText.append(line).append("\n");
                lastLineEmpty = false;
            } else {
                // 空行：仅保留一个连续空行
                if (!lastLineEmpty) {
                    rawText.append("\n");
                    lastLineEmpty = true;
                }
            }
        }
        return rawText.toString().trim(); // 移除首尾空白
    }

}