package com.ticketrouting.ticket_routing_api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CreateTicketRequest {

    @NotBlank
    private String subject;

    @Email
    @NotBlank
    private String requesterEmail;

    // optional - customer name if you want
    private String requesterName;

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
}
