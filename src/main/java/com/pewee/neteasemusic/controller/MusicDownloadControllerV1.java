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
 * 基于https://github.com/Suxiaoqinx/Netease_url api的接口,通过http api的方式调用python项目
 * 需要先启动python项目后才可以调用
 * @author pewee
 *
 */
@RestController
@Slf4j
@RequestMapping("/v1")
public class MusicDownloadControllerV1 {
	
	@Resource
	private MusicDownloadService musicService;
	
	@GetMapping("/single")
	public RespEntity<String> downloadSingle(@RequestParam(value = "id") Long id) {
		musicService.downloadSingleSong(id);
		return RespEntity.apply(CommonRespInfo.SUCCESS,"OK");
	}
	
	@GetMapping("/playlist")
	public RespEntity<String> downloadPlaylist(@RequestParam(value = "id") Long id) {
		musicService.downloadPlaylist(id);
		return RespEntity.apply(CommonRespInfo.SUCCESS,"OK");
	}
	
	
	@GetMapping("/album")
	public RespEntity<String> downloadAlbum(@RequestParam(value = "id") Long id) {
		musicService.downloadAlbum(id);
		return RespEntity.apply(CommonRespInfo.SUCCESS,"OK");
	}
}
