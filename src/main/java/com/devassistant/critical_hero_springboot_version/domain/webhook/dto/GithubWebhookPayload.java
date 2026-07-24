package com.devassistant.critical_hero_springboot_version.domain.webhook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GithubWebhookPayload {

    @JsonProperty("ref")
    public String ref;

    @JsonProperty("commits")
    public List<CommitPayload> commits;

    @JsonProperty("repository")
    public RepositoryPayload repository;

    public static class CommitPayload {
        @JsonProperty("id")
        public String id;

        @JsonProperty("message")
        public String message;

        @JsonProperty("author")
        public AuthorPayload author;

        @JsonProperty("url")
        public String url;
    }

    public static class AuthorPayload {
        @JsonProperty("name")
        public String name;
    }

    public static class RepositoryPayload {
        @JsonProperty("full_name")
        public String fullName;
    }
}
