package com.devassistant.critical_hero_springboot_version.domain.github;

import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.github.service.GithubCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GithubCommandServiceTest {

    @Autowired
    private GithubCommandService githubCommandService;

    @Autowired
    private CommitRepository commitRepository;

    @Test
    @DisplayName("GitHub 커밋 수집 후 DB에 저장되는지 확인")
    void indexRepository_shouldSaveCommits() throws Exception {
        // when
        githubCommandService.indexRepository();

        // then
        long count = commitRepository.count();
        System.out.println("저장된 커밋 수: " + count);
        assertThat(count).isGreaterThan(0);
    }
}
