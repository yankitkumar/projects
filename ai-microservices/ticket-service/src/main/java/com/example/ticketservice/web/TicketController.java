package com.example.ticketservice.web;

import com.example.ticketservice.api.CreateTicketRequest;
import com.example.ticketservice.api.TicketStats;
import com.example.ticketservice.model.Ticket;
import com.example.ticketservice.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public List<Ticket> list(@RequestParam(name = "status", required = false) String status,
                             @RequestParam(name = "priority", required = false) String priority,
                             @RequestParam(name = "category", required = false) String category) {
        return ticketService.findAll(status, priority, category);
    }

    // Literal paths take precedence over /{id} in Spring MVC, so "stats" is never parsed as an id.
    @GetMapping("/stats")
    public TicketStats stats() {
        return ticketService.stats();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Ticket> getById(@PathVariable("id") Long id) {
        return ResponseEntity.of(ticketService.findById(id));
    }

    @PostMapping
    public ResponseEntity<Ticket> create(@Valid @RequestBody CreateTicketRequest request) {
        Ticket created = ticketService.create(request);
        return ResponseEntity.created(URI.create("/api/tickets/" + created.getId())).body(created);
    }

    @PostMapping("/{id}/analyze")
    public ResponseEntity<Ticket> reanalyze(@PathVariable("id") Long id) {
        return ResponseEntity.of(ticketService.reanalyze(id));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Ticket> resolve(@PathVariable("id") Long id) {
        return ResponseEntity.of(ticketService.resolve(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        if (ticketService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
