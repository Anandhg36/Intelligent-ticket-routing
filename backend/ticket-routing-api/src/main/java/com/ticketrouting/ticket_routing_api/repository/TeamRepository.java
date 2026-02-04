package com.ticketrouting.ticket_routing_api.repository;

import com.ticketrouting.ticket_routing_api.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByNameIgnoreCase(String name);

}
