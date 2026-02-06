package com.ticketrouting.ticket_routing_api.dto;

import java.time.LocalDateTime;

public class TicketActivityResponse {

    private Long id;
    private String aiAssignedTeam;
    private String humanAssignedTeam;
    private boolean aiSuggestedWrong;
    private String teamReview;
    private LocalDateTime createdAt;

    public TicketActivityResponse(
            Long id,
            String aiAssignedTeam,
            String humanAssignedTeam,
            boolean aiSuggestedWrong,
            String teamReview,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.aiAssignedTeam = aiAssignedTeam;
        this.humanAssignedTeam = humanAssignedTeam;
        this.aiSuggestedWrong = aiSuggestedWrong;
        this.teamReview = teamReview;
        this.createdAt = createdAt;
    }

    // getters only
    public Long getId() { return id; }
    public String getAiAssignedTeam() { return aiAssignedTeam; }
    public String getHumanAssignedTeam() { return humanAssignedTeam; }
    public boolean isAiSuggestedWrong() { return aiSuggestedWrong; }
    public String getTeamReview() { return teamReview; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
