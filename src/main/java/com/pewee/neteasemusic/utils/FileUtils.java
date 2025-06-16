package com.pewee.neteasemusic.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import com.pewee.neteasemusic.enums.CommonRespInfo;
import com.pewee.neteasemusic.exceptions.ServiceException;


public class FileUtils {
	
	public static Path writeToFile(Path path, byte[] bytes) {
		try {
	        if (!Files.exists(path)) {
	        	// 创建,追加到文件
	            Files.createDirectories(path.getParent());
	            Files.createFile(path);
		        Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	        } 
	        return path;
	    } catch (Exception e) {
	        throw new RuntimeException("文件写入错误", e);
	    } finally {
	    	bytes = null;
	    }
    }
	
	public static Path writeToFile(Path path, InputStream is) {
		try {
	        if (!Files.exists(path)) {
	        	// 创建,追加到文件
	            Files.createDirectories(path.getParent());
	            //Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
	            try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
	                byte[] buffer = new byte[8192];
	                int len;
	                while ((len = is.read(buffer)) != -1) {
	                    os.write(buffer, 0, len);
	                }
	            }
	        } 
	        return path;
	    } catch (Exception e) {
	    	try {
				Files.deleteIfExists(path);
			} catch (IOException e1) {
				throw new ServiceException(CommonRespInfo.SYS_ERROR,e1);
			}
	        throw new RuntimeException("文件写入错误", e);
	    } finally {
	    	try {
				if (null != is) {
					is.close();
				}
			} catch (IOException e) {
				throw new ServiceException(CommonRespInfo.SYS_ERROR,e);
			}
	    }
    }
}
