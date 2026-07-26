package com.devassistant.critical_hero_springboot_version.domain.webhook.exception;

import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import com.devassistant.critical_hero_springboot_version.global.apiPayload.exception.GeneralException;

public class WebhookException extends GeneralException {
    public WebhookException(BaseErrorCode code) {
        super(code);
    }
}
