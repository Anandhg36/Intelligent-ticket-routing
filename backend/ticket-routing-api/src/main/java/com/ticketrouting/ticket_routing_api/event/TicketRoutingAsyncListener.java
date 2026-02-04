package com.ticketrouting.ticket_routing_api.event;


import com.ticketrouting.ticket_routing_api.model.Team;
import com.ticketrouting.ticket_routing_api.model.Ticket;
import com.ticketrouting.ticket_routing_api.model.TicketDetail;
import com.ticketrouting.ticket_routing_api.repository.TeamRepository;
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

    public TicketRoutingAsyncListener(
            TicketRepository ticketRepository,
            TicketDetailRepository ticketDetailRepository,
            TeamRepository teamRepository,
            AiRoutingClient aiRoutingClient
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketDetailRepository = ticketDetailRepository;
        this.teamRepository = teamRepository;
        this.aiRoutingClient = aiRoutingClient;
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

        // Call AI using subject
        List<AiSearchResult> results = aiRoutingClient.search(ticket.getSubject());
        System.out.println(">>> [AI-LISTENER] AI returned " + results.size() + " results");
        String aiSuggestedMessage = null;
        if (!results.isEmpty()) {
            aiSuggestedMessage = results.get(0).getAiSuggestedMessage();
        }


        Optional<AiSearchResult> best = results.stream()
                .filter(r -> r.getTeam() != null && !r.getTeam().isBlank())
                .max(Comparator.comparing(r -> r.getScore() == null ? 0.0 : r.getScore()));

        // Update ticket_detail
        TicketDetail detail = ticketDetailRepository.findByTicketId(ticket.getId()).orElse(null);
        if (detail == null) {
            System.out.println(">>> [AI-LISTENER] TicketDetail not found, exiting");
            return;
        }

        if (best.isPresent()) {
            AiSearchResult top = best.get();

            // 1) Save AI suggestion
            detail.setAiSuggestedTeam(top.getTeam());
            detail.setDescription(aiSuggestedMessage);

            // 2) Confidence calc (0..100)
            double score = (top.getScore() == null) ? 0.0 : top.getScore();
            Double confidence = toConfidence(top.getScore());
            System.out.println(">>> [AI-LISTENER] Best team=" + top.getTeam()
                    + " confidence=" + confidence);
            detail.setAiConfidence((double) confidence);

            ticketDetailRepository.save(detail);

            // 3) Assign only if confidence >= 80, else keep unassigned
            if (confidence >= 80) {
                Team team = teamRepository.findByNameIgnoreCase(top.getTeam()).orElse(null);
                if (team != null) {
                    ticket.setAssignedTeam(team);
                    System.out.println(">>> [AI-LISTENER] Ticket auto-assigned to team=" + team.getName());
                } else {
                    ticket.setAssignedTeam(null); // team name not found in DB
                }
            } else {
                ticket.setAssignedTeam(null); // below threshold => unassigned
                System.out.println(">>> [AI-LISTENER] Confidence < 80, ticket left unassigned");
            }

            ticketRepository.save(ticket);


        } else {
            detail.setAiSuggestedTeam(null);
            detail.setAiConfidence(null);
            ticketDetailRepository.save(detail);
            System.out.println(">>> [AI-LISTENER] No AI suggestion found");
        }

    }

    private Double toConfidence(Double score) {
        if (score == null) return 0.0;

        // sigmoid mapping: 0..1
        double conf = 1.0 / (1.0 + Math.exp(-score));

        // keep precision (0.00 – 100.00)
        return conf * 100.0;
    }
}
