package com.devassistant.critical_hero_springboot_version.domain.webhook.controller;

import com.devassistant.critical_hero_springboot_version.domain.webhook.dto.GithubWebhookPayload;
import com.devassistant.critical_hero_springboot_version.domain.webhook.service.WebhookProcessCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookProcessCommandService webhookProcessCommandService;

    @PostMapping("/github")
    public ResponseEntity<Void> handlePush(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event,
            @RequestBody GithubWebhookPayload payload
    ) {
        if (!"push".equals(event)) {
            return ResponseEntity.ok().build();
        }

        if (payload.ref == null ||
                (!payload.ref.equals("refs/heads/main") && !payload.ref.equals("refs/heads/master"))) {
            log.info("main 브랜치 아닌 push 무시: {}", payload.ref);
            return ResponseEntity.ok().build();
        }


        webhookProcessCommandService.process(payload);

        return ResponseEntity.ok().build();
    }
}
