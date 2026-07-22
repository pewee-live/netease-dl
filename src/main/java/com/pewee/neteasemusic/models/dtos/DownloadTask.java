package com.pewee.neteasemusic.models.dtos;

import lombok.Data;

@Data
public class DownloadTask {

    public enum Status {
        WAITING, DOWNLOADING, COMPLETED, FAILED
    }

    public enum Type {
        SINGLE, ALBUM, PLAYLIST
    }

    private String taskId;
    private Long songId;
    private String songName;
    private String artist;
    private String album;
    private Status status;
    private Type type;
    private Long parentId;
    private String parentName;
    private String errorMessage;
    private Long createTime;
    private Long completeTime;

    // 进度追踪字段
    private long totalSize;        // 文件总字节数
    private long bytesDownloaded;  // 已下载字节数
    private double speed;          // 下载速度 bytes/s
    private long eta;              // 预计剩余时间（秒）

    // 内部追踪字段（不序列化给前端）
    private transient long downloadStartTime;  // 下载开始时间戳

    public static DownloadTask create(Long songId, String songName, String artist, String album,
                                       Type type, Long parentId, String parentName) {
        DownloadTask task = new DownloadTask();
        task.setTaskId(java.util.UUID.randomUUID().toString().replace("-", ""));
        task.setSongId(songId);
        task.setSongName(songName);
        task.setArtist(artist);
        task.setAlbum(album);
        task.setStatus(Status.WAITING);
        task.setType(type);
        task.setParentId(parentId);
        task.setParentName(parentName);
        task.setCreateTime(System.currentTimeMillis());
        return task;
    }

    /**
     * 更新进度：根据已下载字节数和总大小计算速度和ETA
     */
    public void updateProgress(long bytesDownloaded, long totalSize) {
        this.bytesDownloaded = bytesDownloaded;
        this.totalSize = totalSize;
        if (downloadStartTime > 0 && bytesDownloaded > 0) {
            long elapsedMs = System.currentTimeMillis() - downloadStartTime;
            if (elapsedMs > 0) {
                double elapsedSec = elapsedMs / 1000.0;
                this.speed = bytesDownloaded / elapsedSec;
                if (totalSize > 0 && this.speed > 0) {
                    long remaining = totalSize - bytesDownloaded;
                    this.eta = (long) (remaining / this.speed);
                } else {
                    this.eta = 0;
                }
            }
        }
    }

    public void markDownloadStart() {
        this.downloadStartTime = System.currentTimeMillis();
        this.bytesDownloaded = 0;
        this.speed = 0;
        this.eta = 0;
    }
}
