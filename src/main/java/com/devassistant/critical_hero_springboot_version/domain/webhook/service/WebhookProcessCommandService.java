package com.devassistant.critical_hero_springboot_version.domain.webhook.service;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.embedding.service.EmbeddingCommandService;
import com.devassistant.critical_hero_springboot_version.domain.github.entity.GithubRepo;
import com.devassistant.critical_hero_springboot_version.domain.github.repository.GithubRepoRepository;
import com.devassistant.critical_hero_springboot_version.domain.webhook.dto.GithubWebhookPayload;
import com.devassistant.critical_hero_springboot_version.domain.webhook.exception.WebhookException;
import com.devassistant.critical_hero_springboot_version.domain.webhook.exception.code.WebhookErrorCode;
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
public class WebhookProcessCommandService {

    private final WebhookAnalysisCommandService analysisService;
    private final WebhookNotificationService notificationService;
    private final CommitRepository commitRepository;
    private final GithubRepoRepository githubRepoRepository;
    private final EmbeddingCommandService embeddingCommandService;
    private final WebClient githubClient;

    public WebhookProcessCommandService(
            WebhookAnalysisCommandService analysisService,
            WebhookNotificationService notificationService,
            CommitRepository commitRepository,
            GithubRepoRepository githubRepoRepository,
            EmbeddingCommandService embeddingCommandService,
            @Value("${github.token}") String githubToken
    ) {
        this.analysisService = analysisService;
        this.notificationService = notificationService;
        this.commitRepository = commitRepository;
        this.githubRepoRepository = githubRepoRepository;
        this.embeddingCommandService = embeddingCommandService;
        this.githubClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    @Async
    public void process(GithubWebhookPayload payload) {

        if (payload.commits == null || payload.commits.isEmpty()) return;

        if (payload.repository == null || payload.repository.fullName == null) {
            log.error("payload에 레포지토리 정보 없음. 처리 중단.");
            throw new WebhookException(WebhookErrorCode.INVALID_PAYLOAD);
        }

        String repo = payload.repository.fullName;

        for (GithubWebhookPayload.CommitPayload commit : payload.commits) {
            try {
                if (commitRepository.existsBySha(commit.id)) {
                    log.info("이미 처리된 커밋 스킵: {}", commit.id);
                    continue;
                }

                // GitHub API로 커밋 상세 조회
                Map commitDetail = fetchCommitDetail(repo, commit.id);

                List<Map> parents = (List<Map>) commitDetail.get("parents");
                if (parents != null && parents.size() >= 2) {
                    log.info("Merge 커밋 스킵 (parents={}): {}", parents.size(), commit.id);
                    continue;
                }

                if (commit.message != null && commit.message.contains("[auto]")) {
                    log.info("Agent 자동 생성 커밋 스킵: {}", commit.id);
                    continue;
                }

                String diff = extractDiff(commitDetail);
                String author = commit.author != null ? commit.author.name : "unknown";

                //위험도 분석
                WebhookAnalysisCommandService.AnalysisResult result =
                        analysisService.analyze(commit.message, diff);

                log.info("커밋 {} 분석 결과: {}", commit.id.substring(0, 7), result.level());

                //DB에 커밋 저장
                saveCommitIfAbsent(repo, commit.id, author, commit.message, diff, result.level().name());

                //SAFE는 알림 없이 통과
                if (result.level() == WebhookAnalysisCommandService.RiskLevel.SAFE) continue;

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

    // GitHub API로 커밋 상세 조회 (parents 포함)
    private Map fetchCommitDetail(String repo, String sha) {
        String[] parts = repo.split("/");
        return githubClient.get()
                .uri("/repos/{owner}/{repo}/commits/{sha}", parts[0], parts[1], sha)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
    }

    // 커밋 상세에서 diff 문자열 추출
    private String extractDiff(Map commitDetail) {
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
