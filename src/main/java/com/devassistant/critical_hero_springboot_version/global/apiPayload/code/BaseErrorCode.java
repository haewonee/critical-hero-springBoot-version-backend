package com.devassistant.critical_hero_springboot_version.global.apiPayload.code;


import org.springframework.http.HttpStatus;

public interface BaseErrorCode {

    HttpStatus getStatus();
    String getCode();
    String getMessage();
}
