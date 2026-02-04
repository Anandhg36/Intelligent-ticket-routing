package com.ticketrouting.ticket_routing_api.controller;

import com.ticketrouting.ticket_routing_api.model.TicketDetail;
import com.ticketrouting.ticket_routing_api.service.TicketDetailService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets/{ticketId}/detail")
@CrossOrigin(origins = "http://localhost:4200")
public class TicketDetailController {

    private final TicketDetailService service;

    public TicketDetailController(TicketDetailService service) {
        this.service = service;
    }

    @GetMapping
    public TicketDetail get(@PathVariable Long ticketId) {
        return service.getByTicketId(ticketId);
    }

    @PutMapping
    public TicketDetail upsert(@PathVariable Long ticketId, @RequestBody TicketDetail detail) {
        return service.upsert(ticketId, detail);
    }

    @DeleteMapping
    public void delete(@PathVariable Long ticketId) {
        service.deleteByTicketId(ticketId);
    }
}
