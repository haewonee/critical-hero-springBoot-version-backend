package com.devassistant.critical_hero_springboot_version.domain.embedding.exception.code;

import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum EmbeddingErrorCode implements BaseErrorCode {

    EMBEDDING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMBEDDING500_1", "임베딩 생성 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
