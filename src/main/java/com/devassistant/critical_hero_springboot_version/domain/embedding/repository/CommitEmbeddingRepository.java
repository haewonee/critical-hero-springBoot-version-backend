package com.devassistant.critical_hero_springboot_version.domain.embedding.repository;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.embedding.entity.CommitEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommitEmbeddingRepository extends JpaRepository<CommitEmbedding, Long> {

    boolean existsByCommit(Commit commit);

    @Query(value = """
            SELECT c.id, c.repo_id, c.sha, c.author, c.message, c.diff, c.committed_at, c.created_at, c.updated_at, c.risk_level
            FROM commit_embeddings ce
            JOIN commits c ON ce.commit_id = c.id
            ORDER BY ce.embedding <=> CAST(:queryVector AS vector)
            LIMIT 5
            """, nativeQuery = true)
    List<Commit> findSimilarCommits(@Param("queryVector") String queryVector);

    // CRITICAL/WARNING 커밋만 유사도 검색 (위험 커밋 조회용)
    @Query(value = """
            SELECT c.id, c.repo_id, c.sha, c.author, c.message, c.diff, c.committed_at, c.created_at, c.updated_at, c.risk_level
            FROM commit_embeddings ce
            JOIN commits c ON ce.commit_id = c.id
            WHERE c.risk_level IN ('CRITICAL', 'WARNING')
            ORDER BY ce.embedding <=> CAST(:queryVector AS vector)
            LIMIT 5
            """, nativeQuery = true)
    List<Commit> findRiskyCommits(@Param("queryVector") String queryVector);
}
