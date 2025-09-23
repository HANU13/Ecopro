package com.litemax.ECoPro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.litemax.ECoPro.repository")
public class ECoProApplication {
    public static void main(String[] args) {
        SpringApplication.run(ECoProApplication.class, args);
    }
}