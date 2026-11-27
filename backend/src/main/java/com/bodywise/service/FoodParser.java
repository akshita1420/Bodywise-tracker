package com.bodywise.service;

import com.bodywise.domain.Food;
import com.bodywise.repo.FoodRepo;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns free text like "5 soaked almonds, 2 jowar roti, 100 g low fat curd" into foods and quantities.
 * Matching uses each food's name and aliases; unknown items come back unmatched so the user can define them.
 */
@Service
public class FoodParser {

    /** One parsed line. quantity is in the food's unit; food is null when not matched. */
    /** approximate = matched on some words only (e.g. "kuttu roti" → "Whole wheat roti"); worth a look. */
    public record Parsed(String raw, String query, Food food, double quantity, String note, boolean approximate) {
        public boolean matched() { return food != null; }
    }

    private static final Pattern SPLIT = Pattern.compile("\\s*(?:,|;|\\n|\\+|&|\\band\\b)\\s*", Pattern.CASE_INSENSITIVE);
    private static final String NUM = "(\\d+(?:\\.\\d+)?|\\d+/\\d+|½|¼|¾|half|one|two|three|four|five|six|seven|eight|nine|ten)";
    private static final String UNIT = "(glass(?:es)?|cups?|bowls?|katori(?:s)?|plates?|pieces?|pcs?|nos?|tbsps?|tablespoons?|tsps?|teaspoons?|grams?|gms?|gm|g|ml|slices?|scoops?|servings?|handfuls?)";
    private static final Pattern LEAD = Pattern.compile("^" + NUM + "\\s*(?:x\\s*)?(?:" + UNIT + "\\b\\.?)?\\s*(?:of\\s+)?(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAIL = Pattern.compile("^(.*?)\\s*(?:x\\s*|\\(\\s*)" + NUM + "\\s*\\)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern GRAMS_IN_UNIT = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(?:g|ml)\\b", Pattern.CASE_INSENSITIVE);

    private static final Set<String> STOP = Set.of("of", "a", "an", "the", "ki", "ka", "ke", "some", "fresh", "homemade",
            "home", "made", "plain", "bowl", "glass", "cup", "piece", "katori", "serving", "small", "medium", "large");
    private static final Map<String, String> SPELLING = Map.ofEntries(
            Map.entry("sabji", "sabzi"), Map.entry("subzi", "sabzi"), Map.entry("subji", "sabzi"), Map.entry("sabzee", "sabzi"),
            Map.entry("chapatti", "chapati"), Map.entry("roties", "roti"), Map.entry("dhania", "dhaniya"),
            Map.entry("dahi", "curd"), Map.entry("yoghurt", "yogurt"), Map.entry("panner", "paneer"),
            Map.entry("lowfat", "low fat"), Map.entry("chai", "tea"), Map.entry("chaas", "buttermilk"),
            Map.entry("veg", "vegetable"), Map.entry("veggie", "vegetable"), Map.entry("veggies", "vegetable"),
            Map.entry("vegetables", "vegetable"), Map.entry("subzee", "sabzi"), Map.entry("aaloo", "aloo"),
            Map.entry("alu", "aloo"), Map.entry("panir", "paneer"), Map.entry("daal", "dal"), Map.entry("dhal", "dal"),
            Map.entry("chawal", "rice"), Map.entry("curd", "curd"), Map.entry("yogurt", "curd"), Map.entry("sprout", "sprouts"),
            Map.entry("parantha", "paratha"), Map.entry("paratha", "paratha"), Map.entry("idly", "idli"));

    private final FoodRepo foods;

    public FoodParser(FoodRepo foods) {
        this.foods = foods;
    }

    public List<Parsed> parse(Long userId, String text) {
        if (text == null || text.isBlank()) return List.of();
        List<Food> catalog = foods.visibleTo(userId);
        List<Parsed> out = new ArrayList<>();
        for (String part : SPLIT.split(text)) {
            String raw = part.trim().replaceAll("^[-•*\\s]+", "");
            if (raw.isEmpty()) continue;
            out.add(parseOne(raw, catalog, userId));
        }
        return out;
    }

    private Parsed parseOne(String raw, List<Food> catalog, Long userId) {
        double number = 1;
        String unitWord = null;
        String name = raw;

        Matcher lead = LEAD.matcher(raw);
        if (lead.matches() && !lead.group(3).isBlank()) {
            number = toNumber(lead.group(1));
            unitWord = lead.group(2);
            name = lead.group(3);
        } else {
            Matcher trail = TRAIL.matcher(raw);
            if (trail.matches() && !trail.group(1).isBlank()) {
                number = toNumber(trail.group(2));
                name = trail.group(1);
            }
        }
        name = name.replaceAll("\\(.*?\\)", " ").trim();

        Food best = null;
        double bestScore = 0;
        String q = normalize(name);
        for (Food f : catalog) {
            double s = score(q, f);
            // Prefer the user's own food when scores tie.
            if (s > bestScore || (s == bestScore && s > 0 && f.userId != null && userId.equals(f.userId))) {
                bestScore = s;
                best = f;
            }
        }
        if (best == null || bestScore < 0.5) return new Parsed(raw, cleanName(name), null, number, null, false);

        double qty = number;
        String note = null;
        if (unitWord != null && isWeight(unitWord)) {
            Matcher g = GRAMS_IN_UNIT.matcher(best.unit);
            if (g.find()) {
                qty = number / Double.parseDouble(g.group(1));
            } else {
                qty = 1;
                note = "Couldn't convert " + fmt(number) + " " + unitWord + " to " + best.unit + "; used 1";
            }
        }
        qty = Math.round(qty * 100) / 100.0;
        if (qty <= 0) qty = 1;
        return new Parsed(raw, cleanName(name), best, qty, note, bestScore < 0.95);
    }

    private static double score(String q, Food f) {
        double best = nameScore(q, normalize(f.name));
        if (f.aliases != null) {
            for (String a : f.aliases.split(",")) {
                if (!a.isBlank()) best = Math.max(best, nameScore(q, normalize(a)));
            }
        }
        return best;
    }

    /**
     * Words that describe how food is cooked or served rather than what it is. Sharing only these
     * is not a match ("roasted vegetables" must not become "vegetable upma").
     */
    private static final Set<String> GENERIC = Set.of("vegetable", "mixed", "mix", "roasted", "steamed", "boiled",
            "fried", "grilled", "sauteed", "baked", "masala", "curry", "sabzi", "fry", "dry", "gravy", "spicy", "sweet",
            "salted", "light", "low", "fat", "soaked", "cooked", "raw", "hot", "cold", "warm", "water", "green", "red",
            "white", "brown", "whole", "tadka", "stuffed", "chaat", "salad", "bowl");

    /** 1.0 exact; otherwise token overlap, rewarding when all of the food's words appear in the text. */
    private static double nameScore(String q, String n) {
        if (q.isEmpty() || n.isEmpty()) return 0;
        if (q.equals(n)) return 1.0;
        Set<String> qt = new LinkedHashSet<>(List.of(q.split(" ")));
        Set<String> nt = new LinkedHashSet<>(List.of(n.split(" ")));
        Set<String> common = new HashSet<>(qt);
        common.retainAll(nt);
        if (common.isEmpty()) return 0;
        if (common.stream().allMatch(GENERIC::contains)) return 0;
        double coverFood = (double) common.size() / nt.size();
        double coverQuery = (double) common.size() / qt.size();
        double s = 0.55 * coverFood + 0.45 * coverQuery;
        if (coverFood == 1.0 && coverQuery == 1.0) s = 0.98; // same words, different order
        return s;
    }

    static String normalize(String s) {
        String x = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z\\s]", " ").replaceAll("\\s+", " ").trim();
        StringBuilder sb = new StringBuilder();
        for (String t : x.split(" ")) {
            if (t.isEmpty()) continue;
            t = SPELLING.getOrDefault(t, t);
            for (String part : t.split(" ")) {
                if (STOP.contains(part)) continue;
                sb.append(singular(part)).append(' ');
            }
        }
        return sb.toString().trim();
    }

