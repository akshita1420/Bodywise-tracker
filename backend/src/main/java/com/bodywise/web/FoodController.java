package com.bodywise.web;

import com.bodywise.domain.Food;
import com.bodywise.repo.FoodRepo;
import com.bodywise.service.CardService;
import com.bodywise.service.FoodParser;
import com.bodywise.web.Dtos.FoodDto;
import com.bodywise.web.Dtos.FoodRequest;
import com.bodywise.web.Dtos.ParseRequest;
import com.bodywise.web.Dtos.ParsedItem;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FoodController {

    private final FoodRepo foods;
    private final FoodParser parser;
    private final CardService cardService;

    public FoodController(FoodRepo foods, FoodParser parser, CardService cardService) {
        this.foods = foods;
        this.parser = parser;
        this.cardService = cardService;
    }

    @GetMapping("/foods")
    public List<FoodDto> search(@AuthenticationPrincipal Jwt jwt, @RequestParam(defaultValue = "") String q) {
        return foods.search(Support.uid(jwt), q.trim()).stream().limit(50).map(FoodDto::of).toList();
    }

    /** Free text → foods with quantities and calories ("5 soaked almonds, 2 jowar roti"). */
    @PostMapping("/parse")
    public List<ParsedItem> parse(@AuthenticationPrincipal Jwt jwt, @RequestBody ParseRequest r) {
        return parser.parse(Support.uid(jwt), r.text()).stream().map(ParsedItem::of).toList();
    }

    /** Adds one of your own foods. Cards with unknown items are re-read so they pick it up. */
    @PostMapping("/foods")
    public FoodDto create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FoodRequest r) {
        Food f = new Food(r.name().trim(), r.unit().trim(), r.kcal(), r.protein(), r.carbs(), r.fat(), r.fiber());
        f.userId = Support.uid(jwt);
        f.aliases = r.aliases();
        f.ingredients = r.ingredients() == null || r.ingredients().isBlank() ? null : r.ingredients().trim();
        Food saved = foods.save(f);
        cardService.reparseAll(f.userId);
        return FoodDto.of(saved);
    }

    @DeleteMapping("/foods/{id}")
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        Food f = foods.findById(id).filter(x -> Support.uid(jwt).equals(x.userId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Only your own foods can be deleted"));
        foods.delete(f);
    }
}
