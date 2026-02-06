package com.ticketrouting.ticket_routing_api.controller;

import com.ticketrouting.ticket_routing_api.dto.TicketActivityResponse;
import com.ticketrouting.ticket_routing_api.model.Ticket;
import com.ticketrouting.ticket_routing_api.model.TicketActivity;
import com.ticketrouting.ticket_routing_api.repository.TicketRepository;
import com.ticketrouting.ticket_routing_api.service.TicketActivityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketNumber}/activities")
@CrossOrigin(origins = "http://localhost:4200")
public class TicketActivityController {

    private final TicketActivityService service;
    private final TicketRepository ticketRepository;

    public TicketActivityController(
            TicketActivityService service,
            TicketRepository ticketRepository
    ) {
        this.service = service;
        this.ticketRepository = ticketRepository;
    }

    // 🔹 List activities for a ticket
    @GetMapping
    public List<TicketActivity> list(@PathVariable String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketNumber));

        return service.listByTicket(ticket.getId());
    }

    // 🔹 Reassign ticket (AI vs Human tracking)
    @PostMapping("/reassign")
    public TicketActivityResponse reassignTicket(
            @PathVariable String ticketNumber,
            @RequestBody TicketActivity payload
    ) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + ticketNumber));

        TicketActivity saved = service.recordReassignment(ticket.getId(), payload);

        return new TicketActivityResponse(
                saved.getId(),
                saved.getAiAssignedTeam(),
                saved.getHumanAssignedTeam(),
                saved.getAiSuggestedWrong(),
                saved.getTeamReview(),
                saved.getCreatedAt()
        );
    }

}
