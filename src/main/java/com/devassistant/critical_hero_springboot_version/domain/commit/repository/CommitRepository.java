package com.devassistant.critical_hero_springboot_version.domain.commit.repository;

import com.devassistant.critical_hero_springboot_version.domain.commit.entity.Commit;
import com.devassistant.critical_hero_springboot_version.domain.github.entity.GithubRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommitRepository extends JpaRepository<Commit, Long> {

    boolean existsBySha(String sha);

    Optional<Commit> findBySha(String sha);

    List<Commit> findTop20ByRepoOrderByCommittedAtDesc(GithubRepo repo);
}
