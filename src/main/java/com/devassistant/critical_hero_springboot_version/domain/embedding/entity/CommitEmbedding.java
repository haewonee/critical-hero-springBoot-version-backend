package com.devassistant.critical_hero_springboot_version.domain.embedding.entity;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "commit_embeddings")
@Getter
@NoArgsConstructor
public class CommitEmbedding extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commit_id", nullable = false)
    private Commit commit;

    @Column(columnDefinition = "vector(1536)", nullable = false)
    private String embedding;

    @Builder
    public CommitEmbedding(Commit commit, String embedding) {
        this.commit = commit;
        this.embedding = embedding;
    }
}
