package com.devassistant.critical_hero_springboot_version.domain.github.exception;

import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import com.devassistant.critical_hero_springboot_version.global.apiPayload.exception.GeneralException;

public class GithubException extends GeneralException {
    public GithubException(BaseErrorCode code) {
        super(code);
    }
}
