package com.pewee.neteasemusic.exceptions;

import com.pewee.neteasemusic.models.common.IResponse;

/**
 * 通用的服务异常
 * @author pewee
 *
 */
public class ServiceException extends RuntimeException implements IResponse {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5933752584125462129L;

	private String retutnCode;
	
	private String returnMsg; 
	
	public ServiceException(IResponse resp){	
		super(resp.getMsg(),new RuntimeException());
        this.retutnCode = resp.getCode();
        this.returnMsg = resp.getMsg();	
    }
	
	public ServiceException(String retutnCode, String returnMsg){
		super(returnMsg,new RuntimeException());
        this.retutnCode = retutnCode;
        this.returnMsg = returnMsg;	
    }
	
	public ServiceException(String retutnCode, String returnMsg,Exception e){
		super(returnMsg,e);
        this.retutnCode = retutnCode;
        this.returnMsg = returnMsg;	
    }
	
	public ServiceException(IResponse resp, Exception e) {
		super(e.getMessage(), e);
        this.retutnCode = resp.getCode();
        this.returnMsg = resp.getMsg();	   
    }


	@Override
	public String getCode() {
		return this.retutnCode;
	}

	@Override
	public String getMsg() {
		return this.returnMsg;
	}

}
