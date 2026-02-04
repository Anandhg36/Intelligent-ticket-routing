package com.ticketrouting.ticket_routing_api.controller;

import com.ticketrouting.ticket_routing_api.model.TicketActivity;
import com.ticketrouting.ticket_routing_api.service.TicketActivityService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/activities")
@CrossOrigin(origins = "http://localhost:4200")
public class TicketActivityController {

    private final TicketActivityService service;

    public TicketActivityController(TicketActivityService service) {
        this.service = service;
    }

    @GetMapping
    public List<TicketActivity> list(@PathVariable Long ticketId) {
        return service.listByTicket(ticketId);
    }

    @PostMapping
    public TicketActivity add(@PathVariable Long ticketId, @RequestBody TicketActivity activity) {
        return service.addActivity(ticketId, activity);
    }

    // optional delete by activity id
    @DeleteMapping("/{activityId}")
    public void delete(@PathVariable Long activityId) {
        service.delete(activityId);
    }
}
