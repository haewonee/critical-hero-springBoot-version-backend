package com.devassistant.critical_hero_springboot_version;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class CriticalHeroSpringbootVersionApplication {

	public static void main(String[] args) {
		SpringApplication.run(CriticalHeroSpringbootVersionApplication.class, args);
	}

}
