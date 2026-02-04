package com.ticketrouting.ticket_routing_api.service;

import com.ticketrouting.ticket_routing_api.model.Team;
import com.ticketrouting.ticket_routing_api.repository.TeamRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    private final TeamRepository repo;

    public TeamService(TeamRepository repo) {
        this.repo = repo;
    }

    public Team create(Team t) {
        return repo.save(t);
    }

    public List<Team> findAll() {
        return repo.findAll();
    }

    public Team findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found: " + id));
    }

    public Team update(Long id, Team input) {
        Team existing = findById(id);
        existing.setName(input.getName());
        existing.setDescription(input.getDescription());
        existing.setActive(input.getActive());
        return repo.save(existing);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new RuntimeException("Team not found: " + id);
        repo.deleteById(id);
    }
}
