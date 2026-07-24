package com.devassistant.critical_hero_springboot_version.domain.github.entity;

import com.devassistant.critical_hero_springboot_version.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "github_repos")
@Getter
@NoArgsConstructor
public class GithubRepo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String name;



    @Builder
    public GithubRepo(String owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    public String getFullName() {
        return owner + "/" + name;
    }
}
