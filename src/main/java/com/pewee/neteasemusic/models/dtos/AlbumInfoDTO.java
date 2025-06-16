package com.pewee.neteasemusic.models.dtos;

import java.util.List;

import lombok.Data;

@Data
public class AlbumInfoDTO {
	
	//作者
	private String artist;
	
	//专辑封面
	private String coverImgUrl;
	
	//id
	private Long id;
	
	//专辑名
	private String name;
	
	//发布时间
	private Long publishTime;
	
	//tracks
	private List<TrackDTO> songs;
}
