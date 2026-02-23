package com.microservices.loan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AiAgentsFeedback — document stored under the `aiAgentsFeedback` Firebase collection.
 *
 * Stored fields:
 *  - id              : auto-generated unique feedback ID
 *  - loggedUserId    : ID of the user who triggered/logged this feedback
 *  - loanId          : ID of the loan being evaluated
 *  - loanUserId      : ID of the user who owns the loan
 *  - timestamp       : epoch milliseconds of creation
 *  - dateTime        : human-readable ISO-8601 date-time string
 *  - summary         : high-level risk/loan-type summary
 *  - agentResponseSummary : detailed per-agent task log and decisions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentsFeedback {

    private String id;
    private String loggedUserId;
    private String loggedUserName;
    private String loanId;
    private String loanUserId;
    private String loanUserName;
    private Long timestamp;
    private String dateTime;
    private AiFeedbackSummary summary;
    private AgentResponseSummary agentResponseSummary;
}
