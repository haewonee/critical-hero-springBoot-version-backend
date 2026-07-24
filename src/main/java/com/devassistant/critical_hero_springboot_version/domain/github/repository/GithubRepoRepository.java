package com.devassistant.critical_hero_springboot_version.domain.github.repository;

import com.devassistant.critical_hero_springboot_version.domain.github.entity.GithubRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GithubRepoRepository extends JpaRepository<GithubRepo, Long> {

    Optional<GithubRepo> findByOwnerAndName(String owner, String name);
}
