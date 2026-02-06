package com.ticketrouting.ticket_routing_api.repository;

import com.ticketrouting.ticket_routing_api.model.Ticket;
import com.ticketrouting.ticket_routing_api.model.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByAssignedTeam_Id(Long teamId);

    List<Ticket> findByCategory(String category);

    Optional<Ticket> findByTicketNumber(String ticketNumber);
}
