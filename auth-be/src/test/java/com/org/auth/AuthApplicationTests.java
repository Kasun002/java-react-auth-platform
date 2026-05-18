package com.org.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class AuthApplicationTests {

	@MockitoBean RedisConnectionFactory redisConnectionFactory;
	@MockitoBean JavaMailSender         mailSender;

	@Test
	void contextLoads() {
	}

}
