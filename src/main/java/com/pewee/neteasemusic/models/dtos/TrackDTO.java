package com.pewee.neteasemusic.models.dtos;

import lombok.Data;

@Data
public class TrackDTO {
	
	//专辑名
	private String album;
	
	//作者
	private String artists;
	
	//id
	private Long id;
	
	//track名字
	private String name;
	
	//pic url
	private String picUrl;
	
	//光盘编号
	private Integer discNumber;
	
	//音轨号
	private Integer trackNumber;
}
