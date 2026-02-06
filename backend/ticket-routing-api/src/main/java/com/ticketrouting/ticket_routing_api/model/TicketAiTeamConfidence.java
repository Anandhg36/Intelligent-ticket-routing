package com.ticketrouting.ticket_routing_api.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_ai_team_confidence")
public class TicketAiTeamConfidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    private String teamName;
    private Double confidence;
    private Integer rankOrder;

    private LocalDateTime createdAt = LocalDateTime.now();

    public void setTicket(Ticket ticket) {
        this.ticket= ticket;
    }

    public void setTeamName(String team) {
        this.teamName = team;
    }

    public void setConfidence(Double score) {
        this.confidence=score;
    }

    public void setRankOrder(int rank) {
        this.rankOrder=rank;
    }

    public String getTeamName() {
        return  this.teamName;
    }

    public Double getConfidence() {
        return this.confidence;
    }

    public Integer getRankOrder() {
        return  this.rankOrder;
    }
}

