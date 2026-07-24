package com.devassistant.critical_hero_springboot_version.domain.webhook.controller;

import com.devassistant.critical_hero_springboot_version.domain.webhook.dto.GithubWebhookPayload;
import com.devassistant.critical_hero_springboot_version.domain.webhook.service.WebhookAnalysisService;
import com.devassistant.critical_hero_springboot_version.domain.webhook.service.WebhookNotificationService;
import com.devassistant.critical_hero_springboot_version.domain.webhook.service.WebhookProcessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookProcessService webhookProcessService;

    @PostMapping("/github")
    public ResponseEntity<Void> handlePush(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event,
            @RequestBody GithubWebhookPayload payload
    ) {
        if (!"push".equals(event)) {
            return ResponseEntity.ok().build();
        }

        log.info("GitHub push 이벤트 수신 - ref: {}, 커밋 수: {}",
                payload.ref, payload.commits == null ? 0 : payload.commits.size());

        // 비동기 처리 (GitHub은 10초 내 응답 요구)
        webhookProcessService.process(payload);

        return ResponseEntity.ok().build();
    }
}
