package com.pewee.neteasemusic.models.dtos;

import lombok.Data;

@Data
public class SingleMusicAnalysisRespDTO {
	
	private Long id;
	
	private String al_name;
	
	private String ar_name;
	
	// 专辑艺术家（第一个艺术家）
	private String album_artist;
	
	private String lyric;
	
	private String name;
	
	private String pic;
	
	private String size;
	
	private Integer status;
	
	private String tlyric;
	
	private String url;
	
	// 发行年份
	private Integer year;
	
	// 光盘编号
	private Integer disc_number;
	
	// 音轨号
	private Integer track_number;
	
}
