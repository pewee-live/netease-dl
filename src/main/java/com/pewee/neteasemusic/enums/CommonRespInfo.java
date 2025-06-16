package com.pewee.neteasemusic.enums;

import com.pewee.neteasemusic.models.common.IResponse;

/**
 * 返回
 * @author pewee
 *
 */
public enum CommonRespInfo implements IResponse {
	SYS_ERROR("-100000","系统错误"),
	NOT_LEGAL_PARAM("-100002","参数错误"),
	SERVICE_EXECUTION_ERROR("-100022","服务执行错误"),
	NO_COOKIE_ERROR("-100023","还未设置cookie!"),
	SUCCESS("000000","成功");
	
	
	CommonRespInfo(String code, String msg) {
		this.code = code;
		this.msg = msg;
	}
	
	private String code;
	
	private String msg;
	
	
	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMsg() {
		return msg;
	}
	
	
	
}
