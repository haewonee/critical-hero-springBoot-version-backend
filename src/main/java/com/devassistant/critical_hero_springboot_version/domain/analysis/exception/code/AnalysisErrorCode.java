package com.devassistant.critical_hero_springboot_version.domain.analysis.exception.code;

import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AnalysisErrorCode implements BaseErrorCode {

    CLAUDE_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS500_1", "Claude API 호출 중 오류가 발생했습니다."),
    ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS500_2", "인시던트 분석 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
