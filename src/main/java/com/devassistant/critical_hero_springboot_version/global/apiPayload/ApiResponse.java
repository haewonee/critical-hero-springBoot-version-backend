package com.devassistant.critical_hero_springboot_version.global.apiPayload;


import com.devassistant.critical_hero_springboot_version.global.apiPayload.code.BaseErrorCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "result"})
public class ApiResponse<T> {

    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    @JsonProperty("code")
    private final String code;

    @JsonProperty("message")
    private final String message;

    @JsonProperty("result")
    private T result;

    // 성공한 경우 (result 포함)
    public static <T> ApiResponse<T> onSuccess(T result) {
        return new ApiResponse<>(true, "SUCCESS", "요청이 성공적으로 처리되었습니다.", result);
    }

    // 성공한 경우 (result 없음)
    public static ApiResponse<Void> onSuccess() {
        return new ApiResponse<>(true, "SUCCESS", "요청이 성공적으로 처리되었습니다.", null);
    }

    // 실패한 경우 (result 포함)
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T result) {
        return new ApiResponse<>(false, code.getCode(), code.getMessage(), result);
    }
}
