package com.devassistant.critical_hero_springboot_version.global.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
import com.devassistant.critical_hero_springboot_version.global.agent.AgentService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackEventHandler {

    private final App slackApp;
    private final AgentService agentService;
    private final SlackPrService slackPrService;

    @Value("${slack.app-token}")
    private String appToken;

    private SocketModeApp socketModeApp;

    private final Set<String> processedEventIds = Collections.synchronizedSet(
            Collections.newSetFromMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 200;
                }
            })
    );

    @PostConstruct
    public void start() throws Exception {
        // @Critical-hero 멘션 처리
        slackApp.event(com.slack.api.model.event.AppMentionEvent.class, (payload, ctx) -> {
            String text = payload.getEvent().getText();

            String eventId = payload.getEvent().getEventTs();
            if (eventId != null && !processedEventIds.add(eventId)) {
                log.info("중복 이벤트 무시: {}", eventId);
                return ctx.ack();
            }

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
                ctx.say("분석 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
            }

            return ackResponse;
        });

        // "🔧 PR 자동 생성" 버튼 클릭 처리
        slackApp.blockAction("create_pr", (req, ctx) -> {
            ctx.ack();

            String value = req.getPayload().getActions().get(0).getValue();
            String[] parts = value.split("\\|", 2);
            String sha = parts[0];
            String filePath = parts.length > 1 ? parts[1] : "unknown";

            log.info("PR 생성 버튼 클릭 - sha: {}, 파일: {}", sha.substring(0, 7), filePath);

            slackPrService.createPrAsync(
                    sha,
                    filePath,
                    errorMsg -> {
                        try { ctx.respond(errorMsg); } catch (Exception ignored) {}
                    },
                    prUrl -> ctx.respond(r -> r
                            .blocks(List.of(
                                    SectionBlock.builder()
                                            .text(MarkdownTextObject.builder()
                                                    .text("PR이 생성되었습니다. 아래 버튼을 눌러 확인하세요.")
                                                    .build())
                                            .build(),
                                    ActionsBlock.builder()
                                            .elements(List.of(
                                                    ButtonElement.builder()
                                                            .text(PlainTextObject.builder()
                                                                    .text("PR 바로 열기").build())
                                                            .url(prUrl)
                                                            .actionId("open_pr_link")
                                                            .style("primary")
                                                            .build()
                                            ))
                                            .build()
                            ))
                    )
            );

            return ctx.ack();
        });

        slackApp.blockAction("open_pr_link", (req, ctx) -> ctx.ack());

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
