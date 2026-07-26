package com.devassistant.critical_hero_springboot_version.domain.webhook.dto.res;

import com.devassistant.critical_hero_springboot_version.domain.webhook.enums.RiskLevel;

public record AnalysisResDto(RiskLevel level, String reason) {}
