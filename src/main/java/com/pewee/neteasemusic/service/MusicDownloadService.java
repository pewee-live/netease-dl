package com.pewee.neteasemusic.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.pewee.neteasemusic.config.AnalysisConfig;
import com.pewee.neteasemusic.enums.CommonRespInfo;
import com.pewee.neteasemusic.exceptions.ServiceException;
import com.pewee.neteasemusic.models.dtos.AlbumAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.PlaylistAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.SingleMusicAnalysisRespDTO;
import com.pewee.neteasemusic.models.dtos.TrackDTO;
import com.pewee.neteasemusic.utils.FileUtils;
import com.pewee.neteasemusic.utils.HttpClientUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class MusicDownloadService {
	
	public static final ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 60, TimeUnit.MINUTES, new ArrayBlockingQueue<>(10000));
	@Resource
	private AnalysisConfig config;
	
	@Value("${download.path}")
	private String path;
	
	@Resource
	private AnalysisService analysisService;
	
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}




	private static final String SINGLE_SONG = "Song_V1";
	private static final String PLAY_LIST = "Playlist";
	private static final String ALBUM = "Album";
	
	
	
	private String getType(String url) {
		return url.substring(url.lastIndexOf(".") , url.indexOf("?"));
	}
	
	public void downloadAlbum(Long id) {
		AlbumAnalysisRespDTO analysisAlbum = analysisAlbum(id);
		if (200 != analysisAlbum.getStatus()) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR);
		}
		List<TrackDTO> tracks = analysisAlbum.getAlbum().getSongs();
		for (TrackDTO trackDTO : tracks) {
			executor.execute( () -> {
				downloadSingleSong(trackDTO.getId());
			} );
		}
	}
	
	public void downloadPlaylist(Long id) {
		PlaylistAnalysisRespDTO analysisPlaylist = analysisPlaylist(id);
		if (200 != analysisPlaylist.getStatus()) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR);
		}
		List<TrackDTO> tracks = analysisPlaylist.getPlaylist().getTracks();
		for (TrackDTO trackDTO : tracks) {
			executor.execute( () -> {
				downloadSingleSong(trackDTO.getId());
			} );
		}
	}
	
	public void downloadSingleSong(Long id) {
		SingleMusicAnalysisRespDTO analysisSingleMusic = analysisSingleMusic(id);
		if (200 != analysisSingleMusic.getStatus()) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR);
		}
		String dir = path + analysisSingleMusic.getAl_name();
		String fileName = analysisSingleMusic.getName();
		log.info("开始将歌曲: {} 写入目录: {}",fileName, dir);
		FileUtils.writeToFile(Paths.get(dir ,fileName + getType(analysisSingleMusic.getUrl())), 
				HttpClientUtil.getInputStream(analysisSingleMusic.getUrl(), null));
		log.info("将歌曲: {} 写入目录: {} 已完成!",fileName, dir);
		try {
			log.info("开始将歌词: {} 写入目录: {}",fileName, dir);
			FileUtils.writeToFile(Paths.get(dir ,fileName + ".lrc"), 
					analysisSingleMusic.getLyric().getBytes("UTF-8"));
			log.info("将歌词: {} 写入目录: {} 已完成!",fileName, dir);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}
	
	private AlbumAnalysisRespDTO analysisAlbum(Long id) {
		log.info("解析专辑id:{}",id);
		StringBuilder stringBuilder = new StringBuilder("");
		stringBuilder.append("http://").append(config.getIp())
		.append(":").append(config.getPort()).append("/").append(ALBUM)
		.append("?type=json&level=lossless").append("&id=").append(id);
		String executeGet = null;
		try {
			executeGet = HttpClientUtil.executeGet(stringBuilder.toString(), null, null);
		} catch (IOException | URISyntaxException e) {
			log.error("调用解析失败!!",e);
			throw new ServiceException(CommonRespInfo.SYS_ERROR, e);
		}
		AlbumAnalysisRespDTO respDTO = JSON.parseObject(executeGet, AlbumAnalysisRespDTO.class);
		log.info("调用解析返回:{}",JSON.toJSONString(respDTO));
		return  respDTO;
	}
	
	private PlaylistAnalysisRespDTO analysisPlaylist(Long id) {
		log.info("解析歌单id:{}",id);
		StringBuilder stringBuilder = new StringBuilder("");
		stringBuilder.append("http://").append(config.getIp())
		.append(":").append(config.getPort()).append("/").append(PLAY_LIST)
		.append("?type=json&level=lossless").append("&id=").append(id);
		String executeGet = null;
		try {
			executeGet = HttpClientUtil.executeGet(stringBuilder.toString(), null, null);
		} catch (IOException | URISyntaxException e) {
			log.error("调用解析失败!!",e);
			throw new ServiceException(CommonRespInfo.SYS_ERROR, e);
		}
		PlaylistAnalysisRespDTO respDTO = JSON.parseObject(executeGet, PlaylistAnalysisRespDTO.class);
		log.info("调用解析返回:{}",JSON.toJSONString(respDTO));
		return  respDTO;
	}
	
	private SingleMusicAnalysisRespDTO analysisSingleMusic(Long ids) {
		log.info("解析音乐id:{}",ids);
		StringBuilder stringBuilder = new StringBuilder("");
		stringBuilder.append("http://").append(config.getIp())
		.append(":").append(config.getPort()).append("/").append(SINGLE_SONG)
		.append("?type=json&level=lossless").append("&ids=").append(ids);
		String executeGet = null;
		try {
			executeGet = HttpClientUtil.executeGet(stringBuilder.toString(), null, null);
		} catch (IOException | URISyntaxException e) {
			log.error("调用解析失败!!",e);
			throw new ServiceException(CommonRespInfo.SYS_ERROR, e);
		}
		SingleMusicAnalysisRespDTO respDTO = JSON.parseObject(executeGet, SingleMusicAnalysisRespDTO.class);
		log.info("调用解析返回:{}",JSON.toJSONString(respDTO));
		return respDTO;
	}
	

	public void downloadSingleSongV2(Long id) {
		SingleMusicAnalysisRespDTO analysisSingleMusic = analysisService.analyzeSingleSong(id, "lossless");
		if (200 != analysisSingleMusic.getStatus()) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR);
		}
		String dir = path + analysisSingleMusic.getAl_name();
		String fileName = analysisSingleMusic.getName();
		log.info("开始将歌曲: {} 写入目录: {}",fileName, dir);
		FileUtils.writeToFile(Paths.get(dir ,fileName + getType(analysisSingleMusic.getUrl())), 
				HttpClientUtil.getInputStream(analysisSingleMusic.getUrl(), null));
		log.info("将歌曲: {} 写入目录: {} 已完成!",fileName, dir);
		try {
			log.info("开始将歌词: {} 写入目录: {}",fileName, dir);
			FileUtils.writeToFile(Paths.get(dir ,fileName + ".lrc"), 
					analysisSingleMusic.getLyric().getBytes("UTF-8"));
			log.info("将歌词: {} 写入目录: {} 已完成!",fileName, dir);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
	}

	public void downloadPlaylistV2(Long id) {
		PlaylistAnalysisRespDTO analysisPlaylist = analysisService.analyzePlaylist(id);
		if (200 != analysisPlaylist.getStatus()) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR);
		}
		List<TrackDTO> tracks = analysisPlaylist.getPlaylist().getTracks();
		for (TrackDTO trackDTO : tracks) {
			executor.execute( () -> {
				downloadSingleSongV2(trackDTO.getId());
			} );
		}
	}

	public void downloadAlbumV2(Long id) {
		AlbumAnalysisRespDTO analysisAlbum = analysisService.analyzeAlbum(id);
		if (200 != analysisAlbum.getStatus()) {
			throw new ServiceException(CommonRespInfo.SYS_ERROR);
		}
		List<TrackDTO> tracks = analysisAlbum.getAlbum().getSongs();
		for (TrackDTO trackDTO : tracks) {
			executor.execute( () -> {
				downloadSingleSongV2(trackDTO.getId());
			} );
		}
		
	}
	
	
}
