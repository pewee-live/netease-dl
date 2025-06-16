package com.pewee.neteasemusic.models.common;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author by GongRan
 * @Classname AttachmentInfo
 * @Description TODO
 * @Date 2022/8/12 15:13
 */
@Data
@ToString
@Accessors(chain = true)
public class AttachmentInfo {
    private boolean success;

    private String errorJson;

    private byte[] data;
}
