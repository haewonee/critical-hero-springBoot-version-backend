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
            SELECT c.id, c.repo_id, c.sha, c.author, c.message, c.diff, c.committed_at, c.created_at, c.updated_at
            FROM commit_embeddings ce
            JOIN commits c ON ce.commit_id = c.id
            ORDER BY ce.embedding <=> CAST(:queryVector AS vector)
            LIMIT 5
            """, nativeQuery = true)
    List<Commit> findSimilarCommits(@Param("queryVector") String queryVector);
}
