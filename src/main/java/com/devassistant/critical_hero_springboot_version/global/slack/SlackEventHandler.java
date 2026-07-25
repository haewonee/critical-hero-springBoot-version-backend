package com.devassistant.critical_hero_springboot_version.global.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.devassistant.critical_hero_springboot_version.global.agent.AgentService;
import com.devassistant.critical_hero_springboot_version.global.agent.GithubAgentTools;
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
    private final GithubAgentTools githubAgentTools;

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

        // "🔧 PR 자동 생성" 버튼 클릭 처리
        slackApp.blockAction("create_pr", (req, ctx) -> {
            String value = req.getPayload().getActions().get(0).getValue();
            // value 형식: "sha|filePath"
            String[] parts = value.split("\\|", 2);
            String sha = parts[0];
            String filePath = parts.length > 1 ? parts[1] : "unknown";

            log.info("PR 생성 버튼 클릭 - sha: {}, 파일: {}", sha.substring(0, 7), filePath);

            // 즉시 ack 후 비동기로 PR 생성
            ctx.ack();

            new Thread(() -> {
                try {
                    // 파일 현재 내용 읽기
                    String currentContent = githubAgentTools.getFileContent(filePath);
                    if (currentContent.startsWith("파일을 찾을 수 없습니다")) {
                        ctx.respond("파일을 찾을 수 없어서 PR을 생성할 수 없습니다: " + filePath);
                        return;
                    }

                    // Agent로 코드 수정 + PR 생성 요청
                    String question = String.format(
                            "%s 파일의 보안/품질 문제를 수정하고 PR을 생성해줘. 현재 코드:\n%s",
                            filePath, currentContent
                    );
                    String result = agentService.run(question);

                    ctx.respond("✅ " + result);
                } catch (Exception e) {
                    log.error("PR 생성 실패", e);
                    try {
                        ctx.respond("PR 생성 중 오류가 발생했습니다: " + e.getMessage());
                    } catch (Exception ignored) {}
                }
            }).start();

            return ctx.ack();
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
