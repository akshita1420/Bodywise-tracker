package com.bodywise.web;

import com.bodywise.domain.FlashCard;
import com.bodywise.repo.FlashCardRepo;
import com.bodywise.service.CardService;
import com.bodywise.service.PlanService;
import com.bodywise.web.Dtos.CardDto;
import com.bodywise.web.Dtos.CardRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Page 2: whole-day flash cards. Each card has text for every meal slot; calories are worked out from it. */
@RestController
@RequestMapping("/api/cards")
public class FlashCardController {

    private final FlashCardRepo cards;
    private final CardService cardService;
    private final PlanService planService;

    public FlashCardController(FlashCardRepo cards, CardService cardService, PlanService planService) {
        this.cards = cards;
        this.cardService = cardService;
        this.planService = planService;
    }

    @GetMapping
    public List<CardDto> list(@AuthenticationPrincipal Jwt jwt) {
        return cards.findByUserIdOrderByTitleAsc(Support.uid(jwt)).stream().map(CardDto::of).toList();
    }

    @PostMapping
    public CardDto create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CardRequest r) {
        FlashCard c = new FlashCard();
        c.userId = Support.uid(jwt);
        apply(c, r);
        return CardDto.of(cards.save(c));
    }

    @PutMapping("/{id}")
    public CardDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody CardRequest r) {
        FlashCard c = own(jwt, id);
        apply(c, r);
        return CardDto.of(cards.save(c));
    }

    @PostMapping("/{id}/duplicate")
    public CardDto duplicate(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        FlashCard src = own(jwt, id);
        FlashCard c = new FlashCard();
        c.userId = src.userId;
        c.title = src.title + " (copy)";
        c.notes = src.notes;
        cardService.setMeals(c, src.mealText);
        return CardDto.of(cards.save(c));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        FlashCard c = own(jwt, id);
        planService.forgetCard(c.id);
        cards.delete(c);
    }

    private void apply(FlashCard c, CardRequest r) {
        c.title = r.title().trim();
        c.notes = r.notes();
        cardService.setMeals(c, r.meals());
    }

    private FlashCard own(Jwt jwt, Long id) {
        return cards.findById(id).filter(c -> c.userId.equals(Support.uid(jwt)))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found"));
    }
}
