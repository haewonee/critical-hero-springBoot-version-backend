package com.devassistant.critical_hero_springboot_version.domain.analysis.exception;

import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import com.devassistant.critical_hero_springboot_version.global.apiPayload.exception.GeneralException;

public class AnalysisException extends GeneralException {
    public AnalysisException(BaseErrorCode code) {
        super(code);
    }
}


