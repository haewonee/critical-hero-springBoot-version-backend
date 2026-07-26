package com.devassistant.critical_hero_springboot_version.domain.commit.dto.res;

import java.time.LocalDateTime;

public class CommitResDto {

    public record Summary(
            String sha,
            String author,
            String message,
            LocalDateTime committedAt,
            String riskLevel
    ) {}
}
