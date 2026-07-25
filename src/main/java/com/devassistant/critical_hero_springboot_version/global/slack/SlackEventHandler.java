package com.devassistant.critical_hero_springboot_version.global.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.devassistant.critical_hero_springboot_version.global.agent.AgentService;
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
    private final AgentService agentService;

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
        slackApp.event(com.slack.api.model.event.AppMentionEvent.class, (payload, ctx) -> {
            String text = payload.getEvent().getText();

            // 중복 이벤트 무시
            String eventId = payload.getEvent().getEventTs();
            if (eventId != null && !processedEventIds.add(eventId)) {
                log.info("중복 이벤트 무시: {}", eventId);
                return ctx.ack();
            }

            // 멘션 제거 후 질문 추출
            String question = text.replaceAll("<@[A-Z0-9]+>", "").trim();
            log.info("Slack 멘션 수신 - 질문: {}", question);

            var ackResponse = ctx.ack();

            if (question.isEmpty()) {
                ctx.say("질문을 입력해 주세요.\n예: @Critical-hero 결제 모듈 버그 왜 났어?\n예: @Critical-hero payment.py null 체크 추가해줘");
                return ackResponse;
            }

            ctx.say("분석 중입니다... 잠시만 기다려 주세요. 🤖");

            try {
                String result = agentService.run(question);
                ctx.say(result);
            } catch (Exception e) {
                log.error("Agent 실행 중 오류 발생", e);
                ctx.say("오류가 발생했습니다: " + e.getMessage());
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
