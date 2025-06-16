package com.pewee.neteasemusic.models.dtos;

import java.util.List;

import lombok.Data;

@Data
public class PlaylistInfoDTO {
	
	//歌单封面
	private String coverImgUrl;
	
	//创建人
	private String creator;
	
	//描述
	private String description;
	
	//id
	private Long id;
	
	//歌单名
	private String name;
	
	//track数量
	private Integer trackCount;
	
	//track列表
	private List<TrackDTO> tracks;
	
	
}
