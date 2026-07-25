package com.devassistant.critical_hero_springboot_version.domain.webhook.service;

import com.devassistant.critical_hero_springboot_version.domain.webhook.dto.GithubWebhookPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WebhookProcessService {

    private final WebhookAnalysisService analysisService;
    private final WebhookNotificationService notificationService;
    private final WebClient githubClient;
    private final String repoFullName;

    public WebhookProcessService(
            WebhookAnalysisService analysisService,
            WebhookNotificationService notificationService,
            @Value("${github.token}") String githubToken,
            @Value("${github.repo}") String repoFullName
    ) {
        this.analysisService = analysisService;
        this.notificationService = notificationService;
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

        String repo = payload.repository != null ? payload.repository.fullName : repoFullName;

        for (GithubWebhookPayload.CommitPayload commit : payload.commits) {
            try {
                String diff = fetchDiff(repo, commit.id);
                String author = commit.author != null ? commit.author.name : "unknown";

                WebhookAnalysisService.AnalysisResult result =
                        analysisService.analyze(commit.message, diff);

                log.info("커밋 {} 분석 결과: {}", commit.id.substring(0, 7), result.level());

                // SAFE는 알림 없이 통과
                if (result.level() == WebhookAnalysisService.RiskLevel.SAFE) continue;

                notificationService.sendAlert(
                        commit.id, author, commit.message, commit.url, diff, result
                );
            } catch (Exception e) {
                log.error("커밋 {} 처리 실패", commit.id, e);
            }
        }
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
