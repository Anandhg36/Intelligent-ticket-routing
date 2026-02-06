package com.ticketrouting.ticket_routing_api.service;

import com.ticketrouting.ticket_routing_api.dto.TicketActivityResponse;
import com.ticketrouting.ticket_routing_api.model.Team;
import com.ticketrouting.ticket_routing_api.model.Ticket;
import com.ticketrouting.ticket_routing_api.model.TicketActivity;
import com.ticketrouting.ticket_routing_api.repository.TeamRepository;
import com.ticketrouting.ticket_routing_api.repository.TicketActivityRepository;
import com.ticketrouting.ticket_routing_api.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketActivityService {

    private final TicketRepository ticketRepo;
    private final TicketActivityRepository activityRepo;
    private final TeamRepository teamRepo;

    public TicketActivityService(
            TicketRepository ticketRepo,
            TicketActivityRepository activityRepo,
            TeamRepository teamRepo
    ) {
        this.ticketRepo = ticketRepo;
        this.activityRepo = activityRepo;
        this.teamRepo = teamRepo;
    }

    public List<TicketActivity> listByTicket(Long ticketId) {
        return activityRepo.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public TicketActivity recordReassignment(Long ticketId, TicketActivity input) {

        Ticket ticket = ticketRepo.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        // ✅ Lookup team by name
        Team newTeam = teamRepo.findByNameIgnoreCase(input.getHumanAssignedTeam())
                .orElseThrow(() ->
                        new RuntimeException("Team not found: " + input.getHumanAssignedTeam())
                );

        // ✅ Update ticket FK (this WILL update DB)
        ticket.setAssignedTeam(newTeam);
        ticketRepo.save(ticket);

        // ✅ Create activity record
        TicketActivity activity = new TicketActivity();
        activity.setTicket(ticket);
        activity.setAiAssignedTeam(input.getAiAssignedTeam());
        activity.setHumanAssignedTeam(input.getHumanAssignedTeam());
        activity.setAiSuggestedWrong(input.getAiSuggestedWrong());
        activity.setTeamReview(input.getTeamReview());
        activity.setCreatedAt(LocalDateTime.now());

        return activityRepo.save(activity);
    }

}
