package com.pewee.neteasemusic.controller;

import javax.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pewee.neteasemusic.enums.CommonRespInfo;
import com.pewee.neteasemusic.models.common.RespEntity;
import com.pewee.neteasemusic.service.MusicDownloadService;

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
}
