package com.pewee.neteasemusic.models.dtos;

import lombok.Data;

/**
 * 列表展示用的专辑实体
 * @author pewee
 *
 */
@Data
public class AlbumDTO 	{
	
	private Long id;
    private String name;
    private Integer size;
    private String picUrl;
    private Long publishTime;
    private String description;
    private String tags;
    private String company;
    private ArtistDTO artist;

}
