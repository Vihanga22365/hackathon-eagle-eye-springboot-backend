package com.microservices.loan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for creating an AI agents feedback record.
 *
 * Required fields:
 *  - loggedUserId       : ID of the user submitting the feedback
 *  - loanId             : ID of the loan being evaluated
 *  - loanUserId         : ID of the user who owns the loan
 *  - summary            : high-level risk/loan-type summary
 *  - agentResponseSummary : detailed per-agent task log and decisions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentsFeedbackRequest {

    private String loggedUserId;
    private String loanId;
    private String loanUserId;
    private AiFeedbackSummary summary;
    private AgentResponseSummary agentResponseSummary;
}
