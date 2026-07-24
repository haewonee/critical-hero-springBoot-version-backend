package com.devassistant.critical_hero_springboot_version.domain.github.service;

import com.devassistant.critical_hero_springboot_version.domain.commit.converter.CommitConverter;
import com.devassistant.critical_hero_springboot_version.domain.commit.dto.res.CommitResDto;
import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.github.entity.GithubRepo;
import com.devassistant.critical_hero_springboot_version.domain.github.repository.GithubRepoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GithubQueryService {

    private final GithubRepoRepository githubRepoRepository;
    private final CommitRepository commitRepository;

    @Value("${github.repo}")
    private String defaultRepo;

    @Transactional(readOnly = true)
    public List<CommitResDto.Summary> getRecentCommits() {
        String[] parts = defaultRepo.split("/");
        GithubRepo repo = githubRepoRepository.findByOwnerAndName(parts[0], parts[1])
                .orElseThrow(() -> new IllegalStateException("레포가 인덱싱되지 않았습니다."));

        return commitRepository.findTop20ByRepoOrderByCommittedAtDesc(repo)
                .stream()
                .map(CommitConverter::toSummary)
                .toList();
    }
}
