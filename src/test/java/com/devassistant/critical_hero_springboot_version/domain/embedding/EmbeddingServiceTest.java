package com.devassistant.critical_hero_springboot_version.domain.embedding;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.commit.repository.CommitRepository;
import com.devassistant.critical_hero_springboot_version.domain.embedding.repository.CommitEmbeddingRepository;
import com.devassistant.critical_hero_springboot_version.domain.embedding.service.EmbeddingCommandService;
import com.devassistant.critical_hero_springboot_version.domain.embedding.service.EmbeddingQueryService;
import com.devassistant.critical_hero_springboot_version.domain.github.service.GithubCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class EmbeddingServiceTest {

    @Autowired
    private GithubCommandService githubCommandService;

    @Autowired
    private EmbeddingCommandService embeddingCommandService;

    @Autowired
    private EmbeddingQueryService embeddingQueryService;

    @Autowired
    private CommitRepository commitRepository;

    @Autowired
    private CommitEmbeddingRepository commitEmbeddingRepository;

    @Test
    @DisplayName("커밋 임베딩 생성 후 pgvector에 저장되는지 확인")
    void embedAllCommits_shouldSaveEmbeddings() throws Exception {
        // given - 커밋이 없으면 먼저 수집
        if (commitRepository.count() == 0) {
            githubCommandService.indexRepository();
        }

        // when
        embeddingCommandService.embedAllCommits();

        // then
        long count = commitEmbeddingRepository.count();
        System.out.println("저장된 임베딩 수: " + count);
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("질문으로 유사한 커밋 검색되는지 확인")
    void findSimilarCommits_shouldReturnRelatedCommits() throws Exception {
        // given - 임베딩이 없으면 먼저 생성
        if (commitEmbeddingRepository.count() == 0) {
            githubCommandService.indexRepository();
            embeddingCommandService.embedAllCommits();
        }

        // when
        String question = "결제 모듈에서 500 에러가 발생했는데 왜 그럴까?";
        List<Commit> similarCommits = embeddingQueryService.findSimilarCommits(question);

        // then
        System.out.println("유사 커밋 수: " + similarCommits.size());
        similarCommits.forEach(c ->
                System.out.println("- " + c.getSha().substring(0, 7) + " | " + c.getMessage())
        );
        assertThat(similarCommits).isNotEmpty();
    }
}
