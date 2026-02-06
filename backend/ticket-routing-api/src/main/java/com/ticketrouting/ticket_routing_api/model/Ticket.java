package com.ticketrouting.ticket_routing_api.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_number", nullable = false, length = 30, unique = true)
    private String ticketNumber;

    @Lob
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority =  TicketPriority.LOW;

    @Column(length = 120)
    private String category; // "Cluster Architecture", "Networking", etc.

    @Column(nullable = false)
    private Boolean archived = false;

    // requester_id -> customer.id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private com.ticketrouting.ticket_routing_api.model.Customer requester;

    // assigned_team_id -> team.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_team_id")
    private com.ticketrouting.ticket_routing_api.model.Team assignedTeam;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "ticket", fetch = FetchType.LAZY)
    private TicketDetail ticketDetail;

    public TicketDetail getTicketDetail() {
        return ticketDetail;
    }


    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Ticket() {}

    // --- getters/setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public TicketStatus getStatus() { return status; }
    public void setStatus(TicketStatus status) { this.status = status; }

    public TicketPriority getPriority() { return priority; }
    public void setPriority(TicketPriority priority) { this.priority = priority; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Boolean getArchived() { return archived; }
    public void setArchived(Boolean archived) { this.archived = archived; }

    public Customer getRequester() { return requester; }
    public void setRequester(Customer requester) { this.requester = requester; }

    public Team getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(Team assignedTeam) { this.assignedTeam = assignedTeam; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public void setAssignedTeamName(String humanAssignedTeam) {
    }
}
