package am.ik.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;

@TestConfiguration(proxyBeanMethods = false)
public class TestImageProxyApplication {

	public static void main(String[] args) {
		SpringApplication.from(ImageProxyApplication::main).with(TestImageProxyApplication.class).run(args);
	}

}
