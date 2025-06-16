package com.pewee.neteasemusic;

import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NeteasemusicApplicationTests {

	@Test
	void contextLoads() {
	}
	
	public static void main(String[] args) {
		Long mills = 1749657600000L;
		Date date = new Date(mills);
		
		System.out.println(date);
		
	}

}
