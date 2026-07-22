package com.pewee.neteasemusic.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TagUtils {

    /**
     * 写入完整ID3标签（含封面图片）
     */
    public static void setTags(File file, String title, String artist, String album,
                               String albumArtist, Integer discNumber, Integer trackNumber,
                               Integer year, String coverUrl) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (tag == null) {
                tag = audioFile.createDefaultTag();
                audioFile.setTag(tag);
            }

            if (title != null) {
                tag.setField(FieldKey.TITLE, title);
            }
            if (artist != null) {
                tag.setField(FieldKey.ARTIST, artist);
            }
            if (album != null) {
                tag.setField(FieldKey.ALBUM, album);
            }
            if (albumArtist != null) {
                tag.setField(FieldKey.ALBUM_ARTIST, albumArtist);
            }
            if (discNumber != null) {
                tag.setField(FieldKey.DISC_NO, String.valueOf(discNumber));
            }
            if (trackNumber != null) {
                tag.setField(FieldKey.TRACK, String.valueOf(trackNumber));
            }
            if (year != null) {
                tag.setField(FieldKey.YEAR, String.valueOf(year));
            }

            // 下载封面并写入
            if (coverUrl != null && !coverUrl.isEmpty()) {
                writeCoverArt(tag, coverUrl);
            }

            audioFile.commit();
            log.info("Successfully set tags for file: {}", file.getName());
        } catch (Exception e) {
            log.error("Failed to set tags for file: " + file.getName(), e);
        }
    }

    /**
     * 下载封面图片并写入音频文件标签
     * 使用临时文件 + ArtworkFactory.createArtworkFromFile 确保兼容性
     */
    private static void writeCoverArt(Tag tag, String coverUrl) {
        File tempFile = null;
        try {
            // 将封面图片下载到临时文件
            tempFile = downloadImageToTempFile(coverUrl);
            if (tempFile != null && tempFile.exists() && tempFile.length() > 0) {
                Artwork artwork = ArtworkFactory.createArtworkFromFile(tempFile);
                tag.setField(artwork);
                log.info("封面图片写入成功");
            } else {
                log.warn("封面图片下载结果为空");
            }
        } catch (Exception e) {
            log.warn("封面图片下载或写入失败: {}", e.getMessage());
        } finally {
            // 清理临时文件
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 兼容旧方法（不写封面和扩展标签）
     */
    public static void setTags(File file, String title, String artist, String album) {
        setTags(file, title, artist, album, null, null, null, null, null);
    }

    /**
     * 下载图片到临时文件
     */
    private static File downloadImageToTempFile(String imageUrl) {
        File tempFile = null;
        try {
            // 从 URL 中判断文件后缀
            String suffix = ".jpg";
            String pathPart = imageUrl;
            int queryIdx = imageUrl.indexOf('?');
            if (queryIdx > 0) {
                pathPart = imageUrl.substring(0, queryIdx);
            }
            if (pathPart.endsWith(".png")) {
                suffix = ".png";
            } else if (pathPart.endsWith(".webp")) {
                suffix = ".webp";
            }

            tempFile = File.createTempFile("cover_", suffix);

            HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();

            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.flush();
            }

            log.info("封面图片已下载到临时文件: {} ({}bytes)", tempFile.getAbsolutePath(), tempFile.length());
            return tempFile;
        } catch (Exception e) {
            log.warn("下载封面图片失败: {}", e.getMessage());
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            return null;
        }
    }
}
