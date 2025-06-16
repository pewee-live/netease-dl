package com.pewee.neteasemusic.config;

import static com.pewee.neteasemusic.models.common.RespEntity.applyRespCodeMsg;

import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.pewee.neteasemusic.enums.CommonRespInfo;
import com.pewee.neteasemusic.exceptions.ServiceException;
import com.pewee.neteasemusic.models.common.RespEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * 全局异常处理器
 * 
 * @author ruoyi
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // add other exception handler
    
    /**
     * 拦截非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public RespEntity<String> handleIllegalArgumentException(IllegalArgumentException e,
            HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.error("请求地址'{}',发生非法参数异常.", requestUri, e);
        RespEntity<String> entity = RespEntity.<String>applyRespCodeMsg(CommonRespInfo.NOT_LEGAL_PARAM);
        entity.setMsg(e.getMessage());
        return entity;
    }
    
    /**
     * 非法状态
     */
    @ExceptionHandler(IllegalStateException.class)
    public RespEntity<String> handleIllegalArgumentException(IllegalStateException e,
            HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.error("请求地址'{}',发生非法状态异常.", requestUri, e);
        RespEntity<String> entity = RespEntity.<String>applyRespCodeMsg(CommonRespInfo.SERVICE_EXECUTION_ERROR);
        entity.fillData(e.getMessage());
        return entity;
    }
    
    
    /**
     * 拦截参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RespEntity<String> handleMethodArgumentNotValidException(MethodArgumentNotValidException e,
        HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.error("请求地址'{}',发生参数校验异常.", requestUri, e);
        RespEntity<String> respEntity = RespEntity.<String>applyRespCodeMsg(CommonRespInfo.NOT_LEGAL_PARAM);
        return respEntity.setMsg(e.getBindingResult()
            .getFieldErrors().stream().map(FieldError::getDefaultMessage).collect(Collectors.joining("\n")));
    }
    
    /**
     * validated校验的参数异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public RespEntity<String> handleIllegalArgumentException(ConstraintViolationException e,HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.error("请求地址'{}',发生非法参数异常.", requestUri, e);
        RespEntity<String> entity = RespEntity.<String>applyRespCodeMsg(CommonRespInfo.NOT_LEGAL_PARAM);
        entity.fillData(e.getMessage());
        return entity;
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public RespEntity handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestUri, e);
        return applyRespCodeMsg(CommonRespInfo.SERVICE_EXECUTION_ERROR);
    }

    /**
     * 业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public RespEntity<String> handleServiceException(ServiceException e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.error("请求地址'{}',发生自定义服务异常.", requestUri, e);
        RespEntity<String> fillData = RespEntity.<String>applyRespCodeMsg(e);
       // fillData.setMsg(e.getMsg());
        return fillData;
    }

    
    /**
     * 参数缺失异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public RespEntity<String> handleMissingServletRequestParameterException(MissingServletRequestParameterException e,HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.error("请求地址'{}',发生参数缺失异常.", requestUri, e);
        RespEntity<String> entity = RespEntity.<String>applyRespCodeMsg(CommonRespInfo.NOT_LEGAL_PARAM);
        entity.fillData(e.getMessage());
        return entity;
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public RespEntity handleException(Exception e, HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestUri, e);
        return applyRespCodeMsg(CommonRespInfo.SYS_ERROR);
    }

}
