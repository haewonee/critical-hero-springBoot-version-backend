package com.devassistant.critical_hero_springboot_version.global.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.devassistant.critical_hero_springboot_version.domain.analysis.dto.res.AnalysisResDto;
import com.devassistant.critical_hero_springboot_version.domain.analysis.service.AnalysisQueryService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackEventHandler {

    private final App slackApp;
    private final AnalysisQueryService analysisQueryService;

    @Value("${slack.app-token}")
    private String appToken;

    private SocketModeApp socketModeApp;

    // 중복 이벤트 필터링용 (최근 200개 event_id 캐시)
    private final Set<String> processedEventIds = Collections.newSetFromMap(
            new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 200;
                }
            }
    );

    @PostConstruct
    public void start() throws Exception {
        // @Critical-hero 멘션 이벤트 처리
        slackApp.event(com.slack.api.model.event.AppMentionEvent.class, (payload, ctx) -> {
            String text = payload.getEvent().getText();

            // 중복 이벤트 무시 (Slack은 동일 이벤트에 같은 event_ts 부여)
            String eventId = payload.getEvent().getEventTs();
            if (eventId != null && !processedEventIds.add(eventId)) {
                log.info("중복 이벤트 무시: {}", eventId);
                return ctx.ack();
            }

            // 멘션 부분 제거 후 질문 추출 (<@BOTID> 제거)
            String question = text.replaceAll("<@[A-Z0-9]+>", "").trim();
            log.info("Slack 멘션 수신 - 질문: {}", question);

            // Slack 3초 타임아웃 안에 먼저 ack 응답
            var ackResponse = ctx.ack();

            if (question.isEmpty()) {
                ctx.say("질문을 입력해 주세요. 예: @Critical-hero 결제 모듈에서 500 에러가 발생했는데 왜 그럴까?");
                return ackResponse;
            }

            ctx.say("분석 중입니다... 잠시만 기다려 주세요.");

            try {
                AnalysisResDto.Result result = analysisQueryService.analyze(question);

                StringBuilder response = new StringBuilder();
                response.append("*질문:* ").append(question).append("\n\n");
                response.append("*관련 커밋:*\n");
                result.relatedCommits().forEach(commit ->
                        response.append("• `").append(commit.sha(), 0, 7).append("` ")
                                .append(commit.message()).append("\n")
                );
                response.append("\n*분석 결과:*\n").append(result.analysisResult());

                ctx.say(response.toString());
            } catch (Exception e) {
                log.error("분석 중 오류 발생", e);
                ctx.say("분석 중 오류가 발생했습니다: " + e.getMessage());
            }

            return ackResponse;
        });

        socketModeApp = new SocketModeApp(appToken, slackApp);
        socketModeApp.startAsync();
        log.info("Slack Socket Mode 앱 시작됨");
    }

    @PreDestroy
    public void stop() throws Exception {
        if (socketModeApp != null) {
            socketModeApp.stop();
        }
    }
}
