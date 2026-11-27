package com.bodywise.web;

import com.bodywise.domain.GroceryItem;
import com.bodywise.domain.GroceryList;
import com.bodywise.repo.GroceryItemRepo;
import com.bodywise.repo.GroceryListRepo;
import com.bodywise.service.GroceryService;
import com.bodywise.service.ReminderService;
import com.bodywise.web.Dtos.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/** Page 4: the grocery list from tomorrow's plan, the user's own lists, and what is still to buy. */
@RestController
@RequestMapping("/api/grocery")
public class GroceryController {

    private final GroceryService grocery;
    private final ReminderService reminders;
    private final GroceryListRepo lists;
    private final GroceryItemRepo items;
    private final Support support;

    public GroceryController(GroceryService grocery, ReminderService reminders, GroceryListRepo lists,
                             GroceryItemRepo items, Support support) {
        this.grocery = grocery;
        this.reminders = reminders;
        this.lists = lists;
        this.items = items;
        this.support = support;
    }

    /** Defaults to tomorrow. */
    @GetMapping
    public GroceryView view(@AuthenticationPrincipal Jwt jwt,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return grocery.view(Support.uid(jwt), date != null ? date : support.today().plusDays(1));
    }

    @PostMapping("/plan-check")
    public void planCheck(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PlanCheck r) {
        grocery.setPlanCheck(Support.uid(jwt), r.date(), r.key(), r.purchased());
    }

    @PostMapping("/lists")
    public ListDto createList(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NameRequest r) {
        GroceryList l = new GroceryList();
        l.userId = Support.uid(jwt);
        l.name = r.name().trim();
        lists.save(l);
        return new ListDto(l.id, l.name, java.util.List.of());
    }

    @PutMapping("/lists/{id}")
    public void renameList(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody NameRequest r) {
        GroceryList l = ownList(jwt, id);
        l.name = r.name().trim();
        lists.save(l);
    }

    @DeleteMapping("/lists/{id}")
    @Transactional
    public void deleteList(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        GroceryList l = ownList(jwt, id);
        items.deleteAll(items.findByListId(l.id));
        lists.delete(l);
    }

    @PostMapping("/lists/{id}/items")
    public GItemDto addItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody GItemRequest r) {
        GroceryList l = ownList(jwt, id);
        GroceryItem i = new GroceryItem();
        i.userId = l.userId;
        i.listId = l.id;
        i.name = r.name().trim();
        i.quantity = r.quantity();
        items.save(i);
        return new GItemDto(i.id, i.name, i.quantity, i.purchased);
    }

    @PatchMapping("/items/{id}")
    public GItemDto updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @RequestBody GItemUpdate r) {
        GroceryItem i = ownItem(jwt, id);
        if (r.name() != null && !r.name().isBlank()) i.name = r.name().trim();
        if (r.quantity() != null) i.quantity = r.quantity();
        if (r.purchased() != null) i.purchased = r.purchased();
        items.save(i);
        return new GItemDto(i.id, i.name, i.quantity, i.purchased);
    }

    @DeleteMapping("/items/{id}")
    public void deleteItem(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        items.delete(ownItem(jwt, id));
    }

    /** "Send me the list now": the same email the 7 pm job sends. */
    @PostMapping("/send-now")
    public SendResult sendNow(@AuthenticationPrincipal Jwt jwt) {
        try {
            String msg = reminders.sendFor(support.user(jwt));
            return new SendResult(msg.startsWith("Sent"), msg);
        } catch (Exception e) {
            return new SendResult(false, "Sending failed: " + e.getMessage());
        }
    }

    private GroceryList ownList(Jwt jwt, Long id) {
        return lists.findById(id).filter(l -> l.userId.equals(Support.uid(jwt)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "List not found"));
    }

    private GroceryItem ownItem(Jwt jwt, Long id) {
        return items.findById(id).filter(i -> i.userId.equals(Support.uid(jwt)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
    }
}
