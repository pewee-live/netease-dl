package com.pewee.neteasemusic.exceptions;

import com.pewee.neteasemusic.models.common.IResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * {@link ServiceException} 工具类
 *
 */
@Slf4j
public class ServiceExceptionUtil {


    // ========== 和 ServiceException 的集成 ==========

    public static ServiceException exception(IResponse errorCode) {
        String messagePattern =errorCode.getMsg();
        return exception0(errorCode.getCode(), messagePattern);
    }

    public static ServiceException exception(IResponse errorCode, Object... params) {
    	 String messagePattern =errorCode.getMsg();
        return exception0(errorCode.getCode(), messagePattern, params);
    }


    public static ServiceException exception0(String code, String messagePattern, Object... params) {
        String message = doFormat(code, messagePattern, params);
        return new ServiceException(code, message);
    }

    // ========== 格式化方法 ==========

    /**
     * 将错误编号对应的消息使用 params 进行格式化。
     *
     * @param code           错误编号
     * @param messagePattern 消息模版
     * @param params         参数
     * @return 格式化后的提示
     */
    public static String doFormat(String code, String messagePattern, Object... params) {
        StringBuilder sbuf = new StringBuilder(messagePattern.length() + 50);
        int i = 0;
        int j;
        int l;
        for (l = 0; l < params.length; l++) {
            j = messagePattern.indexOf("{}", i);
            if (j == -1) {
                log.error("[doFormat][参数过多：错误码({})|错误内容({})|参数({})", code, messagePattern, params);
                if (i == 0) {
                    return messagePattern;
                } else {
                    sbuf.append(messagePattern.substring(i));
                    return sbuf.toString();
                }
            } else {
                sbuf.append(messagePattern, i, j);
                sbuf.append(params[l]);
                i = j + 2;
            }
        }
        if (messagePattern.indexOf("{}", i) != -1) {
            log.error("[doFormat][参数过少：错误码({})|错误内容({})|参数({})", code, messagePattern, params);
        }
        sbuf.append(messagePattern.substring(i));
        return sbuf.toString();
    }

}
