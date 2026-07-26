package com.devassistant.critical_hero_springboot_version.domain.commit.converter;

import com.devassistant.critical_hero_springboot_version.domain.commit.dto.res.CommitResDto;
import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;

public class CommitConverter {

    public static CommitResDto.Summary toSummary(Commit commit) {
        return new CommitResDto.Summary(
                commit.getSha(),
                commit.getAuthor(),
                commit.getMessage(),
                commit.getCommittedAt(),
                commit.getRiskLevel()
        );
    }
}
