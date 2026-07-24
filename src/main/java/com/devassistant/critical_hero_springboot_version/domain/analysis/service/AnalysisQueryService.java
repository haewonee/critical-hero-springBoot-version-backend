package com.devassistant.critical_hero_springboot_version.domain.analysis.service;

import com.devassistant.critical_hero_springboot_version.domain.commit.converter.CommitConverter;
import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.analysis.dto.res.AnalysisResDto;
import com.devassistant.critical_hero_springboot_version.domain.commit.dto.res.CommitResDto;
import com.devassistant.critical_hero_springboot_version.domain.embedding.service.EmbeddingQueryService;
import com.devassistant.critical_hero_springboot_version.global.client.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisQueryService {

    private final EmbeddingQueryService embeddingQueryService;
    private final OpenAiClient openAiClient;

    public AnalysisResDto.Result analyze(String question) {
        // 1. 질문과 유사한 커밋 검색 (RAG)
        List<Commit> similarCommits = embeddingQueryService.findSimilarCommits(question);
        log.info("유사 커밋 {}개 검색됨", similarCommits.size());

        // 2. 커밋 내용을 컨텍스트로 조합
        StringBuilder context = new StringBuilder();
        for (Commit commit : similarCommits) {
            context.append("커밋: ").append(commit.getSha(), 0, 7).append("\n");
            context.append("메시지: ").append(commit.getMessage()).append("\n");
            context.append("변경 내용:\n").append(commit.getDiff()).append("\n\n");
        }

        // 3. Claude에게 분석 요청
        String systemPrompt = """
                당신은 GitHub 커밋 히스토리를 분석하는 개발 전문가입니다.
                주어진 커밋 내용을 바탕으로 개발자의 질문에 답변해 주세요.
                답변은 한국어로, 간결하고 명확하게 작성해 주세요.
                """;

        String userMessage = String.format("""
                [관련 커밋 히스토리]
                %s

                [질문]
                %s
                """, context, question);

        String analysisResult = openAiClient.analyze(systemPrompt, userMessage);

        // 4. 결과 반환
        List<CommitResDto.Summary> commitSummaries = similarCommits.stream()
                .map(CommitConverter::toSummary)
                .toList();

        return new AnalysisResDto.Result(question, commitSummaries, analysisResult);
    }
}
