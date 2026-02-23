package com.microservices.loan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Aggregated response summary from all AI agents.
 * Contains a list of agent tasks and final decisions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponseSummary {

    private List<AgentTask> agentTasks;
    private Decisions decisions;
}
