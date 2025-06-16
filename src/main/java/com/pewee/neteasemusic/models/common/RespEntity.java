package com.pewee.neteasemusic.models.common;

import java.io.Serializable;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pewee.neteasemusic.exceptions.ServiceException;

/**
 * 返回对象
 * @author pewee && gongran
 *
 */
public class RespEntity<T> implements IResponse,Serializable{

	private static final long serialVersionUID = 239216757737141280L;

	private String code;
	
	private String msg;
	
	private T data;

	private RespEntity() {
		super();
	}

	private RespEntity(IResponse resp, T data) {
		this.code = resp.getCode();
		this.msg = resp.getMsg();
		this.data = data;
	}
	
	private RespEntity(String code,String msg , T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}
	
	public RespEntity<T> setCode(String code){
		this.code = code;
		return this;
	}
	
	public RespEntity<T> setMsg(String msg){
		this.msg = msg;
		return this;
	}
	
	public static <T> RespEntity<T> apply(String code,String msg, T data){
		return new RespEntity<>(code,msg,data);
	}

	public static <T> RespEntity<T> apply(IResponse resp, T data){
		return new RespEntity<>(resp,data);
	}
	
	public static <T> RespEntity<T> applyRespCodeMsg(IResponse resp) {
		return new RespEntity<>(resp, null);
	}

	public RespEntity<T> fillData(T data){
		this.data = data;
		return this;
	}

	public RespEntity<T> fillRespCodeMsg(IResponse resp){
		this.code = resp.getCode();
		this.msg = resp.getMsg();
		return this;
	}
	
	@Override
	public String getCode() {
		return code;
	}

	@Override
	public String getMsg() {
		return msg;
	}
	
	public T getData() {
		return data;
	}
	
	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}
	
	@JsonIgnore
	@JSONField(serialize = false)
	public T getCheckedData() {
        checkError();
        return data;
    }
	
	public void checkError() throws ServiceException {
        if (!code.startsWith("-")) {
            return;
        }
        // 业务异常
        throw new ServiceException(code, msg);
    }
}
