package com.org.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
        "app.jwt.secret=test-only-jwt-secret-value-32chars-ok",
        "app.ad.jwks-uri=http://localhost/jwks",
        "app.ad.issuer=http://localhost/issuer",
        "app.ad.audience=test-audience"
})
class AuthApplicationTests {

	@MockitoBean RedisConnectionFactory redisConnectionFactory;
	@MockitoBean JavaMailSender         mailSender;

	@Test
	void contextLoads() {
	}

}
