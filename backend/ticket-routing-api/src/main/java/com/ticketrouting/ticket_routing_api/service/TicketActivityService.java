package com.ticketrouting.ticket_routing_api.service;

import com.ticketrouting.ticket_routing_api.model.Ticket;
import com.ticketrouting.ticket_routing_api.model.TicketActivity;
import com.ticketrouting.ticket_routing_api.repository.TicketActivityRepository;
import com.ticketrouting.ticket_routing_api.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketActivityService {

    private final TicketActivityRepository activityRepo;
    private final TicketRepository ticketRepo;

    public TicketActivityService(TicketActivityRepository activityRepo, TicketRepository ticketRepo) {
        this.activityRepo = activityRepo;
        this.ticketRepo = ticketRepo;
    }

    public List<TicketActivity> listByTicket(Long ticketId) {
        return activityRepo.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public TicketActivity addActivity(Long ticketId, TicketActivity input) {
        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketId));

        TicketActivity a = new TicketActivity();
        a.setTicket(ticket);
        a.setAction(input.getAction());
        a.setMeta(input.getMeta());

        return activityRepo.save(a);
    }

    public void delete(Long id) {
        if (!activityRepo.existsById(id)) throw new RuntimeException("Activity not found: " + id);
        activityRepo.deleteById(id);
    }
}