    private static String singular(String t) {
        if (t.length() > 4 && t.endsWith("ies")) return t.substring(0, t.length() - 3) + "y";
        if (t.length() > 4 && t.endsWith("oes")) return t.substring(0, t.length() - 2);
        if (t.length() > 3 && t.endsWith("s") && !t.endsWith("ss")) return t.substring(0, t.length() - 1);
        return t;
    }

    private static boolean isWeight(String unit) {
        String u = unit.toLowerCase(Locale.ROOT);
        return u.equals("g") || u.startsWith("gm") || u.startsWith("gram") || u.equals("ml");
    }

    private static double toNumber(String s) {
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "½", "half" -> 0.5;
            case "¼" -> 0.25;
            case "¾" -> 0.75;
            case "one" -> 1; case "two" -> 2; case "three" -> 3; case "four" -> 4; case "five" -> 5;
            case "six" -> 6; case "seven" -> 7; case "eight" -> 8; case "nine" -> 9; case "ten" -> 10;
            default -> {
                if (s.contains("/")) {
                    String[] p = s.split("/");
                    double d = Double.parseDouble(p[1]);
                    yield d == 0 ? 1 : Double.parseDouble(p[0]) / d;
                }
                yield Double.parseDouble(s);
            }
        };
    }

    private static String cleanName(String name) {
        String n = name.replaceAll("\\s+", " ").trim();
        return n.isEmpty() ? n : Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    private static String fmt(double d) {
        return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
    }
}
