package com.ticketrouting.ticket_routing_api.controller;

import com.ticketrouting.ticket_routing_api.model.Team;
import com.ticketrouting.ticket_routing_api.service.TeamService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "http://localhost:4200")
public class TeamController {

    private final TeamService service;

    public TeamController(TeamService service) {
        this.service = service;
    }

    @PostMapping
    public Team create(@RequestBody Team t) { return service.create(t); }

    @GetMapping
    public List<Team> findAll() { return service.findAll(); }

    @GetMapping("/{id}")
    public Team findById(@PathVariable Long id) { return service.findById(id); }

    @PutMapping("/{id}")
    public Team update(@PathVariable Long id, @RequestBody Team t) { return service.update(id, t); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
