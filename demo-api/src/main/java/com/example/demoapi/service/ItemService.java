package com.example.demoapi.service;

import com.example.demoapi.model.Item;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ItemService {

    private final Map<Long, Item> items = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(0);

    public ItemService() {
        Item seed = new Item(idSequence.incrementAndGet(), "Sample item", "Created at startup", false);
        items.put(seed.getId(), seed);
    }

    public List<Item> findAll() {
        return List.copyOf(items.values());
    }

    public Optional<Item> findById(Long id) {
        return Optional.ofNullable(items.get(id));
    }

    public Item create(Item item) {
        item.setId(idSequence.incrementAndGet());
        items.put(item.getId(), item);
        return item;
    }

    public Optional<Item> update(Long id, Item update) {
        if (!items.containsKey(id)) {
            return Optional.empty();
        }
        update.setId(id);
        items.put(id, update);
        return Optional.of(update);
    }

    // Creates the item at id if absent, otherwise replaces it; returns true if it was newly created.
    public boolean upsert(Long id, Item item) {
        boolean isNew = !items.containsKey(id);
        item.setId(id);
        items.put(id, item);
        idSequence.updateAndGet(current -> Math.max(current, id));
        return isNew;
    }

    public boolean delete(Long id) {
        return items.remove(id) != null;
    }

    public Collection<Item> all() {
        return items.values();
    }
}
