package com.pewee.neteasemusic.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.pewee.neteasemusic.enums.CommonRespInfo;
import com.pewee.neteasemusic.exceptions.ServiceException;
import com.pewee.neteasemusic.models.dtos.AlbumAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.DownloadTask;
import com.pewee.neteasemusic.models.dtos.PlaylistAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.SingleMusicAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.TrackDTO;
import com.pewee.neteasemusic.repository.TaskRepository;
import com.pewee.neteasemusic.utils.FileUtils;
import com.pewee.neteasemusic.utils.HttpClientUtil;
import com.pewee.neteasemusic.utils.TagUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MusicDownloadService implements InitializingBean {

    public static final ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 60, TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(10000));

    @Resource
    private AnalysisService analysisService;

    @Value("${download.path}")
    private String path;

    private Boolean repeat = true;

    // 音质等级配置
    private volatile String qualityLevel = "lossless";

    // 歌单下载时是否将歌单名作为专辑信息写入标签
    private volatile Boolean playlistAsAlbum = false;

    private BufferedWriter bw;
    private HashSet<Long> hs;
    private ArrayBlockingQueue<Long> queue;

    // 下载任务队列管理
    private final ConcurrentHashMap<String, DownloadTask> taskMap = new ConcurrentHashMap<>();

    // SQLite 持久化仓库
    private final TaskRepository taskRepository = new TaskRepository();

    @Override
    public void afterPropertiesSet() throws Exception {
        // 首先确保下载目录存在（必须最先执行）
        File pathDir = new File(path);
        if (!pathDir.exists()) {
            boolean created = pathDir.mkdirs();
            if (!created) {
                log.warn("无法创建下载目录: {}，将使用备用目录: {}", path, System.getProperty("user.home") + "/neteasemusic-dl/");
                path = System.getProperty("user.home") + "/neteasemusic-dl/";
                pathDir = new File(path);
                if (!pathDir.exists()) {
                    pathDir.mkdirs();
                }
            }
        }

        String repeatFilePath = path + "repeat";
        File repeatFile = new File(repeatFilePath);
        if (!repeatFile.exists()) {
            this.repeat = true;
        } else {
            try (FileInputStream fileInputStream = new FileInputStream(repeatFile)) {
                byte[] arr = new byte[1];
                fileInputStream.read(arr);
                String string = new String(arr);
                log.info("读取到repeat:{}", string);
                this.repeat = "1".equals(string);
            } catch (IOException e) {
                log.error("读取文件错误!", e);
            }
        }

        // 读取音质配置
        String qualityFilePath = path + "quality.txt";
        File qualityFile = new File(qualityFilePath);
        if (qualityFile.exists()) {
            try (FileInputStream fis = new FileInputStream(qualityFile)) {
                byte[] arr = new byte[(int) qualityFile.length()];
                fis.read(arr);
                String q = new String(arr).trim();
                if (!q.isEmpty()) {
                    this.qualityLevel = q;
                    log.info("读取到音质配置:{}", q);
                }
            } catch (IOException e) {
                log.error("读取音质配置错误!", e);
            }
        }

        // 读取歌单作为专辑配置
        String playlistAsAlbumFilePath = path + "playlist_as_album.txt";
        File playlistAsAlbumFile = new File(playlistAsAlbumFilePath);
        if (playlistAsAlbumFile.exists()) {
            try (FileInputStream fis = new FileInputStream(playlistAsAlbumFile)) {
                byte[] arr = new byte[1];
                fis.read(arr);
                String val = new String(arr).trim();
                this.playlistAsAlbum = "1".equals(val);
                log.info("读取到playlistAsAlbum配置:{}", val);
            } catch (IOException e) {
                log.error("读取playlistAsAlbum配置错误!", e);
            }
        }

        String idsFile = path + "ids.txt";
        File file = new File(idsFile);
        if (!file.exists()) {
            file.createNewFile();
        }
        bw = new BufferedWriter(new FileWriter(idsFile, true));
        hs = new HashSet<>();
        queue = new ArrayBlockingQueue<>(5000);

        long length = file.length();
        byte[] arr = new byte[(int) length];
        try (FileInputStream i = new FileInputStream(file)) {
            i.read(arr);
        }
        String origin = new String(arr);
        hs.addAll(Arrays.asList(origin.split(" ")).stream().map(String::trim).filter(StringUtils::isNotBlank)
                .map(Long::valueOf).collect(Collectors.toList()));
        log.info("读取到已下载记录{}条!", hs.size());

        // ===================== 初始化 SQLite 任务持久化 =====================
        taskRepository.init(path);
        restoreTasksFromDb();
    }

    /**
     * 从 SQLite 恢复任务记录
     * - COMPLETED / FAILED 任务原样恢复
     * - WAITING / DOWNLOADING 任务标记为 FAILED（因重启中断）
     */
    private void restoreTasksFromDb() {
        List<DownloadTask> dbTasks = taskRepository.findAll();
        if (dbTasks.isEmpty()) {
            log.info("SQLite 中无历史任务记录");
            return;
        }
        int interrupted = 0;
        for (DownloadTask task : dbTasks) {
            if (task.getStatus() == DownloadTask.Status.WAITING
                    || task.getStatus() == DownloadTask.Status.DOWNLOADING) {
                // 重启前未完成的任务标记为失败
                task.setStatus(DownloadTask.Status.FAILED);
                task.setErrorMessage("应用重启，任务中断");
                task.setCompleteTime(System.currentTimeMillis());
                taskRepository.save(task);
                interrupted++;
            }
            taskMap.put(task.getTaskId(), task);
        }
        log.info("从 SQLite 恢复了 {} 条任务记录（其中 {} 条因重启标记为失败）",
                dbTasks.size(), interrupted);
    }

    @Scheduled(cron = "*/5 * * * * ?")
    public void syncLocalFile() {
        if (!repeat && !queue.isEmpty()) {
            String join = " " + String.join(" ", queue.stream().map(String::valueOf).collect(Collectors.toList()));
            try {
                bw.append(join);
                bw.flush();
                queue.clear();
            } catch (IOException e) {
                log.error("写入文件失败!", e);
            }
        }
    }

    // ===================== 音质配置 =====================

    public String getQualityLevel() {
        return qualityLevel;
    }

    public void setQualityLevel(String level) {
        this.qualityLevel = level;
        // 持久化到文件
        String qualityFilePath = path + "quality.txt";
        File qualityFile = new File(qualityFilePath);
        try {
            if (!qualityFile.exists()) {
                File parent = qualityFile.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                qualityFile.createNewFile();
            }
            try (FileOutputStream fos = new FileOutputStream(qualityFile)) {
                fos.write(level.getBytes());
                fos.flush();
            }
        } catch (IOException e) {
            log.error("保存音质配置失败!", e);
        }
    }

    // ===================== 下载路径配置 =====================

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    // ===================== 歌单作为专辑配置 =====================

    public Boolean getPlaylistAsAlbum() {
        return this.playlistAsAlbum;
    }

    public void setPlaylistAsAlbum(Boolean playlistAsAlbum) {
        this.playlistAsAlbum = playlistAsAlbum;
        // 持久化到文件
        String filePath = path + "playlist_as_album.txt";
        File file = new File(filePath);
        try {
            if (!file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && !parent.exists()) parent.mkdirs();
                file.createNewFile();
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(playlistAsAlbum ? "1".getBytes() : "0".getBytes());
                fos.flush();
            }
        } catch (IOException e) {
            log.error("保存playlistAsAlbum配置失败!", e);
        }
    }

    // ===================== 重复下载配置 =====================

    public void setRepeat(Boolean repeat) {
        String repeatFile = path + "repeat";
        File file = new File(repeatFile);
        if (file.exists()) {
            file.delete();
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            log.error("创建文件错误!", e);
        }
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            fileOutputStream.write(repeat ? "1".getBytes() : "0".getBytes());
            fileOutputStream.flush();
        } catch (IOException e) {
            log.error("写入文件错误!", e);
        }
        this.repeat = repeat;
    }

    public Boolean getRepeat() {
        return this.repeat;
    }

    // ===================== 下载队列查询 =====================

    public List<DownloadTask> getDownloadQueue() {
        List<DownloadTask> list = new ArrayList<>(taskMap.values());
        list.sort(Comparator.comparingLong(t -> -t.getCreateTime()));
        return list;
    }

    public List<DownloadTask> getRecentTasks(int limit) {
        return getDownloadQueue().stream().limit(limit).collect(Collectors.toList());
    }

    public long getWaitingCount() {
        return taskMap.values().stream()
                .filter(t -> t.getStatus() == DownloadTask.Status.WAITING || t.getStatus() == DownloadTask.Status.DOWNLOADING)
                .count();
    }

    /**
     * 获取下载统计数据
     */
    public java.util.Map<String, Object> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total", taskRepository.countAll());
        stats.put("completed", taskRepository.countByStatus(DownloadTask.Status.COMPLETED));
        stats.put("failed", taskRepository.countByStatus(DownloadTask.Status.FAILED));
        stats.put("downloading", taskRepository.countByStatus(DownloadTask.Status.DOWNLOADING));
        stats.put("waiting", taskRepository.countByStatus(DownloadTask.Status.WAITING));
        return stats;
    }

    /**
     * 清理已完成和失败的任务
     * @return 清理的任务数量
     */
    public int clearFinishedTasks() {
        int before = taskMap.size();
        taskMap.entrySet().removeIf(entry -> {
            DownloadTask.Status status = entry.getValue().getStatus();
            return status == DownloadTask.Status.COMPLETED || status == DownloadTask.Status.FAILED;
        });
        int removed = before - taskMap.size();
        // 同步删除 SQLite
        taskRepository.deleteByStatuses("COMPLETED", "FAILED");
        log.info("清理了 {} 个已完成/失败的任务", removed);
        return removed;
    }

    /**
     * 清理所有任务（包括正在下载的）
     * @return 清理的任务数量
     */
    public int clearAllTasks() {
        int before = taskMap.size();
        taskMap.clear();
        // 同步删除 SQLite
        taskRepository.deleteAll();
        log.info("清理了全部 {} 个任务", before);
        return before;
    }

    // ===================== 下载逻辑 =====================

    private String getType(String url) {
        return url.substring(url.lastIndexOf("."), url.indexOf("?"));
    }

    public void downloadSingleSongV2(Long id) {
        doDownloadSingleSongV2(id, this.path, DownloadTask.Type.SINGLE, null, null, null, null, 0);
    }

    public void downloadPlaylistV2(Long id) {
        PlaylistAnalysisRespDTO analysisPlaylist = analysisService.analyzePlaylist(id);
        if (200 != analysisPlaylist.getStatus()) {
            throw new ServiceException(CommonRespInfo.SYS_ERROR);
        }
        List<TrackDTO> tracks = analysisPlaylist.getPlaylist().getTracks();
        String parentName = analysisPlaylist.getPlaylist().getName();
        String playlistCreator = analysisPlaylist.getPlaylist().getCreator();
        for (int i = 0; i < tracks.size(); i++) {
            TrackDTO trackDTO = tracks.get(i);
            final int trackIndex = i + 1;
            executor.execute(() -> {
                doDownloadSingleSongV2(trackDTO.getId(),
                        this.path + "歌单/" + FileUtils.getValidatedPathName(parentName) + "/",
                        DownloadTask.Type.PLAYLIST, id, parentName, parentName, playlistCreator, trackIndex);
            });
        }
    }

    public void downloadAlbumV2(Long id) {
        AlbumAnalysisRespDTO analysisAlbum = analysisService.analyzeAlbum(id);
        if (200 != analysisAlbum.getStatus()) {
            throw new ServiceException(CommonRespInfo.SYS_ERROR);
        }
        List<TrackDTO> tracks = analysisAlbum.getAlbum().getSongs();
        String parentName = analysisAlbum.getAlbum().getName();
        for (TrackDTO trackDTO : tracks) {
            executor.execute(() -> {
                doDownloadSingleSongV2(trackDTO.getId(),
                        this.path + "专辑/" + FileUtils.getValidatedPathName(parentName) + "/",
                        DownloadTask.Type.ALBUM, id, parentName, null, null, 0);
            });
        }
    }

    private void doDownloadSingleSongV2(Long id, String dirPath, DownloadTask.Type type,
                                         Long parentId, String parentName,
                                         String overrideAlbum, String overrideAlbumArtist,
                                         int playlistTrackIndex) {
        if (!repeat && hs.contains(id)) {
            log.info("歌曲id: {} 已存在,跳过!", id);
            return;
        }

        // 先获取歌曲信息以创建任务
        SingleMusicAnalysisRespDTO analysisSingleMusic = analysisService.analyzeSingleSong(id, qualityLevel);
        if (analysisSingleMusic == null || 200 != analysisSingleMusic.getStatus()) {
            log.error("解析歌曲id: {} 失败!", id);
            DownloadTask failedTask = DownloadTask.create(id, "未知歌曲(id:" + id + ")", "", "",
                    type, parentId, parentName);
            failedTask.setStatus(DownloadTask.Status.FAILED);
            failedTask.setErrorMessage("歌曲解析失败");
            failedTask.setCompleteTime(System.currentTimeMillis());
            taskMap.put(failedTask.getTaskId(), failedTask);
            taskRepository.save(failedTask);
            return;
        }

        // 创建下载任务并加入队列
        DownloadTask task = DownloadTask.create(id, analysisSingleMusic.getName(),
                analysisSingleMusic.getAr_name(), analysisSingleMusic.getAl_name(),
                type, parentId, parentName);
        taskMap.put(task.getTaskId(), task);
        taskRepository.save(task);

        task.setStatus(DownloadTask.Status.DOWNLOADING);
        task.markDownloadStart();
        taskRepository.save(task);

        CloseableHttpResponse response = null;
        try {
            String fileName = analysisSingleMusic.getName();
            String fileExt = getType(analysisSingleMusic.getUrl());
            Path targetPath = Paths.get(dirPath, fileName + fileExt);

            log.info("开始将歌曲: {} 写入目录: {}", fileName, dirPath);

            // 使用 HttpClient 直接下载，获取 Content-Length 用于进度追踪
            HttpGet httpGet = new HttpGet(analysisSingleMusic.getUrl());
            httpGet.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            response = HttpClientUtil.getInstance().execute(httpGet);
            HttpEntity entity = response.getEntity();

            // 获取文件总大小
            long totalSize = entity.getContentLength();
            if (totalSize <= 0) {
                // 从 Header 尝试获取
                Header contentLengthHeader = response.getFirstHeader("Content-Length");
                if (contentLengthHeader != null) {
                    try {
                        totalSize = Long.parseLong(contentLengthHeader.getValue());
                    } catch (NumberFormatException ignored) {}
                }
            }
            task.setTotalSize(totalSize > 0 ? totalSize : 0);

            // 带进度追踪的文件写入
            Files.createDirectories(targetPath.getParent());
            try (InputStream is = entity.getContent();
                 OutputStream os = Files.newOutputStream(targetPath, StandardOpenOption.CREATE_NEW)) {
                byte[] buffer = new byte[8192];
                long bytesDownloaded = 0;
                int len;
                int progressUpdateCounter = 0;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                    bytesDownloaded += len;
                    progressUpdateCounter++;
                    // 每 32 次循环（约 256KB）更新一次进度，避免过于频繁
                    if (progressUpdateCounter >= 32) {
                        task.updateProgress(bytesDownloaded, totalSize);
                        progressUpdateCounter = 0;
                    }
                }
                os.flush();
                // 最终更新进度为 100%
                task.updateProgress(bytesDownloaded, totalSize);
            }

            // 下载失败时清理不完整的文件
            if (task.getBytesDownloaded() > 0 && totalSize > 0
                    && task.getBytesDownloaded() < totalSize) {
                throw new IOException("下载不完整: 已下载 " + task.getBytesDownloaded()
                        + " / " + totalSize + " 字节");
            }

            File file = targetPath.toFile();

            // 写入完整ID3标签（含封面、专辑艺术家、光盘号、音轨号、年份）
            String albumName = analysisSingleMusic.getAl_name();
            String albumArtist = analysisSingleMusic.getAlbum_artist();
            Integer discNumber = analysisSingleMusic.getDisc_number();
            Integer trackNumber = analysisSingleMusic.getTrack_number();
            // 歌单下载时，若开启playlistAsAlbum配置，使用歌单名和创建者覆盖专辑信息
            if (type == DownloadTask.Type.PLAYLIST && playlistAsAlbum
                    && overrideAlbum != null && !overrideAlbum.isEmpty()) {
                albumName = overrideAlbum;
                if (overrideAlbumArtist != null && !overrideAlbumArtist.isEmpty()) {
                    albumArtist = overrideAlbumArtist;
                }
                // 使用歌单内曲目序号作为音轨号，光盘号统一为1
                if (playlistTrackIndex > 0) {
                    trackNumber = playlistTrackIndex;
                }
                discNumber = 1;
                //将年份信息设置为当前年份
                String currYear = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
                analysisSingleMusic.setYear(Integer.valueOf(currYear));
            }
            TagUtils.setTags(file, analysisSingleMusic.getName(), analysisSingleMusic.getAr_name(),
                    albumName, albumArtist,
                    discNumber, trackNumber,
                    analysisSingleMusic.getYear(), analysisSingleMusic.getPic());

            log.info("将歌曲: {} 写入目录: {} 已完成!", fileName, dirPath);

            // 写入歌词
            try {
                log.info("开始将歌词: {} 写入目录: {}", fileName, dirPath);
                FileUtils.writeToFile(Paths.get(dirPath, fileName + ".lrc"),
                        analysisSingleMusic.getLyric().getBytes("UTF-8"));
                log.info("将歌词: {} 写入目录: {} 已完成!", fileName, dirPath);
            } catch (UnsupportedEncodingException e) {
                log.error("歌词写入失败", e);
            }

            task.setStatus(DownloadTask.Status.COMPLETED);
            task.setCompleteTime(System.currentTimeMillis());
            task.setSpeed(0);
            task.setEta(0);
            taskRepository.save(task);

            hs.add(id);
            queue.offer(id);

        } catch (Exception e) {
            log.error("下载歌曲id: {} 失败!", id, e);
            task.setStatus(DownloadTask.Status.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompleteTime(System.currentTimeMillis());
            task.setSpeed(0);
            task.setEta(0);
            taskRepository.save(task);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    log.warn("关闭 HTTP 响应失败", e);
                }
            }
        }
    }
}
