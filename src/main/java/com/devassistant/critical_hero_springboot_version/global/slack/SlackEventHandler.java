package com.devassistant.critical_hero_springboot_version.global.slack;

import com.slack.api.bolt.App;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
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
import java.util.List;
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
                ctx.say("오류가 발생했습니다: " + e.getMessage());
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

            new Thread(() -> {
                try {
                    // 파일 현재 내용 읽기
                    String currentContent = githubAgentTools.getFileContent(filePath);
                    if (currentContent.startsWith("파일을 찾을 수 없습니다")) {
                        ctx.respond("파일을 찾을 수 없습니다: " + filePath);
                        return;
                    }

                    // Agent로 수정 코드 생성
                    String agentQuestion = String.format(
                            "%s 파일의 보안 및 품질 문제를 수정한 코드를 작성해줘. PR은 직접 생성하지 말고 수정된 코드만 알려줘. 현재 코드:\n%s",
                            filePath, currentContent
                    );
                    String fixedCode = agentService.run(agentQuestion);

                    // PR 생성 후 URL 받기
                    String prUrl = githubAgentTools.createPullRequestAndGetUrl(
                            filePath,
                            fixedCode,
                            filePath + " 보안 및 품질 문제 수정",
                            "Critical Hero AI Agent가 감지한 보안 취약점 및 코드 품질 문제를 수정했습니다."
                    );

                    // PR 링크 버튼으로 응답
                    ctx.respond(r -> r
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
                    );

                } catch (Exception e) {
                    log.error("PR 생성 실패", e);
                    try {
                        ctx.respond("PR 생성 중 오류가 발생했습니다: " + e.getMessage());
                    } catch (Exception ignored) {}
                }
            }).start();

            return ctx.ack();
        });

        // PR 링크 버튼은 URL 클릭이라 별도 처리 불필요 (Slack이 자동으로 열어줌)
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
