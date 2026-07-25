package com.devassistant.critical_hero_springboot_version.global.apiPayload.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeneralSuccessCode implements BaseSuccessCode {

    OK(HttpStatus.OK, "SUCCESS", "요청이 성공적으로 처리되었습니다."),
    CREATED(HttpStatus.CREATED, "CREATED", "리소스가 성공적으로 생성되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
