package com.pewee.neteasemusic.exceptions;

/**
 * @Description 客户端加载失败
 * @Author Mr. GongRan
 * @Date 2022/7/21 18:30
 * @Version 1.0
 */
public class ComponentLoadException extends RuntimeException {

  private static final long serialVersionUID = -5729317076315201572L;

  public ComponentLoadException(String message) {
    super(message);
  }

  public ComponentLoadException(String message, Throwable e) {
    super(message, e);
  }

}

