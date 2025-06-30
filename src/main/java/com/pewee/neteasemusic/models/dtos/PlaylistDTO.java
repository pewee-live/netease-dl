package com.pewee.neteasemusic.models.dtos;

import lombok.Data;

/**
 * 查询歌单用实体
 * @author pewee
 *
 */
@Data
public class PlaylistDTO {
	private Long id;
    private String name;
    private Integer trackCount;
    private String coverImgUrl;
    private String description;
}
