package com.ticketrouting.ticket_routing_api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_activity")
public class TicketActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "ai_assigned_team", length = 100)
    private String aiAssignedTeam;

    @Column(name = "human_assigned_team", length = 100)
    private String humanAssignedTeam;

    @Column(name = "ai_suggested_wrong")
    private Boolean aiSuggestedWrong;

    @Column(name = "team_review", columnDefinition = "TEXT")
    private String teamReview;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public TicketActivity() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // -------- getters & setters --------

    public Long getId() { return id; }

    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    public String getAiAssignedTeam() { return aiAssignedTeam; }
    public void setAiAssignedTeam(String aiAssignedTeam) {
        this.aiAssignedTeam = aiAssignedTeam;
    }

    public String getHumanAssignedTeam() { return humanAssignedTeam; }
    public void setHumanAssignedTeam(String humanAssignedTeam) {
        this.humanAssignedTeam = humanAssignedTeam;
    }

    public Boolean getAiSuggestedWrong() { return aiSuggestedWrong; }
    public void setAiSuggestedWrong(Boolean aiSuggestedWrong) {
        this.aiSuggestedWrong = aiSuggestedWrong;
    }

    public String getTeamReview() { return teamReview; }
    public void setTeamReview(String teamReview) {
        this.teamReview = teamReview;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setCreatedAt(LocalDateTime now) {
        this.createdAt = now;
    }
}
