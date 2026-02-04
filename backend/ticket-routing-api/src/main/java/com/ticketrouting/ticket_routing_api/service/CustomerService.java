package com.ticketrouting.ticket_routing_api.service;

import com.ticketrouting.ticket_routing_api.model.Customer;
import com.ticketrouting.ticket_routing_api.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repo;

    public CustomerService(CustomerRepository repo) {
        this.repo = repo;
    }

    public Customer create(Customer c) {
        return repo.save(c);
    }

    public List<Customer> findAll() {
        return repo.findAll();
    }

    public Customer findById(Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Customer not found: " + id));
    }

    public Customer update(Long id, Customer input) {
        Customer existing = findById(id);
        existing.setFullName(input.getFullName());
        existing.setEmail(input.getEmail());
        existing.setActive(input.getActive());
        return repo.save(existing);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
