package com.devassistant.critical_hero_springboot_version.domain.analysis.dto.res;

import com.devassistant.critical_hero_springboot_version.domain.commit.dto.res.CommitResDto;

import java.util.List;

public class AnalysisResDto {

    public record Result(
            String question,
            List<CommitResDto.Summary> relatedCommits,
            String analysisResult
    ) {}
}
