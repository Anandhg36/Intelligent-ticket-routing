package com.ticketrouting.ticket_routing_api.repository;

import com.ticketrouting.ticket_routing_api.model.TicketAiTeamConfidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAiTeamConfidenceRepository
        extends JpaRepository<TicketAiTeamConfidence, Long> {

    List<TicketAiTeamConfidence> findByTicketIdOrderByRankOrderAsc(Long ticketId);
}

