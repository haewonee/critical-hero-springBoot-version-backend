package com.devassistant.critical_hero_springboot_version.domain.webhook.exception.code;

import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum WebhookErrorCode implements BaseErrorCode {

    INVALID_PAYLOAD(HttpStatus.BAD_REQUEST, "WEBHOOK400_1", "Webhook payload에 레포지토리 정보가 없습니다."),
    COMMIT_PROCESSING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WEBHOOK500_1", "커밋 처리 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
