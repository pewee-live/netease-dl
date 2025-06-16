package com.pewee.neteasemusic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "analysis")
@Component
@Data
public class AnalysisConfig {
	
	private String ip;
	
	private Integer port;

}
