package com.devassistant.critical_hero_springboot_version.domain.commit.entity;

import com.devassistant.critical_hero_springboot_version.domain.github.entity.GithubRepo;
import com.devassistant.critical_hero_springboot_version.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "commits")
@Getter
@NoArgsConstructor
public class Commit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private GithubRepo repo;

    @Column(nullable = false, unique = true, length = 40)
    private String sha;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String diff;

    @Column(name = "committed_at")
    private LocalDateTime committedAt;

    // Webhook 분석 결과 저장 (CRITICAL/WARNING/SAFE/UNKNOWN)
    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Builder
    public Commit(GithubRepo repo, String sha, String author, String message, String diff,
                  LocalDateTime committedAt, String riskLevel) {
        this.repo = repo;
        this.sha = sha;
        this.author = author;
        this.message = message;
        this.diff = diff;
        this.committedAt = committedAt;
        this.riskLevel = riskLevel != null ? riskLevel : "UNKNOWN";
    }

    public void updateRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
