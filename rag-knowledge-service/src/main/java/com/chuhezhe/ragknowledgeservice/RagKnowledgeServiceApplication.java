package com.chuhezhe.ragknowledgeservice;

import com.chuhezhe.ragcommonservice.config.FeignConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;

@Slf4j
@SpringBootApplication
@ComponentScan("com.chuhezhe")
@EnableDiscoveryClient
@MapperScan("com.chuhezhe.ragknowledgeservice.mapper")
@EnableFeignClients(basePackages = "com.chuhezhe.ragknowledgeservice.feign", defaultConfiguration = FeignConfiguration.class)
public class RagKnowledgeServiceApplication {

	public static void main(String[] args) {
		Environment env = SpringApplication.run(RagKnowledgeServiceApplication.class, args).getEnvironment();
		log.info("==========================================================================");
		log.info("{} 服务启动成功, 启动端口：{}, profiles:{}", env.getProperty("spring.application.name")
				, env.getProperty("server.port"), env.getProperty("spring.profiles.active"));
		log.info("==========================================================================");
	}

}
