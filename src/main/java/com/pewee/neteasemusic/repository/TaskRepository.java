package com.pewee.neteasemusic.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import com.pewee.neteasemusic.models.dtos.DownloadTask;

import lombok.extern.slf4j.Slf4j;

/**
 * SQLite 任务持久化仓库
 * 所有方法均为 synchronized，保证多线程安全（SQLite 单写者模型）
 */
@Slf4j
public class TaskRepository {

    private Connection conn;
    private String dbPath;

    /**
     * 初始化数据库连接并创建表
     */
    public synchronized void init(String downloadPath) {
        this.dbPath = downloadPath + "tasks.db";
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            // 开启 WAL 模式以提升并发读性能
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
            }
            createTable();
            log.info("SQLite 任务数据库初始化成功: {}", dbPath);
        } catch (SQLException e) {
            log.error("SQLite 初始化失败!", e);
        }
    }

    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS download_tasks ("
                + "task_id TEXT PRIMARY KEY,"
                + "song_id INTEGER,"
                + "song_name TEXT,"
                + "artist TEXT,"
                + "album TEXT,"
                + "status TEXT,"
                + "type TEXT,"
                + "parent_id INTEGER,"
                + "parent_name TEXT,"
                + "error_message TEXT,"
                + "create_time INTEGER,"
                + "complete_time INTEGER,"
                + "total_size INTEGER DEFAULT 0,"
                + "bytes_downloaded INTEGER DEFAULT 0"
                + ")";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * 插入或更新任务（upsert）
     */
    public synchronized void save(DownloadTask task) {
        if (conn == null) return;
        String sql = "INSERT OR REPLACE INTO download_tasks "
                + "(task_id, song_id, song_name, artist, album, status, type, parent_id, parent_name, "
                + "error_message, create_time, complete_time, total_size, bytes_downloaded) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, task.getTaskId());
            ps.setLong(2, task.getSongId());
            ps.setString(3, task.getSongName());
            ps.setString(4, task.getArtist());
            ps.setString(5, task.getAlbum());
            ps.setString(6, task.getStatus().name());
            ps.setString(7, task.getType().name());
            if (task.getParentId() != null) {
                ps.setLong(8, task.getParentId());
            } else {
                ps.setNull(8, Types.INTEGER);
            }
            ps.setString(9, task.getParentName());
            ps.setString(10, task.getErrorMessage());
            ps.setLong(11, task.getCreateTime());
            if (task.getCompleteTime() != null) {
                ps.setLong(12, task.getCompleteTime());
            } else {
                ps.setNull(12, Types.INTEGER);
            }
            ps.setLong(13, task.getTotalSize());
            ps.setLong(14, task.getBytesDownloaded());
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("保存任务失败: taskId={}", task.getTaskId(), e);
        }
    }

    /**
     * 仅更新进度相关字段（轻量级更新）
     */
    public synchronized void updateProgress(String taskId, long bytesDownloaded, long totalSize) {
        if (conn == null) return;
        String sql = "UPDATE download_tasks SET bytes_downloaded = ?, total_size = ? WHERE task_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bytesDownloaded);
            ps.setLong(2, totalSize);
            ps.setString(3, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("更新进度失败: taskId={}", taskId, e);
        }
    }

    /**
     * 删除任务
     */
    public synchronized void delete(String taskId) {
        if (conn == null) return;
        String sql = "DELETE FROM download_tasks WHERE task_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, taskId);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("删除任务失败: taskId={}", taskId, e);
        }
    }

    /**
     * 按状态批量删除
     */
    public synchronized int deleteByStatuses(String... statuses) {
        if (conn == null) return 0;
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < statuses.length; i++) {
            if (i > 0) placeholders.append(",");
            placeholders.append("?");
        }
        String sql = "DELETE FROM download_tasks WHERE status IN (" + placeholders + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < statuses.length; i++) {
                ps.setString(i + 1, statuses[i]);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("按状态删除任务失败", e);
            return 0;
        }
    }

    /**
     * 删除所有任务
     */
    public synchronized int deleteAll() {
        if (conn == null) return 0;
        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate("DELETE FROM download_tasks");
        } catch (SQLException e) {
            log.error("删除所有任务失败", e);
            return 0;
        }
    }

    /**
     * 查询所有任务
     */
    public synchronized List<DownloadTask> findAll() {
        List<DownloadTask> list = new ArrayList<>();
        if (conn == null) return list;
        String sql = "SELECT * FROM download_tasks ORDER BY create_time DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("查询所有任务失败", e);
        }
        return list;
    }

    /**
     * 查询最近 N 条任务
     */
    public synchronized List<DownloadTask> findRecent(int limit) {
        List<DownloadTask> list = new ArrayList<>();
        if (conn == null) return list;
        String sql = "SELECT * FROM download_tasks ORDER BY create_time DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            log.error("查询最近任务失败", e);
        }
        return list;
    }

    /**
     * 统计总数
     */
    public synchronized int countAll() {
        if (conn == null) return 0;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM download_tasks")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("统计任务数失败", e);
        }
        return 0;
    }

    /**
     * 按状态统计
     */
    public synchronized int countByStatus(DownloadTask.Status status) {
        if (conn == null) return 0;
        String sql = "SELECT COUNT(*) FROM download_tasks WHERE status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.error("按状态统计失败", e);
        }
        return 0;
    }

    /**
     * 关闭连接
     */
    public synchronized void close() {
        if (conn != null) {
            try {
                conn.close();
                log.info("SQLite 连接已关闭");
            } catch (SQLException e) {
                log.error("关闭 SQLite 连接失败", e);
            }
        }
    }

    /**
     * ResultSet 行映射到 DownloadTask
     */
    private DownloadTask mapRow(ResultSet rs) throws SQLException {
        DownloadTask task = new DownloadTask();
        task.setTaskId(rs.getString("task_id"));
        task.setSongId(rs.getLong("song_id"));
        task.setSongName(rs.getString("song_name"));
        task.setArtist(rs.getString("artist"));
        task.setAlbum(rs.getString("album"));
        task.setStatus(DownloadTask.Status.valueOf(rs.getString("status")));
        task.setType(DownloadTask.Type.valueOf(rs.getString("type")));
        long parentId = rs.getLong("parent_id");
        task.setParentId(rs.wasNull() ? null : parentId);
        task.setParentName(rs.getString("parent_name"));
        task.setErrorMessage(rs.getString("error_message"));
        task.setCreateTime(rs.getLong("create_time"));
        long completeTime = rs.getLong("complete_time");
        task.setCompleteTime(rs.wasNull() ? null : completeTime);
        task.setTotalSize(rs.getLong("total_size"));
        task.setBytesDownloaded(rs.getLong("bytes_downloaded"));
        return task;
    }
}
