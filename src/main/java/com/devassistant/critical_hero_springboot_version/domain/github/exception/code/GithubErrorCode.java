package com.devassistant.critical_hero_springboot_version.domain.github.exception.code;

import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GithubErrorCode implements BaseErrorCode {

    REPO_NOT_FOUND(HttpStatus.NOT_FOUND, "GITHUB404_1", "레포지토리를 찾을 수 없습니다."),
    REPO_NOT_INDEXED(HttpStatus.BAD_REQUEST, "GITHUB400_1", "레포지토리가 인덱싱되지 않았습니다."),
    GITHUB_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GITHUB500_1", "GitHub API 호출 중 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
