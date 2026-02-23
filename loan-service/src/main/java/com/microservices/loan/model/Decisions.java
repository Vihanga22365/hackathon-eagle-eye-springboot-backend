package com.microservices.loan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Decisions made by each AI agent in the feedback process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Decisions {

    private String verificationAnalyzerAgent;
    private String policyReviewerAgent;
    private String marketAnalyzerAgent;
}
