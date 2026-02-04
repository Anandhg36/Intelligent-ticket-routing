package com.ticketrouting.ticket_routing_api.repository;

import com.ticketrouting.ticket_routing_api.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

}
