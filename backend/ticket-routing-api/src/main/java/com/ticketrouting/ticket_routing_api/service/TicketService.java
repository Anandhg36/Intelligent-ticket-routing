package com.ticketrouting.ticket_routing_api.service;

import com.ticketrouting.ticket_routing_api.dto.AiTeamConfidence;
import com.ticketrouting.ticket_routing_api.dto.CreateTicketRequest;
import com.ticketrouting.ticket_routing_api.dto.TicketDetailResponse;
import com.ticketrouting.ticket_routing_api.dto.TicketResponse;
import com.ticketrouting.ticket_routing_api.event.TicketCreatedEvent;
import com.ticketrouting.ticket_routing_api.model.*;
import com.ticketrouting.ticket_routing_api.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private final TicketRepository ticketRepo;
    private final CustomerRepository customerRepo;
    private final TeamRepository teamRepo;
    private final TicketDetailRepository ticketDetailRepository;
    private final TicketAiTeamConfidenceRepository ticketAiTeamConfidenceRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TicketService(TicketRepository ticketRepo,
                         CustomerRepository customerRepo,
                         TeamRepository teamRepo,
                         TicketDetailRepository ticketDetailRepository, TicketAiTeamConfidenceRepository ticketAiTeamConfidenceRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.ticketRepo = ticketRepo;
        this.customerRepo = customerRepo;
        this.teamRepo = teamRepo;
        this.ticketDetailRepository = ticketDetailRepository;
        this.ticketAiTeamConfidenceRepository = ticketAiTeamConfidenceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Ticket create(CreateTicketRequest req) {
        Customer customer = customerRepo.findByEmailIgnoreCase(req.getRequesterEmail())
                .orElseGet(() -> {
                    Customer c = new Customer();
                    c.setEmail(req.getRequesterEmail());
                    c.setFullName(req.getRequesterName() != null ? req.getRequesterName() : "Unknown");
                    c.setActive(true);
                    c.setCreatedAt(java.time.LocalDateTime.now());
                    return customerRepo.save(c);
                });

        Ticket t = new Ticket();
        t.setSubject(req.getSubject());
        System.out.println(">>> [TicketService] Creating ticket for subject: " + t.getSubject());

        t.setPriority(TicketPriority.LOW);
        t.setAssignedTeam(null);
        t.setTicketNumber(generateTicketNumber());
        t.setRequester(customer);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        t.setCreatedAt(now);
        t.setUpdatedAt(now);

        Ticket saved = ticketRepo.save(t);

        TicketDetail detail = new TicketDetail();
        detail.setTicket(saved);
        detail.setCreatedAt(now);
        ticketDetailRepository.save(detail);

        eventPublisher.publishEvent(new TicketCreatedEvent(saved.getId(), saved.getSubject()));

        return saved;
    }

    private String generateTicketNumber() {
        return "TCK-" + System.currentTimeMillis();
    }

    public List<Ticket> list(String status, Long teamId, String teamName, String priority, String search) {
        System.out.println("ANAA"+teamName);
        List<Ticket> tickets = ticketRepo.findAll();

        if (status != null && !status.isBlank()) {
            TicketStatus st = TicketStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            tickets = tickets.stream().filter(t -> t.getStatus() == st).collect(Collectors.toList());
        }

        if (teamId != null) {
            tickets = tickets.stream()
                    .filter(t -> t.getAssignedTeam() != null && teamId.equals(t.getAssignedTeam().getId()))
                    .collect(Collectors.toList());
        }

        if (teamName != null && !teamName.isBlank() && !"All".equalsIgnoreCase(teamName)) {
            String tn = teamName.trim().toLowerCase(Locale.ROOT);
            tickets = tickets.stream()
                    .filter(t -> t.getAssignedTeam() != null
                            && t.getAssignedTeam().getName() != null
                            && t.getAssignedTeam().getName().trim().toLowerCase(Locale.ROOT).equals(tn))
                    .collect(Collectors.toList());
        }

        if (priority != null && !priority.isBlank()) {
            TicketPriority pr = TicketPriority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
            tickets = tickets.stream().filter(t -> t.getPriority() == pr).collect(Collectors.toList());
        }

        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase(Locale.ROOT);
            tickets = tickets.stream().filter(t ->
                    (t.getTicketNumber() != null && t.getTicketNumber().toLowerCase().contains(term)) ||
                            (t.getSubject() != null && t.getSubject().toLowerCase().contains(term)) ||
                            (t.getRequester() != null && t.getRequester().getFullName() != null &&
                                    t.getRequester().getFullName().toLowerCase().contains(term)) ||
                            (t.getRequester() != null && t.getRequester().getEmail() != null &&
                                    t.getRequester().getEmail().toLowerCase().contains(term))
            ).collect(Collectors.toList());
        }

        return tickets;
    }

    public Ticket get(Long id) {
        return ticketRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + id));
    }

    public Ticket update(Long id, Ticket input) {
        Ticket existing = get(id);

        existing.setSubject(input.getSubject());
        existing.setStatus(input.getStatus());
        existing.setPriority(input.getPriority());
        existing.setCategory(input.getCategory());
        existing.setArchived(input.getArchived());

        if (input.getRequester() != null && input.getRequester().getId() != null) {
            Customer requester = customerRepo.findById(input.getRequester().getId())
                    .orElseThrow(() -> new RuntimeException("Customer not found: " + input.getRequester().getId()));
            existing.setRequester(requester);
        }

        if (input.getAssignedTeam() != null && input.getAssignedTeam().getId() != null) {
            existing.setAssignedTeam(
                    teamRepo.findById(input.getAssignedTeam().getId())
                            .orElseThrow(() -> new RuntimeException("Team not found: " + input.getAssignedTeam().getId()))
            );
        } else {
            existing.setAssignedTeam(null);
        }

        return ticketRepo.save(existing);
    }

    public void delete(Long id) {
        if (!ticketRepo.existsById(id)) throw new RuntimeException("Ticket not found: " + id);
        ticketRepo.deleteById(id);
    }

    public List<TicketResponse> listAsDto(String status, Long teamId, String teamName, String priority, String search) {
        return list(status, teamId, teamName, priority, search).stream()
                .map(t -> {
                    TicketResponse r = TicketResponse.from(t);

                    // ✅ Load detail row (1:1) and attach
                    TicketDetail detail = ticketDetailRepository.findByTicketId(t.getId()).orElse(null);
                    if (detail != null) {
                        r.setTicketDetail(TicketDetailResponse.from(detail));
                    }

                    // 3️⃣ AI team confidences (TOP 3)
                    List<AiTeamConfidence> aiTeams =
                            ticketAiTeamConfidenceRepository
                                    .findByTicketIdOrderByRankOrderAsc(t.getId())
                                    .stream()
                                    .map(AiTeamConfidence::from)
                                    .toList();

                    if (!aiTeams.isEmpty()) {
                        r.setTeams(aiTeams);
                    }

                    return r;
                })
                .toList();
    }
}