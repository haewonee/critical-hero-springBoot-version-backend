package com.devassistant.critical_hero_springboot_version.domain.github.service;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.github.entity.GithubRepo;
import com.devassistant.critical_hero_springboot_version.domain.github.repository.GithubRepoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GithubCommandService {

    private final GithubRepoRepository githubRepoRepository;
    private final CommitRepository commitRepository;

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.repo}")
    private String defaultRepo;

    @Transactional
    public GithubRepo indexRepository() throws IOException {
        String[] parts = defaultRepo.split("/");
        String owner = parts[0];
        String name = parts[1];

        GithubRepo repo = githubRepoRepository.findByOwnerAndName(owner, name)
                .orElseGet(() -> githubRepoRepository.save(
                        GithubRepo.builder()
                                .owner(owner)
                                .name(name)
                                .build()
                ));

        GitHub github = new GitHubBuilder().withOAuthToken(githubToken).build();
        GHRepository ghRepo = github.getRepository(defaultRepo);
        List<GHCommit> commits = ghRepo.listCommits().withPageSize(20).toList();

        for (GHCommit ghCommit : commits) {
            String sha = ghCommit.getSHA1();

            if (commitRepository.existsBySha(sha)) {
                log.info("커밋 이미 존재, 스킵: {}", sha);
                continue;
            }

            LocalDateTime committedAt = ghCommit.getCommitDate()
                    .toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDateTime();

            Commit commit = Commit.builder()
                    .repo(repo)
                    .sha(sha)
                    .author(ghCommit.getCommitShortInfo().getAuthor().getName())
                    .message(ghCommit.getCommitShortInfo().getMessage())
                    .diff(fetchDiff(ghCommit))
                    .committedAt(committedAt)
                    .build();

            commitRepository.save(commit);
            log.info("커밋 저장: {}", sha);
        }

        return repo;
    }

    private String fetchDiff(GHCommit ghCommit) {
        try {
            StringBuilder sb = new StringBuilder();
            for (GHCommit.File file : ghCommit.listFiles().toList()) {
                sb.append("파일: ").append(file.getFileName()).append("\n");
                if (file.getPatch() != null) {
                    sb.append(file.getPatch()).append("\n");
                }
            }
            return sb.toString();
        } catch (IOException e) {
            log.warn("diff 가져오기 실패: {}", ghCommit.getSHA1());
            return "";
        }
    }
}
