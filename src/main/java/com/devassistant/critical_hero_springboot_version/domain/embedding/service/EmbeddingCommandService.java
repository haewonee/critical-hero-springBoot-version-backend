package com.devassistant.critical_hero_springboot_version.domain.embedding.service;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.embedding.entity.CommitEmbedding;
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
public class EmbeddingCommandService {

    private final CommitRepository commitRepository;
    private final CommitEmbeddingRepository commitEmbeddingRepository;
    private final OpenAiClient openAiClient;

    @Transactional
    public void embedAllCommits() {
        List<Commit> commits = commitRepository.findAll();

        for (Commit commit : commits) {
            if (commitEmbeddingRepository.existsByCommit(commit)) {
                log.info("임베딩 이미 존재, 스킵: {}", commit.getSha());
                continue;
            }

            String text = commit.getMessage() + "\n" + commit.getDiff();
            float[] embedding = openAiClient.createEmbedding(text);
            String vectorString = openAiClient.toVectorString(embedding);

            commitEmbeddingRepository.save(
                    CommitEmbedding.builder()
                            .commit(commit)
                            .embedding(vectorString)
                            .build()
            );
            log.info("임베딩 저장 완료: {}", commit.getSha());
        }
    }
}
