package com.ticketrouting.ticket_routing_api.service;

import com.ticketrouting.ticket_routing_api.model.Ticket;
import com.ticketrouting.ticket_routing_api.model.TicketDetail;
import com.ticketrouting.ticket_routing_api.repository.TicketDetailRepository;
import com.ticketrouting.ticket_routing_api.repository.TicketRepository;
import org.springframework.stereotype.Service;

@Service
public class TicketDetailService {

    private final TicketDetailRepository detailRepo;
    private final TicketRepository ticketRepo;

    public TicketDetailService(TicketDetailRepository detailRepo, TicketRepository ticketRepo) {
        this.detailRepo = detailRepo;
        this.ticketRepo = ticketRepo;
    }

    public TicketDetail getByTicketId(Long ticketId) {
        return detailRepo.findByTicketId(ticketId)
                .orElseThrow(() -> new RuntimeException("TicketDetail not found for ticket: " + ticketId));
    }

    public TicketDetail upsert(Long ticketId, TicketDetail input) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        TicketDetail detail = detailRepo.findByTicketId(ticketId).orElse(null);

        if (detail == null) {
            detail = new TicketDetail();
            detail.setTicket(ticket);
        }

        detail.setDescription(input.getDescription());
        detail.setAiSuggestedTeam(input.getAiSuggestedTeam());
        detail.setAiConfidence(input.getAiConfidence());

        return detailRepo.save(detail);
    }

    public void deleteByTicketId(Long ticketId) {
        TicketDetail detail = getByTicketId(ticketId);
        detailRepo.delete(detail);
    }
}
