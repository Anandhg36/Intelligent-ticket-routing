package com.ticketrouting.ticket_routing_api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_detail")
public class TicketDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // uq_ticket_detail_ticket ensures 1:1
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @Lob
    private String description;

    @Column(name = "ai_suggested_team", length = 120)
    private String aiSuggestedTeam;

    @Column(name = "ai_confidence")
    private Double aiConfidence;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public TicketDetail() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAiSuggestedTeam() { return aiSuggestedTeam; }
    public void setAiSuggestedTeam(String aiSuggestedTeam) { this.aiSuggestedTeam = aiSuggestedTeam; }

    public Double getAiConfidence() { return aiConfidence; }
    public void setAiConfidence(Double aiConfidence) { this.aiConfidence = aiConfidence; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Double getAiConfidenceScore() {
        return this.aiConfidence;
    }

    public String getAiPredictedTeam() {
        return this.aiSuggestedTeam;
    }

    public String getAiReason() {
        return null;
    }

    public Boolean getAiVerified() {
        return null;
    }
}