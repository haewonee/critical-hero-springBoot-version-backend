package com.devassistant.critical_hero_springboot_version.domain.github.service;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.github.entity.GithubRepo;
import com.devassistant.critical_hero_springboot_version.domain.github.repository.GithubRepoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GithubCommandService {

    private final GithubRepoRepository githubRepoRepository;
    private final CommitRepository commitRepository;
    private final WebClient webClient;
    private final String defaultRepo;

    public GithubCommandService(
            GithubRepoRepository githubRepoRepository,
            CommitRepository commitRepository,
            @Value("${github.token}") String githubToken,
            @Value("${github.repo}") String defaultRepo
    ) {
        this.githubRepoRepository = githubRepoRepository;
        this.commitRepository = commitRepository;
        this.defaultRepo = defaultRepo;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    @Transactional
    public GithubRepo indexRepository() {
        String[] parts = defaultRepo.split("/");
        String owner = parts[0];
        String name = parts[1];

        GithubRepo repo = githubRepoRepository.findByOwnerAndName(owner, name)
                .orElseGet(() -> githubRepoRepository.save(
                        GithubRepo.builder()
                                .owner(owner)
                                .name(name)
                                .build()
                ));

        // 커밋 목록 조회 (최근 20개)
        List<Map> commits = webClient.get()
                .uri("/repos/{owner}/{repo}/commits?per_page=20", owner, name)
                .retrieve()
                .bodyToFlux(Map.class)
                .collectList()
                .block();

        for (Map commitSummary : commits) {
            String sha = (String) commitSummary.get("sha");

            if (commitRepository.existsBySha(sha)) {
                log.info("커밋 이미 존재, 스킵: {}", sha);
                continue;
            }

            // 커밋 상세 조회 (diff 포함)
            Map commitDetail = webClient.get()
                    .uri("/repos/{owner}/{repo}/commits/{sha}", owner, name, sha)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            Map commitInfo = (Map) commitDetail.get("commit");
            Map authorInfo = (Map) commitInfo.get("author");

            String message = (String) commitInfo.get("message");
            String author = (String) authorInfo.get("name");
            LocalDateTime committedAt = OffsetDateTime.parse((String) authorInfo.get("date")).toLocalDateTime();
            String diff = fetchDiff(commitDetail);

            commitRepository.save(
                    Commit.builder()
                            .repo(repo)
                            .sha(sha)
                            .author(author)
                            .message(message)
                            .diff(diff)
                            .committedAt(committedAt)
                            .build()
            );
            log.info("커밋 저장: {}", sha);
        }

        return repo;
    }

    // 커밋 상세 응답의 files 배열에서 patch(diff)를 추출
    private String fetchDiff(Map commitDetail) {
        List<Map> files = (List<Map>) commitDetail.get("files");
        if (files == null) return "";

        StringBuilder sb = new StringBuilder();
        for (Map file : files) {
            sb.append("파일: ").append(file.get("filename")).append("\n");
            String patch = (String) file.get("patch");
            if (patch != null) {
                sb.append(patch).append("\n");
            }
        }
        return sb.toString();
    }
}
