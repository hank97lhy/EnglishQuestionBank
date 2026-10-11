package com.kate.bank.question.fce;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class FceQuestionBankApplicationTests {

	@Test
	// 测试目的：在内存数据库中验证所有 model、repo、service 和 API 能启动，禁止连接业务库。
	void contextLoads() {
	}

}
