package com.devassistant.critical_hero_springboot_version.domain.embedding.service;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.embedding.repository.CommitEmbeddingRepository;
import com.devassistant.critical_hero_springboot_version.global.client.OpenAiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbcTemplate;

    // 새로 들어온 커밋 하나만 임베딩 생성 (Webhook용)
    @Transactional
    public void embedSingleCommit(Commit commit) {
        if (commitEmbeddingRepository.existsByCommit(commit)) {
            log.info("임베딩 이미 존재, 스킵: {}", commit.getSha());
            return;
        }
        String text = commit.getMessage() + "\n" + (commit.getDiff() != null ? commit.getDiff() : "");
        float[] embedding = openAiClient.createEmbedding(text);
        String vectorString = openAiClient.toVectorString(embedding);

        try {
            PGobject pgVector = new PGobject();
            pgVector.setType("vector");
            pgVector.setValue(vectorString);
            jdbcTemplate.update(
                    "INSERT INTO commit_embeddings (commit_id, embedding, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                    commit.getId(), pgVector
            );
            log.info("임베딩 저장 완료: {}", commit.getSha());
        } catch (Exception e) {
            throw new RuntimeException("임베딩 저장 실패: " + commit.getSha(), e);
        }
    }

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

            // Hibernate가 PGobject → bytea로 잘못 변환하는 문제 때문에
            // JDBC 레벨에서 직접 vector 타입으로 INSERT
            try {
                PGobject pgVector = new PGobject();
                pgVector.setType("vector");
                pgVector.setValue(vectorString);

                jdbcTemplate.update(
                        "INSERT INTO commit_embeddings (commit_id, embedding, created_at, updated_at) VALUES (?, ?, NOW(), NOW())",
                        commit.getId(), pgVector
                );
                log.info("임베딩 저장 완료: {}", commit.getSha());
            } catch (Exception e) {
                throw new RuntimeException("임베딩 저장 실패: " + commit.getSha(), e);
            }
        }
    }
}
