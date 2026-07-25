package com.devassistant.critical_hero_springboot_version.global.agent;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.embedding.service.EmbeddingQueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final EmbeddingQueryService embeddingQueryService;
    private final GithubAgentTools githubAgentTools;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.chat-url}")
    private String chatUrl;

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.chat-model}")
    private String chatModel;

    // GPT에게 알려줄 도구 목록 (Function Calling 스펙)
    private static final List<Map<String, Object>> TOOLS = List.of(
            Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", "search_commits",
                            "description", "질문과 관련된 GitHub 커밋 히스토리를 검색합니다. 버그 원인 파악, 코드 변경 이력 조회에 사용하세요.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "query", Map.of("type", "string", "description", "검색할 내용 (한국어 가능)")
                                    ),
                                    "required", List.of("query")
                            )
                    )
            ),
            Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", "get_file_content",
                            "description", "GitHub 레포지토리에서 특정 파일의 현재 전체 내용을 가져옵니다. 코드 수정이 필요할 때 현재 파일 내용을 먼저 확인하세요.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "file_path", Map.of("type", "string", "description", "파일 경로 (예: src/main/java/Payment.java)")
                                    ),
                                    "required", List.of("file_path")
                            )
                    )
            ),
            Map.of(
                    "type", "function",
                    "function", Map.of(
                            "name", "create_pull_request",
                            "description", "코드를 수정하고 Pull Request를 생성합니다. 반드시 get_file_content로 현재 코드를 확인한 후 사용하세요.",
                            "parameters", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "file_path", Map.of("type", "string", "description", "수정할 파일 경로"),
                                            "new_content", Map.of("type", "string", "description", "수정된 파일의 전체 내용"),
                                            "pr_title", Map.of("type", "string", "description", "PR 제목"),
                                            "pr_body", Map.of("type", "string", "description", "PR 설명 (어떤 문제를 왜 어떻게 수정했는지)")
                                    ),
                                    "required", List.of("file_path", "new_content", "pr_title", "pr_body")
                            )
                    )
            )
    );

    // Agent 루프: GPT가 도구 호출이 없을 때까지 반복
    public String run(String userQuestion) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", """
                당신은 GitHub 커밋 히스토리를 분석하고 코드 품질 문제를 해결하는 AI 어시스턴트입니다.

                - 버그 원인이나 코드 이력을 물어보면 search_commits로 관련 커밋을 찾아 답변하세요.
                - 코드 수정을 요청받으면 반드시 get_file_content로 현재 파일을 확인한 후 create_pull_request로 PR을 생성하세요.
                - 답변은 항상 한국어로 작성하세요.
                """));
        messages.add(Map.of("role", "user", "content", userQuestion));

        // 최대 5번 루프 (무한루프 방지)
        for (int i = 0; i < 5; i++) {
            Map<String, Object> response = callGpt(messages);
            Map<String, Object> responseMessage = (Map<String, Object>) response.get("message");
            String finishReason = (String) response.get("finish_reason");

            messages.add(responseMessage);

            // 도구 호출 없으면 최종 답변 반환
            if (!"tool_calls".equals(finishReason)) {
                return (String) responseMessage.get("content");
            }

            // 도구 호출 처리
            List<Map<String, Object>> toolCalls = (List<Map<String, Object>>) responseMessage.get("tool_calls");
            for (Map<String, Object> toolCall : toolCalls) {
                String toolCallId = (String) toolCall.get("id");
                Map<String, Object> function = (Map<String, Object>) toolCall.get("function");
                String toolName = (String) function.get("name");
                String argsJson = (String) function.get("arguments");

                log.info("Agent 도구 호출: {} / 인자: {}", toolName, argsJson);

                String toolResult = executeTool(toolName, argsJson);

                // 도구 실행 결과를 메시지에 추가
                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", toolCallId,
                        "content", toolResult
                ));
            }
        }

        return "분석을 완료하지 못했습니다. 다시 시도해 주세요.";
    }

    // 도구 이름에 따라 실제 실행
    private String executeTool(String toolName, String argsJson) {
        try {
            Map<String, String> args = objectMapper.readValue(argsJson, Map.class);
            return switch (toolName) {
                case "search_commits" -> {
                    String query = args.get("query");
                    List<Commit> commits = embeddingQueryService.findSimilarCommits(query);
                    StringBuilder sb = new StringBuilder("관련 커밋 검색 결과:\n");
                    commits.forEach(c -> sb
                            .append("- ").append(c.getSha(), 0, 7)
                            .append(" | ").append(c.getMessage()).append("\n")
                            .append("  변경내용: ").append(c.getDiff() != null ? c.getDiff().substring(0, Math.min(300, c.getDiff().length())) : "(없음)").append("\n")
                    );
                    yield sb.toString();
                }
                case "get_file_content" -> githubAgentTools.getFileContent(args.get("file_path"));
                case "create_pull_request" -> githubAgentTools.createPullRequest(
                        args.get("file_path"),
                        args.get("new_content"),
                        args.get("pr_title"),
                        args.get("pr_body")
                );
                default -> "알 수 없는 도구: " + toolName;
            };
        } catch (Exception e) {
            log.error("도구 실행 실패: {}", toolName, e);
            return "도구 실행 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    // GPT API 호출 (Function Calling 포함)
    private Map<String, Object> callGpt(List<Map<String, Object>> messages) {
        WebClient client = WebClient.builder()
                .baseUrl(chatUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", chatModel);
        requestBody.put("messages", messages);
        requestBody.put("tools", TOOLS);

        Map response = client.post()
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        Map<String, Object> choice = choices.get(0);

        return Map.of(
                "message", choice.get("message"),
                "finish_reason", choice.get("finish_reason")
        );
    }
}
