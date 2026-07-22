package com.pewee.neteasemusic.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pewee.neteasemusic.enums.CommonRespInfo;
import com.pewee.neteasemusic.models.common.RespEntity;
import com.pewee.neteasemusic.models.dtos.DownloadTask;
import com.pewee.neteasemusic.service.MusicDownloadService;
import com.pewee.neteasemusic.service.NeteaseAPIService;

import lombok.extern.slf4j.Slf4j;

/**
 * 基于java直接调用api的方式解析的下载功能
 * @author pewee
 *
 */
@RestController
@Slf4j
@RequestMapping("/v2")
public class MusicDownloadControllerV2 {
	
	@Resource
	private MusicDownloadService musicService;
	
	@Resource
	private NeteaseAPIService neteaseAPIService;
	
	@GetMapping("/setRepeat")
	public RespEntity<String> setFlag(@RequestParam(value = "repeat") Boolean repeat) {
		musicService.setRepeat(repeat);
		return RespEntity.apply(CommonRespInfo.SUCCESS,"OK");
	}
	
	@GetMapping("/getRepeat")
	public RespEntity<Boolean> getFlag() {
		Boolean flag = musicService.getRepeat();
		return RespEntity.apply(CommonRespInfo.SUCCESS,flag);
	}
	
	@GetMapping("/single")
	public RespEntity<String> downloadSingle(@RequestParam(value = "id") Long id) {
		musicService.downloadSingleSongV2(id);
		return RespEntity.apply(CommonRespInfo.SUCCESS,"OK");
	}
	
	@GetMapping("/playlist")
	public RespEntity<String> downloadPlaylist(@RequestParam(value = "id") Long id) {
		musicService.downloadPlaylistV2(id);
		return RespEntity.apply(CommonRespInfo.SUCCESS,"OK");
	}
	
	@GetMapping("/album")
	public RespEntity<String> downloadAlbum(@RequestParam(value = "id") Long id) {
		musicService.downloadAlbumV2(id);
		return RespEntity.apply(CommonRespInfo.SUCCESS,"OK");
	}
	
	// ===================== 下载队列 =====================
	
	@GetMapping("/queue")
	public RespEntity<List<DownloadTask>> getQueue(
			@RequestParam(value = "limit", defaultValue = "50") int limit) {
		return RespEntity.apply(CommonRespInfo.SUCCESS, musicService.getRecentTasks(limit));
	}
	
	@GetMapping("/queue/count")
	public RespEntity<Map<String, Object>> getQueueCount() {
		Map<String, Object> result = new HashMap<>();
		result.put("waiting", musicService.getWaitingCount());
		result.put("total", musicService.getDownloadQueue().size());
		return RespEntity.apply(CommonRespInfo.SUCCESS, result);
	}
	
	/**
	 * 下载统计数据（历史总量、已完成、失败等）
	 */
	@GetMapping("/queue/stats")
	public RespEntity<Map<String, Object>> getQueueStats() {
		return RespEntity.apply(CommonRespInfo.SUCCESS, musicService.getStats());
	}
	
	/**
	 * 清理已完成和失败的任务
	 */
	@PostMapping("/queue/clear")
	public RespEntity<Map<String, Object>> clearFinishedTasks() {
		int count = musicService.clearFinishedTasks();
		Map<String, Object> result = new HashMap<>();
		result.put("cleared", count);
		return RespEntity.apply(CommonRespInfo.SUCCESS, result);
	}
	
	/**
	 * 清理所有任务
	 */
	@PostMapping("/queue/clearAll")
	public RespEntity<Map<String, Object>> clearAllTasks() {
		int count = musicService.clearAllTasks();
		Map<String, Object> result = new HashMap<>();
		result.put("cleared", count);
		return RespEntity.apply(CommonRespInfo.SUCCESS, result);
	}
	
	// ===================== 音质配置 =====================
	
	@GetMapping("/quality")
	public RespEntity<String> getQuality() {
		return RespEntity.apply(CommonRespInfo.SUCCESS, musicService.getQualityLevel());
	}
	
	@PostMapping("/quality")
	public RespEntity<String> setQuality(@RequestParam(value = "level") String level) {
		musicService.setQualityLevel(level);
		return RespEntity.apply(CommonRespInfo.SUCCESS, "OK");
	}
	
	// ===================== 下载路径 =====================
	
	@GetMapping("/path")
	public RespEntity<String> getDownloadPath() {
		return RespEntity.apply(CommonRespInfo.SUCCESS, musicService.getPath());
	}
	
	// ===================== 歌单作为专辑配置 =====================
	
	@GetMapping("/playlistAsAlbum")
	public RespEntity<Boolean> getPlaylistAsAlbum() {
		return RespEntity.apply(CommonRespInfo.SUCCESS, musicService.getPlaylistAsAlbum());
	}
	
	@PostMapping("/playlistAsAlbum")
	public RespEntity<String> setPlaylistAsAlbum(@RequestParam(value = "enabled") Boolean enabled) {
		musicService.setPlaylistAsAlbum(enabled);
		return RespEntity.apply(CommonRespInfo.SUCCESS, "OK");
	}
	
	// ===================== 登出/重新登录 =====================
	
	/**
	 * 登出当前账号，清除cookie，以便重新登录
	 */
	@PostMapping("/logout")
	public RespEntity<String> logout() {
		neteaseAPIService.logout();
		return RespEntity.apply(CommonRespInfo.SUCCESS, "OK");
	}
}
