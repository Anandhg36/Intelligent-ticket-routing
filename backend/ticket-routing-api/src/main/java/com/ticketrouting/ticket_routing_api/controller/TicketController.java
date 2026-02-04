package com.ticketrouting.ticket_routing_api.controller;

import com.ticketrouting.ticket_routing_api.dto.CreateTicketRequest;
import com.ticketrouting.ticket_routing_api.dto.TicketResponse;
import com.ticketrouting.ticket_routing_api.model.Ticket;
import com.ticketrouting.ticket_routing_api.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = "http://localhost:4200")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ticket create(@Valid @RequestBody CreateTicketRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<TicketResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String teamName,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search
    ) {
        System.out.println("Controller check"+teamName);
        return service.listAsDto(status, teamId, teamName,priority, search);
    }

    @GetMapping("/{id}")
    public Ticket get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/{id}")
    public Ticket update(@PathVariable Long id, @RequestBody Ticket t) { return service.update(id, t); }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(id); }
}
