package com.microservices.loan.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * High-level risk and loan-type summary produced by the AI pipeline.
 *
 * Fields map to the AI response JSON:
 *   riskLevel             ← riskLevel
 *   riskReason            ← reasonForRiskLevel
 *   loanTypeDecision      ← loanTypeWhenGIveToCustomerGetBack
 *   loanTypeReason        ← reasonForLoanType
 *   recommendedRate       ← loanRateCanGiveToCustomer
 *   rateReason            ← reasonForLoanRateRange
 *   requestedLoanAmount   ← requestedLoanAmount  (REQUESTED_AMOUNT_NOT_AVAILABLE if unknown)
 *   recommendedLoanAmount ← recommendedLoanAmount
 *   reasonForLoanAmount   ← reasonForLoanAmount
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedbackSummary {

    private Integer riskLevel;
    private String riskReason;
    private String loanTypeDecision;
    private String loanTypeReason;
    private String recommendedRate;
    private String rateReason;
    private String requestedLoanAmount;
    private String recommendedLoanAmount;
    private String reasonForLoanAmount;
}
