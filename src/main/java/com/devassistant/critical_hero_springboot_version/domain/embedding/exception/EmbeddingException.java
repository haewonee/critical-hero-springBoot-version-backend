package com.devassistant.critical_hero_springboot_version.domain.embedding.exception;

import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import com.devassistant.critical_hero_springboot_version.global.apiPayload.exception.GeneralException;

public class EmbeddingException extends GeneralException {
    public EmbeddingException(BaseErrorCode code) {
        super(code);
    }
}
