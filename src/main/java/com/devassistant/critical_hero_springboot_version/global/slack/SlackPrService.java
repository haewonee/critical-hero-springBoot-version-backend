package com.devassistant.critical_hero_springboot_version.global.slack;

import com.devassistant.critical_hero_springboot_version.global.agent.AgentService;
import com.devassistant.critical_hero_springboot_version.global.agent.GithubAgentTools;
import com.slack.api.bolt.response.Response;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackPrService {

    private final AgentService agentService;
    private final GithubAgentTools githubAgentTools;

    @Async
    public void createPrAsync(String sha, String filePath, Consumer<String> onError,
                              SlackResponder responder) {
        try {
            String currentContent = githubAgentTools.getFileContent(filePath);
            if (currentContent.startsWith("파일을 찾을 수 없습니다")) {
                onError.accept("파일을 찾을 수 없습니다: " + filePath);
                return;
            }

            String agentQuestion = String.format("""
                    아래 파일의 보안 및 품질 문제를 분석하고 수정해줘.
                    반드시 아래 형식으로만 답해줘. 다른 말은 하지 마:

                    ISSUES:
                    (이 파일에서 발견한 문제점을 구체적으로 항목별 작성)
                    FIXED_DESCRIPTION:
                    (무엇을 왜 어떻게 수정했는지 구체적으로 항목별 작성)
                    FIXED_CODE:
                    (수정된 파일의 전체 코드만. 설명, 마크다운, 코드블록 없이 코드만)

                    파일명: %s
                    현재 코드:
                    %s
                    """, filePath, currentContent);

            String agentResponse = agentService.run(agentQuestion);

            String foundIssues = extractSection(agentResponse, "ISSUES:", "FIXED_DESCRIPTION:");
            String fixDescription = extractSection(agentResponse, "FIXED_DESCRIPTION:", "FIXED_CODE:");
            String fixedCode = extractSection(agentResponse, "FIXED_CODE:", null);
            String prBody = foundIssues + "\n\n" + fixDescription;

            if (fixedCode.isBlank()) {
                onError.accept("코드 수정에 실패했습니다. 다시 시도해 주세요.");
                return;
            }

            String prUrl = githubAgentTools.createPullRequestAndGetUrl(
                    filePath,
                    fixedCode,
                    filePath + " 보안 및 품질 문제 수정",
                    prBody
            );

            responder.respond(prUrl);

        } catch (Exception e) {
            log.error("PR 생성 실패 - sha: {}, 파일: {}", sha, filePath, e);
            onError.accept("PR 생성 중 오류가 발생했습니다.");
        }
    }

    private String extractSection(String text, String startMarker, String endMarker) {
        int start = text.indexOf(startMarker);
        if (start == -1) return text;
        start += startMarker.length();

        if (endMarker != null) {
            int end = text.indexOf(endMarker, start);
            return end == -1 ? text.substring(start).trim() : text.substring(start, end).trim();
        }
        return text.substring(start).trim();
    }

    @FunctionalInterface
    public interface SlackResponder {
        void respond(String prUrl) throws Exception;
    }
}
