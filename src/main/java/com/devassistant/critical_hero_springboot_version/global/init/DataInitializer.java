package com.devassistant.critical_hero_springboot_version.global.init;

import com.devassistant.critical_hero_springboot_version.domain.embedding.service.EmbeddingCommandService;
import com.devassistant.critical_hero_springboot_version.domain.github.service.GithubCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 앱 시작 시 GitHub 커밋 수집 + 임베딩 생성 자동 실행
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final GithubCommandService githubCommandService;
    private final EmbeddingCommandService embeddingCommandService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            log.info("커밋 수집 시작...");
            githubCommandService.indexRepository();
            log.info("임베딩 생성 시작...");
            embeddingCommandService.embedAllCommits();
            log.info("초기 데이터 세팅 완료");
        } catch (Exception e) {
            log.error("초기 데이터 세팅 실패", e);
        }
    }
}
