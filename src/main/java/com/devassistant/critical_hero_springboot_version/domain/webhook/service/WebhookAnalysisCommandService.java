package com.devassistant.critical_hero_springboot_version.domain.webhook.service;

import com.devassistant.critical_hero_springboot_version.global.client.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookAnalysisCommandService {

    private final OpenAiClient openAiClient;

    public enum RiskLevel {
        CRITICAL, WARNING, SAFE
    }

    public record AnalysisResult(RiskLevel level, String reason) {}

    public AnalysisResult analyze(String commitMessage, String diff) {
        String systemPrompt = """
                당신은 코드 보안 및 품질 전문가입니다.
                커밋 메시지와 diff를 보고 위험도를 3단계로 판단하세요.

                🔴 CRITICAL 기준:
                - 하드코딩된 비밀번호/API키/토큰/시크릿 (password="...", api_key="sk-..." 등)
                - Bearer 토큰, JWT, DB 연결 문자열 등 인증 정보를 응답/로그에 노출
                - 암호화 없이 민감 데이터(개인정보, 카드번호 등)를 평문 저장 또는 전송
                - 외부 API로 민감 데이터 무단 전송
                - SQL Injection (사용자 입력을 직접 쿼리에 문자열 연결)
                - Command Injection (사용자 입력을 shell=True 명령어에 삽입)
                - 인증/권한 체크 로직 삭제 또는 우회
                - 결제/금융 관련 핵심 로직 무단 변경
                - DROP TABLE, DELETE FROM (WHERE 없음), rm -rf 등 대량 데이터 삭제

                🟡 WARNING 기준:
                - NullPointerException 가능성 (null 체크 없는 객체 접근)
                - 예외처리 누락 (빈 catch 블록, Exception 묻어버리기)
                - 인증 없이 민감한 작업 수행 (삭제, 수정 등)
                - TODO/FIXME/HACK 주석과 함께 올라온 미완성 코드
                - 동시성 문제 가능성 (Thread-safe하지 않은 공유 자원 접근)
                - 리소스 누수 (File, Connection, Stream 미닫음)
                - 성능 문제 (N+1 쿼리, 루프 내 DB 호출, 무한 루프 가능성)
                - 핵심 비즈니스 로직 대규모 삭제

                🟢 SAFE 기준:
                - README, 문서, 주석 수정
                - 코드 스타일/포맷 변경
                - 테스트 코드 추가
                - 단순 변수명/메서드명 변경

                반드시 아래 형식으로만 답하세요:
                CRITICAL: {한국어로 위험 이유 1~2줄}
                또는
                WARNING: {한국어로 경고 이유 1~2줄}
                또는
                SAFE: 안전한 커밋입니다.
                """;

        String userMessage = String.format("""
                [커밋 메시지]
                %s

                [변경된 코드 diff]
                %s
                """, commitMessage, diff != null ? diff : "(diff 없음)");

        String response = openAiClient.analyze(systemPrompt, userMessage).trim();
        log.info("GPT 위험도 분석 결과: {}", response);

        if (response.startsWith("CRITICAL")) {
            String reason = response.substring("CRITICAL:".length()).trim();
            return new AnalysisResult(RiskLevel.CRITICAL, reason);
        } else if (response.startsWith("WARNING")) {
            String reason = response.substring("WARNING:".length()).trim();
            return new AnalysisResult(RiskLevel.WARNING, reason);
        } else {
            return new AnalysisResult(RiskLevel.SAFE, "안전한 커밋입니다.");
        }
    }
}
