package com.ticketrouting.ticket_routing_api.event;

public class TicketCreatedEvent {
    private final Long ticketId;
    private final String subject;

    public TicketCreatedEvent(Long ticketId, String subject) {
        this.ticketId = ticketId;
        this.subject = subject;
    }

    public Long getTicketId() { return ticketId; }
    public String getSubject() { return subject; }
}
