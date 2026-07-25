package com.devassistant.critical_hero_springboot_version.domain.embedding.service;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.embedding.repository.CommitEmbeddingRepository;
import com.devassistant.critical_hero_springboot_version.global.client.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingQueryService {

    private final CommitEmbeddingRepository commitEmbeddingRepository;
    private final OpenAiClient openAiClient;

    @Transactional(readOnly = true)
    public List<Commit> findSimilarCommits(String question) {
        float[] questionEmbedding = openAiClient.createEmbedding(question);
        String queryVector = openAiClient.toVectorString(questionEmbedding);
        return commitEmbeddingRepository.findSimilarCommits(queryVector);
    }

    // CRITICAL/WARNING 커밋만 검색
    @Transactional(readOnly = true)
    public List<Commit> findRiskyCommits(String question) {
        float[] questionEmbedding = openAiClient.createEmbedding(question);
        String queryVector = openAiClient.toVectorString(questionEmbedding);
        return commitEmbeddingRepository.findRiskyCommits(queryVector);
    }
}
