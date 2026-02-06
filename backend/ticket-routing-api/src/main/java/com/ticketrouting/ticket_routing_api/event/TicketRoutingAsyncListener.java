package com.ticketrouting.ticket_routing_api.event;


import com.ticketrouting.ticket_routing_api.dto.AiTeamConfidence;
import com.ticketrouting.ticket_routing_api.model.Team;
import com.ticketrouting.ticket_routing_api.model.Ticket;
import com.ticketrouting.ticket_routing_api.model.TicketAiTeamConfidence;
import com.ticketrouting.ticket_routing_api.model.TicketDetail;
import com.ticketrouting.ticket_routing_api.repository.TeamRepository;
import com.ticketrouting.ticket_routing_api.repository.TicketAiTeamConfidenceRepository;
import com.ticketrouting.ticket_routing_api.repository.TicketDetailRepository;
import com.ticketrouting.ticket_routing_api.repository.TicketRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class TicketRoutingAsyncListener {

    private final TicketRepository ticketRepository;
    private final TicketDetailRepository ticketDetailRepository;
    private final TeamRepository teamRepository;
    private final AiRoutingClient aiRoutingClient;
    private final TicketAiTeamConfidenceRepository ticketAiTeamConfidenceRepository;

    public TicketRoutingAsyncListener(
            TicketRepository ticketRepository,
            TicketDetailRepository ticketDetailRepository,
            TeamRepository teamRepository,
            TicketAiTeamConfidenceRepository ticketAiTeamConfidenceRepository,
            AiRoutingClient aiRoutingClient
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketDetailRepository = ticketDetailRepository;
        this.teamRepository = teamRepository;
        this.aiRoutingClient = aiRoutingClient;
        this.ticketAiTeamConfidenceRepository = ticketAiTeamConfidenceRepository;
    }

    // IMPORTANT: run only after transaction commits successfully
    @Async("aiExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketCreated(TicketCreatedEvent event) {

        System.out.println(">>> [AI-LISTENER] Event received for ticketId=" + event.getTicketId());
        System.out.println(">>> [AI-LISTENER] Running in thread: " + Thread.currentThread().getName());

        Ticket ticket = ticketRepository.findById(event.getTicketId()).orElse(null);
        if (ticket == null) {
            System.out.println(">>> [AI-LISTENER] Ticket not found, exiting");
            return;
        }

        System.out.println(">>> [AI-LISTENER] Calling AI for subject: " + ticket.getSubject());

        // =========================
        // STEP 0 — CALL AI
        // =========================
        AiSearchResponse response = aiRoutingClient.search(ticket.getSubject());
        if (response == null || response.getTeams() == null || response.getTeams().isEmpty()) {
            System.out.println(">>> [AI-LISTENER] Empty AI response");
            return;
        }

        List<AiTeamConfidence> teams = response.getTeams();
        List<AiSearchResult> results = response.getResults();

        // =========================
        // STEP 1 — SAVE TOP 3 TEAM CONFIDENCES (ROWS)
        // =========================
        int rank = 1;
        for (AiTeamConfidence t : teams.stream().limit(3).toList()) {

            TicketAiTeamConfidence conf = new TicketAiTeamConfidence();
            conf.setTicket(ticket);
            conf.setTeamName(t.getTeam());
            conf.setConfidence(t.getConfidence()); // already 0–100 from Python
            conf.setRankOrder(rank++);

            ticketAiTeamConfidenceRepository.save(conf);
        }

        // =========================
        // STEP 2 — TOP TEAM DECISION
        // =========================
        AiTeamConfidence topTeam = teams.get(0);
        double topConfidence = topTeam.getConfidence();

        TicketDetail detail = ticketDetailRepository
                .findByTicketId(ticket.getId())
                .orElse(null);

        if (detail == null) {
            System.out.println(">>> [AI-LISTENER] TicketDetail not found");
            return;
        }

        // Best supporting chunk (optional but useful)
        String aiSuggestedMessage = null;
        if (results != null && !results.isEmpty()) {
            aiSuggestedMessage = results.get(0).getAiSuggestedMessage();
        }

        // Save AI summary
        detail.setAiSuggestedTeam(topTeam.getTeam());
        detail.setAiConfidence(topConfidence);
        detail.setDescription(aiSuggestedMessage);

        ticketDetailRepository.save(detail);

        // =========================
        // STEP 3 — AUTO ASSIGN (>= 80 ONLY)
        // =========================
        if (topConfidence >= 80) {
            teamRepository.findByNameIgnoreCase(topTeam.getTeam())
                    .ifPresentOrElse(
                            team -> {
                                ticket.setAssignedTeam(team);
                                System.out.println(">>> [AI-LISTENER] Auto-assigned to " + team.getName());
                            },
                            () -> ticket.setAssignedTeam(null)
                    );
        } else {
            ticket.setAssignedTeam(null);
            System.out.println(">>> [AI-LISTENER] Confidence < 80, not auto-assigning");
        }

        ticketRepository.save(ticket);
    }

    private Double toConfidence(Double score) {
        if (score == null) return 0.0;

        // sigmoid mapping: 0..1
        double conf = 1.0 / (1.0 + Math.exp(-score));

        // keep precision (0.00 – 100.00)
        return conf * 100.0;
    }
}
