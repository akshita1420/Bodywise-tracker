package com.bodywise.service;

import com.bodywise.domain.*;
import com.bodywise.repo.*;
import com.bodywise.web.Dtos.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Builds the grocery page: items from the next day's flash-card plan plus the user's own lists. */
@Service
public class GroceryService {

    private final PlanService planService;
    private final GroceryListRepo lists;
    private final GroceryItemRepo items;
    private final PlanGroceryCheckRepo checks;
    private final UserRepo users;
    private final FoodRepo foods;
    private final String mailHost;

    public GroceryService(PlanService planService, GroceryListRepo lists, GroceryItemRepo items,
                          PlanGroceryCheckRepo checks, UserRepo users, FoodRepo foods, @Value("${spring.mail.host:}") String mailHost) {
        this.planService = planService;
        this.lists = lists;
        this.items = items;
        this.checks = checks;
        this.users = users;
        this.foods = foods;
        this.mailHost = mailHost;
    }

    public boolean mailConfigured() {
        return mailHost != null && !mailHost.isBlank();
    }

    public static String key(String name, String unit) {
        return (name == null ? "" : name.trim().toLowerCase()) + "|" + (unit == null ? "" : unit.trim().toLowerCase());
    }

    public static String fmtQty(double q, String unit) {
        String n = q == Math.rint(q) ? String.valueOf((long) q) : String.valueOf(Math.round(q * 100) / 100.0);
        return unit == null || unit.isBlank() ? n : n + " × " + unit;
    }

    @Transactional
    public GroceryView view(Long userId, LocalDate forDate) {
        FlashCard card = planService.ensurePlan(userId, forDate);
        Set<String> bought = checks.findByUserIdAndDate(userId, forDate).stream()
                .map(c -> c.itemKey).collect(Collectors.toSet());

        // Walk the card meal by meal, dish by dish. Each ingredient is listed under the first dish that
        // needs it; later dishes only mention it as "already listed".
        List<GroceryDish> dishes = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (card != null) {
            Map<Long, Food> foodById = new HashMap<>();
            foods.findAllById(card.items.stream().map(i -> i.foodId).filter(Objects::nonNull).toList())
                    .forEach(f -> foodById.put(f.id, f));
            for (MealType meal : MealType.values()) {
                for (CardItem it : card.items) {
                    if (it.mealType != meal) continue;
                    List<GroceryIngredient> fresh = new ArrayList<>();
                    List<String> repeated = new ArrayList<>();
                    for (String ing : ingredientsOf(it, foodById.get(it.foodId))) {
                        String k = ing.trim().toLowerCase();
                        if (seen.add(k)) fresh.add(new GroceryIngredient(k, ing.trim(), bought.contains(k)));
                        else repeated.add(ing.trim());
                    }
                    dishes.add(new GroceryDish(meal, it.name, it.matched ? fmtQty(it.quantity, it.unit) : null, fresh, repeated));
                }
            }
        }

        Map<Long, List<GroceryItem>> byList = items.findByUserIdOrderById(userId).stream()
                .collect(Collectors.groupingBy(i -> i.listId));
        List<ListDto> listDtos = lists.findByUserIdOrderById(userId).stream()
                .map(l -> new ListDto(l.id, l.name, byList.getOrDefault(l.id, List.of()).stream()
                        .map(i -> new GItemDto(i.id, i.name, i.quantity, i.purchased)).toList()))
                .toList();

        List<ToBuy> toBuy = new ArrayList<>();
        for (GroceryDish d : dishes) {
            for (GroceryIngredient i : d.ingredients()) {
                if (!i.purchased()) toBuy.add(new ToBuy(d.dish(), i.name(), null));
            }
        }
        for (ListDto l : listDtos) {
            for (GItemDto i : l.items()) {
                if (!i.purchased()) toBuy.add(new ToBuy(l.name(), i.name(), i.quantity()));
            }
        }

        User u = users.findById(userId).orElseThrow();
        String email = u.reminderEmail != null && !u.reminderEmail.isBlank() ? u.reminderEmail : u.email;
        return new GroceryView(forDate, card != null ? card.title : null, dishes, listDtos, toBuy, mailConfigured(), email);
    }

    /** Recipe ingredients of a dish; the food itself when it has none listed; nothing for "" (e.g. water). */
    static List<String> ingredientsOf(CardItem it, Food f) {
        String list = f != null ? f.ingredients : null;
        if (list == null) return List.of(it.name);
        if (list.isBlank()) return List.of();
        return Arrays.stream(list.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Transactional
    public void setPlanCheck(Long userId, LocalDate date, String key, boolean purchased) {
        Optional<PlanGroceryCheck> existing = checks.findByUserIdAndDateAndItemKey(userId, date, key);
        if (purchased && existing.isEmpty()) {
            PlanGroceryCheck c = new PlanGroceryCheck();
            c.userId = userId;
            c.date = date;
            c.itemKey = key;
            checks.save(c);
        } else if (!purchased) {
            existing.ifPresent(checks::delete);
        }
    }
}
