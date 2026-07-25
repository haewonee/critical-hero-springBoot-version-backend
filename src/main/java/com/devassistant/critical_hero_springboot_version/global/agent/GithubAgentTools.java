package com.devassistant.critical_hero_springboot_version.global.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Base64;
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
            // GitHub API는 Base64로 인코딩해서 줌 (줄바꿈 제거 필요)
            String decoded = new String(Base64.getDecoder().decode(encoded.replaceAll("\\s", "")));
            log.info("파일 조회 성공: {}", filePath);
            return decoded;
        } catch (Exception e) {
            log.error("파일 조회 실패: {}", filePath, e);
            return "파일을 찾을 수 없습니다: " + filePath;
        }
    }

    // 새 브랜치 생성 + 파일 수정 + PR 생성
    public String createPullRequest(String filePath, String newContent, String prTitle, String prBody) {
        try {
            String[] parts = repo.split("/");
            String owner = parts[0];
            String name = parts[1];
            String branchName = "fix/" + UUID.randomUUID().toString().substring(0, 8);

            // 1. main 브랜치의 최신 SHA 조회
            Map refResponse = githubClient.get()
                    .uri("/repos/{owner}/{repo}/git/ref/heads/main", owner, name)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            Map refObject = (Map) refResponse.get("object");
            String mainSha = (String) refObject.get("sha");

            // 2. 새 브랜치 생성
            githubClient.post()
                    .uri("/repos/{owner}/{repo}/git/refs", owner, name)
                    .bodyValue(Map.of(
                            "ref", "refs/heads/" + branchName,
                            "sha", mainSha
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // 3. 기존 파일 SHA 조회 (파일 수정 시 필요)
            String fileSha = null;
            try {
                Map fileResponse = githubClient.get()
                        .uri("/repos/{owner}/{repo}/contents/{path}?ref=main", owner, name, filePath)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                fileSha = (String) fileResponse.get("sha");
            } catch (Exception ignored) {
                // 파일이 없으면 새로 생성
            }

            // 4. 파일 수정 (Base64 인코딩)
            String encodedContent = Base64.getEncoder().encodeToString(newContent.getBytes());
            var updateRequest = new java.util.HashMap<String, Object>();
            updateRequest.put("message", prTitle);
            updateRequest.put("content", encodedContent);
            updateRequest.put("branch", branchName);
            if (fileSha != null) updateRequest.put("sha", fileSha);

            githubClient.put()
                    .uri("/repos/{owner}/{repo}/contents/{path}", owner, name, filePath)
                    .bodyValue(updateRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // 5. PR 생성
            Map prResponse = githubClient.post()
                    .uri("/repos/{owner}/{repo}/pulls", owner, name)
                    .bodyValue(Map.of(
                            "title", prTitle,
                            "body", prBody,
                            "head", branchName,
                            "base", "main"
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String prUrl = (String) prResponse.get("html_url");
            log.info("PR 생성 완료: {}", prUrl);
            return "PR 생성 완료: " + prUrl;

        } catch (Exception e) {
            log.error("PR 생성 실패", e);
            return "PR 생성 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
