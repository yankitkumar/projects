package com.example.demoapi.controller;

import com.example.demoapi.model.Item;
import com.example.demoapi.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public List<Item> getAll() {
        return itemService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getById(@PathVariable Long id) {
        return itemService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Item> create(@Valid @RequestBody Item item) {
        Item created = itemService.create(item);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> update(@PathVariable Long id, @Valid @RequestBody Item item) {
        return itemService.update(id, item)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/done")
    public ResponseEntity<Item> markDone(@PathVariable Long id) {
        return itemService.findById(id)
                .map(item -> {
                    item.setDone(true);
                    itemService.update(id, item);
                    return ResponseEntity.ok(item);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (itemService.delete(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
