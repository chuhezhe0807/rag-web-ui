package com.chuhezhe.raggateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.env.Environment;


@Slf4j
@SpringBootApplication
@ComponentScan("com.chuhezhe")
@EnableDiscoveryClient
public class RagGatewayApplication {

    public static void main(String[] args) {
        Environment env = SpringApplication.run(RagGatewayApplication.class, args).getEnvironment();
        log.info("==========================================================================");
        log.info("{} 服务启动成功, 启动端口：{}, profiles:{}", env.getProperty("spring.application.name")
                , env.getProperty("server.port"), env.getProperty("spring.profiles.active"));
        log.info("==========================================================================");
    }

}
