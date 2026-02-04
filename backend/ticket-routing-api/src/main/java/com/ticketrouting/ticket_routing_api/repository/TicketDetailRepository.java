package com.ticketrouting.ticket_routing_api.repository;

import com.ticketrouting.ticket_routing_api.model.TicketDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketDetailRepository extends JpaRepository<TicketDetail, Long> {
    Optional<TicketDetail> findByTicketId(Long ticketId);
}
