package com.ticketrouting.ticket_routing_api.dto;

import com.ticketrouting.ticket_routing_api.model.Ticket;

import java.time.LocalDateTime;
import java.util.List;

public class TicketResponse {

    private Long id;
    private String ticketNumber;
    private String subject;
    private String status;
    private String priority;

    private String requesterName;
    private String requesterEmail;

    private String assignedTeamName;

    private LocalDateTime createdAt;

    // ✅ change: use DTO not entity
    private TicketDetailResponse ticketDetail;
    private List<AiTeamConfidence> teams;

    public TicketResponse() {}

    public TicketResponse(Long id, String ticketNumber, String subject, String status, String priority,
                          String requesterName, String requesterEmail,
                          String assignedTeamName, LocalDateTime createdAt) {
        this.id = id;
        this.ticketNumber = ticketNumber;
        this.subject = subject;
        this.status = status;
        this.priority = priority;
        this.requesterName = requesterName;
        this.requesterEmail = requesterEmail;
        this.assignedTeamName = assignedTeamName;
        this.createdAt = createdAt;
    }

    // ✅ minimal change: accept detail DTO input (or build from t.getTicketDetail())
    public static TicketResponse from(Ticket t) {
        TicketResponse r = new TicketResponse();
        r.setId(t.getId());
        r.setTicketNumber(t.getTicketNumber());
        r.setSubject(t.getSubject());
        r.setStatus(t.getStatus() != null ? t.getStatus().toString() : null);
        r.setPriority(t.getPriority() != null ? t.getPriority().toString() : null);
        r.setCreatedAt(t.getCreatedAt());

        if (t.getRequester() != null) {
            r.setRequesterName(t.getRequester().getFullName());
            r.setRequesterEmail(t.getRequester().getEmail());
        }

        if (t.getAssignedTeam() != null) {
            r.setAssignedTeamName(t.getAssignedTeam().getName());
        }

        if (t.getTicketDetail() != null) {
            r.setTicketDetail(TicketDetailResponse.from(t.getTicketDetail()));
        }

        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }

    public String getAssignedTeamName() { return assignedTeamName; }
    public void setAssignedTeamName(String assignedTeamName) { this.assignedTeamName = assignedTeamName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // ✅ add proper getter/setter
    public TicketDetailResponse getTicketDetail() { return ticketDetail; }
    public void setTicketDetail(TicketDetailResponse ticketDetail) { this.ticketDetail = ticketDetail; }

    public List<AiTeamConfidence> getTeams() {
        return teams;
    }

    public void setTeams(List<AiTeamConfidence> teams) {
        this.teams = teams;
    }
}