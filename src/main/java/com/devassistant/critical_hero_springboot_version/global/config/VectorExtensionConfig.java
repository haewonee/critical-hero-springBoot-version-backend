package com.devassistant.critical_hero_springboot_version.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class VectorExtensionConfig {

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ContextRefreshedEvent.class)
    public void enableVectorExtension() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        log.info("pgvector extension enabled");
    }
}
