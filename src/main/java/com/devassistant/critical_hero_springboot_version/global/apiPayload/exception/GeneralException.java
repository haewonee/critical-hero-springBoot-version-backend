package com.devassistant.critical_hero_springboot_version.global.apiPayload.exception;

import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {
    private final BaseErrorCode code;
}


