package com.devassistant.critical_hero_springboot_version.global.init;

import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.embedding.service.EmbeddingCommandService;
import com.devassistant.critical_hero_springboot_version.domain.github.service.GithubCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// 앱 시작 시 커밋/임베딩 데이터가 없을 때만 GitHub에서 수집
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final GithubCommandService githubCommandService;
    private final EmbeddingCommandService embeddingCommandService;
    private final CommitRepository commitRepository;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (commitRepository.count() > 0) {
                log.info("커밋 데이터 이미 존재 ({} 건), 초기화 스킵", commitRepository.count());
                return;
            }
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
