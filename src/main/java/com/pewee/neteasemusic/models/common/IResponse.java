package com.pewee.neteasemusic.models.common;

/**
 * 微服务通用的返回信息
 * @author pewee
 */
public interface IResponse {
    public String getCode();

    public String getMsg();
}
