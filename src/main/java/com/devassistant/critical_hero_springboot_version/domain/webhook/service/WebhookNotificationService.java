package com.devassistant.critical_hero_springboot_version.domain.webhook.service;

import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.element.ButtonElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookNotificationService {

    private final App slackApp;

    @Value("${slack.alert-channel:general}")
    private String alertChannel;

    public void sendAlert(String sha, String author, String commitMessage,
                          String commitUrl, String diff, WebhookAnalysisService.AnalysisResult result) {
        String emoji = switch (result.level()) {
            case CRITICAL -> "🔴";
            case WARNING -> "🟡";
            case SAFE -> "🟢";
        };

        String levelText = switch (result.level()) {
            case CRITICAL -> "CRITICAL - 즉시 확인 필요";
            case WARNING -> "WARNING - 코드 리뷰 필요";
            case SAFE -> "SAFE";
        };

        // diff에서 앞 300자만 잘라서 문제 코드 스니펫으로 표시
        String codeSnippet = "";
        if (diff != null && !diff.isBlank()) {
            String trimmed = diff.length() > 300 ? diff.substring(0, 300) + "\n..." : diff;
            codeSnippet = "\n*문제 코드:*\n```" + trimmed + "```";
        }

        String messageText = String.format("""
                %s *[%s]*
                *커밋:* <%s|%s>
                *작성자:* %s
                *메시지:* %s
                *분석:* %s%s
                """,
                emoji, levelText,
                commitUrl, sha.substring(0, 7),
                author,
                commitMessage,
                result.reason(),
                codeSnippet
        );

        // Block Kit - 메시지 + PR 생성 버튼
        // value에 sha와 파일 경로 정보를 넣어 버튼 클릭 시 사용
        String buttonValue = sha + "|" + extractFilePath(diff);

        try {
            slackApp.client().chatPostMessage(req -> req
                    .channel(alertChannel)
                    .text(messageText)
                    .blocks(List.of(
                            SectionBlock.builder()
                                    .text(MarkdownTextObject.builder().text(messageText).build())
                                    .build(),
                            ActionsBlock.builder()
                                    .elements(List.of(
                                            ButtonElement.builder()
                                                    .text(com.slack.api.model.block.composition.PlainTextObject.builder()
                                                            .text("🔧 PR 자동 생성").build())
                                                    .actionId("create_pr")
                                                    .value(buttonValue)
                                                    .style("danger")
                                                    .build()
                                    ))
                                    .build()
                    ))
            );
            String shortSha = sha != null && sha.length() >= 7 ? sha.substring(0, 7) : sha;
            log.info("Slack 알림 전송 완료: {} - {}", shortSha, result.level());
        } catch (IOException | SlackApiException e) {
            log.error("Slack 알림 전송 실패", e);
        }
    }

    // diff에서 첫 번째 파일 경로 추출
    private String extractFilePath(String diff) {
        if (diff == null || diff.isBlank()) return "unknown";
        for (String line : diff.split("\n")) {
            if (line.startsWith("파일: ")) {
                return line.replace("파일: ", "").trim();
            }
        }
        return "unknown";
    }
}
