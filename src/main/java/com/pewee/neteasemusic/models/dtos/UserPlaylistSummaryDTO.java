package com.pewee.neteasemusic.models.dtos;

import lombok.Data;

@Data
public class UserPlaylistSummaryDTO {
	private Long id;

    private String name;

    private String coverImgUrl;

    private String creator;

    private Integer trackCount;

    private String description;
}
