package com.devassistant.critical_hero_springboot_version.global.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class GithubAgentTools {

    private final WebClient githubClient;
    private final String repo;

    public GithubAgentTools(
            @Value("${github.token}") String token,
            @Value("${github.repo}") String repo
    ) {
        this.repo = repo;
        this.githubClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
    }

    // 파일 전체 내용 조회
    public String getFileContent(String filePath) {
        try {
            String[] parts = repo.split("/");
            Map response = githubClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}", parts[0], parts[1], filePath)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String encoded = (String) response.get("content");
            String decoded = new String(Base64.getDecoder().decode(encoded.replaceAll("\\s", "")));
            log.info("파일 조회 성공: {}", filePath);
            return decoded;
        } catch (Exception e) {
            log.error("파일 조회 실패: {}", filePath, e);
            return "파일을 찾을 수 없습니다: " + filePath;
        }
    }

    // Agent용 - 문자열 반환 (Function Calling 결과)
    public String createPullRequest(String filePath, String newContent, String prTitle, String prBody) {
        try {
            String prUrl = createPr(filePath, newContent, prTitle, prBody);
            return "PR 생성 완료: " + prUrl;
        } catch (Exception e) {
            log.error("PR 생성 실패", e);
            return "PR 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    // 버튼 클릭용 - PR URL 직접 반환
    public String createPullRequestAndGetUrl(String filePath, String newContent, String prTitle, String prBody) {
        return createPr(filePath, newContent, prTitle, prBody);
    }

    private String createPr(String filePath, String newContent, String prTitle, String prBody) {
        String[] parts = repo.split("/");
        String owner = parts[0];
        String name = parts[1];
        String branchName = "fix/" + UUID.randomUUID().toString().substring(0, 8);

        // 1. main 브랜치 최신 SHA 조회
        Map refResponse = githubClient.get()
                .uri("/repos/{owner}/{repo}/git/ref/heads/main", owner, name)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        String mainSha = (String) ((Map) refResponse.get("object")).get("sha");

        // 2. 새 브랜치 생성
        githubClient.post()
                .uri("/repos/{owner}/{repo}/git/refs", owner, name)
                .bodyValue(Map.of("ref", "refs/heads/" + branchName, "sha", mainSha))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 3. 기존 파일 SHA 조회 (수정 시 필요)
        String fileSha = null;
        try {
            Map fileResponse = githubClient.get()
                    .uri("/repos/{owner}/{repo}/contents/{path}?ref=main", owner, name, filePath)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            fileSha = (String) fileResponse.get("sha");
        } catch (Exception ignored) {}

        // 4. 파일 수정 커밋 (커밋 메시지에 fix: prefix)
        String commitMessage = prTitle.startsWith("fix:") ? prTitle : "fix: " + prTitle;
        String encodedContent = Base64.getEncoder().encodeToString(newContent.getBytes());
        HashMap<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("message", commitMessage);
        updateRequest.put("content", encodedContent);
        updateRequest.put("branch", branchName);
        if (fileSha != null) updateRequest.put("sha", fileSha);

        githubClient.put()
                .uri("/repos/{owner}/{repo}/contents/{path}", owner, name, filePath)
                .bodyValue(updateRequest)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        // 5. PR 생성 (title에 fix: prefix, 설명 풍부하게)
        String finalTitle = prTitle.startsWith("fix:") ? prTitle : "fix: " + prTitle;
        String fullBody = String.format("""
                ## 수정 대상 파일
                `%s`

                %s

                ## 주의사항
                - 머지 전 반드시 코드 리뷰를 진행해 주세요.
                - 이 PR은 AI Agent가 자동 생성한 코드입니다. 실제 운영 환경 적용 전 반드시 사람이 직접 검토하고 테스트를 수행해 주세요.

                ---
                *이 PR은 Critical Hero AI Agent에 의해 자동 생성되었습니다.*
                """, filePath, prBody);

        Map prResponse = githubClient.post()
                .uri("/repos/{owner}/{repo}/pulls", owner, name)
                .bodyValue(Map.of(
                        "title", finalTitle,
                        "body", fullBody,
                        "head", branchName,
                        "base", "main"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        String prUrl = (String) prResponse.get("html_url");
        log.info("PR 생성 완료: {}", prUrl);
        return prUrl;
    }
}
