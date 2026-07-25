package com.devassistant.critical_hero_springboot_version.domain.webhook.service;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.embedding.service.EmbeddingCommandService;
import com.devassistant.critical_hero_springboot_version.domain.github.entity.GithubRepo;
import com.devassistant.critical_hero_springboot_version.domain.github.repository.GithubRepoRepository;
import com.devassistant.critical_hero_springboot_version.domain.webhook.dto.GithubWebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WebhookProcessService {

    private final WebhookAnalysisService analysisService;
    private final WebhookNotificationService notificationService;
    private final CommitRepository commitRepository;
    private final GithubRepoRepository githubRepoRepository;
    private final EmbeddingCommandService embeddingCommandService;
    private final WebClient githubClient;
    private final String repoFullName;

    public WebhookProcessService(
            WebhookAnalysisService analysisService,
            WebhookNotificationService notificationService,
            CommitRepository commitRepository,
            GithubRepoRepository githubRepoRepository,
            EmbeddingCommandService embeddingCommandService,
            @Value("${github.token}") String githubToken,
            @Value("${github.repo}") String repoFullName
    ) {
        this.analysisService = analysisService;
        this.notificationService = notificationService;
        this.commitRepository = commitRepository;
        this.githubRepoRepository = githubRepoRepository;
        this.embeddingCommandService = embeddingCommandService;
        this.repoFullName = repoFullName;
        this.githubClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    @Async
    public void process(GithubWebhookPayload payload) {
        if (payload.commits == null || payload.commits.isEmpty()) return;

        String repo = (payload.repository != null && payload.repository.fullName != null)
                ? payload.repository.fullName : repoFullName;

        for (GithubWebhookPayload.CommitPayload commit : payload.commits) {
            try {
                // Agent가 생성한 커밋은 스킵 (무한루프 방지)
                if (commit.message != null && commit.message.contains("[auto]")) {
                    log.info("Agent 자동 생성 커밋 스킵: {}", commit.id);
                    continue;
                }

                // 이미 처리된 커밋이면 스킵 (중복 알림 방지)
                if (commitRepository.existsBySha(commit.id)) {
                    log.info("이미 처리된 커밋 스킵: {}", commit.id);
                    continue;
                }

                String diff = fetchDiff(repo, commit.id);
                String author = commit.author != null ? commit.author.name : "unknown";

                // 1. 위험도 분석
                WebhookAnalysisService.AnalysisResult result =
                        analysisService.analyze(commit.message, diff);

                log.info("커밋 {} 분석 결과: {}", commit.id.substring(0, 7), result.level());

                // 2. DB에 커밋 저장 (분석 결과 포함)
                saveCommitIfAbsent(repo, commit.id, author, commit.message, diff, result.level().name());

                // 3. SAFE는 알림 없이 통과
                if (result.level() == WebhookAnalysisService.RiskLevel.SAFE) continue;

                notificationService.sendAlert(
                        commit.id, author, commit.message, commit.url, diff, result
                );
            } catch (Exception e) {
                log.error("커밋 {} 처리 실패", commit.id, e);
            }
        }
    }

    // 새 커밋을 DB에 저장하고 임베딩 생성
    private void saveCommitIfAbsent(String repoFullName, String sha, String author,
                                     String message, String diff, String riskLevel) {
        if (commitRepository.existsBySha(sha)) {
            // 이미 있으면 riskLevel만 업데이트
            commitRepository.findBySha(sha).ifPresent(c -> {
                c.updateRiskLevel(riskLevel);
                commitRepository.save(c);
            });
            return;
        }

        String[] parts = repoFullName.split("/");
        GithubRepo repo = githubRepoRepository.findByOwnerAndName(parts[0], parts[1])
                .orElseGet(() -> githubRepoRepository.save(
                        GithubRepo.builder().owner(parts[0]).name(parts[1]).build()
                ));

        Commit commit = commitRepository.save(Commit.builder()
                .repo(repo)
                .sha(sha)
                .author(author)
                .message(message)
                .diff(diff)
                .committedAt(LocalDateTime.now())
                .riskLevel(riskLevel)
                .build());

        log.info("새 커밋 저장: {} ({})", sha, riskLevel);

        // 임베딩 생성 (RAG 검색용)
        embeddingCommandService.embedSingleCommit(commit);
    }

    // GitHub API로 특정 커밋의 diff 조회
    private String fetchDiff(String repo, String sha) {
        String[] parts = repo.split("/");
        String owner = parts[0];
        String name = parts[1];

        Map commitDetail = githubClient.get()
                .uri("/repos/{owner}/{repo}/commits/{sha}", owner, name, sha)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map> files = (List<Map>) commitDetail.get("files");
        if (files == null) return "";

        StringBuilder sb = new StringBuilder();
        for (Map file : files) {
            sb.append("파일: ").append(file.get("filename")).append("\n");
            String patch = (String) file.get("patch");
            if (patch != null) sb.append(patch).append("\n");
        }
        return sb.toString();
    }
}
