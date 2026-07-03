package io.shinhanlife;

import io.shinhanlife.glow.GlowMybatisMapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "io.shinhanlife", annotationClass = GlowMybatisMapper.class)  // @GlowMybatisMapper 어노테이션이 있는 인터페이스만 스캔
public class AxHubAdminApplication {
	public static void main(String[] args) {
		SpringApplication.run(AxHubAdminApplication.class, args);
	}
}