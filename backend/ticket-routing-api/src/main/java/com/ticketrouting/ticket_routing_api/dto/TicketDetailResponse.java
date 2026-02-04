package com.ticketrouting.ticket_routing_api.dto;

import com.ticketrouting.ticket_routing_api.model.TicketDetail;
import org.antlr.v4.runtime.misc.DoubleKeyMap;

import java.time.LocalDateTime;

public class TicketDetailResponse {

    private Double aiConfidenceScore;
    private String aiPredictedTeam;
    private String aiReason;
    private Boolean aiVerified;
    private LocalDateTime createdAt;

    public TicketDetailResponse() {}

    public static TicketDetailResponse from(TicketDetail d) {
        if (d == null) return null;

        TicketDetailResponse r = new TicketDetailResponse();
        r.setAiConfidenceScore(d.getAiConfidenceScore());
        r.setAiPredictedTeam(d.getAiPredictedTeam());
        r.setAiReason(d.getAiReason());
        r.setAiVerified(d.getAiVerified());
        r.setCreatedAt(d.getCreatedAt());
        return r;
    }

    public Double getAiConfidenceScore() {
        return aiConfidenceScore;
    }

    public void setAiConfidenceScore(Double aiConfidenceScore) {
        this.aiConfidenceScore = aiConfidenceScore;
    }

    public String getAiPredictedTeam() {
        return aiPredictedTeam;
    }

    public void setAiPredictedTeam(String aiPredictedTeam) {
        this.aiPredictedTeam = aiPredictedTeam;
    }

    public String getAiReason() {
        return aiReason;
    }

    public void setAiReason(String aiReason) {
        this.aiReason = aiReason;
    }

    public Boolean getAiVerified() {
        return aiVerified;
    }

    public void setAiVerified(Boolean aiVerified) {
        this.aiVerified = aiVerified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
