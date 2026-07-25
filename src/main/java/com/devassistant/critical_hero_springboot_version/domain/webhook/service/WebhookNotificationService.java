package com.devassistant.critical_hero_springboot_version.domain.webhook.service;

import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

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

        String message = String.format("""
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

        try {
            slackApp.client().chatPostMessage(req -> req
                    .channel(alertChannel)
                    .text(message)
            );
            log.info("Slack 알림 전송 완료: {} - {}", sha.substring(0, 7), result.level());
        } catch (IOException | SlackApiException e) {
            log.error("Slack 알림 전송 실패", e);
        }
    }
}
