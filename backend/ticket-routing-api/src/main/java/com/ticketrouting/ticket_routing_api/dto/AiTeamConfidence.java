package com.ticketrouting.ticket_routing_api.dto;

import com.ticketrouting.ticket_routing_api.model.TicketAiTeamConfidence;

public class AiTeamConfidence {

    private String team;
    private Double confidence;
    private Integer rankOrder;

    public AiTeamConfidence() {}

    public AiTeamConfidence(String team, Double confidence, Integer rankOrder) {
        this.team = team;
        this.confidence = confidence;
        this.rankOrder = rankOrder;
    }

    // ✅ STATIC MAPPER
    public static AiTeamConfidence from(TicketAiTeamConfidence entity) {
        AiTeamConfidence dto = new AiTeamConfidence();
        dto.setTeam(entity.getTeamName());
        dto.setConfidence(entity.getConfidence());
        dto.setRankOrder(entity.getRankOrder());
        return dto;
    }

    // getters & setters
    public String getTeam() { return team; }
    public void setTeam(String team) { this.team = team; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public Integer getRankOrder() { return rankOrder; }
    public void setRankOrder(Integer rankOrder) { this.rankOrder = rankOrder; }
}
